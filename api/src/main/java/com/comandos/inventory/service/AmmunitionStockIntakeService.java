package com.comandos.inventory.service;
import com.fariamiguel.app.inventory.service.StockIntakeService;

import com.fariamiguel.app.inventory.model.*;
import com.comandos.inventory.model.*;
import com.comandos.security.service.ComandosAccessPolicy;
import jakarta.persistence.*;
import java.math.BigDecimal;
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
@org.springframework.context.annotation.Primary
@Transactional
public class AmmunitionStockIntakeService extends StockIntakeService {
    public record BoxRow(String boxes, String roundsPerBox) {}
    public record BoxesRequest(String requestId, Long modelId, Long openingLocationId, String lotNumber,
        String validUntil, List<BoxRow> boxes, String looseUnits) {}

    public AmmunitionStockIntakeService(EntityManager em, ComandosInventoryService inventory, ComandosAccessPolicy access,
            com.comandos.audit.service.ComandosAuditService audit) {
        super(em, inventory, access, audit);
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
        int physicalBoxes = 0;
        List<BigInteger> individualBoxes = new ArrayList<>();
        List<String> packaging = new ArrayList<>();

        for (int i = 0; i < request.boxes().size(); i++) {
            var row = request.boxes().get(i);
            if (row == null) bad("Row " + (i + 1) + ": enter boxes and rounds per box.");
            BigInteger boxes = positiveInteger(row.boxes(), i);
            BigInteger rounds = positiveInteger(row.roundsPerBox(), i);
            if (boxes.compareTo(BigInteger.valueOf(1000L - physicalBoxes)) > 0)
                bad("A single stock entry may contain at most 1000 individually tracked ammunition boxes.");
            int count = boxes.intValueExact();
            physicalBoxes += count;
            for (int box = 0; box < count; box++) individualBoxes.add(rounds);
            total = total.add(boxes.multiply(rounds));
            packaging.add(boxes + " x " + rounds);
        }
        if (total.signum() <= 0 || total.toString().length() > 15)
            bad("Total rounds must be positive and contain at most 15 digits.");
        if (!loose.equals("0")) packaging.add(loose + " loose");

        var data = new HashMap<String, Object>();
        data.put("modelId", request.modelId());
        data.put("openingLocationId", request.openingLocationId());
        data.put("lotNumber", request.lotNumber());
        data.put("validUntil", request.validUntil());
        data.put("initialQuantity", total.toString());
        var lotView = inventory.receiveBoxes(data, String.join(" + ", packaging));
        long lotId = ((Number) lotView.get("id")).longValue();
        StockLot lot = em.find(StockLot.class, lotId);

        List<Long> boxIds = new ArrayList<>();
        for (int i = 0; i < individualBoxes.size(); i++) {
            AmmunitionBox box = new AmmunitionBox();
            box.lot = lot;
            box.location = lot.openingLocation;
            box.intakeRequestId = request.requestId();
            box.sequenceNumber = i + 1;
            box.boxCode = "AMMO-" + lot.id + "-BOX-" + String.format(Locale.ROOT, "%06d", i + 1);
            box.nominalQuantity = new BigDecimal(individualBoxes.get(i));
            box.available = box.nominalQuantity;
            box.reserved = BigDecimal.ZERO;
            box.blocked = BigDecimal.ZERO;
            box.status = "SEALED";
            em.persist(box);
            em.flush();
            boxIds.add(box.id);
        }

        Receipt result = save(request.requestId(), "lots", fingerprint, List.of(lotId), total.toString());
        audit.record("inventory/lots", lotId, "AMMUNITION_BOXES_CREATE", null,
            Map.of("requestId", request.requestId(), "lotId", lotId, "boxCount", boxIds.size(),
                "boxIds", boxIds, "looseUnits", loose, "totalRounds", total.toString()));
        return result;
    }

    private static BigInteger positiveInteger(String value, int row) {
        if (value == null || !value.matches("[1-9][0-9]{0,14}"))
            bad("Row " + (row + 1) + ": quantities must be positive whole numbers, up to 15 digits.");
        return new BigInteger(value);
    }

}