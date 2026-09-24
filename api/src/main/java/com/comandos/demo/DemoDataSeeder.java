package com.comandos.demo;

import com.comandos.core.model.EconomicActivity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationNature;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.OrganizationalUnitType;
import com.comandos.core.model.Person;
import com.comandos.core.model.PersonAddress;
import com.comandos.core.model.PersonEmail;
import com.comandos.core.model.PersonPhone;
import com.comandos.core.model.PersonRole;
import com.comandos.core.model.PersonRoleAssignment;
import com.comandos.core.model.PersonType;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(10)
public class DemoDataSeeder implements ApplicationRunner {

    private final EntityManager entityManager;

    public DemoDataSeeder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
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

        firearmSpecification(apx, caliber9);
        ammunitionSpecification(ammo9, caliber9, ammunitionType, projectileType, caseType, primerType);
        ballisticSpecification(vest, protectionType, protectionLevel, material, sizeM);

        asset(apx, centralVault, "PAT-DEMO-0001", "APX-DEMO-0001", "ARM-0001", "GOOD", "AVAILABLE", "6500.00");
        asset(apx, centralVault, "PAT-DEMO-0002", "APX-DEMO-0002", "ARM-0002", "GOOD", "AVAILABLE", "6500.00");
        asset(apx, operationalVault, "PAT-DEMO-0003", "APX-DEMO-0003", "ARM-0003", "GOOD", "AVAILABLE", "6500.00");
        asset(apx, operationalVault, "PAT-DEMO-0004", "APX-DEMO-0004", "ARM-0004", "GOOD", "BLOCKED", "6500.00");
        asset(vest, operationalVault, "PAT-DEMO-0101", "VEST-DEMO-0101", "COL-0101", "GOOD", "AVAILABLE", "3200.00");
        asset(vest, trainingStore, "PAT-DEMO-0102", "VEST-DEMO-0102", "COL-0102", "GOOD", "AVAILABLE", "3200.00");
        asset(vest, centralVault, "PAT-DEMO-0103", "VEST-DEMO-0103", "COL-0103", "GOOD", "BLOCKED", "3200.00");

        StockLot lot = lot(ammo9, centralVault, "CBC-DEMO-2026-001", "5000.0000", LocalDate.of(2031, 9, 30));
        balance(lot, centralVault, "5000.0000");

        StockLot trainingLot = lot(ammo9, trainingStore, "CBC-DEMO-2026-002", "1500.0000", LocalDate.of(2030, 6, 30));
        balance(trainingLot, trainingStore, "1250.0000");

        StockLot operationalLot = lot(ammo9, operationalVault, "CBC-DEMO-2026-003", "800.0000", LocalDate.of(2029, 12, 31));
        balance(operationalLot, operationalVault, "800.0000");

        entityManager.flush();
    }

    private Organization organization(
            String name,
            String acronym,
            String natureName,
            String economicActivityDescription,
            boolean publicOrganization) {

        var existing = entityManager.createQuery(
                "select o from Organization o where o.acronym = :acronym",
                Organization.class)
            .setParameter("acronym", acronym)
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return existing.getFirst();

        String natureCode = acronym + "-NATURE";
        OrganizationNature nature = entityManager.createQuery(
                "select n from OrganizationNature n where n.code = :code",
                OrganizationNature.class)
            .setParameter("code", natureCode)
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst()
            .orElseGet(() -> {
                OrganizationNature created = new OrganizationNature();
                created.code = natureCode;
                created.name = natureName;
                created.description = "Natureza institucional do ambiente de demonstração";
                created.active = true;
                entityManager.persist(created);
                return created;
            });

        String activityCode = acronym + "-ACTIVITY";
        EconomicActivity economicActivity = entityManager.createQuery(
                "select a from EconomicActivity a where a.code = :code",
                EconomicActivity.class)
            .setParameter("code", activityCode)
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst()
            .orElseGet(() -> {
                EconomicActivity created = new EconomicActivity();
                created.code = activityCode;
                created.description = economicActivityDescription;
                created.active = true;
                entityManager.persist(created);
                return created;
            });

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
        var existing = entityManager.createQuery(
                "select u from OrganizationalUnit u where u.organization = :organization and u.code = :code",
                OrganizationalUnit.class)
            .setParameter("organization", organization)
            .setParameter("code", code)
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return existing.getFirst();

        OrganizationalUnit value = new OrganizationalUnit();
        value.organization = organization;
        value.parentUnit = parent;
        value.code = code;
        value.name = name;
        value.unitType = entityManager.createQuery(
                "select t from OrganizationalUnitType t where t.code = :code",
                OrganizationalUnitType.class)
            .setParameter("code", type)
            .setMaxResults(1)
            .getSingleResult();
        value.type = value.unitType.name;
        entityManager.persist(value);
        return value;
    }

    private Person person(String type, String name, String taxId, String email) {
        var existing = entityManager.createQuery(
                "select p from Person p where p.taxId = :taxId",
                Person.class)
            .setParameter("taxId", taxId)
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return existing.getFirst();

        Person value = new Person();
        value.personTypeRef = entityManager.createQuery(
                "select t from PersonType t where t.code = :code",
                PersonType.class)
            .setParameter("code", type)
            .setMaxResults(1)
            .getSingleResult();
        value.personType = value.personTypeRef.code;
        value.fullName = name;
        value.taxId = taxId;
        value.email = email;
        value.active = true;
        entityManager.persist(value);
        return value;
    }

    private void address(Person person, String type, String postalCode, String street, String number,
                         String district, String city, String state, String country, boolean foreign, boolean primary) {
        Long existing = entityManager.createQuery(
                "select count(a) from PersonAddress a where a.person = :person and a.type = :type and a.street = :street and a.number = :number",
                Long.class)
            .setParameter("person", person)
            .setParameter("type", type)
            .setParameter("street", street)
            .setParameter("number", number)
            .getSingleResult();
        if (existing > 0) return;

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
        Long existing = entityManager.createQuery(
                "select count(p) from PersonPhone p where p.person = :person and p.number = :number",
                Long.class)
            .setParameter("person", person)
            .setParameter("number", number)
            .getSingleResult();
        if (existing > 0) return;

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
        Long existing = entityManager.createQuery(
                "select count(e) from PersonEmail e where e.person = :person and e.email = :email",
                Long.class)
            .setParameter("person", person)
            .setParameter("email", email)
            .getSingleResult();
        if (existing > 0) return;

        PersonEmail value = new PersonEmail();
        value.person = person;
        value.type = type;
        value.email = email;
        value.primaryEmail = primary;
        entityManager.persist(value);
    }

    private ArmamentParameter parameter(String type, String code, String name, String description) {
        var existing = entityManager.createQuery(
                "select p from ArmamentParameter p where p.parameterType = :type and p.code = :code",
                ArmamentParameter.class)
            .setParameter("type", type)
            .setParameter("code", code)
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return existing.getFirst();

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
        var existing = entityManager.createQuery(
                "select r from PersonRole r where r.code = :code",
                PersonRole.class)
            .setParameter("code", code)
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return existing.getFirst();

        PersonRole value = new PersonRole();
        value.code = code;
        value.name = name;
        entityManager.persist(value);
        return value;
    }

    private void assignment(Person person, PersonRole role, Organization organization, OrganizationalUnit unit) {
        Long existing = entityManager.createQuery(
                "select count(a) from PersonRoleAssignment a where a.person = :person and a.role = :role and a.organization = :organization and ((:unit is null and a.unit is null) or a.unit = :unit)",
                Long.class)
            .setParameter("person", person)
            .setParameter("role", role)
            .setParameter("organization", organization)
            .setParameter("unit", unit)
            .getSingleResult();
        if (existing > 0) return;

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
        var existing = entityManager.createQuery(
                "select l from StockLocation l where l.organization = :organization and l.code = :code",
                StockLocation.class)
            .setParameter("organization", organization)
            .setParameter("code", code)
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return existing.getFirst();

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
        var existing = entityManager.createQuery(
                "select c from ItemCategory c where c.name = :name and c.family = :family",
                ItemCategory.class)
            .setParameter("name", name)
            .setParameter("family", family)
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return existing.getFirst();

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
        var existing = entityManager.createQuery(
                "select b from Brand b where b.name = :name",
                Brand.class)
            .setParameter("name", name)
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return existing.getFirst();

        Brand value = new Brand();
        value.name = name;
        value.manufacturer = manufacturer;
        value.manufacturingCountryCode = country;
        entityManager.persist(value);
        return value;
    }

    private ItemModel model(ItemCategory category, Brand brand, String name, String unit, String sku, String description, String price) {
        var existing = entityManager.createQuery(
                "select m from ItemModel m where m.sku = :sku",
                ItemModel.class)
            .setParameter("sku", sku)
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return existing.getFirst();

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
        Long existing = entityManager.createQuery(
                "select count(a) from AssetItem a where a.assetCode = :assetCode or a.serialNumber = :serial",
                Long.class)
            .setParameter("assetCode", assetCode)
            .setParameter("serial", serial)
            .getSingleResult();
        if (existing > 0) return;

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

    private void firearmSpecification(ItemModel model, ArmamentParameter caliber) {
        Long existing = entityManager.createQuery(
                "select count(s) from FirearmSpecification s where s.model = :model",
                Long.class)
            .setParameter("model", model)
            .getSingleResult();
        if (existing > 0) return;

        FirearmSpecification value = new FirearmSpecification();
        value.model = model;
        value.caliber = caliber.name;
        value.caliberRef = caliber;
        value.operatingMechanism = "SEMI_AUTOMATIC";
        value.capacity = 17;
        value.barrelLength = new BigDecimal("108.00");
        entityManager.persist(value);
    }

    private void ammunitionSpecification(ItemModel model, ArmamentParameter caliber, ArmamentParameter ammunitionType,
                                          ArmamentParameter projectileType, ArmamentParameter caseType, ArmamentParameter primerType) {
        Long existing = entityManager.createQuery(
                "select count(s) from AmmunitionSpecification s where s.model = :model",
                Long.class)
            .setParameter("model", model)
            .getSingleResult();
        if (existing > 0) return;

        AmmunitionSpecification value = new AmmunitionSpecification();
        value.model = model;
        value.caliber = caliber.name;
        value.caliberRef = caliber;
        value.ammunitionType = ammunitionType.name;
        value.ammunitionTypeRef = ammunitionType;
        value.lethalityClassification = "LETHAL";
        value.projectileType = projectileType.name;
        value.projectileTypeRef = projectileType;
        value.caseType = caseType.name;
        value.caseTypeRef = caseType;
        value.primerType = primerType.name;
        value.primerTypeRef = primerType;
        entityManager.persist(value);
    }

    private void ballisticSpecification(ItemModel model, ArmamentParameter protectionType, ArmamentParameter protectionLevel,
                                         ArmamentParameter material, ArmamentParameter size) {
        Long existing = entityManager.createQuery(
                "select count(s) from BallisticProtectionSpecification s where s.model = :model",
                Long.class)
            .setParameter("model", model)
            .getSingleResult();
        if (existing > 0) return;

        BallisticProtectionSpecification value = new BallisticProtectionSpecification();
        value.model = model;
        value.protectionType = protectionType.name;
        value.protectionTypeRef = protectionType;
        value.protectionLevel = protectionLevel.name;
        value.protectionLevelRef = protectionLevel;
        value.material = material.name;
        value.materialRef = material;
        value.certification = "DEMO-CERT-III-A";
        value.size = size.name;
        value.sizeRef = size;
        value.serviceLifeMonths = 60;
        entityManager.persist(value);
    }

    private StockLot lot(ItemModel model, StockLocation location, String lotNumber, String quantity, LocalDate validUntil) {
        var existing = entityManager.createQuery(
                "select l from StockLot l where l.model = :model and l.lotNumber = :lotNumber",
                StockLot.class)
            .setParameter("model", model)
            .setParameter("lotNumber", lotNumber)
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return existing.getFirst();

        StockLot value = new StockLot();
        value.model = model;
        value.openingLocation = location;
        value.lotNumber = lotNumber;
        value.initialQuantity = new BigDecimal(quantity);
        value.availableQuantity = new BigDecimal(quantity);
        value.validUntil = validUntil;
        value.condition = "GOOD";
        value.status = "AVAILABLE";
        entityManager.persist(value);
        return value;
    }

    private void balance(StockLot lot, StockLocation location, String available) {
        Long existing = entityManager.createQuery(
                "select count(b) from StockBalance b where b.lot = :lot and b.location = :location",
                Long.class)
            .setParameter("lot", lot)
            .setParameter("location", location)
            .getSingleResult();
        if (existing > 0) return;

        StockBalance value = new StockBalance();
        value.lot = lot;
        value.location = location;
        value.available = new BigDecimal(available);
        value.reserved = BigDecimal.ZERO;
        value.blocked = BigDecimal.ZERO;
        entityManager.persist(value);
    }
}
