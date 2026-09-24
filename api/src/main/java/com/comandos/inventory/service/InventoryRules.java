package com.comandos.inventory.service;

import com.comandos.core.model.*;
import com.comandos.enterprise.catalog.CatalogTrackingPolicy;
import com.comandos.enterprise.catalog.UnitOfMeasureCode;
import com.comandos.inventory.model.*;
import com.comandos.reservation.model.ReservationStatusType;
import com.comandos.reconciliation.model.*;
import com.comandos.sales.model.*;
import com.comandos.custody.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

@Component
public class InventoryRules {
    private final EntityManager em;
    public InventoryRules(EntityManager em) { this.em = em; }

    public void lockOrganization(InventoryCatalog.Resource spec, Map<String, Object> data) {
        if (spec.entity() != StockLocation.class && spec.entity() != EquipmentSet.class) return;
        try {
            long id = new BigDecimal(String.valueOf(data.get("organizationId"))).longValueExact();
            if (em.find(Organization.class, id, LockModeType.PESSIMISTIC_WRITE) == null) bad("Organization not found.");
        } catch (NumberFormatException | ArithmeticException ex) { bad("Organization is required."); }
    }

    public void validate(CoreEntity entity, Map<String, Object> previous) {
        if (entity instanceof ArmamentParameter parameter) {
            if (parameter.parameterType == null || parameter.parameterType.isBlank())
                bad("Parameter type is required.");
            parameter.code = parameter.code == null ? null : parameter.code.trim().toUpperCase(Locale.ROOT);
            parameter.name = parameter.name == null ? null : parameter.name.trim();
            registry(parameter.code, 0);
            if (parameter.name == null || parameter.name.isBlank())
                bad("Parameter name is required.");

            long duplicateCode = em.createQuery(
                    "select count(p) from ArmamentParameter p where p.parameterType = :type and p.code = :code and p.id <> :id",
                    Long.class)
                .setParameter("type", parameter.parameterType)
                .setParameter("code", parameter.code)
                .setParameter("id", parameter.id == null ? -1L : parameter.id)
                .setFlushMode(jakarta.persistence.FlushModeType.COMMIT)
                .getSingleResult();
            if (duplicateCode > 0) bad("A parameter with this code already exists.");

            long duplicateName = em.createQuery(
                    "select count(p) from ArmamentParameter p where p.parameterType = :type and lower(p.name) = lower(:name) and p.id <> :id",
                    Long.class)
                .setParameter("type", parameter.parameterType)
                .setParameter("name", parameter.name)
                .setParameter("id", parameter.id == null ? -1L : parameter.id)
                .setFlushMode(jakarta.persistence.FlushModeType.COMMIT)
                .getSingleResult();
            if (duplicateName > 0) bad("A parameter with this name already exists.");
        }
        if (entity instanceof ArmamentType type) {
            registry(type.code, 0);
            if (!type.code.matches("[A-Z][A-Z0-9_]{1,49}")) bad("Code must be uppercase.");
            if (type.id != null && typeUsage(type.id) > 0 && (!type.active
                    || !Objects.equals(previous.get("categoryId"), type.category.id)))
                bad("A used armament type cannot be deactivated or moved to another category. Reassign its references first.");
        }
        if (entity instanceof ArmamentClassification classification) {
            registry(classification.code, 0);
            if (!classification.code.matches("[A-Z][A-Z0-9_]{1,49}")) bad("Code must be uppercase.");
            if (!classification.type.active) bad("Select an active armament type.");
            if (classification.id != null && classificationUsage(classification.id) > 0 && (!classification.active
                    || !Objects.equals(previous.get("typeId"), classification.type.id)))
                bad("A used classification cannot be deactivated or moved to another type. Reassign its references first.");
        }
        if (entity instanceof ItemModel model) {
            try {
                model.unitOfMeasure = new UnitOfMeasureCode(model.unitOfMeasure).value();
            } catch (IllegalArgumentException ex) {
                bad(ex.getMessage());
            }
            if (model.armamentType != null && (!model.armamentType.active
                    || !Objects.equals(model.armamentType.category.id, model.category.id)))
                bad("Select an active armament type belonging to the model category.");
            if (model.armamentClassification != null && (!model.armamentClassification.active
                    || model.armamentType == null || !Objects.equals(model.armamentClassification.type.id, model.armamentType.id)))
                bad("Select an active classification belonging to the model armament type.");
        }
        if (entity instanceof ItemCategory category) {
            try {
                new CatalogTrackingPolicy(category.serialized, category.lotControlled, category.consumable);
            } catch (IllegalArgumentException ex) {
                bad(ex.getMessage());
            }
            Set<Long> visited = new HashSet<>();
            if (category.id != null) visited.add(category.id);
            for (var parent = category.parentCategory; parent != null; parent = parent.parentCategory) {
                if (!visited.add(parent.id)) bad("Category hierarchy cannot contain cycles.");
            }
            if (!previous.isEmpty() && count("select count(m) from ItemModel m where m.category.id = :id", category.id) > 0) {
                for (String field : List.of("serialized", "lotControlled", "consumable", "family")) {
                    Object value = switch (field) { case "serialized" -> category.serialized; case "lotControlled" -> category.lotControlled; case "consumable" -> category.consumable; default -> category.family; };
                    if (!Objects.equals(previous.get(field), value)) bad("Category tracking rules cannot change while models reference it.");
                }
            }
        }
        if (entity instanceof ItemModel model && !previous.isEmpty()) {
            if (!Objects.equals(previous.get("categoryId"), model.category.id) || !Objects.equals(previous.get("unitOfMeasure"), model.unitOfMeasure)) {
                if (modelUsage(model.id) > 0 || specificationUsage(model.id) > 0
                        || count("select count(v) from ModelCharacteristicValue v where v.model.id = :id", model.id) > 0)
                    bad("Category and unit of measure cannot change after specifications, characteristics or stock are registered.");
            }
        }
        if (entity instanceof FirearmSpecification specification) {
            specification.caliber = parameterName(specification.caliberRef, "CALIBER", "Caliber", true);
            if (!"FIREARM".equals(specification.model.category.family))
                bad("Firearm specifications require a model in the FIREARM family.");
            if (specification.capacity == null || specification.capacity <= 0)
                bad("Capacity must be greater than zero.");
            if (specification.barrelLength == null || specification.barrelLength.signum() <= 0)
                bad("Barrel length must be greater than zero.");
        }
        if (entity instanceof AmmunitionSpecification specification) {
            specification.caliber = parameterName(specification.caliberRef, "CALIBER", "Caliber", true);
            specification.ammunitionType = parameterName(specification.ammunitionTypeRef, "AMMUNITION_TYPE", "Ammunition type", true);
            specification.projectileType = parameterName(specification.projectileTypeRef, "PROJECTILE_TYPE", "Projectile type", true);
            specification.caseType = parameterName(specification.caseTypeRef, "CASE_TYPE", "Case type", true);
            specification.primerType = parameterName(specification.primerTypeRef, "PRIMER_TYPE", "Primer type", true);
            if (!"AMMUNITION".equals(specification.model.category.family))
                bad("Ammunition specifications require a model in the AMMUNITION family.");
            if (!Set.of("LETHAL", "LESS_LETHAL").contains(specification.lethalityClassification))
                bad("Lethality classification must be LETHAL or LESS_LETHAL.");
        }
        if (entity instanceof GrenadeSpecification specification) {
            specification.grenadeType = parameterName(specification.grenadeTypeRef, "GRENADE_TYPE", "Grenade type", true);
            specification.agent = parameterName(specification.agentRef, "AGENT", "Agent", true);
            specification.composition = parameterName(specification.compositionRef, "COMPOSITION", "Composition", true);
            if (!"GRENADE".equals(specification.model.category.family))
                bad("Grenade specifications require a model in the GRENADE family.");
            if (specification.delaySeconds == null || specification.delaySeconds <= 0)
                bad("Delay time must be greater than zero.");
            if (specification.safetyRadius == null || specification.safetyRadius.signum() <= 0)
                bad("Safety radius must be greater than zero.");
            if (specification.shelfLifeMonths == null || specification.shelfLifeMonths <= 0)
                bad("Shelf life must be greater than zero.");
        }
        if (entity instanceof SpraySpecification specification) {
            specification.agent = parameterName(specification.agentRef, "AGENT", "Agent", true);
            specification.composition = parameterName(specification.compositionRef, "COMPOSITION", "Composition", true);
            if (!"SPRAY".equals(specification.model.category.family))
                bad("Spray specifications require a model in the SPRAY family.");
            if (specification.concentration == null || specification.concentration.signum() <= 0
                    || specification.concentration.compareTo(new BigDecimal("100")) > 0)
                bad("Concentration must be greater than zero and at most 100 percent.");
            if (specification.volumeMl == null || specification.volumeMl.signum() <= 0)
                bad("Volume must be greater than zero.");
            if (specification.rangeMeters == null || specification.rangeMeters.signum() <= 0)
                bad("Range must be greater than zero.");
            if (specification.shelfLifeMonths == null || specification.shelfLifeMonths <= 0)
                bad("Shelf life must be greater than zero.");
        }
        if (entity instanceof BallisticProtectionSpecification specification) {
            specification.protectionType = parameterName(specification.protectionTypeRef, "PROTECTION_TYPE", "Protection type", true);
            specification.protectionLevel = parameterName(specification.protectionLevelRef, "PROTECTION_LEVEL", "Protection level", true);
            specification.material = parameterName(specification.materialRef, "MATERIAL", "Material", true);
            specification.size = parameterName(specification.sizeRef, "SIZE", "Size", true);
            if (!"BALLISTIC_PROTECTION".equals(specification.model.category.family))
                bad("Ballistic protection specifications require a model in the BALLISTIC_PROTECTION family.");
            if (specification.serviceLifeMonths == null || specification.serviceLifeMonths <= 0)
                bad("Service life must be greater than zero.");
        }
        if (entity instanceof ElectricalDeviceSpecification specification) {
            specification.cartridgeType = parameterName(specification.cartridgeTypeRef, "CARTRIDGE_TYPE", "Cartridge type", true);
            if (!"ELECTRICAL_DEVICE".equals(specification.model.category.family))
                bad("Electrical device specifications require a model in the ELECTRICAL_DEVICE family.");
            if (specification.voltage == null || specification.voltage.signum() <= 0)
                bad("Voltage must be greater than zero.");
            if (specification.cycles == null || specification.cycles <= 0)
                bad("Cycles must be greater than zero.");
        }
        if (entity instanceof OpticalSpecification specification) {
            specification.opticalType = parameterName(specification.opticalTypeRef, "OPTICAL_TYPE", "Optical type", true);
            if (!"OPTICAL".equals(specification.model.category.family))
                bad("Optical specifications require a model in the OPTICAL family.");
            if (specification.minimumMagnification == null || specification.minimumMagnification.signum() <= 0)
                bad("Minimum magnification must be greater than zero.");
            if (specification.maximumMagnification == null || specification.maximumMagnification.signum() <= 0)
                bad("Maximum magnification must be greater than zero.");
            if (specification.maximumMagnification.compareTo(specification.minimumMagnification) < 0)
                bad("Maximum magnification cannot be lower than minimum magnification.");
            positiveOptional(specification.objectiveDiameterMm, "Objective diameter");
        }
        if (entity instanceof HelmetSpecification specification) {
            specification.protectionLevel = parameterName(specification.protectionLevelRef, "PROTECTION_LEVEL", "Protection level", true);
            specification.material = parameterName(specification.materialRef, "MATERIAL", "Material", true);
            specification.size = parameterName(specification.sizeRef, "SIZE", "Size", true);
            if (!"HELMET".equals(specification.model.category.family))
                bad("Helmet specifications require a model in the HELMET family.");
            positiveOptional(specification.weightGrams, "Helmet weight");
        }
        if (entity instanceof ShieldSpecification specification) {
            specification.shieldType = parameterName(specification.shieldTypeRef, "SHIELD_TYPE", "Shield type", true);
            specification.protectionLevel = parameterName(specification.protectionLevelRef, "PROTECTION_LEVEL", "Protection level", false);
            specification.material = parameterName(specification.materialRef, "MATERIAL", "Material", true);
            if (!"SHIELD".equals(specification.model.category.family))
                bad("Shield specifications require a model in the SHIELD family.");
            positiveOptional(specification.heightMm, "Shield height");
            positiveOptional(specification.widthMm, "Shield width");
            positiveOptional(specification.weightGrams, "Shield weight");
        }
        if (entity instanceof RestraintSpecification specification) {
            specification.material = parameterName(specification.materialRef, "MATERIAL", "Material", true);
            specification.lockingMechanism = parameterName(specification.lockingMechanismRef, "LOCKING_MECHANISM", "Locking mechanism", false);
            if (!"RESTRAINT".equals(specification.model.category.family))
                bad("Restraint specifications require a model in the RESTRAINT family.");
        }
        if (entity instanceof AccessoryComponentSpecification specification) {
            specification.componentType = parameterName(specification.componentTypeRef, "COMPONENT_TYPE", "Component type", true);
            specification.compatibleWith = parameterName(specification.compatibilityRef, "COMPATIBILITY", "Compatibility", false);
            specification.mountingInterface = parameterName(specification.interfaceRef, "INTERFACE", "Interface", false);
            if (!"ACCESSORY_COMPONENT".equals(specification.model.category.family))
                bad("Accessory/component specifications require a model in the ACCESSORY_COMPONENT family.");
        }
        if (entity instanceof TacticalEquipmentSpecification specification) {
            specification.material = parameterName(specification.materialRef, "MATERIAL", "Material", false);
            specification.size = parameterName(specification.sizeRef, "SIZE", "Size", false);
            if (!"TACTICAL_EQUIPMENT".equals(specification.model.category.family))
                bad("Tactical equipment specifications require a model in the TACTICAL_EQUIPMENT family.");
            positiveOptional(specification.weightGrams, "Tactical equipment weight");
        }
        if (entity instanceof RegulatoryControl control) {
            if (!"FIREARM".equals(control.asset.model.category.family))
                bad("Regulatory controls require an individual asset in the FIREARM family.");
            if (AssetStatus.terminalCodes().contains(control.asset.status))
                bad("A regulatory registration cannot be created or changed for a terminal asset.");
            if ("ACTIVE".equals(control.status) && control.validUntil != null && control.validUntil.isBefore(LocalDate.now()))
                bad("An expired regulatory registration cannot remain active.");
        }
        if (entity instanceof ExpirationRecord expiration) {
            requireOne(expiration.asset, expiration.lot, "Expiration control");
            validateScope(expiration.organization, expiration.unit, expiration.asset, expiration.lot);
            if ("VALID".equals(expiration.status) && expiration.expirationDate.isBefore(LocalDate.now()))
                bad("A past expiration cannot remain valid.");
            if ("EXPIRED".equals(expiration.status) && expiration.asset != null && AssetStatus.AVAILABLE.name().equals(expiration.asset.status))
                expiration.asset.status = AssetStatus.BLOCKED.name();
        }
        if (entity instanceof CertificationRecord certification) {
            requireOne(certification.asset, certification.lot, "Certification");
            validateScope(certification.organization, certification.unit, certification.asset, certification.lot);
            if ("ACTIVE".equals(certification.status) && certification.validUntil.isBefore(LocalDate.now()))
                bad("An expired certification cannot remain active.");
            if ("EXPIRED".equals(certification.status) && certification.asset != null && AssetStatus.AVAILABLE.name().equals(certification.asset.status))
                certification.asset.status = AssetStatus.BLOCKED.name();
        }
        if (entity instanceof Recall recall && recall.unit != null
                && !recall.unit.organization.id.equals(recall.organization.id)) bad("Recall unit must belong to its organization.");
        if (entity instanceof RecallItem item) {
            requireOne(item.asset, item.lot, "Recall item");
            if (!Set.of("OPEN", "IN_PROGRESS").contains(item.recall.status))
                bad("Items can only be added to an open or in-progress recall.");
            validateScope(item.recall.organization, item.recall.unit, item.asset, item.lot);
            if (item.asset != null && AssetStatus.AVAILABLE.name().equals(item.asset.status)) item.asset.status = AssetStatus.BLOCKED.name();
        }
        if (entity instanceof ReservationStatusType status) {
            status.code = status.code.trim().toUpperCase(Locale.ROOT);
            if (!status.code.matches("[A-Z][A-Z0-9_]{1,49}")) bad("Status code must use 2 to 50 uppercase letters, numbers or underscores.");
            if (status.displayOrder == null || status.displayOrder < 0) bad("Display order cannot be negative.");
            if (!previous.isEmpty() && !Objects.equals(previous.get("code"), status.code)) bad("Status code cannot be changed.");
            if (!previous.isEmpty() && Boolean.TRUE.equals(previous.get("systemProtected")) && !status.systemProtected)
                bad("System protection cannot be removed.");
        }
        if (entity instanceof InventoryCountStatusType status) {
            registry(status.code, status.displayOrder);
            status.code = status.code.trim().toUpperCase(Locale.ROOT);
            if (!previous.isEmpty() && !Objects.equals(previous.get("code"), status.code)) bad("Status code cannot be changed.");
            if (!previous.isEmpty() && Boolean.TRUE.equals(previous.get("systemProtected")) && !status.systemProtected) bad("System protection cannot be removed.");
        }
        if (entity instanceof InventoryCountResultType result) {
            registry(result.code, result.displayOrder);
            result.code = result.code.trim().toUpperCase(Locale.ROOT);
            if (!previous.isEmpty() && !Objects.equals(previous.get("code"), result.code)) bad("Result code cannot be changed.");
            if (!previous.isEmpty() && Boolean.TRUE.equals(previous.get("systemProtected")) && !result.systemProtected) bad("System protection cannot be removed.");
        }
        if (entity instanceof SaleReturnReasonType reason) {
            registry(reason.code, reason.displayOrder); reason.code = reason.code.trim().toUpperCase(Locale.ROOT);
            if (!previous.isEmpty() && !Objects.equals(previous.get("code"), reason.code)) bad("Reason code cannot be changed.");
            if (!previous.isEmpty() && Boolean.TRUE.equals(previous.get("systemProtected")) && !reason.systemProtected) bad("System protection cannot be removed.");
        }
        if (entity instanceof CustodyReturnConditionType condition) {
            registry(condition.code, condition.displayOrder); condition.code = condition.code.trim().toUpperCase(Locale.ROOT);
            if (!previous.isEmpty() && !Objects.equals(previous.get("code"), condition.code)) bad("Condition code cannot be changed.");
            if (!previous.isEmpty() && Boolean.TRUE.equals(previous.get("systemProtected")) && !condition.systemProtected)
                bad("System protection cannot be removed.");
        }
        if (entity instanceof StockLocation location) {
            if (location.unit != null && !Objects.equals(location.unit.organization.id, location.organization.id)) bad("Unit must belong to the selected organization.");
            if (!previous.isEmpty() && !Objects.equals(previous.get("organizationId"), location.organization.id))
                bad("Location organization cannot be changed. Register a new location instead.");
        }
        if (entity instanceof EquipmentSet set) {
            if (set.unit != null && !Objects.equals(set.unit.organization.id, set.organization.id))
                bad("Unit must belong to the selected organization.");
            if (!previous.isEmpty() && !Objects.equals(previous.get("organizationId"), set.organization.id))
                bad("Equipment set organization cannot be changed. Register a new set instead.");
            if (!previous.isEmpty() && !Objects.equals(previous.get("unitId"), set.unit == null ? null : set.unit.id))
                bad("Equipment set unit cannot be changed after registration.");
            if (set.active && (set.id == null || count("select count(c) from EquipmentSetComponent c where c.equipmentSet.id = :id", set.id) == 0))
                bad("An equipment set must contain at least one component before activation.");
            if (set.active) validateActiveSet(set);
        }
        if (entity instanceof EquipmentSetComponent component) {
            requireOne(component.asset, component.balance, "Equipment set component");
            if (component.equipmentSet.active) bad("Deactivate the equipment set before changing its components.");
            if (component.quantity == null || component.quantity.signum() <= 0)
                bad("Component quantity must be greater than zero.");
            validateSetComponentScope(component.equipmentSet, component.asset, component.balance);
            if (component.asset != null && component.quantity.compareTo(BigDecimal.ONE) != 0)
                bad("An individual asset component must have quantity one.");
            if (component.balance != null && component.quantity.compareTo(physical(component.balance)) > 0)
                bad("Component quantity cannot exceed the physical stock balance.");
        }
        if (entity instanceof TechnicalCharacteristic characteristic && !previous.isEmpty()
                && !Objects.equals(previous.get("dataType"), characteristic.dataType)) {
            if (count("select count(v) from ModelCharacteristicValue v where v.characteristic.id = :id", characteristic.id)
                    + count("select count(v) from ItemCharacteristicValue v where v.characteristic.id = :id", characteristic.id) > 0)
                bad("Data type cannot change while characteristic values exist.");
        }
        if (entity instanceof CategoryCharacteristic binding) {
            if (binding.perItem && !binding.category.serialized) bad("Per-item characteristics require a serialized category.");
            boolean changed = !previous.isEmpty() && (!Objects.equals(previous.get("requiredValue"), binding.requiredValue)
                || !Objects.equals(previous.get("perItem"), binding.perItem));
            if ((changed || previous.isEmpty() && binding.requiredValue) && categoryUsage(binding.category.id) > 0)
                bad("Required characteristic rules cannot change after assets or lots are registered.");
            if (changed && count("select count(v) from ModelCharacteristicValue v where v.characteristic.id = :id", binding.characteristic.id)
                    + (changed ? count("select count(v) from ItemCharacteristicValue v where v.characteristic.id = :id", binding.characteristic.id) : 0) > 0)
                bad("Characteristic scope cannot change while values exist.");
        }
        if (entity instanceof ModelCharacteristicValue value) {
            var binding = binding(value.model.category.id, value.characteristic.id);
            if (binding.perItem) bad("This characteristic belongs to individual assets.");
            validateValue(value.characteristic, value.value);
        }
        if (entity instanceof ItemCharacteristicValue value) {
            var binding = binding(value.asset.model.category.id, value.characteristic.id);
            if (!binding.perItem) bad("This characteristic belongs to the model.");
            validateValue(value.characteristic, value.value);
        }
        if (entity instanceof AssetItem asset) {
            if (workflowStatus(asset.status) || workflowStatus(previous.get("status")))
                bad("Disposed assets cannot be changed through registration. Use the corresponding workflow.");
            if (asset.id != null && !AssetStatus.BLOCKED.name().equals(asset.status)
                    && count("select count(i) from ReservationItem i where i.asset.id = :id and i.reservation.status.code = 'ACTIVE'", asset.id) > 0)
                bad("An actively reserved asset must remain blocked.");
            if (asset.id != null && AssetStatus.AVAILABLE.name().equals(asset.status)
                    && count("select count(i) from InventoryCountItem i where i.asset.id = :id and i.inventoryCount.status.code = 'APPROVED' and i.result.code = 'SHORTAGE'", asset.id) > 0)
                bad("An asset reported missing by an approved inventory count must remain blocked.");
            if (asset.model.category.consumable || asset.model.category.lotControlled && !asset.model.category.serialized)
                bad("This model must be registered as a stock lot.");
            if (asset.model.category.serialized && asset.serialNumber == null) bad("Serial number is required for this category.");
            if (AssetStatus.AVAILABLE.name().equals(asset.status)) {
                if (asset.validUntil != null && asset.validUntil.isBefore(LocalDate.now())) bad("Expired assets cannot be available.");
                requireModelValues(asset.model);
                for (var binding : requiredBindings(asset.model.category.id, true)) {
                    if (asset.id == null || em.createQuery("select count(v) from ItemCharacteristicValue v where v.asset.id = :asset and v.characteristic.id = :characteristic", Long.class)
                            .setParameter("asset", asset.id).setParameter("characteristic", binding.characteristic.id).getSingleResult() == 0)
                        bad("Required asset characteristic missing: " + binding.characteristic.name + ". Save the asset as DRAFT and add its characteristics first.");
                }
            }
        }
        if (entity instanceof StockLot lot) {
            if (lot.model.category.serialized || !lot.model.category.lotControlled) bad("This category does not allow quantity-controlled stock lots.");
            if (lot.initialQuantity.signum() <= 0) bad("Opening quantity must be greater than zero.");
            if (lot.validUntil != null && lot.validUntil.isBefore(LocalDate.now())) bad("Expired lots cannot be registered as available stock.");
            requireModelValues(lot.model);
        }
    }

    public void beforeDelete(CoreEntity entity) {
        if (entity instanceof ArmamentParameter parameter && parameterUsage(parameter.id) > 0)
            bad("A parameter currently used by a technical specification cannot be deleted. Deactivate it instead.");
        if (entity instanceof ArmamentType type && typeUsage(type.id) > 0)
            bad("A used armament type cannot be deleted.");
        if (entity instanceof ArmamentClassification classification && classificationUsage(classification.id) > 0)
            bad("A used armament classification cannot be deleted.");
        if (entity instanceof AssetItem || entity instanceof StockLot) bad("Registered stock cannot be deleted. A dedicated disposal or reversal operation is required.");
        if (entity instanceof EquipmentSet set && count("select count(c) from EquipmentSetComponent c where c.equipmentSet.id = :id", set.id) > 0)
            bad("Remove every component before deleting the equipment set.");
        if (entity instanceof EquipmentSetComponent component && component.equipmentSet.active)
            bad("Deactivate the equipment set before removing a component.");
        if (entity instanceof CategoryCharacteristic binding && count("select count(m) from ItemModel m where m.category.id = :id", binding.category.id) > 0)
            bad("Category characteristics cannot be removed while models use the category.");
        if (entity instanceof ModelCharacteristicValue value && binding(value.model.category.id, value.characteristic.id).requiredValue && modelUsage(value.model.id) > 0)
            bad("Required model characteristics cannot be removed while stock exists.");
        if (entity instanceof ItemCharacteristicValue value && AssetStatus.AVAILABLE.name().equals(value.asset.status)
                && binding(value.asset.model.category.id, value.characteristic.id).requiredValue)
            bad("Required characteristics cannot be removed from an available asset.");
        if (entity instanceof ExpirationRecord || entity instanceof CertificationRecord || entity instanceof Recall || entity instanceof RecallItem)
            bad("Compliance history cannot be deleted.");
        if (entity instanceof ReservationStatusType status && (status.systemProtected
                || count("select count(r) from InventoryReservation r where r.status.id = :id", status.id) > 0))
            bad("A protected or used reservation status cannot be deleted. Deactivate it instead.");
        if (entity instanceof InventoryCountStatusType status && (status.systemProtected
                || count("select count(c) from InventoryCount c where c.status.id = :id", status.id) > 0))
            bad("A protected or used inventory count status cannot be deleted. Deactivate it instead.");
        if (entity instanceof InventoryCountResultType result && (result.systemProtected
                || count("select count(i) from InventoryCountItem i where i.result.id = :id", result.id) > 0))
            bad("A protected or used inventory result cannot be deleted. Deactivate it instead.");
        if (entity instanceof SaleReturnReasonType reason && (reason.systemProtected
                || count("select count(r) from SaleReturn r where r.reason.id = :id", reason.id) > 0))
            bad("A protected or used sale return reason cannot be deleted. Deactivate it instead.");
        if (entity instanceof CustodyReturnConditionType condition && (condition.systemProtected
                || count("select count(i) from CustodyReturnItem i where i.conditionType.id = :id", condition.id) > 0))
            bad("A protected or used custody return condition cannot be deleted. Deactivate it instead.");
    }

    private void validateActiveSet(EquipmentSet set) {
        var components = em.createQuery("select c from EquipmentSetComponent c where c.equipmentSet.id = :id", EquipmentSetComponent.class)
            .setParameter("id", set.id).getResultList();
        for (var component : components) {
            validateSetComponentScope(set, component.asset, component.balance);
            if (component.asset != null) {
                long uses = em.createQuery("select count(c) from EquipmentSetComponent c where c.asset.id = :asset and c.equipmentSet.active = true and c.equipmentSet.id <> :set", Long.class)
                    .setParameter("asset", component.asset.id).setParameter("set", set.id).getSingleResult();
                if (uses > 0) bad("An individual asset can belong to only one active equipment set.");
            } else if (component.quantity.compareTo(physical(component.balance)) > 0) {
                bad("Component quantity cannot exceed the physical stock balance.");
            }
        }
    }

    private static void validateSetComponentScope(EquipmentSet set, AssetItem asset, StockBalance balance) {
        StockLocation location = asset == null ? balance.location : asset.location;
        if (!Objects.equals(location.organization.id, set.organization.id))
            bad("The component must belong to the equipment set organization.");
        if (set.unit != null && (location.unit == null || !Objects.equals(location.unit.id, set.unit.id)))
            bad("The component must belong to the equipment set unit.");
    }

    private static BigDecimal physical(StockBalance balance) {
        return balance.available.add(balance.reserved).add(balance.blocked);
    }

    private void requireModelValues(ItemModel model) {
        for (var binding : requiredBindings(model.category.id, false)) {
            if (em.createQuery("select count(v) from ModelCharacteristicValue v where v.model.id = :model and v.characteristic.id = :characteristic", Long.class)
                    .setParameter("model", model.id).setParameter("characteristic", binding.characteristic.id).getSingleResult() == 0)
                bad("Required model characteristic missing: " + binding.characteristic.name + ".");
        }
    }

    private List<CategoryCharacteristic> requiredBindings(long categoryId, boolean perItem) {
        return em.createQuery("select c from CategoryCharacteristic c where c.category.id = :category and c.requiredValue = true and c.perItem = :perItem", CategoryCharacteristic.class)
            .setParameter("category", categoryId).setParameter("perItem", perItem).getResultList();
    }

    private CategoryCharacteristic binding(long categoryId, long characteristicId) {
        return em.createQuery("select c from CategoryCharacteristic c where c.category.id = :category and c.characteristic.id = :characteristic", CategoryCharacteristic.class)
            .setParameter("category", categoryId).setParameter("characteristic", characteristicId).getResultStream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Characteristic is not configured for this category."));
    }

    private void validateValue(TechnicalCharacteristic characteristic, String value) {
        try {
            switch (characteristic.dataType) {
                case "DECIMAL" -> new BigDecimal(value);
                case "INTEGER" -> new BigInteger(value);
                case "DATE" -> LocalDate.parse(value);
                case "BOOLEAN" -> { if (!Set.of("true", "false").contains(value)) throw new IllegalArgumentException(); }
                default -> { }
            }
        } catch (RuntimeException ex) { bad("Invalid " + characteristic.dataType.toLowerCase(Locale.ROOT) + " value for " + characteristic.name + "."); }
    }

    private long categoryUsage(long id) {
        return count("select count(a) from AssetItem a where a.model.category.id = :id", id)
            + count("select count(l) from StockLot l where l.model.category.id = :id", id);
    }
    private long typeUsage(long id) {
        return count("select count(m) from ItemModel m where m.armamentType.id = :id", id)
            + count("select count(c) from ArmamentClassification c where c.type.id = :id", id);
    }
    private long classificationUsage(long id) {
        return count("select count(m) from ItemModel m where m.armamentClassification.id = :id", id);
    }
    private long modelUsage(long id) {
        return count("select count(a) from AssetItem a where a.model.id = :id", id)
            + count("select count(l) from StockLot l where l.model.id = :id", id);
    }
    private long specificationUsage(long id) {
        return count("select count(v) from FirearmSpecification v where v.model.id = :id", id)
            + count("select count(v) from AmmunitionSpecification v where v.model.id = :id", id)
            + count("select count(v) from GrenadeSpecification v where v.model.id = :id", id)
            + count("select count(v) from SpraySpecification v where v.model.id = :id", id)
            + count("select count(v) from BallisticProtectionSpecification v where v.model.id = :id", id)
            + count("select count(v) from ElectricalDeviceSpecification v where v.model.id = :id", id)
            + count("select count(v) from OpticalSpecification v where v.model.id = :id", id);
    }
    private static void requireOne(Object asset, Object lot, String label) {
        if ((asset == null) == (lot == null)) bad(label + " requires exactly one individual asset or stock lot.");
    }
    private String parameterName(ArmamentParameter parameter, String expectedType, String label, boolean required) {
        if (parameter == null) {
            if (required) bad(label + " is required.");
            return null;
        }
        if (!expectedType.equals(parameter.parameterType))
            bad(label + " belongs to an invalid parameter type.");
        if (!Boolean.TRUE.equals(parameter.active))
            bad("Select an active " + label.toLowerCase(Locale.ROOT) + ".");
        return parameter.name;
    }

    private long parameterUsage(long id) {
        long total = 0;
        for (String query : List.of(
            "select count(v) from FirearmSpecification v where v.caliberRef.id = :id",
            "select count(v) from AmmunitionSpecification v where v.caliberRef.id = :id",
            "select count(v) from AmmunitionSpecification v where v.ammunitionTypeRef.id = :id",
            "select count(v) from AmmunitionSpecification v where v.projectileTypeRef.id = :id",
            "select count(v) from AmmunitionSpecification v where v.caseTypeRef.id = :id",
            "select count(v) from AmmunitionSpecification v where v.primerTypeRef.id = :id",
            "select count(v) from GrenadeSpecification v where v.grenadeTypeRef.id = :id",
            "select count(v) from GrenadeSpecification v where v.agentRef.id = :id",
            "select count(v) from GrenadeSpecification v where v.compositionRef.id = :id",
            "select count(v) from SpraySpecification v where v.agentRef.id = :id",
            "select count(v) from SpraySpecification v where v.compositionRef.id = :id",
            "select count(v) from BallisticProtectionSpecification v where v.protectionTypeRef.id = :id",
            "select count(v) from BallisticProtectionSpecification v where v.protectionLevelRef.id = :id",
            "select count(v) from BallisticProtectionSpecification v where v.materialRef.id = :id",
            "select count(v) from BallisticProtectionSpecification v where v.sizeRef.id = :id",
            "select count(v) from ElectricalDeviceSpecification v where v.cartridgeTypeRef.id = :id",
            "select count(v) from OpticalSpecification v where v.opticalTypeRef.id = :id",
            "select count(v) from HelmetSpecification v where v.protectionLevelRef.id = :id",
            "select count(v) from HelmetSpecification v where v.materialRef.id = :id",
            "select count(v) from HelmetSpecification v where v.sizeRef.id = :id",
            "select count(v) from ShieldSpecification v where v.shieldTypeRef.id = :id",
            "select count(v) from ShieldSpecification v where v.protectionLevelRef.id = :id",
            "select count(v) from ShieldSpecification v where v.materialRef.id = :id",
            "select count(v) from RestraintSpecification v where v.materialRef.id = :id",
            "select count(v) from RestraintSpecification v where v.lockingMechanismRef.id = :id",
            "select count(v) from AccessoryComponentSpecification v where v.componentTypeRef.id = :id",
            "select count(v) from AccessoryComponentSpecification v where v.compatibilityRef.id = :id",
            "select count(v) from AccessoryComponentSpecification v where v.interfaceRef.id = :id",
            "select count(v) from TacticalEquipmentSpecification v where v.materialRef.id = :id",
            "select count(v) from TacticalEquipmentSpecification v where v.sizeRef.id = :id"
        )) {
            total += count(query, id);
        }
        return total;
    }

    private static void validateScope(Organization organization, OrganizationalUnit unit, AssetItem asset, StockLot lot) {
        StockLocation location = asset != null ? asset.location : lot.openingLocation;
        if (!location.organization.id.equals(organization.id)) bad("The controlled item must belong to the selected organization.");
        if (unit != null && (location.unit == null || !location.unit.id.equals(unit.id)))
            bad("The controlled item must belong to the selected unit.");
        if (unit != null && !unit.organization.id.equals(organization.id)) bad("Unit must belong to the selected organization.");
    }
    private long count(String query, long id) { return em.createQuery(query, Long.class).setParameter("id", id).getSingleResult(); }
    private static boolean workflowStatus(Object status) {
        return status != null && AssetStatus.workflowManagedCodes().contains(status.toString());
    }
    private static void registry(String code, Integer order) {
        if (code == null || !code.trim().toUpperCase(Locale.ROOT).matches("[A-Z][A-Z0-9_]{1,49}")) bad("Code must use 2 to 50 uppercase letters, numbers or underscores.");
        if (order == null || order < 0) bad("Display order cannot be negative.");
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    private void positiveOptional(BigDecimal value, String label) {
        if (value != null && value.signum() <= 0) bad(label + " must be greater than zero.");
    }
}
