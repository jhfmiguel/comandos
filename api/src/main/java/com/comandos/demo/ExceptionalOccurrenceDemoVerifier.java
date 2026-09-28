package com.comandos.demo;

import com.comandos.inventory.model.AssetStatus;
import com.comandos.lifecycle.model.ExceptionOccurrence;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1985)
public class ExceptionalOccurrenceDemoVerifier implements ApplicationRunner {
    private static final Set<String> MISSING = Set.of("LOSS", "LOST", "THEFT", "ROBBERY");
    private static final Set<String> BLOCKING = Set.of("SEIZURE", "DAMAGE", "ACCIDENT", "RECALL", "BLOCK");
    private static final Set<String> RECOVERY = Set.of("RECOVERY", "RECOVERED");
    private static final Set<String> CANONICAL_MATRIX = Set.of(
        "LOSS", "THEFT", "ROBBERY", "SEIZURE", "RECOVERY", "DAMAGE", "ACCIDENT", "RECALL", "INVESTIGATION"
    );
    private static final Map<String, String> PORTUGUESE_MATRIX = Map.of(
        "extravio", "LOSS",
        "furto", "THEFT",
        "roubo", "ROBBERY",
        "apreensao", "SEIZURE",
        "recuperacao", "RECOVERY",
        "dano", "DAMAGE",
        "acidente", "ACCIDENT",
        "recolhimento", "RECALL",
        "investigacao", "INVESTIGATION"
    );

    private final EntityManager em;

    public ExceptionalOccurrenceDemoVerifier(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        require(PORTUGUESE_MATRIX.size() == 9, "Exceptional occurrence matrix must cover all nine homologated concepts.");
        require(PORTUGUESE_MATRIX.values().containsAll(CANONICAL_MATRIX), "Exceptional occurrence matrix is incomplete.");

        var occurrences = em.createQuery("select x from ExceptionOccurrence x order by x.id", ExceptionOccurrence.class).getResultList();
        for (ExceptionOccurrence occurrence : occurrences) verify(occurrence);
    }

    private void verify(ExceptionOccurrence occurrence) {
        require(occurrence.organization != null, "Occurrence must preserve organization provenance.");
        require(occurrence.type != null && !occurrence.type.isBlank(), "Occurrence type is required.");
        require(Set.of("OPEN", "UNDER_INVESTIGATION", "RESOLVED").contains(occurrence.status), "Occurrence workflow status is invalid.");
        require((occurrence.asset == null) != (occurrence.lot == null), "Occurrence must reference exactly one asset or lot.");
        require(occurrence.occurredAt != null && occurrence.responsibleId != null && notBlank(occurrence.responsibleLogin), "Occurrence must preserve responsibility history.");

        if (occurrence.investigationStartedAt != null) {
            require(notBlank(occurrence.investigation), "Investigation start requires investigation notes.");
            require(occurrence.investigatedById != null && notBlank(occurrence.investigatedByLogin), "Investigation must preserve investigator identity.");
        }
        if ("RESOLVED".equals(occurrence.status)) {
            require(occurrence.resolvedAt != null && occurrence.resolvedById != null && notBlank(occurrence.resolvedByLogin), "Resolved occurrence must preserve resolver history.");
            require(notBlank(occurrence.investigation), "Resolved occurrence must preserve investigation notes.");
        }

        if (MISSING.contains(occurrence.type)) {
            if (occurrence.asset != null) require(AssetStatus.MISSING.name().equals(occurrence.resultingItemStatus) || RECOVERY.containsRelated(occurrence), "Loss/theft/robbery must preserve missing state until recovery.");
            else require(AssetStatus.BLOCKED.name().equals(occurrence.resultingItemStatus), "Lot loss/theft/robbery must block the lot.");
        }
        if (BLOCKING.contains(occurrence.type)) {
            require(AssetStatus.BLOCKED.name().equals(occurrence.resultingItemStatus), "Seizure/damage/accident/recall must block the item.");
        }
        if (RECOVERY.contains(occurrence.type)) {
            require(occurrence.asset != null, "Recovery requires an individually identified asset.");
            require(occurrence.relatedOccurrence != null, "Recovery must reference the original missing occurrence.");
            require(MISSING.contains(occurrence.relatedOccurrence.type), "Recovery must reference loss, theft or robbery.");
            require(occurrence.relatedOccurrence.asset != null && occurrence.relatedOccurrence.asset.id.equals(occurrence.asset.id), "Recovery relation must preserve the same asset.");
            require(AssetStatus.MISSING.name().equals(occurrence.previousItemStatus), "Recovery must start from a missing asset.");
            if ("RESOLVED".equals(occurrence.status)) require(AssetStatus.AVAILABLE.name().equals(occurrence.resultingItemStatus), "Resolved recovery must return the asset to availability.");
            else require(AssetStatus.BLOCKED.name().equals(occurrence.resultingItemStatus), "Recovery under investigation must keep the asset blocked.");
        }
        if ("INVESTIGATION".equals(occurrence.type) && occurrence.previousItemStatus != null && occurrence.resultingItemStatus != null) {
            require(occurrence.previousItemStatus.equals(occurrence.resultingItemStatus), "Investigation alone cannot change item availability.");
        }
        if (Set.of("THEFT", "ROBBERY", "SEIZURE", "RECOVERY", "RECOVERED").contains(occurrence.type)) {
            require(notBlank(occurrence.documentReference), "Legal exceptional occurrence requires document reference.");
        }
        if (occurrence.asset != null && AssetStatus.terminalCodes().contains(occurrence.asset.status)) {
            require(!RECOVERY.contains(occurrence.type), "Recovery cannot reactivate a terminal asset.");
        }
    }

    private static boolean notBlank(String value) { return value != null && !value.isBlank(); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }

    private static final class RECOVERY {
        private static boolean containsRelated(ExceptionOccurrence occurrence) {
            return occurrence.relatedOccurrence != null && Set.of("RECOVERY", "RECOVERED").contains(occurrence.relatedOccurrence.type);
        }
    }
}
