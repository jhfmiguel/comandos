package com.comandos.demo;

import com.comandos.core.model.EconomicActivity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationNature;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.Person;
import com.comandos.core.model.PersonRole;
import com.comandos.core.model.PersonRoleAssignment;
import com.comandos.inventory.model.AmmunitionSpecification;
import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.BallisticProtectionSpecification;
import com.comandos.inventory.model.Brand;
import com.comandos.inventory.model.FirearmSpecification;
import com.comandos.inventory.model.ItemCategory;
import com.comandos.inventory.model.ItemModel;
import com.comandos.inventory.model.StockBalance;
import com.comandos.inventory.model.StockLocation;
import com.comandos.inventory.model.StockLot;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private final EntityManager entityManager;

    public DemoDataSeeder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long organizations = entityManager
            .createQuery("select count(o) from Organization o", Long.class)
            .getSingleResult();
        if (organizations > 0) {
            return;
        }

        Organization comandos = organization(
            "Secretaria de Segurança Pública - Ambiente de Demonstração",
            "SSP-DEMO",
            "Segurança Pública",
            "Administração pública em geral",
            true
        );
        Organization supplier = organization(
            "Fornecedor Bélico Demonstrativo Ltda.",
            "FBD",
            "Fornecedor",
            "Comércio especializado de equipamentos",
            false
        );

        OrganizationalUnit central = unit(comandos, null, "ARM-CENTRAL", "Armamento Central", "GERENCIA");
        OrganizationalUnit operational = unit(comandos, central, "UOP-01", "Unidade Operacional 01", "UNIDADE");
        OrganizationalUnit training = unit(comandos, central, "TREIN-01", "Centro de Treinamento", "CENTRO");

        Person officer = person("INDIVIDUAL", "João Operacional Demo", "11111111111", "joao.demo@comandos.local");
        Person armorer = person("INDIVIDUAL", "Maria Armeira Demo", "22222222222", "maria.demo@comandos.local");
        Person supplierContact = person("LEGAL_ENTITY", "Fornecedor Bélico Demonstrativo Ltda.", "11111111000191", "fornecedor@comandos.local");

        PersonRole publicServant = role("PUBLIC_SERVANT", "Servidor público");
        PersonRole armorerRole = role("ARMORER", "Armeiro");
        PersonRole supplierRole = role("SUPPLIER", "Fornecedor");

        assignment(officer, publicServant, comandos, operational);
        assignment(armorer, publicServant, comandos, central);
        assignment(armorer, armorerRole, comandos, central);
        assignment(supplierContact, supplierRole, supplier, null);

        StockLocation centralVault = location(comandos, central, "ARM-COFRE-01", "Cofre central", "CONTROLLED", true);
        StockLocation operationalVault = location(comandos, operational, "UOP-COFRE-01", "Cofre da unidade operacional", "CONTROLLED", true);
        StockLocation trainingStore = location(comandos, training, "TREIN-ALMOX-01", "Almoxarifado de treinamento", "WAREHOUSE", true);

        ItemCategory firearms = category("Armas de fogo", "FIREARM", true, false, false);
        ItemCategory ammunition = category("Munições", "AMMUNITION", false, true, true);
        ItemCategory ballistic = category("Proteção balística", "BALLISTIC_PROTECTION", true, false, false);

        Brand beretta = brand("Beretta", "Fabbrica d'Armi Pietro Beretta S.p.A.", "IT");
        Brand cbc = brand("CBC", "Companhia Brasileira de Cartuchos", "BR");
        Brand demoArmor = brand("Armor Demo", "Fabricante Demonstrativo", "BR");

        ItemModel apx = model(firearms, beretta, "APX", "EA", "BER-APX-9", "Pistola semiautomática 9 mm", "6500.00");
        ItemModel ammo9 = model(ammunition, cbc, "9 mm FMJ", "UN", "CBC-9-FMJ", "Munição 9 mm de treinamento", "4.50");
        ItemModel vest = model(ballistic, demoArmor, "Colete Nível III-A", "EA", "VEST-III-A-M", "Colete balístico demonstrativo", "3200.00");

        FirearmSpecification firearmSpec = new FirearmSpecification();
        firearmSpec.model = apx;
        firearmSpec.caliber = "9x19 mm";
        firearmSpec.operatingMechanism = "SEMI_AUTOMATIC";
        firearmSpec.capacity = 17;
        firearmSpec.barrelLength = new BigDecimal("108.00");
        entityManager.persist(firearmSpec);

        AmmunitionSpecification ammunitionSpec = new AmmunitionSpecification();
        ammunitionSpec.model = ammo9;
        ammunitionSpec.caliber = "9x19 mm";
        ammunitionSpec.ammunitionType = "FMJ";
        ammunitionSpec.lethalityClassification = "LETHAL";
        ammunitionSpec.projectileType = "OGIVAL";
        ammunitionSpec.caseType = "BRASS";
        ammunitionSpec.primerType = "CENTERFIRE";
        entityManager.persist(ammunitionSpec);

        BallisticProtectionSpecification ballisticSpec = new BallisticProtectionSpecification();
        ballisticSpec.model = vest;
        ballisticSpec.protectionType = "BODY_ARMOR";
        ballisticSpec.protectionLevel = "III-A";
        ballisticSpec.material = "ARAMID";
        ballisticSpec.certification = "DEMO-CERT-III-A";
        ballisticSpec.size = "M";
        ballisticSpec.serviceLifeMonths = 60;
        entityManager.persist(ballisticSpec);

        asset(apx, centralVault, "PAT-DEMO-0001", "APX-DEMO-0001", "ARM-0001", "GOOD", "AVAILABLE", "6500.00");
        asset(apx, centralVault, "PAT-DEMO-0002", "APX-DEMO-0002", "ARM-0002", "GOOD", "AVAILABLE", "6500.00");
        asset(apx, operationalVault, "PAT-DEMO-0003", "APX-DEMO-0003", "ARM-0003", "GOOD", "AVAILABLE", "6500.00");
        asset(vest, operationalVault, "PAT-DEMO-0101", "VEST-DEMO-0101", "COL-0101", "GOOD", "AVAILABLE", "3200.00");
        asset(vest, trainingStore, "PAT-DEMO-0102", "VEST-DEMO-0102", "COL-0102", "GOOD", "AVAILABLE", "3200.00");

        StockLot lot = new StockLot();
        lot.model = ammo9;
        lot.openingLocation = centralVault;
        lot.lotNumber = "CBC-DEMO-2026-001";
        lot.initialQuantity = new BigDecimal("5000.0000");
        lot.availableQuantity = new BigDecimal("5000.0000");
        lot.validUntil = LocalDate.of(2031, 9, 30);
        lot.condition = "GOOD";
        lot.status = "AVAILABLE";
        entityManager.persist(lot);

        StockBalance balance = new StockBalance();
        balance.lot = lot;
        balance.location = centralVault;
        balance.available = new BigDecimal("5000.0000");
        balance.reserved = BigDecimal.ZERO;
        balance.blocked = BigDecimal.ZERO;
        entityManager.persist(balance);

        entityManager.flush();
    }

    private Organization organization(
            String name,
            String acronym,
            String natureName,
            String economicActivityDescription,
            boolean publicOrganization) {

        OrganizationNature nature = new OrganizationNature();
        nature.code = acronym + "-NATURE";
        nature.name = natureName;
        nature.description = "Natureza institucional do ambiente de demonstração";
        nature.active = true;
        entityManager.persist(nature);

        EconomicActivity economicActivity = new EconomicActivity();
        economicActivity.code = acronym + "-ACTIVITY";
        economicActivity.description = economicActivityDescription;
        economicActivity.active = true;
        entityManager.persist(economicActivity);

        Organization value = new Organization();
        value.name = name;
        value.acronym = acronym;
        value.nature = nature;
        value.legacyNature = natureName;
        value.economicActivity = economicActivity;
        value.publicOrganization = publicOrganization;
        value.active = true;
        entityManager.persist(value);
        return value;
    }

    private OrganizationalUnit unit(Organization organization, OrganizationalUnit parent, String code, String name, String type) {
        OrganizationalUnit value = new OrganizationalUnit();
        value.organization = organization;
        value.parentUnit = parent;
        value.code = code;
        value.name = name;
        value.type = type;
        entityManager.persist(value);
        return value;
    }

    private Person person(String type, String name, String taxId, String email) {
        Person value = new Person();
        value.personType = type;
        value.fullName = name;
        value.taxId = taxId;
        value.email = email;
        value.active = true;
        entityManager.persist(value);
        return value;
    }

    private PersonRole role(String code, String name) {
        PersonRole value = new PersonRole();
        value.code = code;
        value.name = name;
        entityManager.persist(value);
        return value;
    }

    private void assignment(Person person, PersonRole role, Organization organization, OrganizationalUnit unit) {
        PersonRoleAssignment value = new PersonRoleAssignment();
        value.person = person;
        value.role = role;
        value.organization = organization;
        value.unit = unit;
        value.startDate = LocalDate.of(2026, 1, 1);
        value.status = "ACTIVE";
        entityManager.persist(value);
    }

    private StockLocation location(Organization organization, OrganizationalUnit unit, String code, String name, String type, boolean controlled) {
        StockLocation value = new StockLocation();
        value.organization = organization;
        value.unit = unit;
        value.code = code;
        value.name = name;
        value.type = type;
        value.warehouseType = type;
        value.controlled = controlled;
        value.active = true;
        entityManager.persist(value);
        return value;
    }

    private ItemCategory category(String name, String family, boolean serialized, boolean lotControlled, boolean consumable) {
        ItemCategory value = new ItemCategory();
        value.name = name;
        value.family = family;
        value.serialized = serialized;
        value.lotControlled = lotControlled;
        value.consumable = consumable;
        entityManager.persist(value);
        return value;
    }

    private Brand brand(String name, String manufacturer, String country) {
        Brand value = new Brand();
        value.name = name;
        value.manufacturer = manufacturer;
        value.manufacturingCountryCode = country;
        entityManager.persist(value);
        return value;
    }

    private ItemModel model(ItemCategory category, Brand brand, String name, String unit, String sku, String description, String price) {
        ItemModel value = new ItemModel();
        value.category = category;
        value.brand = brand;
        value.name = name;
        value.unitOfMeasure = unit;
        value.sku = sku;
        value.description = description;
        value.listPrice = new BigDecimal(price);
        entityManager.persist(value);
        return value;
    }

    private void asset(ItemModel model, StockLocation location, String assetCode, String serial, String internalCode,
                       String condition, String status, String currentValue) {
        AssetItem value = new AssetItem();
        value.model = model;
        value.location = location;
        value.assetCode = assetCode;
        value.serialNumber = serial;
        value.internalCode = internalCode;
        value.condition = condition;
        value.status = status;
        value.currentValue = new BigDecimal(currentValue);
        entityManager.persist(value);
    }
}
