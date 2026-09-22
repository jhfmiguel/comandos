package com.weaponsregistration.inventory.service;

import com.weaponsregistration.inventory.model.*;
import com.weaponsregistration.security.service.AccessPolicy;
import jakarta.persistence.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional
public class StockIntakeService {
    public record AssetRow(String assetCode, String serialNumber) {}
    public record AssetsRequest(String requestId, Map<String, Object> common, List<AssetRow> items) {}
    public record BoxRow(String boxes, String roundsPerBox) {}
    public record BoxesRequest(String requestId, Long modelId, Long openingLocationId, String lotNumber,
        String validUntil, List<BoxRow> boxes, String looseUnits) {}
    public record Receipt(long id, List<Long> recordIds, String quantity) {}
    public record RowResult(int row, String assetCode, String serialNumber, String status, List<String> errors) {}
    public record AssetResult(Receipt receipt, boolean accepted, String detail, List<RowResult> rows) {}
    private final com.weaponsregistration.audit.service.AuditService audit;
    private final EntityManager em;
    private final InventoryService inventory;
    private final AccessPolicy access;
    private final JsonMapper json = JsonMapper.builder().build();

    public StockIntakeService(EntityManager em, InventoryService inventory, AccessPolicy access, com.weaponsregistration.audit.service.AuditService audit) {
        this.em = em; this.inventory = inventory; this.access = access; this.audit = audit;
    }

    public AssetResult assets(AssetsRequest request) { return processAssets(request, false); }
    public AssetResult reviewAssets(AssetsRequest request) { return processAssets(request, true); }

    private AssetResult processAssets(AssetsRequest request, boolean review) {
        access.requireAny("inventory/assets", "CREATE");
        if (request == null || request.common() == null || request.items() == null || request.items().isEmpty()
            || request.items().size() > 1000) bad("Select common asset fields and 1 to 1000 asset code / serial number pairs.");
        if (!Set.of("modelId", "locationId", "condition", "status", "validUntil", "currentValue").containsAll(request.common().keySet()))
            bad("Only model, location, condition, availability, expiry and value are common fields.");
        var canonical = new AssetsRequest(request.requestId(), new TreeMap<>(request.common()), request.items());
        String fingerprint = fingerprint(canonical);
        var existing = existing(request.requestId(), "assets", fingerprint);
        if (existing != null) return accepted(receipt(existing), request);
        Map<String, Integer> codes = new HashMap<>(), serials = new HashMap<>();
        for (var row : request.items()) if (row != null) {
            codes.merge(AssetIdentity.normalize(row.assetCode()), 1, Integer::sum);
            serials.merge(AssetIdentity.normalize(row.serialNumber()), 1, Integer::sum);
        }
        var registered = em.createQuery("select a from AssetItem a", AssetItem.class).getResultList();
        List<RowResult> results = new ArrayList<>();
        List<Map<String, Object>> dataRows = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < request.items().size(); i++) {
            var row = request.items().get(i);
                String code = row == null ? "" : AssetIdentity.normalize(row.assetCode());
                String serial = row == null ? "" : AssetIdentity.normalize(row.serialNumber());
                List<String> errors = new ArrayList<>();
                if (code.isEmpty() || code.length() > 255) errors.add("Asset code must contain 1 to 255 characters.");
                if (serial.isEmpty() || serial.length() > 255) errors.add("Serial number must contain 1 to 255 characters.");
                if (codes.getOrDefault(code, 0) > 1) errors.add("Duplicate asset code in this list. Related rows: " + relatedRows(request, code, true));
                if (serials.getOrDefault(serial, 0) > 1) errors.add("Duplicate serial number in this list. Related rows: " + relatedRows(request, serial, false));
                var data = new HashMap<>(request.common()); data.put("assetCode", code); data.put("serialNumber", serial);
                String invalid = inventory.validateAsset(data);
                if (invalid != null) errors.add(invalid);
                errors.addAll(AssetIdentity.conflicts(registered, code, serial, null));
                results.add(new RowResult(i + 1, code, serial, errors.isEmpty() ? "VALID" : "REJECTED", errors));
                dataRows.add(data);
        }
        boolean valid = results.stream().allMatch(row -> row.errors().isEmpty());
        if (!valid || review) {
            String detail = valid ? "All rows validated. Confirm the complete batch."
                : String.join("; ", results.stream().filter(row -> !row.errors().isEmpty())
                    .map(row -> "Row " + row.row() + ": " + String.join(" ", row.errors())).toList());
            var result = new AssetResult(null, valid, detail, results);
            audit.record("inventory/assets", 0, valid ? "BATCH_REVIEW" : "BATCH_REJECTED", null,
                Map.of("requestId", request.requestId(), "common", request.common(), "rowCount", results.size(), "result", result));
            return result;
        }
        for (var data : dataRows) ids.add(((Number) inventory.save("assets", null, data).get("id")).longValue());
        var receipt = save(request.requestId(), "assets", fingerprint, ids, Integer.toString(ids.size()));
        var result = accepted(receipt, request);
        audit.record("inventory/assets", ids.getFirst(), "BATCH_CREATE", null,
            Map.of("requestId", request.requestId(), "common", request.common(), "rowCount", ids.size(), "result", result));
        return result;
    }

    private static List<Integer> relatedRows(AssetsRequest request, String identifier, boolean code) {
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < request.items().size(); i++) {
            var row = request.items().get(i);
            if (row != null && identifier.equals(AssetIdentity.normalize(code ? row.assetCode() : row.serialNumber())))
                rows.add(i + 1);
        }
        return rows;
    }

    private AssetResult accepted(Receipt receipt, AssetsRequest request) {
        List<RowResult> rows = new ArrayList<>();
        for (int i = 0; i < request.items().size(); i++) {
            var row = request.items().get(i);
            rows.add(new RowResult(i + 1, AssetIdentity.normalize(row.assetCode()), AssetIdentity.normalize(row.serialNumber()), "ACCEPTED", List.of()));
        }
        return new AssetResult(receipt, true, "All rows accepted.", rows);
    }

    public Receipt boxes(BoxesRequest request) {
        if (request == null || request.boxes() == null || request.boxes().size() > 1000)
            bad("Enter up to 1000 rows of boxes and rounds per box, or loose rounds.");
        String fingerprint = fingerprint(request);
        var existing = existing(request.requestId(), "lots", fingerprint);
        if (existing != null) return receipt(existing);
        if (request.modelId() == null) bad("Select an ammunition model.");
        var model = em.find(ItemModel.class, request.modelId());
        if (model == null) bad("Select an ammunition model.");
        access.requireEntity("inventory/models", "READ", model);
        if (!"AMMUNITION".equals(model.category.family)) bad("Box entry requires an ammunition model.");
        String loose = request.looseUnits() == null ? "0" : request.looseUnits();
        if (!loose.matches("0|[1-9][0-9]{0,14}")) bad("Loose rounds must be a nonnegative whole number, up to 15 digits.");
        BigInteger total = new BigInteger(loose);
        List<String> packaging = new ArrayList<>();
        for (int i = 0; i < request.boxes().size(); i++) {
            var row = request.boxes().get(i);
            if (row == null) bad("Row " + (i + 1) + ": enter boxes and rounds per box.");
            BigInteger boxes = positiveInteger(row.boxes(), i);
            BigInteger rounds = positiveInteger(row.roundsPerBox(), i);
            total = total.add(boxes.multiply(rounds));
            packaging.add(boxes + " x " + rounds);
        }
        if (total.signum() <= 0 || total.toString().length() > 15) bad("Total rounds must be positive and contain at most 15 digits.");
        if (!loose.equals("0")) packaging.add(loose + " loose");
        var data = new HashMap<String, Object>();
        data.put("modelId", request.modelId()); data.put("openingLocationId", request.openingLocationId());
        data.put("lotNumber", request.lotNumber()); data.put("validUntil", request.validUntil());
        data.put("initialQuantity", total.toString());
        // Stored with the opening record and included in its inventory audit snapshot.
        var lot = inventory.receiveBoxes(data, String.join(" + ", packaging));
        return save(request.requestId(), "lots", fingerprint, List.of(((Number) lot.get("id")).longValue()), total.toString());
    }

    private StockIntake existing(String requestId, String resource, String fingerprint) {
        try { if (requestId == null || !UUID.fromString(requestId).toString().equals(requestId)) throw new IllegalArgumentException(); }
        catch (IllegalArgumentException ex) { bad("A canonical UUID request ID is required."); }
        access.requireAny("inventory/" + resource, "CREATE");
        em.createQuery("select c from ItemCategory c order by c.id", ItemCategory.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        var result = em.createQuery("select i from StockIntake i where i.requestId = :id", StockIntake.class)
            .setParameter("id", requestId).getResultStream().findFirst().orElse(null);
        if (result != null) {
            if (!result.resource.equals(resource) || !result.fingerprint.equals(fingerprint))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This request ID was already used for a different stock entry.");
            for (long id : ids(result)) {
                com.weaponsregistration.core.model.CoreEntity entity = resource.equals("assets")
                    ? em.find(AssetItem.class, id) : em.find(StockLot.class, id);
                if (entity == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "A stock entry record was removed.");
                access.requireEntity("inventory/" + resource, "CREATE", entity);
                access.requireEntity("inventory/" + resource, "READ", entity);
            }
        }
        return result;
    }

    private Receipt save(String requestId, String resource, String fingerprint, List<Long> ids, String quantity) {
        var intake = new StockIntake(); intake.requestId = requestId; intake.resource = resource; intake.fingerprint = fingerprint;
        intake.recordIds = String.join(",", ids.stream().map(String::valueOf).toList()); intake.quantity = quantity;
        em.persist(intake); em.flush(); return receipt(intake);
    }
    private static List<Long> ids(StockIntake entry) { return Arrays.stream(entry.recordIds.split(",")).map(Long::valueOf).toList(); }
    private static Receipt receipt(StockIntake entry) { return new Receipt(entry.id, ids(entry), entry.quantity); }
    private String fingerprint(Object request) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static BigInteger positiveInteger(String value, int row) {
        if (value == null || !value.matches("[1-9][0-9]{0,14}")) bad("Row " + (row + 1) + ": quantities must be positive whole numbers, up to 15 digits.");
        return new BigInteger(value);
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
