package com.comandos.inventory.service;
import com.fariamiguel.app.inventory.service.InventoryCatalogContributor;
import com.fariamiguel.app.inventory.service.InventoryCatalog;

import com.fariamiguel.app.inventory.service.InventoryCatalog.*;
import com.fariamiguel.app.inventory.model.*;
import com.comandos.inventory.model.*;
import com.fariamiguel.app.reservation.model.ReservationStatusType;
import com.fariamiguel.app.reconciliation.model.InventoryCountResultType;
import com.fariamiguel.app.reconciliation.model.InventoryCountStatusType;
import com.fariamiguel.app.sales.model.SaleReturnReasonType;
import com.comandos.custody.model.CustodyReturnConditionType;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@org.springframework.stereotype.Component
public class ComandosInventoryCatalogContributor implements InventoryCatalogContributor {
    private static Resource parameter(String key, String label, String discriminator) {
        return new Resource(key, label, "Reference data", ArmamentParameter.class, List.of(
            new Field("code", "Code", "text", true, null, List.of(), false, true),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false),
            new Field("active", "Active", "boolean", true, null, List.of(), false, false)
        ), false, discriminator);
    }

    @Override
    public List<Resource> contribute(List<Resource> generic) {
        var genericByKey = generic.stream().collect(java.util.stream.Collectors.toMap(Resource::key, java.util.function.Function.identity()));
        var resources = new java.util.ArrayList<Resource>(List.of(
        new Resource("custody-return-condition-types", "Custody return condition types", "Reference data", CustodyReturnConditionType.class, List.of(
            new Field("code", "Code", "text", true, null, List.of(), false, true),
            new Field("name", "Name", "text", true, null, List.of(), false, false),
            new Field("description", "Description", "text", false, null, List.of(), false, false),
            new Field("displayOrder", "Display order", "integer", true, null, List.of(), false, false),
            new Field("active", "Active", "boolean", true, null, List.of(), false, false),
            new Field("blocksAvailability", "Blocks availability", "boolean", true, null, List.of(), false, false),
            new Field("systemProtected", "System protected", "boolean", true, null, List.of(), true, false)
        ), false),
        genericByKey.get("sale-return-reason-types"),
        genericByKey.get("inventory-count-status-types"),
        genericByKey.get("inventory-count-result-types"),
        genericByKey.get("reservation-status-types"),
        parameter("calibers", "Calibers", "CALIBER"),
        parameter("ammunition-types", "Ammunition types", "AMMUNITION_TYPE"),
        parameter("projectile-types", "Projectile types", "PROJECTILE_TYPE"),
        parameter("case-types", "Case types", "CASE_TYPE"),
        parameter("primer-types", "Primer types", "PRIMER_TYPE"),
        parameter("grenade-types", "Grenade types", "GRENADE_TYPE"),
        parameter("agents", "Agents", "AGENT"),
        parameter("compositions", "Compositions", "COMPOSITION"),
        parameter("protection-types", "Protection types", "PROTECTION_TYPE"),
        parameter("protection-levels", "Protection levels", "PROTECTION_LEVEL"),
        parameter("materials", "Materials", "MATERIAL"),
        parameter("sizes", "Sizes", "SIZE"),
        parameter("cartridge-types", "Cartridge types", "CARTRIDGE_TYPE"),
        parameter("optical-types", "Optical types", "OPTICAL_TYPE"),
        parameter("shield-types", "Shield types", "SHIELD_TYPE"),
        parameter("locking-mechanisms", "Locking mechanisms", "LOCKING_MECHANISM"),
        parameter("component-types", "Component types", "COMPONENT_TYPE"),
        parameter("compatibilities", "Compatibilities", "COMPATIBILITY"),
        parameter("interfaces", "Interfaces", "INTERFACE"),
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
        customize(genericByKey.get("categories")),
        genericByKey.get("brands"),
        customize(genericByKey.get("models")),
        genericByKey.get("characteristics"),
        genericByKey.get("category-characteristics"),
        genericByKey.get("model-values"),
        new Resource("firearm-specifications", "Firearm specifications", "Controlled equipment", FirearmSpecification.class, List.of(
            new Field("modelId", "Firearm model", "reference", true, "models", List.of(), false, true),
            new Field("caliberRefId", "Caliber", "reference", true, "calibers", List.of(), false, false),
            new Field("operatingMechanism", "Operating mechanism", "choice", true, null,
                List.of("SINGLE_SHOT", "BOLT_ACTION", "LEVER_ACTION", "PUMP_ACTION", "SEMI_AUTOMATIC", "AUTOMATIC", "REVOLVER"), false, false),
            new Field("capacity", "Capacity", "integer", true, null, List.of(), false, false),
            new Field("barrelLength", "Barrel length (mm)", "decimal", true, null, List.of(), false, false)
        ), false),
        new Resource("ammunition-specifications", "Ammunition specifications", "Controlled equipment", AmmunitionSpecification.class, List.of(
            new Field("modelId", "Ammunition model", "reference", true, "models", List.of(), false, true),
            new Field("caliberRefId", "Caliber", "reference", true, "calibers", List.of(), false, false),
            new Field("ammunitionTypeRefId", "Ammunition type", "reference", true, "ammunition-types", List.of(), false, false),
            new Field("lethalityClassification", "Lethality classification", "choice", true, null,
                List.of("LETHAL", "LESS_LETHAL"), false, false),
            new Field("projectileTypeRefId", "Projectile type", "reference", true, "projectile-types", List.of(), false, false),
            new Field("caseTypeRefId", "Case type", "reference", true, "case-types", List.of(), false, false),
            new Field("primerTypeRefId", "Primer type", "reference", true, "primer-types", List.of(), false, false)
        ), false),
        new Resource("grenade-specifications", "Grenade specifications", "Controlled equipment", GrenadeSpecification.class, List.of(
            new Field("modelId", "Grenade model", "reference", true, "models", List.of(), false, true),
            new Field("grenadeTypeRefId", "Grenade type", "reference", true, "grenade-types", List.of(), false, false),
            new Field("agentRefId", "Agent", "reference", true, "agents", List.of(), false, false),
            new Field("compositionRefId", "Composition", "reference", true, "compositions", List.of(), false, false),
            new Field("shelfLifeMonths", "Shelf life (months)", "integer", true, null, List.of(), false, false),
            new Field("delaySeconds", "Delay time (seconds)", "integer", true, null, List.of(), false, false),
            new Field("safetyRadius", "Safety radius (m)", "decimal", true, null, List.of(), false, false)
        ), false),
        new Resource("spray-specifications", "Spray specifications", "Controlled equipment", SpraySpecification.class, List.of(
            new Field("modelId", "Spray model", "reference", true, "models", List.of(), false, true),
            new Field("agentRefId", "Agent", "reference", true, "agents", List.of(), false, false),
            new Field("compositionRefId", "Composition", "reference", true, "compositions", List.of(), false, false),
            new Field("shelfLifeMonths", "Shelf life (months)", "integer", true, null, List.of(), false, false),
            new Field("concentration", "Concentration (%)", "decimal", true, null, List.of(), false, false),
            new Field("volumeMl", "Volume (mL)", "decimal", true, null, List.of(), false, false),
            new Field("rangeMeters", "Range (m)", "decimal", true, null, List.of(), false, false)
        ), false),
        new Resource("ballistic-protection-specifications", "Ballistic protection specifications", "Controlled equipment", BallisticProtectionSpecification.class, List.of(
            new Field("modelId", "Ballistic protection model", "reference", true, "models", List.of(), false, true),
            new Field("protectionTypeRefId", "Protection type", "reference", true, "protection-types", List.of(), false, false),
            new Field("protectionLevelRefId", "Protection level", "reference", true, "protection-levels", List.of(), false, false),
            new Field("materialRefId", "Material", "reference", true, "materials", List.of(), false, false),
            new Field("certification", "Certification", "text", true, null, List.of(), false, false),
            new Field("sizeRefId", "Size", "reference", true, "sizes", List.of(), false, false),
            new Field("serviceLifeMonths", "Service life (months)", "integer", true, null, List.of(), false, false)
        ), false),
        new Resource("electrical-device-specifications", "Electrical device specifications", "Controlled equipment", ElectricalDeviceSpecification.class, List.of(
            new Field("modelId", "Electrical device model", "reference", true, "models", List.of(), false, true),
            new Field("voltage", "Voltage (V)", "decimal", true, null, List.of(), false, false),
            new Field("cycles", "Cycles", "integer", true, null, List.of(), false, false),
            new Field("cartridgeTypeRefId", "Cartridge type", "reference", true, "cartridge-types", List.of(), false, false)
        ), false),
        new Resource("optical-specifications", "Optical specifications", "Controlled equipment", OpticalSpecification.class, List.of(
            new Field("modelId", "Optical model", "reference", true, "models", List.of(), false, true),
            new Field("opticalTypeRefId", "Optical type", "reference", true, "optical-types", List.of(), false, false),
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
            new Field("protectionLevelRefId", "Protection level", "reference", true, "protection-levels", List.of(), false, false),
            new Field("materialRefId", "Material", "reference", true, "materials", List.of(), false, false),
            new Field("sizeRefId", "Size", "reference", true, "sizes", List.of(), false, false),
            new Field("weightGrams", "Weight (g)", "decimal", false, null, List.of(), false, false),
            new Field("certification", "Certification", "text", false, null, List.of(), false, false)
        ), false),
        new Resource("shield-specifications", "Shield specifications", "Controlled equipment", ShieldSpecification.class, List.of(
            new Field("modelId", "Shield model", "reference", true, "models", List.of(), false, true),
            new Field("shieldTypeRefId", "Shield type", "reference", true, "shield-types", List.of(), false, false),
            new Field("protectionLevelRefId", "Protection level", "reference", false, "protection-levels", List.of(), false, false),
            new Field("materialRefId", "Material", "reference", true, "materials", List.of(), false, false),
            new Field("heightMm", "Height (mm)", "decimal", false, null, List.of(), false, false),
            new Field("widthMm", "Width (mm)", "decimal", false, null, List.of(), false, false),
            new Field("weightGrams", "Weight (g)", "decimal", false, null, List.of(), false, false)
        ), false),
        new Resource("restraint-specifications", "Restraint specifications", "Controlled equipment", RestraintSpecification.class, List.of(
            new Field("modelId", "Restraint model", "reference", true, "models", List.of(), false, true),
            new Field("restraintType", "Restraint type", "choice", true, null, List.of("HANDCUFF", "LEG_IRON", "DISPOSABLE_RESTRAINT", "TRANSPORT_RESTRAINT", "OTHER"), false, false),
            new Field("materialRefId", "Material", "reference", true, "materials", List.of(), false, false),
            new Field("lockingMechanismRefId", "Locking mechanism", "reference", false, "locking-mechanisms", List.of(), false, false),
            new Field("doubleLock", "Double lock", "boolean", true, null, List.of(), false, false)
        ), false),
        new Resource("accessory-component-specifications", "Accessory/component specifications", "Controlled equipment", AccessoryComponentSpecification.class, List.of(
            new Field("modelId", "Accessory/component model", "reference", true, "models", List.of(), false, true),
            new Field("componentTypeRefId", "Component type", "reference", true, "component-types", List.of(), false, false),
            new Field("compatibilityRefId", "Compatibility", "reference", false, "compatibilities", List.of(), false, false),
            new Field("interfaceRefId", "Mounting/interface", "reference", false, "interfaces", List.of(), false, false),
            new Field("controlledComponent", "Controlled component", "boolean", true, null, List.of(), false, false)
        ), false),
        new Resource("tactical-equipment-specifications", "Tactical equipment specifications", "Controlled equipment", TacticalEquipmentSpecification.class, List.of(
            new Field("modelId", "Tactical equipment model", "reference", true, "models", List.of(), false, true),
            new Field("equipmentType", "Equipment type", "text", true, null, List.of(), false, false),
            new Field("materialRefId", "Material", "reference", false, "materials", List.of(), false, false),
            new Field("sizeRefId", "Size", "reference", false, "sizes", List.of(), false, false),
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
        genericByKey.get("expirations"),
        genericByKey.get("certifications"),
        genericByKey.get("recalls"),
        genericByKey.get("recall-items"),
        genericByKey.get("locations"),
        genericByKey.get("equipment-sets"),
        genericByKey.get("equipment-set-components"),
        customize(genericByKey.get("assets")),
        genericByKey.get("item-values"),
        genericByKey.get("lots"),
        genericByKey.get("balances"),
        genericByKey.get("movements")
    ));
        var keys = resources.stream().map(Resource::key).collect(java.util.stream.Collectors.toSet());
        generic.stream().filter(resource -> !keys.contains(resource.key())).forEach(resources::add);
        return List.copyOf(resources);
    }
    private Resource customize(Resource resource) {
        var fields = new java.util.LinkedHashMap<String, Field>();
        for (var field : resource.fields()) fields.put(field.name(), field);
        List<String> order;
        switch (resource.key()) {
            case "assets" -> {
                fields.put("status", new Field("status", "Availability", "choice", true, null, SecurityAssetStatus.codes(), false, false));
                order = List.of("modelId", "locationId", "assetCode", "serialNumber", "internalCode", "condition", "status", "validUntil", "currentValue");
            }
            case "models" -> {
                fields.put("armamentTypeId", new Field("armamentTypeId", "Armament type", "reference", false, "armament-types", List.of(), false, false));
                fields.put("armamentClassificationId", new Field("armamentClassificationId", "Armament classification", "reference", false, "armament-classifications", List.of(), false, false));
                order = List.of("armamentTypeId", "armamentClassificationId", "categoryId", "brandId", "name", "unitOfMeasure", "manufacturerCode", "sku", "description", "listPrice");
            }
            case "categories" -> {
                fields.put("family", new Field("family", "Equipment family", "choice", true, null, List.of("GENERAL", "FIREARM", "AMMUNITION", "GRENADE", "BALLISTIC_PROTECTION", "HELMET", "SHIELD", "SPRAY", "ELECTRICAL_DEVICE", "RESTRAINT", "OPTICAL", "ACCESSORY_COMPONENT", "TACTICAL_EQUIPMENT", "OTHER"), false, false));
                order = List.of("parentCategoryId", "name", "family", "serialized", "lotControlled", "consumable");
            }
            default -> throw new IllegalArgumentException(resource.key());
        }
        return new Resource(resource.key(), resource.label(), resource.group(), resource.entity(),
            order.stream().map(fields::get).toList(), resource.readOnly(), resource.discriminator());
    }
}