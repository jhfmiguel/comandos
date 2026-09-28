package com.comandos.demo;

import com.comandos.inventory.model.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(15)
public class CatalogFamilyDemoSeeder implements ApplicationRunner {

    private final EntityManager em;

    public CatalogFamilyDemoSeeder(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Brand demo = brand("COMANDOS Demo Tactical", "COMANDOS Demo Tactical Industries", "BR");

        ItemCategory grenade = category("Granadas", "GRENADE", false, true, true);
        ItemCategory helmet = category("Capacetes balísticos", "HELMET", true, false, false);
        ItemCategory shield = category("Escudos", "SHIELD", true, false, false);
        ItemCategory spray = category("Espargidores", "SPRAY", false, true, true);
        ItemCategory electrical = category("Dispositivos elétricos", "ELECTRICAL_DEVICE", true, false, false);

        ItemModel grenadeModel = catalogItem(
            grenade, demo,
            "GRENADE_HAND", "Granada de mão", "Dispositivo de emprego manual",
            "GRENADE_LESS_LETHAL", "Menos letal", "Granada operacional menos letal",
            "DEMO-GREN-LL-01", "Granada menos letal de demonstração", "UN", "420.00"
        );
        grenadeSpecification(grenadeModel);

        ItemModel helmetModel = catalogItem(
            helmet, demo,
            "BALLISTIC_HELMET", "Capacete balístico", "Proteção balística craniana",
            "HELMET_IIIA", "Nível III-A", "Capacete balístico nível III-A",
            "DEMO-HELMET-IIIA-01", "Capacete balístico III-A de demonstração", "EA", "2800.00"
        );
        helmetSpecification(helmetModel);

        ItemModel shieldModel = catalogItem(
            shield, demo,
            "TACTICAL_SHIELD", "Escudo tático", "Escudo de proteção para emprego operacional",
            "SHIELD_BALLISTIC", "Balístico", "Escudo com proteção balística",
            "DEMO-SHIELD-BAL-01", "Escudo balístico de demonstração", "EA", "5200.00"
        );
        shieldSpecification(shieldModel);

        ItemModel sprayModel = catalogItem(
            spray, demo,
            "OC_SPRAY", "Espargidor OC", "Espargidor portátil de agente químico",
            "SPRAY_OC", "OC", "Espargidor com agente OC",
            "DEMO-SPRAY-OC-50", "Espargidor OC 50 ml de demonstração", "UN", "95.00"
        );
        spraySpecification(sprayModel);

        ItemModel electricalModel = catalogItem(
            electrical, demo,
            "CEW", "Dispositivo elétrico incapacitante", "Dispositivo elétrico de emprego operacional",
            "CEW_STANDARD", "Padrão", "Dispositivo elétrico com cartucho padrão",
            "DEMO-CEW-01", "Dispositivo elétrico de demonstração", "EA", "9800.00"
        );
        electricalSpecification(electricalModel);
    }

    private ItemModel catalogItem(
        ItemCategory category,
        Brand brand,
        String typeCode,
        String typeName,
        String typeDescription,
        String classificationCode,
        String classificationName,
        String classificationDescription,
        String sku,
        String modelName,
        String unit,
        String price
    ) {
        ArmamentType type = type(typeCode, typeName, typeDescription, category);
        ArmamentClassification classification = classification(
            classificationCode, classificationName, classificationDescription, type
        );
        ItemModel model = em.createQuery("select m from ItemModel m where m.sku = :sku", ItemModel.class)
            .setParameter("sku", sku)
            .getResultStream().findFirst().orElse(null);
        if (model == null) {
            model = new ItemModel();
            model.category = category;
            model.brand = brand;
            model.armamentType = type;
            model.armamentClassification = classification;
            model.name = modelName;
            model.unitOfMeasure = unit;
            model.manufacturerCode = sku;
            model.sku = sku;
            model.description = modelName;
            model.listPrice = new BigDecimal(price);
            em.persist(model);
        } else {
            model.category = category;
            model.brand = brand;
            model.armamentType = type;
            model.armamentClassification = classification;
        }
        return model;
    }

    private void grenadeSpecification(ItemModel model) {
        if (hasSpec(GrenadeSpecification.class, model.id)) return;
        GrenadeSpecification s = new GrenadeSpecification();
        s.model = model;
        s.grenadeTypeRef = parameter("GRENADE_TYPE", "LESS_LETHAL");
        s.grenadeType = s.grenadeTypeRef.name;
        s.agentRef = parameter("AGENT", "CS");
        s.agent = s.agentRef.name;
        s.compositionRef = parameter("COMPOSITION", "CS_COMPOUND");
        s.composition = s.compositionRef.name;
        s.shelfLifeMonths = 60;
        s.delaySeconds = 3;
        s.safetyRadius = new BigDecimal("10");
        em.persist(s);
    }

    private void helmetSpecification(ItemModel model) {
        if (hasSpec(HelmetSpecification.class, model.id)) return;
        HelmetSpecification s = new HelmetSpecification();
        s.model = model;
        s.protectionLevelRef = parameter("PROTECTION_LEVEL", "IIIA");
        s.protectionLevel = s.protectionLevelRef.name;
        s.materialRef = parameter("MATERIAL", "ARAMID");
        s.material = s.materialRef.name;
        s.sizeRef = parameter("SIZE", "M");
        s.size = s.sizeRef.name;
        s.weightGrams = new BigDecimal("1450");
        s.certification = "DEMO-BALLISTIC-IIIA";
        em.persist(s);
    }

    private void shieldSpecification(ItemModel model) {
        if (hasSpec(ShieldSpecification.class, model.id)) return;
        ShieldSpecification s = new ShieldSpecification();
        s.model = model;
        s.shieldTypeRef = parameter("SHIELD_TYPE", "BALLISTIC");
        s.shieldType = s.shieldTypeRef.name;
        s.protectionLevelRef = parameter("PROTECTION_LEVEL", "III");
        s.protectionLevel = s.protectionLevelRef.name;
        s.materialRef = parameter("MATERIAL", "ARAMID");
        s.material = s.materialRef.name;
        s.heightMm = new BigDecimal("1000");
        s.widthMm = new BigDecimal("600");
        s.weightGrams = new BigDecimal("6800");
        em.persist(s);
    }

    private void spraySpecification(ItemModel model) {
        if (hasSpec(SpraySpecification.class, model.id)) return;
        SpraySpecification s = new SpraySpecification();
        s.model = model;
        s.agentRef = parameter("AGENT", "OC");
        s.agent = s.agentRef.name;
        s.compositionRef = parameter("COMPOSITION", "OC_SOLUTION");
        s.composition = s.compositionRef.name;
        s.shelfLifeMonths = 48;
        s.concentration = new BigDecimal("10");
        s.volumeMl = new BigDecimal("50");
        s.rangeMeters = new BigDecimal("3");
        em.persist(s);
    }

    private void electricalSpecification(ItemModel model) {
        if (hasSpec(ElectricalDeviceSpecification.class, model.id)) return;
        ElectricalDeviceSpecification s = new ElectricalDeviceSpecification();
        s.model = model;
        s.voltage = new BigDecimal("50000");
        s.cycles = 5;
        s.cartridgeTypeRef = parameter("CARTRIDGE_TYPE", "STANDARD");
        s.cartridgeType = s.cartridgeTypeRef.name;
        em.persist(s);
    }

    private <T> boolean hasSpec(Class<T> type, Long modelId) {
        return !em.createQuery("select s from " + type.getSimpleName() + " s where s.model.id = :modelId", type)
            .setParameter("modelId", modelId)
            .setMaxResults(1)
            .getResultList().isEmpty();
    }

    private ItemCategory category(String name, String family, boolean serialized, boolean lotControlled, boolean consumable) {
        ItemCategory value = em.createQuery("select c from ItemCategory c where c.family = :family", ItemCategory.class)
            .setParameter("family", family)
            .getResultStream().findFirst().orElse(null);
        if (value == null) {
            value = new ItemCategory();
            value.name = name;
            value.family = family;
            value.serialized = serialized;
            value.lotControlled = lotControlled;
            value.consumable = consumable;
            em.persist(value);
        }
        return value;
    }

    private ArmamentType type(String code, String name, String description, ItemCategory category) {
        ArmamentType value = em.createQuery("select t from ArmamentType t where t.code = :code", ArmamentType.class)
            .setParameter("code", code)
            .getResultStream().findFirst().orElse(null);
        if (value == null) {
            value = new ArmamentType();
            value.code = code;
            value.name = name;
            value.description = description;
            value.active = true;
            value.category = category;
            em.persist(value);
        }
        return value;
    }

    private ArmamentClassification classification(
        String code, String name, String description, ArmamentType type
    ) {
        ArmamentClassification value = em.createQuery(
                "select c from ArmamentClassification c where c.code = :code", ArmamentClassification.class)
            .setParameter("code", code)
            .getResultStream().findFirst().orElse(null);
        if (value == null) {
            value = new ArmamentClassification();
            value.code = code;
            value.name = name;
            value.description = description;
            value.active = true;
            value.type = type;
            em.persist(value);
        }
        return value;
    }

    private Brand brand(String name, String manufacturer, String country) {
        Brand value = em.createQuery(
                "select b from Brand b where b.name = :name and b.manufacturer = :manufacturer", Brand.class)
            .setParameter("name", name)
            .setParameter("manufacturer", manufacturer)
            .getResultStream().findFirst().orElse(null);
        if (value == null) {
            value = new Brand();
            value.name = name;
            value.manufacturer = manufacturer;
            value.manufacturingCountryCode = country;
            em.persist(value);
        }
        return value;
    }

    private ArmamentParameter parameter(String type, String code) {
        return em.createQuery(
                "select p from ArmamentParameter p where p.parameterType = :type and p.code = :code and p.active = true",
                ArmamentParameter.class)
            .setParameter("type", type)
            .setParameter("code", code)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing catalog parameter: " + type + "/" + code));
    }
}
