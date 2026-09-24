package com.comandos.demo;

import com.comandos.consumption.model.*;
import com.comandos.core.model.*;
import com.comandos.custody.model.*;
import com.comandos.inventory.model.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(60)
public class CustodyConsumptionDemoSeeder implements ApplicationRunner {

    private final EntityManager em;

    public CustodyConsumptionDemoSeeder(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Organization org = one(Organization.class, "acronym", "SSP-DEMO");
        OrganizationalUnit operational = one(OrganizationalUnit.class, "code", "UOP-01");
        OrganizationalUnit training = one(OrganizationalUnit.class, "code", "TREIN-01");
        Person officer = one(Person.class, "taxId", "11111111111");
        Person armorer = one(Person.class, "taxId", "22222222222");
        AssetItem asset = one(AssetItem.class, "assetCode", "PAT-DEMO-0002");
        StockLocation central = one(StockLocation.class, "code", "ARM-COFRE-01");
        StockLocation trainingStore = one(StockLocation.class, "code", "TREIN-ALMOX-01");
        StockLot lot = one(StockLot.class, "lotNumber", "CBC-DEMO-2026-002");
        StockBalance balance = lot == null || trainingStore == null ? null : balance(lot, trainingStore);

        if (org == null || operational == null || training == null || officer == null || armorer == null
                || asset == null || central == null || trainingStore == null || lot == null || balance == null) return;

        seedCustody(org, operational, officer, armorer, asset, central);
        seedConsumption(org, training, officer, armorer, lot, balance, trainingStore);
        em.flush();
    }

    private void seedCustody(Organization org, OrganizationalUnit unit, Person recipient, Person authorizer,
                             AssetItem asset, StockLocation location) {
        if (count(Custody.class) > 0) return;

        Custody custody = new Custody();
        custody.organization = org;
        custody.unit = unit;
        custody.recipient = recipient;
        custody.authorizer = authorizer;
        custody.organizationName = org.name;
        custody.unitName = unit.name;
        custody.recipientName = recipient.fullName;
        custody.authorizerName = authorizer.fullName;
        custody.purpose = "Cautela temporária para serviço operacional - cenário didático";
        custody.recipientType = "PERSON";
        custody.custodyScope = "INDIVIDUAL";
        custody.durationType = "TEMPORARY";
        custody.responsibilityTerm = "Termo fictício de responsabilidade para treinamento do sistema.";
        custody.deliveryCondition = "Bem conferido e entregue em boas condições.";
        custody.status = "COMPLETED";
        custody.deliveredAt = LocalDateTime.now().minusDays(15);
        custody.dueAt = LocalDateTime.now().minusDays(5);
        custody.completedAt = LocalDateTime.now().minusDays(5);
        custody.issuedByLogin = "maria.armeira.demo";
        custody.requestId = id("custody");
        custody.requestFingerprint = fingerprint("custody");
        em.persist(custody);

        StockMovement issue = movement(asset, null, location, "CUSTODY_ISSUE", BigDecimal.ONE,
            "Saída fictícia para cautela.");
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
        item.deliveryCondition = "Boa";
        item.accessories = "Estojo e acessórios cadastrados para demonstração.";
        item.returnedAt = custody.completedAt;
        em.persist(item);

        CustodyReturn returned = new CustodyReturn();
        returned.custody = custody;
        returned.requestId = id("custody-return");
        returned.requestFingerprint = fingerprint("custody-return");
        returned.returnedAt = custody.completedAt;
        returned.returnedByLogin = "joao.operacional.demo";
        em.persist(returned);

        StockMovement returnMovement = movement(asset, null, location, "CUSTODY_RETURN", BigDecimal.ONE,
            "Retorno fictício de cautela.");
        CustodyReturnItem returnedItem = new CustodyReturnItem();
        returnedItem.custodyReturn = returned;
        returnedItem.custodyItem = item;
        returnedItem.movement = returnMovement;
        returnedItem.conditionType = first(CustodyReturnConditionType.class);
        returnedItem.inspectionNotes = "Número de série, integridade e acessórios conferidos. Sem divergências.";
        returnedItem.inspectedByLogin = "maria.armeira.demo";
        em.persist(returnedItem);
    }

    private void seedConsumption(Organization org, OrganizationalUnit unit, Person responsible, Person authorizer,
                                 StockLot lot, StockBalance balance, StockLocation location) {
        if (count(AmmunitionConsumption.class) > 0) return;

        AmmunitionConsumption consumption = new AmmunitionConsumption();
        consumption.organization = org;
        consumption.unit = unit;
        consumption.responsible = responsible;
        consumption.authorizer = authorizer;
        consumption.organizationName = org.name;
        consumption.unitName = unit.name;
        consumption.responsibleName = responsible.fullName;
        consumption.authorizerName = authorizer.fullName;
        consumption.purpose = "Treinamento institucional - cenário didático";
        consumption.activityType = "TRAINING";
        consumption.operationTraining = "Turma demonstrativa A";
        consumption.status = "FINALIZED";
        consumption.consumedAt = LocalDateTime.now().minusDays(20);
        consumption.finalizedByLogin = "maria.armeira.demo";
        consumption.requestId = id("consumption");
        consumption.requestFingerprint = fingerprint("consumption");
        em.persist(consumption);

        StockMovement stockMovement = movement(null, lot, location, "CONSUMPTION", new BigDecimal("200"),
            "Consumo fictício associado ao treinamento.");
        AmmunitionConsumptionItem item = new AmmunitionConsumptionItem();
        item.consumption = consumption;
        item.lot = lot;
        item.balance = balance;
        item.location = location;
        item.movement = stockMovement;
        item.modelName = lot.model.name;
        item.sku = lot.model.sku;
        item.lotNumber = lot.lotNumber;
        item.locationName = location.name;
        item.unitOfMeasure = lot.model.unitOfMeasure;
        item.quantity = new BigDecimal("200");
        item.deliveredQuantity = new BigDecimal("200");
        item.usedQuantity = new BigDecimal("180");
        item.returnedQuantity = new BigDecimal("20");
        item.result = "180 utilizadas; 20 devolvidas ao estoque";
        em.persist(item);
    }

    private StockMovement movement(AssetItem asset, StockLot lot, StockLocation location,
                                   String nature, BigDecimal quantity, String notes) {
        StockMovement value = new StockMovement();
        value.asset = asset;
        value.lot = lot;
        value.location = location;
        value.nature = nature;
        value.quantity = quantity;
        value.movedAt = LocalDateTime.now().minusDays(1);
        value.operatorLogin = "maria.armeira.demo";
        value.referenceType = "DEMO";
        value.notes = notes;
        em.persist(value);
        return value;
    }

    private StockBalance balance(StockLot lot, StockLocation location) {
        return em.createQuery("select b from StockBalance b where b.lot = :lot and b.location = :location", StockBalance.class)
            .setParameter("lot", lot).setParameter("location", location)
            .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private <T> T one(Class<T> type, String field, Object value) {
        return em.createQuery("select e from " + type.getSimpleName() + " e where e." + field + " = :value", type)
            .setParameter("value", value).setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private <T> T first(Class<T> type) {
        return em.createQuery("select e from " + type.getSimpleName() + " e order by e.id", type)
            .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private long count(Class<?> type) {
        return em.createQuery("select count(e) from " + type.getSimpleName() + " e", Long.class).getSingleResult();
    }

    private String id(String key) {
        return UUID.nameUUIDFromBytes(("comandos-demo-" + key).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String fingerprint(String key) {
        return String.format("%064x", Math.abs(key.hashCode()) + 2000L);
    }
}
