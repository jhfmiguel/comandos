package com.comandos.demo;
import com.fariamiguel.app.demo.DemoDataScenario;
import com.fariamiguel.app.demo.DemoDataSeedOperations;

import com.fariamiguel.app.core.model.EconomicActivity;
import com.fariamiguel.app.core.model.Organization;
import com.fariamiguel.app.core.model.OrganizationNature;
import com.fariamiguel.app.core.model.OrganizationalUnit;
import com.fariamiguel.app.core.model.OrganizationalUnitType;
import com.fariamiguel.app.core.model.Person;
import com.fariamiguel.app.core.model.PersonAddress;
import com.fariamiguel.app.core.model.PersonEmail;
import com.fariamiguel.app.core.model.PersonPhone;
import com.fariamiguel.app.core.model.PersonRole;
import com.fariamiguel.app.core.model.PersonRoleAssignment;
import com.fariamiguel.app.core.model.PersonType;
import com.comandos.inventory.model.AmmunitionSpecification;
import com.comandos.inventory.model.ArmamentParameter;
import com.comandos.inventory.model.ArmamentType;
import com.comandos.inventory.model.ArmamentClassification;
import com.fariamiguel.app.inventory.model.AssetItem;
import com.comandos.inventory.model.BallisticProtectionSpecification;
import com.fariamiguel.app.inventory.model.Brand;
import com.comandos.inventory.model.FirearmSpecification;
import com.fariamiguel.app.inventory.model.ItemCategory;
import com.fariamiguel.app.inventory.model.ItemModel;
import com.fariamiguel.app.inventory.model.StockBalance;
import com.fariamiguel.app.inventory.model.StockLocation;
import com.fariamiguel.app.inventory.model.StockLot;
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
public class SecurityDemoDataScenario extends DemoDataSeedOperations implements DemoDataScenario {



    public SecurityDemoDataScenario(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    @Transactional
    public void seed() {
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

        ArmamentType pistol = armamentType("PISTOL", "Pistola", "Arma de fogo curta empunhada com uma mão", firearms);
        ArmamentClassification semiAutomaticPistol = armamentClassification(
            "PISTOL_SEMIAUTOMATIC", "Semiautomática", "Pistola com funcionamento semiautomático", pistol
        );
        ArmamentType rifle = armamentType("RIFLE", "Fuzil", "Arma longa raiada de emprego operacional", firearms);
        ArmamentClassification selectiveRifle = armamentClassification(
            "RIFLE_SELECTIVE", "Seletivo", "Fuzil com regimes de tiro selecionáveis", rifle
        );
        ArmamentType shotgun = armamentType("SHOTGUN", "Espingarda", "Arma longa de alma lisa", firearms);
        ArmamentClassification pumpAction = armamentClassification(
            "SHOTGUN_PUMP", "Pump action", "Espingarda de repetição por ação de bomba", shotgun
        );
        ArmamentType pistolAmmo = armamentType("PISTOL_AMMO", "Munição para pistola", "Cartucho para arma curta", ammunition);
        ArmamentClassification fmjClassification = armamentClassification(
            "PISTOL_AMMO_FMJ", "FMJ", "Projétil totalmente encamisado", pistolAmmo
        );
        ArmamentType ballisticVest = armamentType("BALLISTIC_VEST", "Colete balístico", "Proteção balística corporal", ballistic);
        ArmamentClassification iiiaClassification = armamentClassification(
            "BALLISTIC_VEST_IIIA", "Nível III-A", "Colete balístico de nível III-A", ballisticVest
        );

        Brand beretta = brand("Beretta", "Fabbrica d'Armi Pietro Beretta S.p.A.", "IT");
        Brand glock = brand("Glock", "GLOCK Ges.m.b.H.", "AT");
        Brand taurus = brand("Taurus", "Taurus Armas S.A.", "BR");
        Brand imbel = brand("IMBEL", "Indústria de Material Bélico do Brasil", "BR");
        Brand cbc = brand("CBC", "Companhia Brasileira de Cartuchos", "BR");
        Brand pointBlank = brand("Point Blank", "Point Blank Enterprises, Inc.", "US");

        ItemModel apx = model(firearms, beretta, "APX A1 Full Size", "EA", "BER-APX-A1-FS-9", "Pistola Beretta APX A1 Full Size 9x19 mm", "6500.00");
        ItemModel glock17 = model(firearms, glock, "G17 Gen5", "EA", "GLOCK-G17-GEN5-9", "Pistola Glock G17 Gen5 9x19 mm", "6800.00");
        ItemModel taurusG3 = model(firearms, taurus, "G3 Tactical", "EA", "TAURUS-G3-TACTICAL-9", "Pistola Taurus G3 Tactical 9x19 mm", "5200.00");
        ItemModel ia2 = model(firearms, imbel, "Fuzil de Assalto IMBEL 5,56 IA2", "EA", "IMBEL-IA2-556", "Fuzil IMBEL IA2 calibre 5,56 x 45 mm", "12000.00");
        ItemModel military30 = model(firearms, cbc, "Pump Military 3.0 Cal. 12", "EA", "CBC-MILITARY-30-12", "Espingarda CBC Pump Military 3.0 calibre 12", "6500.00");
        ItemModel ammo9 = model(ammunition, cbc, "CBC 9 mm Luger FMJ", "UN", "CBC-9-LUGER-FMJ", "Munição CBC 9 mm Luger com projétil FMJ", "4.50");
        ItemModel vest = model(ballistic, pointBlank, "Alpha Elite Black", "EA", "POINTBLANK-ALPHA-ELITE-BLACK", "Colete balístico Point Blank Alpha Elite Black", "8500.00");

        classifyModel(apx, pistol, semiAutomaticPistol);
        classifyModel(glock17, pistol, semiAutomaticPistol);
        classifyModel(taurusG3, pistol, semiAutomaticPistol);
        classifyModel(ia2, rifle, selectiveRifle);
        classifyModel(military30, shotgun, pumpAction);
        classifyModel(ammo9, pistolAmmo, fmjClassification);
        classifyModel(vest, ballisticVest, iiiaClassification);

        firearmSpecification(apx, caliber9);
        ammunitionSpecification(ammo9, caliber9, ammunitionType, projectileType, caseType, primerType);
        ballisticSpecification(vest, protectionType, protectionLevel, material, sizeM);

        asset(apx, centralVault, "PAT-DEMO-0001", "APX-DEMO-0001", "ARM-0001", "GOOD", "AVAILABLE", "6500.00");
        asset(apx, centralVault, "PAT-DEMO-0002", "APX-DEMO-0002", "ARM-0002", "GOOD", "AVAILABLE", "6500.00");
        asset(glock17, operationalVault, "PAT-DEMO-0003", "G17-DEMO-0003", "ARM-0003", "GOOD", "AVAILABLE", "6800.00");
        asset(taurusG3, operationalVault, "PAT-DEMO-0004", "G3-DEMO-0004", "ARM-0004", "GOOD", "BLOCKED", "5200.00");
        asset(ia2, centralVault, "PAT-DEMO-0005", "IA2-DEMO-0005", "ARM-0005", "GOOD", "AVAILABLE", "12000.00");
        asset(military30, trainingStore, "PAT-DEMO-0006", "CBC12-DEMO-0006", "ARM-0006", "GOOD", "AVAILABLE", "6500.00");
        asset(vest, operationalVault, "PAT-DEMO-0101", "PB-DEMO-0101", "COL-0101", "GOOD", "AVAILABLE", "8500.00");
        asset(vest, trainingStore, "PAT-DEMO-0102", "PB-DEMO-0102", "COL-0102", "GOOD", "AVAILABLE", "8500.00");
        asset(vest, centralVault, "PAT-DEMO-0103", "PB-DEMO-0103", "COL-0103", "GOOD", "BLOCKED", "8500.00");

        StockLot lot = lot(ammo9, centralVault, "CBC-DEMO-2026-001", "5000.0000", LocalDate.of(2031, 9, 30));
        balance(lot, centralVault, "5000.0000");

        StockLot trainingLot = lot(ammo9, trainingStore, "CBC-DEMO-2026-002", "1500.0000", LocalDate.of(2030, 6, 30));
        balance(trainingLot, trainingStore, "1500.0000");

        StockLot operationalLot = lot(ammo9, operationalVault, "CBC-DEMO-2026-003", "800.0000", LocalDate.of(2029, 12, 31));
        balance(operationalLot, operationalVault, "800.0000");

        entityManager.flush();
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









    private ArmamentType armamentType(String code, String name, String description, ItemCategory category) {
        var existing = entityManager.createQuery(
                "select t from ArmamentType t where t.code = :code",
                ArmamentType.class)
            .setParameter("code", code)
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return existing.getFirst();

        ArmamentType value = new ArmamentType();
        value.code = code;
        value.name = name;
        value.description = description;
        value.category = category;
        value.active = true;
        entityManager.persist(value);
        return value;
    }

    private ArmamentClassification armamentClassification(
            String code, String name, String description, ArmamentType type) {
        var existing = entityManager.createQuery(
                "select c from ArmamentClassification c where c.code = :code",
                ArmamentClassification.class)
            .setParameter("code", code)
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return existing.getFirst();

        ArmamentClassification value = new ArmamentClassification();
        value.code = code;
        value.name = name;
        value.description = description;
        value.type = type;
        value.active = true;
        entityManager.persist(value);
        return value;
    }

    private void classifyModel(ItemModel model, ArmamentType type, ArmamentClassification classification) {
        entityManager.flush();
        com.comandos.inventory.model.ArmamentModelDetails.of(entityManager, model).armamentType = type;
        com.comandos.inventory.model.ArmamentModelDetails.of(entityManager, model).armamentClassification = classification;
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




}
