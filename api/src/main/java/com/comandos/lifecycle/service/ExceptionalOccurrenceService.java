package com.comandos.lifecycle.service;

import com.comandos.audit.service.AuditService;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.AssetStatus;
import com.comandos.inventory.model.StockBalance;
import com.comandos.inventory.model.StockLocation;
import com.comandos.inventory.model.StockLot;
import com.comandos.lifecycle.dto.LifecycleContract.OccurrenceRequest;
import com.comandos.lifecycle.dto.LifecycleContract.OccurrenceView;
import com.comandos.lifecycle.dto.LifecycleContract.Page;
import com.comandos.lifecycle.dto.LifecycleContract.ResolveRequest;
import com.comandos.lifecycle.model.ExceptionOccurrence;
import com.comandos.security.service.AccessPolicy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ExceptionalOccurrenceService {
    private static final Set<String> TYPES = Set.of(
        "LOSS", "LOST", "THEFT", "ROBBERY", "SEIZURE", "RECOVERY", "RECOVERED",
        "DAMAGE", "ACCIDENT", "RECALL", "INVESTIGATION", "DIVERGENCE", "BLOCK"
    );
    private static final Set<String> MISSING_TYPES = Set.of("LOSS", "LOST", "THEFT", "ROBBERY");
    private static final Set<String> BLOCKING_TYPES = Set.of("SEIZURE", "DAMAGE", "ACCIDENT", "RECALL", "BLOCK");
    private static final Set<String> LEGAL_DOCUMENT_TYPES = Set.of("THEFT", "ROBBERY", "SEIZURE", "RECOVERY", "RECOVERED");
    private static final Set<String> RECOVERY_TYPES = Set.of("RECOVERY", "RECOVERED");

    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;

    public ExceptionalOccurrenceService(EntityManager em, AccessPolicy access, AuditService audit) {
        this.em = em;
        this.access = access;
        this.audit = audit;
    }

    @Transactional
    public OccurrenceView occur(OccurrenceRequest request) {
        validateRequest(request);
        access.requireScope("inventory/assets", "UPDATE", request.organizationId(), request.unitId());
        Organization organization = locked(Organization.class, request.organizationId());
        OrganizationalUnit unit = unit(organization.id, request.unitId());
        String type = canonical(request.type());
        if (!TYPES.contains(type)) bad("Unsupported exceptional occurrence type.");
        if (LEGAL_DOCUMENT_TYPES.contains(type) && blank(request.documentReference())) bad("This occurrence type requires a document reference.");

        ExceptionOccurrence occurrence = new ExceptionOccurrence();
        occurrence.organization = organization;
        occurrence.unit = unit;
        occurrence.type = type;
        occurrence.description = request.description().trim();
        occurrence.investigation = clean(request.investigation());
        occurrence.documentReference = clean(request.documentReference());
        occurrence.quantity = request.quantity();
        occurrence.occurredAt = LocalDateTime.now();
        var actor = audit.actor();
        occurrence.responsibleId = actor.id();
        occurrence.responsibleLogin = actor.login();

        if (request.assetId() != null) {
            AssetItem asset = locked(AssetItem.class, request.assetId());
            scope(asset.location, organization.id, request.unitId());
            if (AssetStatus.terminalCodes().contains(asset.status)) conflict("Terminal assets cannot receive new lifecycle occurrences.");
            occurrence.asset = asset;
            occurrence.previousItemStatus = asset.status;
            if (RECOVERY_TYPES.contains(type)) validateRecovery(request, occurrence, asset);
            applyAssetOpeningState(occurrence, asset);
            occurrence.resultingItemStatus = asset.status;
        } else {
            StockLot lot = locked(StockLot.class, request.lotId());
            StockBalance balance = request.balanceId() == null ? null : locked(StockBalance.class, request.balanceId());
            if (balance != null && !balance.lot.id.equals(lot.id)) bad("Balance does not belong to selected lot.");
            StockLocation location = balance == null ? lot.openingLocation : balance.location;
            scope(location, organization.id, request.unitId());
            if (AssetStatus.terminalCodes().contains(lot.status)) conflict("Terminal lots cannot receive new lifecycle occurrences.");
            occurrence.lot = lot;
            occurrence.balance = balance;
            occurrence.previousItemStatus = lot.status;
            applyLotOpeningState(occurrence, lot);
            occurrence.resultingItemStatus = lot.status;
        }

        em.persist(occurrence);
        em.flush();
        OccurrenceView view = view(occurrence);
        audit.record("exception-occurrences", occurrence.id, "CREATE", null, view);
        return view;
    }

    @Transactional
    public OccurrenceView investigate(long id, ResolveRequest request) {
        ExceptionOccurrence occurrence = locked(ExceptionOccurrence.class, id);
        require(occurrence, "UPDATE");
        if ("RESOLVED".equals(occurrence.status)) conflict("Resolved occurrence cannot return to investigation.");
        String investigation = clean(request == null ? null : request.investigation());
        if (blank(investigation)) bad("Investigation notes are required.");
        OccurrenceView before = view(occurrence);
        occurrence.investigation = investigation;
        if (request != null && !blank(request.documentReference())) occurrence.documentReference = request.documentReference().trim();
        occurrence.status = "UNDER_INVESTIGATION";
        occurrence.investigationStartedAt = occurrence.investigationStartedAt == null ? LocalDateTime.now() : occurrence.investigationStartedAt;
        var actor = audit.actor();
        occurrence.investigatedById = actor.id();
        occurrence.investigatedByLogin = actor.login();
        em.flush();
        OccurrenceView view = view(occurrence);
        audit.record("exception-occurrences", occurrence.id, "INVESTIGATE", before, view);
        return view;
    }

    @Transactional
    public OccurrenceView resolve(long id, ResolveRequest request) {
        ExceptionOccurrence occurrence = locked(ExceptionOccurrence.class, id);
        require(occurrence, "UPDATE");
        if ("RESOLVED".equals(occurrence.status)) return view(occurrence);
        String investigation = clean(request == null ? null : request.investigation());
        if (!blank(investigation)) occurrence.investigation = investigation;
        if (blank(occurrence.investigation)) bad("Investigation must be recorded before resolving an exceptional occurrence.");
        if (request != null && !blank(request.documentReference())) occurrence.documentReference = request.documentReference().trim();
        if (LEGAL_DOCUMENT_TYPES.contains(occurrence.type) && blank(occurrence.documentReference)) bad("This occurrence type requires a document reference before resolution.");

        OccurrenceView before = view(occurrence);
        if (occurrence.asset != null) {
            AssetItem asset = locked(AssetItem.class, occurrence.asset.id);
            if (AssetStatus.terminalCodes().contains(asset.status)) conflict("A terminal asset cannot be reactivated by occurrence resolution.");
            if (RECOVERY_TYPES.contains(occurrence.type)) {
                asset.status = AssetStatus.AVAILABLE.name();
                closeRelatedMissingOccurrence(occurrence);
            } else if (MISSING_TYPES.contains(occurrence.type)) {
                asset.status = AssetStatus.MISSING.name();
            } else if (BLOCKING_TYPES.contains(occurrence.type)) {
                asset.status = AssetStatus.BLOCKED.name();
            }
            occurrence.resultingItemStatus = asset.status;
        } else if (occurrence.lot != null) {
            StockLot lot = locked(StockLot.class, occurrence.lot.id);
            if (AssetStatus.terminalCodes().contains(lot.status)) conflict("A terminal lot cannot be reactivated by occurrence resolution.");
            if (MISSING_TYPES.contains(occurrence.type) || BLOCKING_TYPES.contains(occurrence.type)) lot.status = AssetStatus.BLOCKED.name();
            occurrence.resultingItemStatus = lot.status;
        }

        occurrence.status = "RESOLVED";
        occurrence.resolvedAt = LocalDateTime.now();
        var actor = audit.actor();
        occurrence.resolvedById = actor.id();
        occurrence.resolvedByLogin = actor.login();
        em.flush();
        OccurrenceView view = view(occurrence);
        audit.record("exception-occurrences", occurrence.id, "RESOLVE", before, view);
        return view;
    }

    public Page<OccurrenceView> list(long organizationId, Long unitId, int page) {
        if (page < 0) bad("Invalid page.");
        access.requireScope("inventory/assets", "READ", organizationId, unitId);
        unit(organizationId, unitId);
        String where = " where x.organization.id=:organization" + (unitId == null ? "" : " and x.unit.id=:unit");
        var query = em.createQuery("select x from ExceptionOccurrence x" + where + " order by x.id desc", ExceptionOccurrence.class);
        var count = em.createQuery("select count(x) from ExceptionOccurrence x" + where, Long.class);
        query.setParameter("organization", organizationId); count.setParameter("organization", organizationId);
        if (unitId != null) { query.setParameter("unit", unitId); count.setParameter("unit", unitId); }
        return new Page<>(query.setFirstResult(page * 20).setMaxResults(20).getResultList().stream().map(this::view).toList(), count.getSingleResult(), page, 20);
    }

    private void validateRequest(OccurrenceRequest request) {
        if (request == null || request.organizationId() == null || blank(request.type()) || blank(request.description())) bad("Organization, type and description are required.");
        if ((request.assetId() == null) == (request.lotId() == null)) bad("Select exactly one asset or lot.");
        if (request.lotId() != null && (request.quantity() == null || request.quantity().signum() <= 0)) bad("Lot occurrence quantity must be positive.");
    }

    private void validateRecovery(OccurrenceRequest request, ExceptionOccurrence occurrence, AssetItem asset) {
        if (request.relatedOccurrenceId() == null) bad("Recovery requires the related missing occurrence.");
        ExceptionOccurrence related = locked(ExceptionOccurrence.class, request.relatedOccurrenceId());
        if (related.asset == null || !related.asset.id.equals(asset.id)) bad("Related occurrence does not belong to recovered asset.");
        if (!MISSING_TYPES.contains(related.type)) bad("Recovery must reference loss, theft or robbery.");
        if (!AssetStatus.MISSING.name().equals(asset.status)) conflict("Only a missing asset can enter recovery.");
        occurrence.relatedOccurrence = related;
    }

    private void applyAssetOpeningState(ExceptionOccurrence occurrence, AssetItem asset) {
        if (MISSING_TYPES.contains(occurrence.type)) asset.status = AssetStatus.MISSING.name();
        else if (BLOCKING_TYPES.contains(occurrence.type) || RECOVERY_TYPES.contains(occurrence.type)) asset.status = AssetStatus.BLOCKED.name();
    }

    private void applyLotOpeningState(ExceptionOccurrence occurrence, StockLot lot) {
        if (MISSING_TYPES.contains(occurrence.type) || BLOCKING_TYPES.contains(occurrence.type)) lot.status = AssetStatus.BLOCKED.name();
        if (RECOVERY_TYPES.contains(occurrence.type)) bad("Recovery currently requires an individually identified asset.");
    }

    private void closeRelatedMissingOccurrence(ExceptionOccurrence recovery) {
        ExceptionOccurrence related = recovery.relatedOccurrence;
        if (related == null || "RESOLVED".equals(related.status)) return;
        related.status = "RESOLVED";
        related.resolvedAt = LocalDateTime.now();
        var actor = audit.actor();
        related.resolvedById = actor.id();
        related.resolvedByLogin = actor.login();
        related.resultingItemStatus = AssetStatus.AVAILABLE.name();
    }

    private OccurrenceView view(ExceptionOccurrence x) {
        String reference = x.asset != null ? x.asset.assetCode : x.lot == null ? null : x.lot.lotNumber;
        return new OccurrenceView(
            x.id, x.type, x.status, x.asset == null ? null : x.asset.id, x.lot == null ? null : x.lot.id,
            reference, x.quantity, x.description, x.investigation, x.documentReference, x.occurredAt.toString(), x.responsibleLogin,
            x.resolvedAt == null ? null : x.resolvedAt.toString(), x.resolvedByLogin,
            x.relatedOccurrence == null ? null : x.relatedOccurrence.id, x.previousItemStatus, x.resultingItemStatus,
            x.investigationStartedAt == null ? null : x.investigationStartedAt.toString(), x.investigatedByLogin
        );
    }

    private void require(ExceptionOccurrence occurrence, String action) {
        access.requireScope("inventory/assets", action, occurrence.organization.id, occurrence.unit == null ? null : occurrence.unit.id);
    }

    private OrganizationalUnit unit(long organizationId, Long id) {
        if (id == null) return null;
        OrganizationalUnit value = em.find(OrganizationalUnit.class, id);
        if (value == null || !value.organization.id.equals(organizationId)) bad("Unit does not belong to organization.");
        if (!Boolean.TRUE.equals(value.active)) bad("Selected unit must be active.");
        return value;
    }

    private void scope(StockLocation location, long organizationId, Long unitId) {
        if (location == null || !Boolean.TRUE.equals(location.active)) bad("Stock location must be active.");
        if (!location.organization.id.equals(organizationId) || unitId != null && (location.unit == null || !location.unit.id.equals(unitId))) bad("Item is outside selected scope.");
    }

    private <T> T locked(Class<T> type, long id) {
        T value = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (value == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found.");
        return value;
    }

    private static String canonical(String value) {
        String type = value.trim().toUpperCase(Locale.ROOT);
        if ("EXTRAVIO".equals(type)) return "LOSS";
        if ("FURTO".equals(type)) return "THEFT";
        if ("ROUBO".equals(type)) return "ROBBERY";
        if ("APREENSAO".equals(type) || "APREENSÃO".equals(type)) return "SEIZURE";
        if ("RECUPERACAO".equals(type) || "RECUPERAÇÃO".equals(type)) return "RECOVERY";
        if ("DANO".equals(type)) return "DAMAGE";
        if ("ACIDENTE".equals(type)) return "ACCIDENT";
        if ("RECOLHIMENTO".equals(type)) return "RECALL";
        if ("INVESTIGACAO".equals(type) || "INVESTIGAÇÃO".equals(type)) return "INVESTIGATION";
        return type;
    }

    private static String clean(String value) { return blank(value) ? null : value.trim(); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
