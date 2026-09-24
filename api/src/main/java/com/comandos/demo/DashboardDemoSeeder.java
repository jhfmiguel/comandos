package com.comandos.demo;

import com.comandos.core.model.*;
import com.comandos.custody.model.*;
import com.comandos.inventory.model.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(90)
public class DashboardDemoSeeder implements ApplicationRunner {

    private final EntityManager em;

    public DashboardDemoSeeder(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Organization organization = one(Organization.class, "acronym", "SSP-DEMO");
        OrganizationalUnit unit = one(OrganizationalUnit.class, "code", "ARM-CENTRAL");
        Person recipient = one(Person.class, "taxId", "11111111111");
        Person authorizer = one(Person.class, "taxId", "22222222222");
        AssetItem asset = one(AssetItem.class, "assetCode", "PAT-DEMO-0002");
        StockLocation location = one(StockLocation.class, "code", "ARM-COFRE-01");
        ItemModel ammunition = one(ItemModel.class, "sku", "CBC-9-LUGER-FMJ");

        if (organization == null || unit == null || recipient == null || authorizer == null
                || asset == null || location == null || ammunition == null) return;

        seedMonthlyHistory(organization, unit, recipient, authorizer, asset, location);
        seedExpiryLots(ammunition, location);
        em.flush();
    }

    private void seedMonthlyHistory(
            Organization organization,
            OrganizationalUnit unit,
            Person recipient,
            Person authorizer,
            AssetItem asset,
            StockLocation location) {

        YearMonth current = YearMonth.now();

        for (int offset = 0; offset < 12; offset++) {
            YearMonth month = current.minusMonths(offset);
            int movementCount = 2 + (offset % 4);

            for (int index = 0; index < movementCount; index++) {
                long referenceId = month.getYear() * 10000L + month.getMonthValue() * 100L + index;
                if (movementExists(referenceId)) continue;

                StockMovement movement = new StockMovement();
                movement.asset = asset;
                movement.location = location;
                movement.nature = index % 2 == 0 ? "INVENTORY_ADJUSTMENT" : "INTERNAL_MOVEMENT";
                movement.quantity = BigDecimal.ONE;
                movement.movedAt = month.atDay(Math.min(4 + index * 3, month.lengthOfMonth())).atTime(10, 0);
                movement.operatorLogin = "maria.armeira.demo";
                movement.referenceType = "DASHBOARD_DEMO";
                movement.referenceId = referenceId;
                movement.notes = "Movimentação histórica fictícia para demonstrar a evolução mensal do dashboard.";
                em.persist(movement);
            }

            seedCompletedCustody(month, organization, unit, recipient, authorizer, asset, location);
        }
    }

    private void seedCompletedCustody(
            YearMonth month,
            Organization organization,
            OrganizationalUnit unit,
            Person recipient,
            Person authorizer,
            AssetItem asset,
            StockLocation location) {

        String key = "dashboard-custody-" + month;
        String requestId = UUID.nameUUIDFromBytes(("comandos-demo-" + key).getBytes(StandardCharsets.UTF_8)).toString();

        Long existing = em.createQuery(
                "select count(c) from Custody c where c.requestId = :requestId",
                Long.class)
            .setParameter("requestId", requestId)
            .getSingleResult();
        if (existing > 0) return;

        LocalDateTime deliveredAt = month.atDay(Math.min(8, month.lengthOfMonth())).atTime(8, 30);
        LocalDateTime returnedAt = deliveredAt.plusDays(2);

        Custody custody = new Custody();
        custody.organization = organization;
        custody.unit = unit;
        custody.recipient = recipient;
        custody.authorizer = authorizer;
        custody.organizationName = organization.name;
        custody.unitName = unit.name;
        custody.recipientName = recipient.fullName;
        custody.authorizerName = authorizer.fullName;
        custody.purpose = "Cautela temporária para instrução operacional - histórico didático";
        custody.recipientType = "PERSON";
        custody.custodyScope = "INDIVIDUAL";
        custody.durationType = "TEMPORARY";
        custody.teamOperation = "Treinamento mensal";
        custody.responsibilityTerm = "Termo fictício para demonstração do histórico.";
        custody.deliveryCondition = "Bem entregue em boas condições.";
        custody.status = "COMPLETED";
        custody.deliveredAt = deliveredAt;
        custody.dueAt = returnedAt;
        custody.completedAt = returnedAt;
        custody.issuedByLogin = "maria.armeira.demo";
        custody.requestId = requestId;
        custody.requestFingerprint = fingerprint(key);
        em.persist(custody);

        StockMovement issue = new StockMovement();
        issue.asset = asset;
        issue.location = location;
        issue.nature = "CUSTODY_ISSUE";
        issue.quantity = BigDecimal.ONE;
        issue.movedAt = deliveredAt;
        issue.operatorLogin = "maria.armeira.demo";
        issue.referenceType = "DASHBOARD_CUSTODY";
        issue.notes = "Saída temporária fictícia para cautela didática.";
        em.persist(issue);

        CustodyItem item = new CustodyItem();
        item.custody = custody;
        item.asset = asset;
        item.location = location;
        item.issueMovement = issue;
        item.modelName = asset.model.name;
        item.assetCode = asset.assetCode;
        item.serialNumber = asset.serialNumber;
        item.quantity = BigDecimal.ONE;
        item.locationName = location.name;
        item.deliveryCondition = "Sem avarias aparentes.";
        item.accessories = "Carregador e estojo de transporte - exemplo didático.";
        item.returnedAt = returnedAt;
        em.persist(item);
    }

    private void seedExpiryLots(ItemModel model, StockLocation location) {
        createLot(model, location, "CBC-DEMO-VALIDADE-VENCIDO", new BigDecimal("300"), LocalDate.now().minusDays(30));
        createLot(model, location, "CBC-DEMO-VALIDADE-60D", new BigDecimal("450"), LocalDate.now().plusDays(60));
        createLot(model, location, "CBC-DEMO-VALIDADE-180D", new BigDecimal("600"), LocalDate.now().plusDays(180));
        createLot(model, location, "CBC-DEMO-VALIDADE-2A", new BigDecimal("900"), LocalDate.now().plusYears(2));
        createLot(model, location, "CBC-DEMO-SEM-VALIDADE", new BigDecimal("250"), null);
    }

    private void createLot(ItemModel model, StockLocation location, String lotNumber, BigDecimal quantity, LocalDate validUntil) {
        StockLot existing = em.createQuery(
                "select l from StockLot l where l.model = :model and l.openingLocation = :location and l.lotNumber = :lotNumber",
                StockLot.class)
            .setParameter("model", model)
            .setParameter("location", location)
            .setParameter("lotNumber", lotNumber)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null);

        if (existing != null) return;

        StockLot lot = new StockLot();
        lot.model = model;
        lot.openingLocation = location;
        lot.lotNumber = lotNumber;
        lot.initialQuantity = quantity;
        lot.availableQuantity = quantity;
        lot.openingPackaging = "Entrada fictícia para demonstração de validade no dashboard.";
        lot.validUntil = validUntil;
        lot.condition = "GOOD";
        lot.status = "AVAILABLE";
        em.persist(lot);

        StockBalance balance = new StockBalance();
        balance.lot = lot;
        balance.location = location;
        balance.available = quantity;
        balance.reserved = BigDecimal.ZERO;
        balance.blocked = BigDecimal.ZERO;
        em.persist(balance);
    }

    private boolean movementExists(long referenceId) {
        return em.createQuery(
                "select count(m) from StockMovement m where m.referenceType = :type and m.referenceId = :referenceId",
                Long.class)
            .setParameter("type", "DASHBOARD_DEMO")
            .setParameter("referenceId", referenceId)
            .getSingleResult() > 0;
    }

    private <T> T one(Class<T> type, String field, Object value) {
        return em.createQuery(
                "select e from " + type.getSimpleName() + " e where e." + field + " = :value",
                type)
            .setParameter("value", value)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null);
    }

    private String fingerprint(String key) {
        return String.format("%064x", Math.abs(key.hashCode()) + 9000L);
    }
}
