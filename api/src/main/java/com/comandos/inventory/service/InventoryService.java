package com.comandos.inventory.service;

import com.comandos.core.model.CoreEntity;
import com.comandos.core.service.CoreCatalog;
import com.comandos.core.service.ProductMasterDataReferenceSynchronizer;
import com.comandos.core.service.MasterDataReferenceService;
import com.comandos.inventory.model.*;
import com.comandos.reservation.model.ReservationStatusType;
import com.comandos.reconciliation.model.InventoryCountResultType;
import com.comandos.reconciliation.model.InventoryCountStatusType;
import com.comandos.sales.model.SaleReturnReasonType;
import com.comandos.custody.model.CustodyReturnConditionType;
import com.comandos.security.service.AccessPolicy;
import com.comandos.audit.service.AuditService;
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
@Transactional(readOnly = true)
public class InventoryService {
    private final EntityManager em;
    private final InventoryRules rules;
    private final AccessPolicy access;
    private final AuditService audit;
    private final ProductMasterDataReferenceSynchronizer masterDataReferences;
    private final MasterDataReferenceService canonicalReferences;
    private final boolean canonicalProductReferenceReadEnabled;

    @org.springframework.beans.factory.annotation.Autowired
    public InventoryService(
            EntityManager em,
            InventoryRules rules,
            AccessPolicy access,
            AuditService audit,
            ProductMasterDataReferenceSynchronizer masterDataReferences,
            MasterDataReferenceService canonicalReferences,
            @org.springframework.beans.factory.annotation.Value(
                "${comandos.master-data.product-reference-primary-read.enabled:false}"
            ) boolean canonicalProductReferenceReadEnabled) {
        this.em = em;
        this.rules = rules;
        this.access = access;
        this.audit = audit;
        this.masterDataReferences = masterDataReferences;
        this.canonicalReferences = canonicalReferences;
        this.canonicalProductReferenceReadEnabled = canonicalProductReferenceReadEnabled;
    }

    InventoryService(
            EntityManager em,
            InventoryRules rules,
            AccessPolicy access,
            AuditService audit,
            ProductMasterDataReferenceSynchronizer masterDataReferences) {
        this(em, rules, access, audit, masterDataReferences, null, false);
    }


    public record PageResult(List<Map<String, Object>> content, long totalElements, int page, int size) {}

    public PageResult list(
            String resource,
            String search,
            int page,
            int size,
            Long organizationId,
            Map<String, String> requestParams) {

        var spec = InventoryCatalog.get(resource);

        if (page < 0 || page > 100000 || size < 1 || size > 100) {
            bad("Invalid pagination.");
        }

        List<String> clauses = new ArrayList<>();
        Map<String, Object> parameters = new LinkedHashMap<>();

        if (spec.discriminator() != null) {
            clauses.add("e.parameterType = :resourceDiscriminator");
            parameters.put("resourceDiscriminator", spec.discriminator());
        }

        // Assets inherit their organizational unit from their current location.
        String unit = requestParams.get("filter.unit");
        if ("assets".equals(resource) && unit != null && !unit.isBlank()) {
            clauses.add("(lower(e.location.unit.name) like :assetUnit escape '!' or lower(e.location.unit.code) like :assetUnit escape '!')");
            parameters.put("assetUnit", likeTerm(unit));
        }
        String assetId = requestParams.get("assetId");
        if ("movements".equals(resource) && assetId != null) {
            clauses.add("e.asset.id = :assetId");
            parameters.put("assetId", integer(assetId, false));
        }

        String lotId = requestParams.get("lotId");
        if ("movements".equals(resource) && lotId != null) {
            clauses.add("e.lot.id = :lotId");
            parameters.put("lotId", integer(lotId, false));
        }

        String locationId = requestParams.get("locationId");
        if ("movements".equals(resource) && locationId != null) {
            clauses.add("e.location.id = :locationId");
            parameters.put("locationId", integer(locationId, false));
        }

        // The persisted model -> category -> family relationship also covers legacy models
        // without an optional armament type or classification.
        String modelFamily = requestParams.get("filter.modelFamily");
        if ("models".equals(resource) && modelFamily != null && !modelFamily.isBlank()) {
            var families = InventoryCatalog.get("categories").fields().stream()
                .filter(field -> field.name().equals("family")).findFirst().orElseThrow().choices();
            if (!families.contains(modelFamily)) bad("Invalid equipment family.");
            clauses.add("e.category.family = :modelFamily");
            parameters.put("modelFamily", modelFamily);
        }

        if (search != null && !search.isBlank()) {
            List<String> expressions =
                new ArrayList<>(List.of("cast(e.id as string)"));

            spec.fields().stream()
                .filter(field ->
                    Set.of("text", "choice").contains(field.type())
                )
                .forEach(field ->
                    expressions.add("lower(e." + field.property() + ")")
                );

            clauses.add(
                "(" +
                String.join(
                    " or ",
                    expressions.stream()
                        .map(expression ->
                            expression + " like :globalSearch escape '!'"
                        )
                        .toList()
                ) +
                ")"
            );

            parameters.put("globalSearch", likeTerm(search));
        }

        int filterIndex = 0;

        for (var entry : requestParams.entrySet()) {
            if (!entry.getKey().startsWith("filter.")) {
                continue;
            }

            String fieldName =
                entry.getKey().substring("filter.".length());

            String value =
                entry.getValue() == null
                    ? ""
                    : entry.getValue().trim();

            if (value.isBlank()) {
                continue;
            }

            String parameter = "columnFilter" + filterIndex++;

            if ("id".equals(fieldName)) {
                clauses.add(
                    "cast(e.id as string) like :" +
                    parameter +
                    " escape '!'"
                );
                parameters.put(parameter, likeTerm(value));
                continue;
            }

            var field = spec.fields().stream()
                .filter(candidate -> candidate.name().equals(fieldName))
                .findFirst()
                .orElse(null);

            if (field == null) {
                continue;
            }

            List<String> expressions = new ArrayList<>();

            if ("reference".equals(field.type())) {
                String relation = "e." + field.property();

                expressions.add("cast(" + relation + ".id as string)");

                if (
                    field.reference() != null &&
                    field.reference().startsWith("core/")
                ) {
                    var target =
                        CoreCatalog.get(
                            field.reference().substring("core/".length())
                        );

                    target.fields().stream()
                        .filter(targetField ->
                            Set.of("text", "email", "choice")
                                .contains(targetField.type())
                        )
                        .limit(4)
                        .forEach(targetField ->
                            expressions.add(
                                "lower(" +
                                relation +
                                "." +
                                targetField.property() +
                                ")"
                            )
                        );
                }
                else if (field.reference() != null) {
                    var target = InventoryCatalog.get(field.reference());

                    target.fields().stream()
                        .filter(targetField ->
                            Set.of("text", "choice")
                                .contains(targetField.type())
                        )
                        .limit(4)
                        .forEach(targetField ->
                            expressions.add(
                                "lower(" +
                                relation +
                                "." +
                                targetField.property() +
                                ")"
                            )
                        );
                }
            }
            else {
                expressions.add(
                    "lower(cast(e." +
                    field.property() +
                    " as string))"
                );
            }

            clauses.add(
                "(" +
                String.join(
                    " or ",
                    expressions.stream()
                        .map(expression ->
                            expression +
                            " like :" +
                            parameter +
                            " escape '!'"
                        )
                        .toList()
                ) +
                ")"
            );

            parameters.put(parameter, likeTerm(value));
        }

        String organizationPath =
            switch (resource) {
                case "locations" ->
                    canonicalProductReferenceReadEnabled
                        ? "e.organizationCanonicalId"
                        : "e.organizationLegacyId";
                case "assets",
                     "balances",
                     "movements" ->
                    canonicalProductReferenceReadEnabled
                        ? "e.location.organizationCanonicalId"
                        : "e.location.organizationLegacyId";
                case "regulatory-controls" ->
                    canonicalProductReferenceReadEnabled
                        ? "e.asset.location.organizationCanonicalId"
                        : "e.asset.location.organizationLegacyId";
                case "expirations",
                     "certifications",
                     "recalls" ->
                    "e.organization.id";
                case "recall-items" ->
                    "e.recall.organization.id";
                case "equipment-sets" ->
                    "e.organization.id";
                case "equipment-set-components" ->
                    "e.equipmentSet.organization.id";
                case "lots" ->
                    canonicalProductReferenceReadEnabled
                        ? "e.openingLocation.organizationCanonicalId"
                        : "e.openingLocation.organizationLegacyId";
                default ->
                    null;
            };

        boolean scoped =
            organizationId != null &&
            organizationPath != null;

        if (scoped) {
            if (canonicalProductReferenceReadEnabled
                    && usesCanonicalLocationScope(resource)) {
                if (canonicalReferences == null) {
                    throw new IllegalStateException(
                        "Canonical product-reference reads require MasterDataReferenceService."
                    );
                }
                String canonicalOrganizationId = canonicalReferences.resolveCanonicalId(
                        MasterDataReferenceService.ORGANIZATION,
                        organizationId)
                    .orElseThrow(() -> new IllegalStateException(
                        "Canonical organization reference is missing for legacy id "
                            + organizationId
                    ));
                clauses.add(organizationPath + " = :organizationCanonicalId");
                parameters.put("organizationCanonicalId", canonicalOrganizationId);
            } else {
                clauses.add(organizationPath + " = :organizationId");
                parameters.put("organizationId", organizationId);
            }
        }

        clauses.add(
            access.predicate("inventory/" + resource, "READ", "e")
        );

        String where =
            " where " +
            String.join(" and ", clauses);

        var query = em.createQuery(
            "select e from " +
            spec.entity().getSimpleName() +
            " e" +
            where +
            " order by e.id",
            spec.entity()
        );

        var count = em.createQuery(
            "select count(e) from " +
            spec.entity().getSimpleName() +
            " e" +
            where,
            Long.class
        );

        parameters.forEach((name, value) -> {
            query.setParameter(name, value);
            count.setParameter(name, value);
        });

        return new PageResult(
            query
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(row -> view(spec, row))
                .toList(),
            count.getSingleResult(),
            page,
            size
        );
    }

    private Map<String, Object> normalizeCanonicalLocationInput(
            String resource,
            Map<String, Object> input) {
        if (!"locations".equals(resource) || input == null
                || (!input.containsKey("organizationCanonicalId")
                    && !input.containsKey("unitCanonicalId"))) {
            return input;
        }
        if (canonicalReferences == null) {
            throw new IllegalStateException(
                "Canonical location input requires MasterDataReferenceService."
            );
        }

        Map<String, Object> normalized = new LinkedHashMap<>(input);
        String organizationCanonicalId = textValue(
            normalized.remove("organizationCanonicalId")
        );
        String unitCanonicalId = textValue(
            normalized.remove("unitCanonicalId")
        );

        if (organizationCanonicalId != null) {
            Long legacyOrganizationId = canonicalReferences.resolveLegacyId(
                    MasterDataReferenceService.ORGANIZATION,
                    organizationCanonicalId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Unknown canonical organization id: "
                        + organizationCanonicalId
                ));
            assertCompatibleLegacyId(
                normalized.get("organizationId"),
                legacyOrganizationId,
                "organization"
            );
            normalized.put("organizationId", legacyOrganizationId);
        }

        if (unitCanonicalId != null) {
            Long legacyUnitId = canonicalReferences.resolveLegacyId(
                    MasterDataReferenceService.UNIT,
                    unitCanonicalId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Unknown canonical unit id: " + unitCanonicalId
                ));
            assertCompatibleLegacyId(
                normalized.get("unitId"),
                legacyUnitId,
                "unit"
            );
            normalized.put("unitId", legacyUnitId);
        }

        return normalized;
    }

    private void assertCompatibleLegacyId(
            Object supplied,
            Long expected,
            String label) {
        if (supplied == null) return;
        Long legacyId = integer(supplied, false);
        if (!Objects.equals(legacyId, expected)) {
            bad("Canonical and legacy " + label + " references disagree.");
        }
    }

    private static String textValue(Object raw) {
        if (raw == null) return null;
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
    }

    private static boolean usesCanonicalLocationScope(String resource) {
        return Set.of(
            "locations",
            "assets",
            "balances",
            "movements",
            "regulatory-controls",
            "lots"
        ).contains(resource);
    }

    private static String likeTerm(String raw) {
        return "%" +
            raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_") +
            "%";
    }

    public Map<String, Object> get(String resource, long id) {
        var spec = InventoryCatalog.get(resource);
        access.requireAny("inventory/" + resource, "READ");
        var entity = find(spec.entity(), id);
        requireResourceDiscriminator(spec, entity);
        access.requireEntity("inventory/" + resource, "READ", entity);
        return view(spec, entity);
    }

    @Transactional
    public Map<String, Object> save(String resource, Long id, Map<String, Object> data) {
        return save(resource, id, data, null);
    }

    @Transactional
    public Map<String, Object> receiveBoxes(Map<String, Object> data, String packaging) {
        return save("lots", null, data, packaging);
    }

    private Map<String, Object> save(String resource, Long id, Map<String, Object> data, String packaging) {
        return save(resource, id, data, packaging, false);
    }

    public String validateAsset(Map<String, Object> data) {
        try { save("assets", null, data, null, true); return null; }
        catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 403) throw ex;
            return ex.getReason();
        }
    }

    private Map<String, Object> save(String resource, Long id, Map<String, Object> data, String packaging, boolean validateOnly) {
        data = normalizeCanonicalLocationInput(resource, data);
        var spec = InventoryCatalog.get(resource);
        String action = id == null ? "CREATE" : "UPDATE";
        access.requireAny("inventory/" + resource, action);
        writable(spec);
        var legacyParameters = legacyParameterFields(resource);
        var allowed = new HashSet<>(spec.fields().stream().filter(f -> !f.readOnly()).map(InventoryCatalog.Field::name).toList());
        allowed.addAll(legacyParameters.values().stream().map(LegacyParameterField::legacyField).toList());
        allowed.add("version");
        if (!allowed.containsAll(data.keySet())) bad("The form contains unsupported or read-only fields.");
        lockCatalog();
        rules.lockOrganization(spec, data);
        CoreEntity entity;
        try { entity = id == null ? spec.entity().getConstructor().newInstance() : find(spec.entity(), id); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
        if (id != null) {
            requireResourceDiscriminator(spec, entity);
            access.requireEntity("inventory/" + resource, action, entity);
        }
        if (id == null && entity instanceof ArmamentParameter parameter && spec.discriminator() != null) {
            parameter.parameterType = spec.discriminator();
        }
        Map<String, Object> previous = id == null ? Map.of() : view(spec, entity);
        if (id != null && !Objects.equals(entity.version, integer(data.get("version"), true))) conflict("This record has changed. Reload before saving.");

        applyLegacyParameterFields(entity, spec, legacyParameters, data);

        for (var field : spec.fields()) {
            if (field.readOnly()) continue;
            LegacyParameterField legacyParameter = legacyParameters.get(field.name());
            if (legacyParameter != null
                    && !data.containsKey(field.name())
                    && data.containsKey(legacyParameter.legacyField())) {
                continue;
            }
            // Older clients must not erase the optional country on brand updates.
            if (id != null && entity instanceof Brand && field.name().equals("manufacturingCountryCode")
                    && !data.containsKey(field.name())) continue;
            // Preserve references when an older client omits the new model fields.
            if (id != null && entity instanceof ItemModel && !data.containsKey(field.name())
                    && Set.of("armamentTypeId", "armamentClassificationId").contains(field.name())) continue;
            // Preserve descriptions when older clients omit the new field on update.
            if (id != null && (entity instanceof Recall || entity instanceof RecallItem)
                    && field.name().equals("description") && !data.containsKey("description")) continue;
            Object raw = data.get(field.name());
            if (entity instanceof StockLocation location
                    && Set.of("organizationId", "unitId").contains(field.name())) {
                Long legacyId = raw == null ? null : integer(raw, false);
                if (legacyId == null && field.required()) bad(field.label() + " is required.");
                if (legacyId != null) {
                    CoreEntity reference = findReference(field.reference(), legacyId);
                    access.requireEntity(field.reference(), "READ", reference);
                }
                if ("organizationId".equals(field.name())) {
                    location.organizationLegacyId = legacyId;
                } else {
                    location.unitLegacyId = legacyId;
                }
                continue;
            }
            if (entity instanceof AssetItem && Set.of("assetCode", "serialNumber", "internalCode").contains(field.name()) && raw instanceof String text)
                raw = AssetIdentity.normalize(text);
            Object value = parse(field, raw);
            if (value instanceof ArmamentParameter parameter && !Boolean.TRUE.equals(parameter.active)) {
                Object current = id == null ? null : read(entity, field.property());
                boolean unchanged = current instanceof ArmamentParameter existing
                    && Objects.equals(existing.id, parameter.id);
                if (!unchanged) bad("Select an active " + field.label().toLowerCase(Locale.ROOT) + ".");
            }
            if (value instanceof CoreEntity reference) access.requireEntity(
                field.reference().startsWith("core/") ? field.reference() : "inventory/" + field.reference(), "READ", reference);
            if (id != null && field.createOnly()) {
                Object current = read(entity, field.property());
                if (entity instanceof AssetItem && Set.of("assetCode", "serialNumber", "internalCode").contains(field.name()) && current instanceof String text)
                    current = AssetIdentity.normalize(text);
                boolean equal = current instanceof CoreEntity ref ? value instanceof CoreEntity other && Objects.equals(ref.id, other.id)
                    : current instanceof BigDecimal decimal && value instanceof BigDecimal other ? decimal.compareTo(other) == 0 : Objects.equals(current, value);
                if (!equal) bad(field.label() + " cannot be changed after registration.");
            }
            write(entity, field.property(), value);
        }
        access.requireEntity("inventory/" + resource, action, entity);
        rules.validate(entity, previous);
        if (validateOnly) return Map.of();

        // Product entities keep their existing JPA relations during cutover, but
        // direct master-data owners (currently StockLocation) receive stable
        // canonical identifiers before persistence.
        if (masterDataReferences != null) {
            if (canonicalProductReferenceReadEnabled) {
                masterDataReferences.synchronizeForBackfill(entity);
            } else {
                masterDataReferences.synchronize(entity);
            }
        }

        if (id == null) {
            // Batch review reports duplicates per field separately. All actual creates
            // share this check under the catalog lock, before stock or audit writes.
            if (entity instanceof AssetItem asset) {
                var errors = AssetIdentity.conflicts(em.createQuery("select a from AssetItem a", AssetItem.class)
                    .getResultList(), asset.assetCode, asset.serialNumber, asset.internalCode);
                if (!errors.isEmpty()) conflict(String.join(" ", errors));
            }
            if (entity instanceof StockLot lot) lot.openingPackaging = packaging;
            if (entity instanceof StockLot lot) lot.availableQuantity = lot.initialQuantity;
            em.persist(entity);
            createOpening(entity);
        }
        em.flush();
        var result = view(spec, entity);
        audit.record("inventory/" + resource, entity.id, action, id == null ? null : previous, result);
        return result;
    }

    @Transactional
    public void delete(String resource, long id, Long version) {
        var spec = InventoryCatalog.get(resource);
        access.requireAny("inventory/" + resource, "DELETE");
        writable(spec);
        lockCatalog();
        var entity = find(spec.entity(), id);
        requireResourceDiscriminator(spec, entity);
        access.requireEntity("inventory/" + resource, "DELETE", entity);
        if (!Objects.equals(entity.version, version)) conflict("This record has changed. Reload before deleting.");
        rules.beforeDelete(entity);
        var before = view(spec, entity);
        em.remove(entity);
        em.flush();
        audit.record("inventory/" + resource, id, "DELETE", before, null);
    }

    private void lockCatalog() {
        em.createQuery("select t from ArmamentType t order by t.id", ArmamentType.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        em.createQuery("select c from ArmamentClassification c order by c.id", ArmamentClassification.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        // Serialize schema and asset writes before reading related category rows.
        // This also prevents concurrent parent changes from creating a cycle.
        em.createQuery("select c from ItemCategory c order by c.id", ItemCategory.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        em.createQuery("select p from ArmamentParameter p order by p.id", ArmamentParameter.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }

    private void createOpening(CoreEntity entity) {
        StockMovement movement = new StockMovement();
        movement.nature = StockMovementNature.OPENING.name();
        movement.movedAt = LocalDateTime.now();
        if (entity instanceof StockLot lot) {
            StockBalance balance = new StockBalance();
            balance.lot = lot; balance.location = lot.openingLocation;
            balance.available = lot.initialQuantity;
            em.persist(balance);
            movement.lot = lot; movement.location = lot.openingLocation;
            movement.quantity = lot.initialQuantity;
        } else if (entity instanceof AssetItem asset) {
            movement.asset = asset; movement.location = asset.location; movement.quantity = BigDecimal.ONE;
        } else return;
        movement.operatorLogin = audit.actor().login(); movement.operatorId = audit.actor().id(); em.persist(movement);
    }

    private record LegacyParameterField(String legacyField, String parameterType) {}

    private Map<String, LegacyParameterField> legacyParameterFields(String resource) {
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

    private void applyLegacyParameterFields(
            CoreEntity entity,
            InventoryCatalog.Resource spec,
            Map<String, LegacyParameterField> aliases,
            Map<String, Object> data) {
        if (aliases.isEmpty()) return;

        for (var entry : aliases.entrySet()) {
            String referenceFieldName = entry.getKey();
            LegacyParameterField alias = entry.getValue();
            if (data.containsKey(referenceFieldName) || !data.containsKey(alias.legacyField())) continue;

            Object raw = data.get(alias.legacyField());
            String value = raw == null ? null : raw.toString().trim();
            var referenceField = spec.fields().stream()
                .filter(field -> field.name().equals(referenceFieldName))
                .findFirst()
                .orElseThrow();

            if ((value == null || value.isBlank()) && referenceField.required()) {
                bad(referenceField.label() + " is required.");
            }

            write(entity, alias.legacyField(), value == null || value.isBlank() ? null : value);
            write(entity, referenceField.property(), null);
        }
    }

    private Object parse(InventoryCatalog.Field field, Object raw) {
        if (raw instanceof String text) raw = text.isBlank() ? null : text.trim();
        if (raw == null) {
            if (field.required()) bad(field.label() + " is required.");
            return null;
        }
        if (field.reference() != null) return findReference(field.reference(), integer(raw, false));
        if (field.type().equals("boolean")) {
            if (!(raw instanceof Boolean)) bad(field.label() + " must be true or false.");
            return raw;
        }
        if (field.type().equals("date")) {
            try { return LocalDate.parse(raw.toString()); }
            catch (RuntimeException ex) { bad(field.label() + " must be a valid date."); }
        }
        if (field.type().equals("decimal")) {
            try {
                var value = new BigDecimal(raw.toString()).stripTrailingZeros();
                if (value.signum() < 0 || Math.max(0, value.scale()) > 4 || value.precision() - value.scale() > 15) throw new IllegalArgumentException();
                return value;
            } catch (RuntimeException ex) { bad(field.label() + " must be nonnegative, with at most 15 integer and 4 decimal digits."); }
        }
        if (field.type().equals("integer")) {
            try {
                int value = new BigDecimal(raw.toString()).intValueExact();
                if (value <= 0) throw new IllegalArgumentException();
                return value;
            } catch (RuntimeException ex) { bad(field.label() + " must be a positive whole number."); }
        }
        if (!(raw instanceof String) || raw.toString().length() > 255) bad(field.label() + " must contain at most 255 characters.");
        if (!field.choices().isEmpty() && !field.choices().contains(raw)) bad(field.label() + " is invalid.");
        return raw;
    }

    private Class<? extends CoreEntity> referenceClass(String reference) {
        return reference.startsWith("core/") ? CoreCatalog.get(reference.substring(5)).entity() : InventoryCatalog.get(reference).entity();
    }

    private CoreEntity findReference(String reference, long id) {
        if (reference.startsWith("core/")) {
            return find(referenceClass(reference), id);
        }
        var target = InventoryCatalog.get(reference);
        var entity = find(target.entity(), id);
        requireResourceDiscriminator(target, entity);
        return entity;
    }

    private void requireResourceDiscriminator(InventoryCatalog.Resource spec, CoreEntity entity) {
        if (spec.discriminator() == null) return;
        if (!(entity instanceof ArmamentParameter parameter)
                || !Objects.equals(spec.discriminator(), parameter.parameterType)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Referenced record not found.");
        }
    }

    private CoreEntity find(Class<? extends CoreEntity> type, long id) {
        var entity = em.find(type, id);
        if (entity == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Referenced record not found.");
        return entity;
    }

    private Map<String, Object> view(InventoryCatalog.Resource spec, CoreEntity entity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", entity.id); result.put("version", entity.version);
        result.put("createdAt", entity.createdAt); result.put("updatedAt", entity.updatedAt);
        result.put("label", label(entity));
        if (entity instanceof ItemModel model) result.put("modelFamily", model.category.family);
        Map<String, String> labels = new LinkedHashMap<>();
        if (entity instanceof StockLocation location) {
            result.put("organizationCanonicalId", location.organizationCanonicalId);
            result.put("unitCanonicalId", location.unitCanonicalId);
            result.put("organizationId", location.organizationLegacyId);
            result.put("unitId", location.unitLegacyId);
            Organization organization = location.organizationLegacyId == null
                ? null : em.find(Organization.class, location.organizationLegacyId);
            OrganizationalUnit unit = location.unitLegacyId == null
                ? null : em.find(OrganizationalUnit.class, location.unitLegacyId);
            if (organization != null) labels.put("organizationId", label(organization));
            if (unit != null) labels.put("unitId", label(unit));
        }
        if (entity instanceof AssetItem asset) {
            result.put("organizationId", asset.location.organizationLegacyId);
            result.put("unitId", asset.location.unitLegacyId);
            Organization organization = asset.location.organizationLegacyId == null
                ? null : em.find(Organization.class, asset.location.organizationLegacyId);
            OrganizationalUnit unit = asset.location.unitLegacyId == null
                ? null : em.find(OrganizationalUnit.class, asset.location.unitLegacyId);
            if (organization != null) labels.put("organizationId", label(organization));
            if (unit != null) labels.put("unitId", label(unit));
        }
        for (var field : spec.fields()) {
            if (entity instanceof StockLocation
                    && Set.of("organizationId", "unitId").contains(field.name())) {
                continue;
            }
            Object value = read(entity, field.property());
            if (value instanceof CoreEntity ref) { result.put(field.name(), ref.id); labels.put(field.name(), label(ref)); }
            else result.put(field.name(), value instanceof BigDecimal decimal ? decimal.toPlainString() : value);
        }
        result.put("referenceLabels", labels);
        return result;
    }

    private String label(CoreEntity entity) {
        String name = null;
        for (String field : List.of("name", "assetCode", "lotNumber", "value", "nature")) {
            try { Object value = entity.getClass().getField(field).get(entity); if (value != null) { name = value.toString(); break; } }
            catch (NoSuchFieldException ignored) { }
            catch (IllegalAccessException ex) { throw new IllegalStateException(ex); }
        }
        if (entity instanceof StockBalance balance) name = balance.lot.lotNumber + " / " + balance.location.name;
        if (entity instanceof EquipmentSet set) name = set.code + " / " + set.name;
        if (entity instanceof EquipmentSetComponent component) name = component.equipmentSet.code + " / "
            + (component.asset == null ? component.balance.lot.lotNumber : component.asset.assetCode);
        if (entity instanceof CategoryCharacteristic binding) name = binding.category.name + " / " + binding.characteristic.name;
        if (entity instanceof FirearmSpecification specification) name = specification.model.name + " / " + specification.caliber;
        if (entity instanceof AmmunitionSpecification specification) name = specification.model.name + " / " + specification.caliber;
        if (entity instanceof GrenadeSpecification specification) name = specification.model.name + " / " + specification.grenadeType;
        if (entity instanceof SpraySpecification specification) name = specification.model.name + " / " + specification.agent;
        if (entity instanceof BallisticProtectionSpecification specification) name = specification.model.name + " / " + specification.protectionLevel;
        if (entity instanceof ElectricalDeviceSpecification specification) name = specification.model.name + " / " + specification.cartridgeType;
        if (entity instanceof OpticalSpecification specification) name = specification.model.name + " / " + specification.opticalType;
        if (entity instanceof RegulatoryControl control) name = control.registrationNumber + " / " + control.asset.assetCode;
        if (entity instanceof ExpirationRecord expiration) name = expiration.type + " / " + expiration.expirationDate;
        if (entity instanceof CertificationRecord certification) name = certification.type + " / " + certification.number;
        if (entity instanceof Recall recall) name = recall.number;
        if (entity instanceof RecallItem item) name = item.recall.number + " / " + (item.asset == null ? item.lot.lotNumber : item.asset.assetCode);
        if (entity instanceof ArmamentParameter parameter) name = parameter.code + " / " + parameter.name;
        if (entity instanceof ReservationStatusType status) name = status.code + " / " + status.name;
        if (entity instanceof InventoryCountStatusType status) name = status.code + " / " + status.name;
        if (entity instanceof InventoryCountResultType result) name = result.code + " / " + result.name;
        if (entity instanceof SaleReturnReasonType reason) name = reason.code + " / " + reason.name;
        if (entity instanceof CustodyReturnConditionType condition) name = condition.code + " / " + condition.name;
        return (name == null ? entity.getClass().getSimpleName() : name) + " (#" + entity.id + ")";
    }

    private Object read(CoreEntity entity, String field) {
        try { return entity.getClass().getField(field).get(entity); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }

    private void write(CoreEntity entity, String field, Object value) {
        try { entity.getClass().getField(field).set(entity, value); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }

    private Long integer(Object raw, boolean allowZero) {
        try { long value = new BigDecimal(String.valueOf(raw)).longValueExact(); if (value < (allowZero ? 0 : 1)) throw new IllegalArgumentException(); return value; }
        catch (RuntimeException ex) { bad("Invalid ID or version."); return null; }
    }

    private void writable(InventoryCatalog.Resource spec) {
        if (spec.readOnly()) throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Stock balances and movements are maintained by inventory operations.");
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
