package com.comandos.inventory.service;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.*;
import com.comandos.reservation.model.ReservationStatusType;
import com.comandos.reconciliation.model.InventoryCountResultType;
import com.comandos.reconciliation.model.InventoryCountStatusType;
import com.comandos.sales.model.SaleReturnReasonType;
import com.comandos.custody.model.CustodyReturnConditionType;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class InventoryCatalog {
    private InventoryCatalog() {}
    public record Field(String name, String label, String type, boolean required,
                        String reference, List<String> choices, boolean readOnly, boolean createOnly) {
        public String property() {
            return reference == null ? name : name.substring(0, name.length() - 2);
        }
    }
    public record Resource(String key, String label, String group, Class<? extends CoreEntity> entity,
                           List<Field> fields, boolean readOnly) {}
    public static final List<Resource> RESOURCES = List.of(
        new Resource("custody-return-condition-types", "Custody return condition types", "Reference data", CustodyReturnConditionType.class, List.of(
            new Field("code", "Code", "text", true, null, List.of(), false, true),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false),
            new Field("active", "Active", "boolean", true, null, List.of(), false, false),
            new Field("blocksAvailability", "Blocks availability", "boolean", true, null, List.of(), false, false),
            new Field("displayOrder", "Display order", "integer", true, null, List.of(), false, false),
            new Field("systemProtected", "System protected", "boolean", true, null, List.of(), true, false)
        ), false),
        new Resource("sale-return-reason-types", "Sale return reason types", "Reference data", SaleReturnReasonType.class, List.of(
            new Field("code", "Code", "text", true, null, List.of(), false, true),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false),
            new Field("active", "Active", "boolean", true, null, List.of(), false, false),
            new Field("displayOrder", "Display order", "integer", true, null, List.of(), false, false),
            new Field("systemProtected", "System protected", "boolean", true, null, List.of(), true, false)
        ), false),
        new Resource("inventory-count-status-types", "Inventory count status types", "Reference data", InventoryCountStatusType.class, List.of(
            new Field("code", "Code", "text", true, null, List.of(), false, true),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false),
            new Field("active", "Active", "boolean", true, null, List.of(), false, false),
            new Field("terminal", "Terminal status", "boolean", true, null, List.of(), false, false),
            new Field("displayOrder", "Display order", "integer", true, null, List.of(), false, false),
            new Field("systemProtected", "System protected", "boolean", true, null, List.of(), true, false)
        ), false),
        new Resource("inventory-count-result-types", "Inventory count result types", "Reference data", InventoryCountResultType.class, List.of(
            new Field("code", "Code", "text", true, null, List.of(), false, true),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false),
            new Field("active", "Active", "boolean", true, null, List.of(), false, false),
            new Field("displayOrder", "Display order", "integer", true, null, List.of(), false, false),
            new Field("systemProtected", "System protected", "boolean", true, null, List.of(), true, false)
        ), false),
        new Resource("reservation-status-types", "Reservation status types", "Reference data", ReservationStatusType.class, List.of(
            new Field("code", "Code", "text", true, null, List.of(), false, true),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false),
            new Field("active", "Active", "boolean", true, null, List.of(), false, false),
            new Field("terminal", "Terminal status", "boolean", true, null, List.of(), false, false),
            new Field("displayOrder", "Display order", "integer", true, null, List.of(), false, false),
            new Field("systemProtected", "System protected", "boolean", true, null, List.of(), true, false)
        ), false),
        new Resource("armament-types", "Armament types", "Catalog", ArmamentType.class, List.of(
            new Field("code", "Code", "text", true, null, List.of(), false, true),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false),
            new Field("active", "Active", "boolean", true, null, List.of(), false, false),
            new Field("categoryId", "category", "reference", true, "categories", List.of(), false, false)
        ), false),
        new Resource("armament-classifications", "Armament classifications", "Catalog", ArmamentClassification.class, List.of(
            new Field("code", "Code", "text", true, null, List.of(), false, true),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false),
            new Field("active", "Active", "boolean", true, null, List.of(), false, false),
            new Field("typeId", "type", "reference", true, "armament-types", List.of(), false, false)
        ), false),
        new Resource("categories", "Item categories", "Catalog", ItemCategory.class, List.of(
            new Field("parentCategoryId", "Parent category", "reference", false, "categories", List.of(), false, false),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("family", "Equipment family", "choice", true, null, List.of("GENERAL", "FIREARM", "AMMUNITION", "GRENADE", "BALLISTIC_PROTECTION", "HELMET", "SHIELD", "SPRAY", "ELECTRICAL_DEVICE", "RESTRAINT", "OPTICAL", "ACCESSORY_COMPONENT", "TACTICAL_EQUIPMENT", "OTHER"), false, false),
            new Field("serialized", "Serialized", "boolean", true, null, List.of(), false, false),
            new Field("lotControlled", "Lot controlled", "boolean", true, null, List.of(), false, false),
            new Field("consumable", "Consumable", "boolean", true, null, List.of(), false, false)
        ), false),
        new Resource("brands", "Brands", "Catalog", Brand.class, List.of(
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("manufacturer", "Manufacturer", "text", true, null, List.of(), false, false),
            new Field("manufacturingCountryCode", "Country of manufacture (ISO)", "choice", false, null,
                java.util.Arrays.stream(java.util.Locale.getISOCountries()).sorted().toList(), false, false)
        ), false),
        new Resource("models", "Item models", "Catalog", ItemModel.class, List.of(
            new Field("armamentTypeId", "Armament type", "reference", false, "armament-types", List.of(), false, false),
            new Field("armamentClassificationId", "Armament classification", "reference", false, "armament-classifications", List.of(), false, false),
            new Field("categoryId", "Category", "reference", true, "categories", List.of(), false, false),
            new Field("brandId", "Brand", "reference", true, "brands", List.of(), false, false),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("unitOfMeasure", "Unit of measure", "text", true, null, List.of(), false, false),
            new Field("manufacturerCode", "Manufacturer code", "text", false, null, List.of(), false, false),
            new Field("sku", "SKU", "text", true, null, List.of(), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false),
            new Field("listPrice", "List price", "decimal", true, null, List.of(), false, false)
        ), false),
        new Resource("characteristics", "Technical characteristics", "Specifications", TechnicalCharacteristic.class, List.of(
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("dataType", "Data type", "choice", true, null, List.of("TEXT", "DECIMAL", "INTEGER", "BOOLEAN", "DATE"), false, false),
            new Field("unitOfMeasure", "Unit of measure", "text", false, null, List.of(), false, false)
        ), false),
        new Resource("category-characteristics", "Category characteristics", "Specifications", CategoryCharacteristic.class, List.of(
            new Field("categoryId", "Category", "reference", true, "categories", List.of(), false, true),
            new Field("characteristicId", "Characteristic", "reference", true, "characteristics", List.of(), false, true),
            new Field("requiredValue", "Required value", "boolean", true, null, List.of(), false, false),
            new Field("perItem", "Per individual item", "boolean", true, null, List.of(), false, false)
        ), false),
        new Resource("model-values", "Model characteristics", "Specifications", ModelCharacteristicValue.class, List.of(
            new Field("modelId", "Model", "reference", true, "models", List.of(), false, true),
            new Field("characteristicId", "Characteristic", "reference", true, "characteristics", List.of(), false, true),
            new Field("value", "Value", "text", true, null, List.of(), false, false)
        ), false),
        new Resource("firearm-specifications", "Firearm specifications", "Controlled equipment", FirearmSpecification.class, List.of(
            new Field("modelId", "Firearm model", "reference", true, "models", List.of(), false, true),
            new Field("caliber", "Caliber", "text", true, null, List.of(), false, false),
            new Field("operatingMechanism", "Operating mechanism", "choice", true, null,
                List.of("SINGLE_SHOT", "BOLT_ACTION", "LEVER_ACTION", "PUMP_ACTION", "SEMI_AUTOMATIC", "AUTOMATIC", "REVOLVER"), false, false),
            new Field("capacity", "Capacity", "integer", true, null, List.of(), false, false),
            new Field("barrelLength", "Barrel length (mm)", "decimal", true, null, List.of(), false, false)
        ), false),
        new Resource("ammunition-specifications", "Ammunition specifications", "Controlled equipment", AmmunitionSpecification.class, List.of(
            new Field("modelId", "Ammunition model", "reference", true, "models", List.of(), false, true),
            new Field("caliber", "Caliber", "text", true, null, List.of(), false, false),
            new Field("ammunitionType", "Ammunition type", "text", true, null, List.of(), false, false),
            new Field("lethalityClassification", "Lethality classification", "choice", true, null,
                List.of("LETHAL", "LESS_LETHAL"), false, false),
            new Field("projectileType", "Projectile type", "text", true, null, List.of(), false, false),
            new Field("caseType", "Case type", "text", true, null, List.of(), false, false),
            new Field("primerType", "Primer type", "text", true, null, List.of(), false, false)
        ), false),
        new Resource("grenade-specifications", "Grenade specifications", "Controlled equipment", GrenadeSpecification.class, List.of(
            new Field("modelId", "Grenade model", "reference", true, "models", List.of(), false, true),
            new Field("grenadeType", "Grenade type", "text", true, null, List.of(), false, false),
            new Field("agent", "Agent", "text", true, null, List.of(), false, false),
            new Field("composition", "Composition", "text", true, null, List.of(), false, false),
            new Field("shelfLifeMonths", "Shelf life (months)", "integer", true, null, List.of(), false, false),
            new Field("delaySeconds", "Delay time (seconds)", "integer", true, null, List.of(), false, false),
            new Field("safetyRadius", "Safety radius (m)", "decimal", true, null, List.of(), false, false)
        ), false),
        new Resource("spray-specifications", "Spray specifications", "Controlled equipment", SpraySpecification.class, List.of(
            new Field("modelId", "Spray model", "reference", true, "models", List.of(), false, true),
            new Field("agent", "Agent", "text", true, null, List.of(), false, false),
            new Field("composition", "Composition", "text", true, null, List.of(), false, false),
            new Field("shelfLifeMonths", "Shelf life (months)", "integer", true, null, List.of(), false, false),
            new Field("concentration", "Concentration (%)", "decimal", true, null, List.of(), false, false),
            new Field("volumeMl", "Volume (mL)", "decimal", true, null, List.of(), false, false),
            new Field("rangeMeters", "Range (m)", "decimal", true, null, List.of(), false, false)
        ), false),
        new Resource("ballistic-protection-specifications", "Ballistic protection specifications", "Controlled equipment", BallisticProtectionSpecification.class, List.of(
            new Field("modelId", "Ballistic protection model", "reference", true, "models", List.of(), false, true),
            new Field("protectionType", "Protection type", "text", true, null, List.of(), false, false),
            new Field("protectionLevel", "Protection level", "text", true, null, List.of(), false, false),
            new Field("material", "Material", "text", true, null, List.of(), false, false),
            new Field("certification", "Certification", "text", true, null, List.of(), false, false),
            new Field("size", "Size", "text", true, null, List.of(), false, false),
            new Field("serviceLifeMonths", "Service life (months)", "integer", true, null, List.of(), false, false)
        ), false),
        new Resource("electrical-device-specifications", "Electrical device specifications", "Controlled equipment", ElectricalDeviceSpecification.class, List.of(
            new Field("modelId", "Electrical device model", "reference", true, "models", List.of(), false, true),
            new Field("voltage", "Voltage (V)", "decimal", true, null, List.of(), false, false),
            new Field("cycles", "Cycles", "integer", true, null, List.of(), false, false),
            new Field("cartridgeType", "Cartridge type", "text", true, null, List.of(), false, false)
        ), false),
        new Resource("optical-specifications", "Optical specifications", "Controlled equipment", OpticalSpecification.class, List.of(
            new Field("modelId", "Optical model", "reference", true, "models", List.of(), false, true),
            new Field("opticalType", "Optical type", "text", true, null, List.of(), false, false),
            new Field("minimumMagnification", "Minimum magnification", "decimal", true, null, List.of(), false, false),
            new Field("maximumMagnification", "Maximum magnification", "decimal", true, null, List.of(), false, false),
            new Field("objectiveDiameterMm", "Objective diameter (mm)", "decimal", false, null, List.of(), false, false),
            new Field("reticle", "Reticle", "text", true, null, List.of(), false, false),
            new Field("fieldOfView", "Field of view", "text", false, null, List.of(), false, false),
            new Field("nightVision", "Night vision", "boolean", true, null, List.of(), false, false),
            new Field("thermalVision", "Thermal vision", "boolean", true, null, List.of(), false, false)
        ), false),
        new Resource("helmet-specifications", "Helmet specifications", "Controlled equipment", HelmetSpecification.class, List.of(
            new Field("modelId", "Helmet model", "reference", true, "models", List.of(), false, true),
            new Field("protectionLevel", "Protection level", "text", true, null, List.of(), false, false),
            new Field("material", "Material", "text", true, null, List.of(), false, false),
            new Field("size", "Size", "text", true, null, List.of(), false, false),
            new Field("weightGrams", "Weight (g)", "decimal", false, null, List.of(), false, false),
            new Field("certification", "Certification", "text", false, null, List.of(), false, false)
        ), false),
        new Resource("shield-specifications", "Shield specifications", "Controlled equipment", ShieldSpecification.class, List.of(
            new Field("modelId", "Shield model", "reference", true, "models", List.of(), false, true),
            new Field("shieldType", "Shield type", "text", true, null, List.of(), false, false),
            new Field("protectionLevel", "Protection level", "text", false, null, List.of(), false, false),
            new Field("material", "Material", "text", true, null, List.of(), false, false),
            new Field("heightMm", "Height (mm)", "decimal", false, null, List.of(), false, false),
            new Field("widthMm", "Width (mm)", "decimal", false, null, List.of(), false, false),
            new Field("weightGrams", "Weight (g)", "decimal", false, null, List.of(), false, false)
        ), false),
        new Resource("restraint-specifications", "Restraint specifications", "Controlled equipment", RestraintSpecification.class, List.of(
            new Field("modelId", "Restraint model", "reference", true, "models", List.of(), false, true),
            new Field("restraintType", "Restraint type", "choice", true, null, List.of("HANDCUFF", "LEG_IRON", "DISPOSABLE_RESTRAINT", "TRANSPORT_RESTRAINT", "OTHER"), false, false),
            new Field("material", "Material", "text", true, null, List.of(), false, false),
            new Field("lockingMechanism", "Locking mechanism", "text", false, null, List.of(), false, false),
            new Field("doubleLock", "Double lock", "boolean", true, null, List.of(), false, false)
        ), false),
        new Resource("accessory-component-specifications", "Accessory/component specifications", "Controlled equipment", AccessoryComponentSpecification.class, List.of(
            new Field("modelId", "Accessory/component model", "reference", true, "models", List.of(), false, true),
            new Field("componentType", "Component type", "text", true, null, List.of(), false, false),
            new Field("compatibleWith", "Compatibility", "text", false, null, List.of(), false, false),
            new Field("mountingInterface", "Mounting/interface", "text", false, null, List.of(), false, false),
            new Field("controlledComponent", "Controlled component", "boolean", true, null, List.of(), false, false)
        ), false),
        new Resource("tactical-equipment-specifications", "Tactical equipment specifications", "Controlled equipment", TacticalEquipmentSpecification.class, List.of(
            new Field("modelId", "Tactical equipment model", "reference", true, "models", List.of(), false, true),
            new Field("equipmentType", "Equipment type", "text", true, null, List.of(), false, false),
            new Field("material", "Material", "text", false, null, List.of(), false, false),
            new Field("size", "Size", "text", false, null, List.of(), false, false),
            new Field("weightGrams", "Weight (g)", "decimal", false, null, List.of(), false, false),
            new Field("operationalNotes", "Operational notes", "text", false, null, List.of(), false, false)
        ), false),
        new Resource("regulatory-controls", "Regulatory controls", "Controlled equipment", RegulatoryControl.class, List.of(
            new Field("assetId", "Firearm asset", "reference", true, "assets", List.of(), false, true),
            new Field("externalSystem", "External system", "text", true, null, List.of(), false, true),
            new Field("registrationNumber", "Registration number", "text", true, null, List.of(), false, false),
            new Field("status", "Registration status", "choice", true, null,
                List.of("PENDING", "ACTIVE", "SUSPENDED", "CANCELLED", "EXPIRED"), false, false),
            new Field("validUntil", "Valid until", "date", false, null, List.of(), false, false)
        ), false),
        new Resource("expirations", "Expiration controls", "Compliance", ExpirationRecord.class, List.of(
            new Field("organizationId", "Organization", "reference", true, "core/organizations", List.of(), false, true),
            new Field("unitId", "Organizational unit", "reference", false, "core/units", List.of(), false, true),
            new Field("assetId", "Individual asset", "reference", false, "assets", List.of(), false, true),
            new Field("lotId", "Stock lot", "reference", false, "lots", List.of(), false, true),
            new Field("type", "Expiration type", "text", true, null, List.of(), false, false),
            new Field("expirationDate", "Expiration date", "date", true, null, List.of(), false, false),
            new Field("status", "Status", "choice", true, null, List.of("PENDING", "VALID", "DUE_SOON", "EXPIRED", "RENEWED"), false, false)
        ), false),
        new Resource("certifications", "Certifications", "Compliance", CertificationRecord.class, List.of(
            new Field("organizationId", "Organization", "reference", true, "core/organizations", List.of(), false, true),
            new Field("unitId", "Organizational unit", "reference", false, "core/units", List.of(), false, true),
            new Field("assetId", "Individual asset", "reference", false, "assets", List.of(), false, true),
            new Field("lotId", "Stock lot", "reference", false, "lots", List.of(), false, true),
            new Field("type", "Certification type", "text", true, null, List.of(), false, false),
            new Field("number", "Certificate number", "text", true, null, List.of(), false, false),
            new Field("validUntil", "Valid until", "date", true, null, List.of(), false, false),
            new Field("status", "Status", "choice", true, null, List.of("ACTIVE", "SUSPENDED", "EXPIRED", "REVOKED"), false, false)
        ), false),
        new Resource("recalls", "Recalls", "Compliance", Recall.class, List.of(
            new Field("organizationId", "Organization", "reference", true, "core/organizations", List.of(), false, true),
            new Field("unitId", "Organizational unit", "reference", false, "core/units", List.of(), false, true),
            new Field("number", "Recall number", "text", true, null, List.of(), false, true),
            new Field("reason", "Reason", "text", true, null, List.of(), false, false),
            new Field("status", "Status", "choice", true, null, List.of("OPEN", "IN_PROGRESS", "CLOSED", "CANCELLED"), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false)
        ), false),
        new Resource("recall-items", "Recall items", "Compliance", RecallItem.class, List.of(
            new Field("recallId", "Recall", "reference", true, "recalls", List.of(), false, true),
            new Field("assetId", "Individual asset", "reference", false, "assets", List.of(), false, true),
            new Field("lotId", "Stock lot", "reference", false, "lots", List.of(), false, true),
            new Field("action", "Required action", "text", true, null, List.of(), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false)
        ), false),
        new Resource("locations", "Stock locations", "Inventory", StockLocation.class, List.of(
            new Field("organizationId", "Organization", "reference", true, "core/organizations", List.of(), false, false),
            new Field("unitId", "Organizational unit", "reference", false, "core/units", List.of(), false, false),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("type", "Type", "text", true, null, List.of(), false, false),
            new Field("controlled", "Controlled location", "boolean", true, null, List.of(), false, false)
        ), false),
        new Resource("equipment-sets", "Equipment sets", "Inventory", EquipmentSet.class, List.of(
            new Field("organizationId", "Organization", "reference", true, "core/organizations", List.of(), false, true),
            new Field("unitId", "Organizational unit", "reference", false, "core/units", List.of(), false, true),
            new Field("code", "Set code", "text", true, null, List.of(), false, true),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false),
            new Field("active", "Active", "boolean", true, null, List.of(), false, false)
        ), false),
        new Resource("equipment-set-components", "Equipment set components", "Inventory", EquipmentSetComponent.class, List.of(
            new Field("equipmentSetId", "Equipment set", "reference", true, "equipment-sets", List.of(), false, true),
            new Field("assetId", "Individual asset", "reference", false, "assets", List.of(), false, true),
            new Field("balanceId", "Stock balance", "reference", false, "balances", List.of(), false, true),
            new Field("role", "Component role", "text", true, null, List.of(), false, false),
            new Field("quantity", "Quantity", "decimal", true, null, List.of(), false, false)
        ), false),
        new Resource("assets", "Individual assets", "Inventory", AssetItem.class, List.of(
            new Field("modelId", "Model", "reference", true, "models", List.of(), false, true),
            new Field("locationId", "Location", "reference", true, "locations", List.of(), false, true),
            new Field("assetCode", "Asset code", "text", true, null, List.of(), false, true),
            new Field("serialNumber", "Serial number", "text", false, null, List.of(), false, true),
            new Field("internalCode", "Internal code", "text", false, null, List.of(), false, true),
            new Field("condition", "Condition", "choice", true, null, List.of("NEW", "GOOD", "NEEDS_INSPECTION", "DAMAGED"), false, false),
            new Field("status", "Availability", "choice", true, null, List.of("DRAFT", "AVAILABLE", "BLOCKED", "CUSTODIED", "IN_MAINTENANCE", "TRANSFER_PENDING", "MISSING", "RESTRICTED", "SOLD", "DONATED", "DISPOSED"), false, false),
            new Field("validUntil", "Valid until", "date", false, null, List.of(), false, false),
            new Field("currentValue", "Current value", "decimal", true, null, List.of(), false, false)
        ), false),
        new Resource("item-values", "Asset characteristics", "Specifications", ItemCharacteristicValue.class, List.of(
            new Field("assetId", "Asset", "reference", true, "assets", List.of(), false, true),
            new Field("characteristicId", "Characteristic", "reference", true, "characteristics", List.of(), false, true),
            new Field("value", "Value", "text", true, null, List.of(), false, false)
        ), false),
        new Resource("lots", "Stock lots", "Inventory", StockLot.class, List.of(
            new Field("modelId", "Model", "reference", true, "models", List.of(), false, true),
            new Field("openingLocationId", "Opening location", "reference", true, "locations", List.of(), false, true),
            new Field("lotNumber", "Lot number", "text", true, null, List.of(), false, true),
            new Field("initialQuantity", "Opening quantity", "decimal", true, null, List.of(), false, true),
            new Field("openingPackaging", "Opening boxes (boxes x rounds)", "text", false, null, List.of(), true, false),
            new Field("availableQuantity", "Available quantity", "decimal", false, null, List.of(), true, false),
            new Field("validUntil", "Valid until", "date", false, null, List.of(), false, false)
        ), false),
        new Resource("balances", "Stock balances", "Stock history", StockBalance.class, List.of(
            new Field("lotId", "Lot", "reference", true, "lots", List.of(), true, false),
            new Field("locationId", "Location", "reference", true, "locations", List.of(), true, false),
            new Field("available", "Available", "decimal", true, null, List.of(), true, false),
            new Field("reserved", "Reserved", "decimal", true, null, List.of(), true, false),
            new Field("blocked", "Blocked", "decimal", true, null, List.of(), true, false)
        ), true),
        new Resource("movements", "Stock movements", "Stock history", StockMovement.class, List.of(
            new Field("assetId", "Asset", "reference", false, "assets", List.of(), true, false),
            new Field("lotId", "Lot", "reference", false, "lots", List.of(), true, false),
            new Field("locationId", "Location", "reference", true, "locations", List.of(), true, false),
            new Field("nature", "Nature", "text", true, null, List.of(), true, false),
            new Field("quantity", "Quantity", "decimal", true, null, List.of(), true, false),
            new Field("movedAt", "Recorded at", "datetime-local", true, null, List.of(), true, false)
        ), true)
    );
    public static Resource get(String key) {
        return RESOURCES.stream().filter(r -> r.key().equals(key)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory resource not found."));
    }
}
