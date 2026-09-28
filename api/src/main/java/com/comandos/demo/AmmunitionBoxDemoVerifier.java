package com.comandos.demo;

import com.comandos.inventory.model.AmmunitionBox;
import com.comandos.inventory.model.ItemModel;
import com.comandos.inventory.model.StockLocation;
import com.comandos.inventory.model.StockLot;
import com.comandos.inventory.service.StockIntakeService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1800)
public class AmmunitionBoxDemoVerifier implements ApplicationRunner {
    private static final String REQUEST_ID = "8d06d03c-0949-45ea-942f-55f425805501";
    private static final String LOT_NUMBER = "BOX-REGRESSION-2026-001";

    private final EntityManager em;
    private final StockIntakeService intake;

    public AmmunitionBoxDemoVerifier(EntityManager em, StockIntakeService intake) {
        this.em = em;
        this.intake = intake;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ItemModel model = em.createQuery("select m from ItemModel m where m.sku = :sku", ItemModel.class)
            .setParameter("sku", "CBC-9-LUGER-FMJ")
            .setMaxResults(1)
            .getResultStream().findFirst().orElseThrow(() -> fail("demo ammunition model is missing"));
        StockLocation location = em.createQuery("select l from StockLocation l where l.code = :code", StockLocation.class)
            .setParameter("code", "ARM-COFRE-01")
            .setMaxResults(1)
            .getResultStream().findFirst().orElseThrow(() -> fail("demo ammunition location is missing"));

        var request = new StockIntakeService.BoxesRequest(
            REQUEST_ID,
            model.id,
            location.id,
            LOT_NUMBER,
            null,
            List.of(new StockIntakeService.BoxRow("2", "50"), new StockIntakeService.BoxRow("3", "20")),
            "7"
        );

        var first = intake.boxes(request);
        if (!"167".equals(first.quantity())) fail("aggregate stock receipt must keep 167 cartridges");
        if (first.recordIds().size() != 1) fail("box intake must still create one aggregate stock lot");

        StockLot lot = em.find(StockLot.class, first.recordIds().getFirst());
        if (lot == null || lot.initialQuantity.compareTo(new BigDecimal("167")) != 0)
            fail("aggregate lot quantity does not match boxes plus loose rounds");

        List<AmmunitionBox> boxes = boxes();
        if (boxes.size() != 5) fail("five physical boxes must produce five independent records");
        if (new HashSet<>(boxes.stream().map(box -> box.boxCode).toList()).size() != boxes.size())
            fail("each ammunition box must have a unique identity");
        if (new HashSet<>(boxes.stream().map(box -> box.sequenceNumber).toList()).size() != boxes.size())
            fail("each ammunition box must have a unique intake sequence");

        BigDecimal boxedRounds = BigDecimal.ZERO;
        for (int i = 0; i < boxes.size(); i++) {
            AmmunitionBox box = boxes.get(i);
            BigDecimal expected = i < 2 ? new BigDecimal("50") : new BigDecimal("20");
            if (box.lot == null || !lot.id.equals(box.lot.id)) fail("box lost its aggregate lot provenance");
            if (box.location == null || !location.id.equals(box.location.id)) fail("box lost its physical location");
            if (box.nominalQuantity.compareTo(expected) != 0 || box.available.compareTo(expected) != 0)
                fail("box balance does not preserve its own rounds-per-box quantity");
            if (box.reserved.signum() != 0 || box.blocked.signum() != 0) fail("new box balance buckets must start at zero");
            if (!"SEALED".equals(box.status)) fail("new physical box must start sealed");
            boxedRounds = boxedRounds.add(box.nominalQuantity);
        }
        if (boxedRounds.compareTo(new BigDecimal("160")) != 0) fail("boxed rounds must total 160");
        if (lot.initialQuantity.subtract(boxedRounds).compareTo(new BigDecimal("7")) != 0)
            fail("loose rounds must remain outside physical box identities");

        var repeated = intake.boxes(request);
        if (repeated.id() != first.id()) fail("same request ID must return the original stock intake");
        if (boxes().size() != 5) fail("idempotent replay must not duplicate ammunition boxes");
    }

    private List<AmmunitionBox> boxes() {
        return em.createQuery(
                "select b from AmmunitionBox b where b.intakeRequestId = :requestId order by b.sequenceNumber",
                AmmunitionBox.class)
            .setParameter("requestId", REQUEST_ID)
            .getResultList();
    }

    private static IllegalStateException fail(String message) {
        return new IllegalStateException("Ammunition box regression failed: " + message);
    }
}
