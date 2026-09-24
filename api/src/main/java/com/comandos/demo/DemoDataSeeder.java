package com.comandos.demo;

import com.comandos.core.model.EconomicActivity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationNature;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.Person;
import com.comandos.core.model.PersonAddress;
import com.comandos.core.model.PersonEmail;
import com.comandos.core.model.PersonPhone;
import com.comandos.core.model.PersonRole;
import com.comandos.core.model.PersonRoleAssignment;
import com.comandos.inventory.model.AmmunitionSpecification;
import com.comandos.inventory.model.ArmamentParameter;
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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@Order(10)
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

        address(officer, "RESIDENTIAL", "74000-000", "Rua Demo", "100", "Centro", "Goiânia", "GO", "Brasil", false, true);
        address(armorer, "RESIDENTIAL", "75800-000", "Avenida Demo", "250", "Setor Central", "Jataí", "GO", "Brasil", false, true);
        address(supplierContact, "BUSINESS", "10001", "Demo Avenue", "500", "Business District", "New York", "NY", "Estados Unidos", true, true);

        phone(officer, "MOBILE", "+55", "(62) 99999-1001", true, true);
        phone(officer, "WORK", "+55", "(62) 3333-1001", false, false);
        phone(armorer, "MOBILE", "+55", "(64) 99999-2002", true, true);
        phone(supplierContact, "WORK", "+1", "2125550100", false, true);

        email(officer, "PERSONAL", "joao.demo@comandos.local", true);
        email(officer, "WORK", "joao.operacional@ssp.demo", false);
        email(armorer, "WORK", "maria.armeira@ssp.demo", true);
        email(supplierContact, "WORK", "fornecedor@comandos.local", true);

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

        ArmamentParameter caliber9 = parameter("CALIBER", "9X19", "9x19 mm", "Calibre de demonstração");
        parameter("CALIBER", "40SW", ".40 S&W", "Calibre alternativo de demonstração");
        ArmamentParameter ammunitionType = parameter("AMMUNITION_TYPE", "FMJ", "FMJ", "Munição encamisada");
        parameter("AMMUNITION_TYPE", "JHP", "JHP", "Munição ponta oca");
        ArmamentParameter projectileType = parameter("PROJECTILE_TYPE", "OGIVAL", "Ogival", "Projétil ogival");
        parameter("PROJECTILE_TYPE", "HOLLOW_POINT", "Hollow point", "Projétil ponta oca");
        ArmamentParameter caseType = parameter("CASE_TYPE", "BRASS", "Latão", "Estojo de latão");
        parameter("CASE_TYPE", "STEEL", "Aço", "Estojo de aço");
        ArmamentParameter primerType = parameter("PRIMER_TYPE", "CENTERFIRE", "Fogo central", "Espoleta de fogo central");
        parameter("PRIMER_TYPE", "RIMFIRE", "Fogo circular", "Espoleta de fogo circular");
        parameter("GRENADE_TYPE", "LESS_LETHAL", "Menos letal", "Granada menos letal");
        parameter("GRENADE_TYPE", "TRAINING", "Treinamento", "Granada de treinamento");
        parameter("AGENT", "OC", "OC", "Oleoresin capsicum");
        parameter("AGENT", "CS", "CS", "Agente lacrimogêneo");
        parameter("COMPOSITION", "OC_SOLUTION", "Solução OC", "Composição de demonstração");
        parameter("COMPOSITION", "INERT", "Composição inerte", "Composição para treinamento");
        ArmamentParameter protectionType = parameter("PROTECTION_TYPE", "VEST", "Colete", "Proteção balística corporal");
        parameter("PROTECTION_TYPE", "PLATE", "Placa", "Placa balística");
        ArmamentParameter protectionLevel = parameter("PROTECTION_LEVEL", "IIIA", "Nível III-A", "Nível de proteção demonstrativo");
        parameter("PROTECTION_LEVEL", "III", "Nível III", "Nível de proteção demonstrativo");
        ArmamentParameter material = parameter("MATERIAL", "ARAMID", "Aramida", "Material balístico");
        parameter("MATERIAL", "STEEL", "Aço", "Material metálico");
        ArmamentParameter sizeM = parameter("SIZE", "M", "M", "Tamanho médio");
        parameter("SIZE", "G", "G", "Tamanho grande");
        parameter("CARTRIDGE_TYPE", "STANDARD", "Padrão", "Cartucho padrão");
        parameter("CARTRIDGE_TYPE", "EXTENDED", "Alcance estendido", "Cartucho de alcance estendido");
        parameter("OPTICAL_TYPE", "RED_DOT", "Red dot", "Mira reflexiva");
        parameter("OPTICAL_TYPE", "SCOPE", "Luneta", "Óptico de ampliação");
        parameter("SHIELD_TYPE", "BALLISTIC", "Balístico", "Escudo balístico");
        parameter("SHIELD_TYPE", "RIOT", "Antitumulto", "Escudo antitumulto");
        parameter("LOCKING_MECHANISM", "DOUBLE_LOCK", "Trava dupla", "Mecanismo de algema");
        parameter("LOCKING_MECHANISM", "KEY", "Chave", "Travamento por chave");
        parameter("COMPONENT_TYPE", "MAGAZINE", "Carregador", "Componente de arma");
        parameter("COMPONENT_TYPE", "STOCK", "Coronha", "Componente de arma");
        parameter("COMPATIBILITY", "APX", "Beretta APX", "Compatibilidade de demonstração");
        parameter("COMPATIBILITY", "PICATINNY", "Picatinny", "Compatibilidade de trilho");
        parameter("INTERFACE", "PICATINNY", "Picatinny", "Interface de montagem");
        parameter("INTERFACE", "MLOK", "M-LOK", "Interface de montagem");

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
        firearmSpec.caliber = caliber9.name;
        firearmSpec.caliberRef = caliber9;
        firearmSpec.operatingMechanism = "SEMI_AUTOMATIC";
        firearmSpec.capacity = 17;
        firearmSpec.barrelLength = new BigDecimal("108.00");
        entityManager.persist(firearmSpec);

        AmmunitionSpecification ammunitionSpec = new AmmunitionSpecification();
        ammunitionSpec.model = ammo9;
        ammunitionSpec.caliber = caliber9.name;
        ammunitionSpec.caliberRef = caliber9;
        ammunitionSpec.ammunitionType = ammunitionType.name;
        ammunitionSpec.ammunitionTypeRef = ammunitionType;
        ammunitionSpec.lethalityClassification = "LETHAL";
        ammunitionSpec.projectileType = projectileType.name;
        ammunitionSpec.projectileTypeRef = projectileType;
        ammunitionSpec.caseType = caseType.name;
        ammunitionSpec.caseTypeRef = caseType;
        ammunitionSpec.primerType = primerType.name;
        ammunitionSpec.primerTypeRef = primerType;
        entityManager.persist(ammunitionSpec);

        BallisticProtectionSpecification ballisticSpec = new BallisticProtectionSpecification();
        ballisticSpec.model = vest;
        ballisticSpec.protectionType = protectionType.name;
        ballisticSpec.protectionTypeRef = protectionType;
        ballisticSpec.protectionLevel = protectionLevel.name;
        ballisticSpec.protectionLevelRef = protectionLevel;
        ballisticSpec.material = material.name;
        ballisticSpec.materialRef = material;
        ballisticSpec.certification = "DEMO-CERT-III-A";
        ballisticSpec.size = sizeM.name;
        ballisticSpec.sizeRef = sizeM;
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

    private void address(Person person, String type, String postalCode, String street, String number,
                         String district, String city, String state, String country, boolean foreign, boolean primary) {
        PersonAddress value = new PersonAddress();
        value.person = person;
        value.type = type;
        value.postalCode = postalCode;
        value.street = street;
        value.number = number;
        value.district = district;
        value.city = city;
        value.state = state;
        value.country = country;
        value.foreignAddress = foreign;
        value.primaryAddress = primary;
        entityManager.persist(value);
    }

    private void phone(Person person, String type, String countryCode, String number, boolean whatsapp, boolean primary) {
        PersonPhone value = new PersonPhone();
        value.person = person;
        value.type = type;
        value.countryCode = countryCode;
        value.number = number;
        value.whatsapp = whatsapp;
        value.primaryPhone = primary;
        entityManager.persist(value);
    }

    private void email(Person person, String type, String email, boolean primary) {
        PersonEmail value = new PersonEmail();
        value.person = person;
        value.type = type;
        value.email = email;
        value.primaryEmail = primary;
        entityManager.persist(value);
    }

    private ArmamentParameter parameter(String type, String code, String name, String description) {
        ArmamentParameter value = new ArmamentParameter();
        value.parameterType = type;
        value.code = code;
        value.name = name;
        value.description = description;
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
