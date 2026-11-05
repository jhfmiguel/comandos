package com.comandos.inventory.service;
import com.fariamiguel.app.inventory.service.InventoryService;
import com.fariamiguel.app.inventory.service.InventoryCatalog;

import com.fariamiguel.app.core.model.CoreEntity;
import com.fariamiguel.app.core.service.CoreCatalog;
import com.comandos.inventory.service.ProductMasterDataReferenceSynchronizer;
import com.fariamiguel.app.core.service.ProductCanonicalScopeResolver;
import com.fariamiguel.app.core.service.CanonicalMasterDataDirectory;
import com.fariamiguel.app.inventory.model.*;
import com.comandos.inventory.model.*;
import com.fariamiguel.app.reservation.model.ReservationStatusType;
import com.fariamiguel.app.reconciliation.model.InventoryCountResultType;
import com.fariamiguel.app.reconciliation.model.InventoryCountStatusType;
import com.fariamiguel.app.sales.model.SaleReturnReasonType;
import com.comandos.custody.model.CustodyReturnConditionType;
import com.comandos.security.service.ComandosAccessPolicy;
import com.comandos.audit.service.ComandosAuditService;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@org.springframework.context.annotation.Primary
@Transactional(readOnly = true)
public class ComandosInventoryService extends InventoryService {
    public ComandosInventoryService(EntityManager em, ComandosInventoryRules rules, ComandosAccessPolicy access,
            ComandosAuditService audit, ProductMasterDataReferenceSynchronizer references,
            ProductCanonicalScopeResolver canonicalScope, CanonicalMasterDataDirectory masterData,
            @org.springframework.beans.factory.annotation.Value("${comandos.master-data.product-reference-primary-read.enabled:false}") boolean primaryRead) {
        super(em, rules, access, audit, references, canonicalScope, masterData, primaryRead);
    }

    @Override
    public PageResult list(String resource, String search, int page, int size, Long organizationId,
            Map<String, String> requestParams) {
        String unit = requestParams.get("filter.unit");
        if ("assets".equals(resource) && unit != null && !unit.isBlank() && organizationId == null)
            bad("Organization is required when filtering assets by unit.");
        return super.list(resource, search, page, size, organizationId, requestParams);
    }

    @Override
    protected void initializeResource(InventoryCatalog.Resource spec, CoreEntity entity) {
        if (entity instanceof ArmamentParameter parameter && spec.discriminator() != null)
            parameter.parameterType = spec.discriminator();
    }

    @Override
    protected void validateReference(CoreEntity entity, InventoryCatalog.Field field, Object value, Long id) {
            if (value instanceof ArmamentParameter parameter && !Boolean.TRUE.equals(parameter.active)) {
                Object current = id == null ? null : read(entity, field.property());
                boolean unchanged = current instanceof ArmamentParameter existing
                    && Objects.equals(existing.id, parameter.id);
                if (!unchanged) bad("Select an active " + field.label().toLowerCase(Locale.ROOT) + ".");
            }
    }
    protected Map<String, LegacyParameterField> legacyParameterFields(String resource) {
        return switch (resource) {
            case "firearm-specifications" -> Map.of(
                "caliberRefId", new LegacyParameterField("caliber", "CALIBER")
            );
            case "ammunition-specifications" -> Map.of(
                "caliberRefId", new LegacyParameterField("caliber", "CALIBER"),
                "ammunitionTypeRefId", new LegacyParameterField("ammunitionType", "AMMUNITION_TYPE"),
                "projectileTypeRefId", new LegacyParameterField("projectileType", "PROJECTILE_TYPE"),
                "caseTypeRefId", new LegacyParameterField("caseType", "CASE_TYPE"),
                "primerTypeRefId", new LegacyParameterField("primerType", "PRIMER_TYPE")
            );
            case "grenade-specifications" -> Map.of(
                "grenadeTypeRefId", new LegacyParameterField("grenadeType", "GRENADE_TYPE"),
                "agentRefId", new LegacyParameterField("agent", "AGENT"),
                "compositionRefId", new LegacyParameterField("composition", "COMPOSITION")
            );
            case "spray-specifications" -> Map.of(
                "agentRefId", new LegacyParameterField("agent", "AGENT"),
                "compositionRefId", new LegacyParameterField("composition", "COMPOSITION")
            );
            case "ballistic-protection-specifications" -> Map.of(
                "protectionTypeRefId", new LegacyParameterField("protectionType", "PROTECTION_TYPE"),
                "protectionLevelRefId", new LegacyParameterField("protectionLevel", "PROTECTION_LEVEL"),
                "materialRefId", new LegacyParameterField("material", "MATERIAL"),
                "sizeRefId", new LegacyParameterField("size", "SIZE")
            );
            case "electrical-device-specifications" -> Map.of(
                "cartridgeTypeRefId", new LegacyParameterField("cartridgeType", "CARTRIDGE_TYPE")
            );
            case "optical-specifications" -> Map.of(
                "opticalTypeRefId", new LegacyParameterField("opticalType", "OPTICAL_TYPE")
            );
            case "helmet-specifications" -> Map.of(
                "protectionLevelRefId", new LegacyParameterField("protectionLevel", "PROTECTION_LEVEL"),
                "materialRefId", new LegacyParameterField("material", "MATERIAL"),
                "sizeRefId", new LegacyParameterField("size", "SIZE")
            );
            case "shield-specifications" -> Map.of(
                "shieldTypeRefId", new LegacyParameterField("shieldType", "SHIELD_TYPE"),
                "protectionLevelRefId", new LegacyParameterField("protectionLevel", "PROTECTION_LEVEL"),
                "materialRefId", new LegacyParameterField("material", "MATERIAL")
            );
            case "restraint-specifications" -> Map.of(
                "materialRefId", new LegacyParameterField("material", "MATERIAL"),
                "lockingMechanismRefId", new LegacyParameterField("lockingMechanism", "LOCKING_MECHANISM")
            );
            case "accessory-component-specifications" -> Map.of(
                "componentTypeRefId", new LegacyParameterField("componentType", "COMPONENT_TYPE"),
                "compatibilityRefId", new LegacyParameterField("compatibleWith", "COMPATIBILITY"),
                "interfaceRefId", new LegacyParameterField("mountingInterface", "INTERFACE")
            );
            case "tactical-equipment-specifications" -> Map.of(
                "materialRefId", new LegacyParameterField("material", "MATERIAL"),
                "sizeRefId", new LegacyParameterField("size", "SIZE")
            );
            default -> Map.of();
        };
    }
    @Override
    protected void requireResourceDiscriminator(InventoryCatalog.Resource spec, CoreEntity entity) {
        if (spec.discriminator() == null) return;
        if (!(entity instanceof ArmamentParameter parameter)
                || !Objects.equals(spec.discriminator(), parameter.parameterType)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Referenced record not found.");
        }
    }
    @Override
    protected void lockCatalog() {
        em.createQuery("select t from ArmamentType t order by t.id", ArmamentType.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        em.createQuery("select c from ArmamentClassification c order by c.id", ArmamentClassification.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        // Serialize schema and asset writes before reading related category rows.
        // This also prevents concurrent parent changes from creating a cycle.
        super.lockCatalog();
        em.createQuery("select p from ArmamentParameter p order by p.id", ArmamentParameter.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }
    @Override
    protected boolean preserveOmittedField(CoreEntity entity, InventoryCatalog.Field field, Map<String, Object> data) {
        return entity instanceof ItemModel && !data.containsKey(field.name())
            && Set.of("armamentTypeId", "armamentClassificationId").contains(field.name());
    }

    @Override
    protected Object read(CoreEntity entity, String field) {
        if (entity instanceof ItemModel model && Set.of("armamentType", "armamentClassification").contains(field)) {
            var details = ArmamentModelDetails.of(em, model);
            return "armamentType".equals(field) ? details.armamentType : details.armamentClassification;
        }
        return super.read(entity, field);
    }

    @Override
    protected void write(CoreEntity entity, String field, Object value) {
        if (entity instanceof ItemModel model && Set.of("armamentType", "armamentClassification").contains(field)) {
            Object previous=read(model,field);
            Object previousId=previous instanceof CoreEntity reference?reference.id:previous;
            Object nextId=value instanceof CoreEntity reference?reference.id:value;
            if(model.id!=null&&!Objects.equals(previousId,nextId))model.updatedAt=java.time.LocalDateTime.now();
            model.extensionValues.put(field, value);
            return;
        }
        super.write(entity, field, value);
    }

    @Override
    protected void afterWrite(CoreEntity entity) {
        if (entity instanceof ItemModel model) ArmamentModelDetails.of(em, model);
    }

    @Override
    protected String label(CoreEntity entity) {
        String name = null;
        if (entity instanceof FirearmSpecification specification) name = specification.model.name + " / " + specification.caliber;
        if (entity instanceof AmmunitionSpecification specification) name = specification.model.name + " / " + specification.caliber;
        if (entity instanceof GrenadeSpecification specification) name = specification.model.name + " / " + specification.grenadeType;
        if (entity instanceof SpraySpecification specification) name = specification.model.name + " / " + specification.agent;
        if (entity instanceof BallisticProtectionSpecification specification) name = specification.model.name + " / " + specification.protectionLevel;
        if (entity instanceof ElectricalDeviceSpecification specification) name = specification.model.name + " / " + specification.cartridgeType;
        if (entity instanceof OpticalSpecification specification) name = specification.model.name + " / " + specification.opticalType;
        if (entity instanceof RegulatoryControl control) name = control.registrationNumber + " / " + control.asset.assetCode;
        if (entity instanceof ArmamentParameter parameter) name = parameter.code + " / " + parameter.name;
        if (entity instanceof CustodyReturnConditionType condition) name = condition.code + " / " + condition.name;
        return name == null ? super.label(entity) : name + " (#" + entity.id + ")";
    }
}