package com.comandos.demo;

import com.comandos.core.model.EconomicActivity;
import com.comandos.core.model.OrganizationNature;
import com.comandos.core.model.OrganizationalUnitType;
import com.comandos.inventory.model.ArmamentParameter;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(5)
public class ReferenceDataDemoSeeder implements ApplicationRunner {

    private final EntityManager entityManager;

    public ReferenceDataDemoSeeder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        nature("PUBLIC_AGENCY", "Órgão público", "Administração pública direta ou indireta");
        nature("PRIVATE_COMPANY", "Empresa privada", "Pessoa jurídica de direito privado");
        nature("SUPPLIER", "Fornecedor", "Organização fornecedora de bens ou serviços");

        economicActivity("PUBLIC_ADMIN", "Administração pública em geral");
        economicActivity("SECURITY_SERVICES", "Atividades de segurança e proteção");
        economicActivity("EQUIPMENT_TRADE", "Comércio especializado de equipamentos");

        unitType("DIRETORIA", "Diretoria", "Unidade organizacional de nível diretivo");
        unitType("GERENCIA", "Gerência", "Unidade organizacional de nível gerencial");
        unitType("COORDENACAO", "Coordenação", "Unidade organizacional de coordenação");
        unitType("CENTRO", "Centro", "Centro especializado ou de treinamento");
        unitType("NUCLEO", "Núcleo", "Núcleo técnico ou operacional");
        unitType("SETOR", "Setor", "Setor administrativo ou operacional");
        unitType("UNIDADE", "Unidade", "Unidade organizacional genérica");

        parameter("CALIBER", "9X19", "9x19 mm", "Calibre de demonstração");
        parameter("CALIBER", "40SW", ".40 S&W", "Calibre alternativo de demonstração");
        parameter("CALIBER", "12GA", "12 GA", "Calibre de espingarda");

        parameter("AMMUNITION_TYPE", "FMJ", "FMJ", "Munição encamisada");
        parameter("AMMUNITION_TYPE", "JHP", "JHP", "Munição ponta oca");
        parameter("AMMUNITION_TYPE", "LESS_LETHAL", "Menos letal", "Munição menos letal");

        parameter("PROJECTILE_TYPE", "OGIVAL", "Ogival", "Projétil ogival");
        parameter("PROJECTILE_TYPE", "HOLLOW_POINT", "Hollow point", "Projétil ponta oca");
        parameter("PROJECTILE_TYPE", "RUBBER", "Borracha", "Projétil de impacto controlado");

        parameter("CASE_TYPE", "BRASS", "Latão", "Estojo de latão");
        parameter("CASE_TYPE", "STEEL", "Aço", "Estojo de aço");
        parameter("CASE_TYPE", "POLYMER", "Polímero", "Estojo polimérico");

        parameter("PRIMER_TYPE", "CENTERFIRE", "Fogo central", "Espoleta de fogo central");
        parameter("PRIMER_TYPE", "RIMFIRE", "Fogo circular", "Espoleta de fogo circular");
        parameter("PRIMER_TYPE", "BOXER", "Boxer", "Espoleta Boxer");

        parameter("GRENADE_TYPE", "LESS_LETHAL", "Menos letal", "Granada menos letal");
        parameter("GRENADE_TYPE", "TRAINING", "Treinamento", "Granada de treinamento");
        parameter("GRENADE_TYPE", "SMOKE", "Fumígena", "Granada fumígena");

        parameter("AGENT", "OC", "OC", "Oleoresin capsicum");
        parameter("AGENT", "CS", "CS", "Agente lacrimogêneo");
        parameter("AGENT", "INERT", "Inerte", "Agente de treinamento");

        parameter("COMPOSITION", "OC_SOLUTION", "Solução OC", "Composição de demonstração");
        parameter("COMPOSITION", "CS_COMPOUND", "Composto CS", "Composição lacrimogênea");
        parameter("COMPOSITION", "INERT", "Composição inerte", "Composição para treinamento");

        parameter("PROTECTION_TYPE", "VEST", "Colete", "Proteção balística corporal");
        parameter("PROTECTION_TYPE", "PLATE", "Placa", "Placa balística");
        parameter("PROTECTION_TYPE", "HELMET", "Capacete", "Proteção balística craniana");

        parameter("PROTECTION_LEVEL", "IIIA", "Nível III-A", "Nível de proteção demonstrativo");
        parameter("PROTECTION_LEVEL", "III", "Nível III", "Nível de proteção demonstrativo");
        parameter("PROTECTION_LEVEL", "IV", "Nível IV", "Nível de proteção demonstrativo");

        parameter("MATERIAL", "ARAMID", "Aramida", "Material balístico");
        parameter("MATERIAL", "STEEL", "Aço", "Material metálico");
        parameter("MATERIAL", "POLYMER", "Polímero", "Material polimérico");

        parameter("SIZE", "P", "P", "Tamanho pequeno");
        parameter("SIZE", "M", "M", "Tamanho médio");
        parameter("SIZE", "G", "G", "Tamanho grande");

        parameter("CARTRIDGE_TYPE", "STANDARD", "Padrão", "Cartucho padrão");
        parameter("CARTRIDGE_TYPE", "EXTENDED", "Alcance estendido", "Cartucho de alcance estendido");

        parameter("OPTICAL_TYPE", "RED_DOT", "Red dot", "Mira reflexiva");
        parameter("OPTICAL_TYPE", "SCOPE", "Luneta", "Óptico de ampliação");
        parameter("OPTICAL_TYPE", "HOLOGRAPHIC", "Holográfica", "Mira holográfica");

        parameter("SHIELD_TYPE", "BALLISTIC", "Balístico", "Escudo balístico");
        parameter("SHIELD_TYPE", "RIOT", "Antitumulto", "Escudo antitumulto");

        parameter("LOCKING_MECHANISM", "DOUBLE_LOCK", "Trava dupla", "Mecanismo de algema");
        parameter("LOCKING_MECHANISM", "KEY", "Chave", "Travamento por chave");

        parameter("COMPONENT_TYPE", "MAGAZINE", "Carregador", "Componente de arma");
        parameter("COMPONENT_TYPE", "STOCK", "Coronha", "Componente de arma");
        parameter("COMPONENT_TYPE", "GRIP", "Empunhadura", "Componente de arma");

        parameter("COMPATIBILITY", "APX", "Beretta APX", "Compatibilidade de demonstração");
        parameter("COMPATIBILITY", "PICATINNY", "Picatinny", "Compatibilidade de trilho");
        parameter("COMPATIBILITY", "MLOK", "M-LOK", "Compatibilidade de trilho");

        parameter("INTERFACE", "PICATINNY", "Picatinny", "Interface de montagem");
        parameter("INTERFACE", "MLOK", "M-LOK", "Interface de montagem");
        parameter("INTERFACE", "DOVETAIL", "Dovetail", "Interface de montagem");
    }

    private void nature(String code, String name, String description) {
        if (count("select count(n) from OrganizationNature n where n.code = :code", code) > 0) return;
        OrganizationNature value = new OrganizationNature();
        value.code = code;
        value.name = name;
        value.description = description;
        value.active = true;
        entityManager.persist(value);
    }

    private void economicActivity(String code, String description) {
        if (count("select count(a) from EconomicActivity a where a.code = :code", code) > 0) return;
        EconomicActivity value = new EconomicActivity();
        value.code = code;
        value.description = description;
        value.active = true;
        entityManager.persist(value);
    }

    private void unitType(String code, String name, String description) {
        if (count("select count(t) from OrganizationalUnitType t where t.code = :code", code) > 0) return;
        OrganizationalUnitType value = new OrganizationalUnitType();
        value.code = code;
        value.name = name;
        value.description = description;
        value.active = true;
        entityManager.persist(value);
    }

    private void parameter(String type, String code, String name, String description) {
        Long existing = entityManager.createQuery(
                "select count(p) from ArmamentParameter p where p.parameterType = :type and p.code = :code",
                Long.class)
            .setParameter("type", type)
            .setParameter("code", code)
            .getSingleResult();
        if (existing > 0) return;

        ArmamentParameter value = new ArmamentParameter();
        value.parameterType = type;
        value.code = code;
        value.name = name;
        value.description = description;
        value.active = true;
        entityManager.persist(value);
    }

    private long count(String query, String code) {
        return entityManager.createQuery(query, Long.class)
            .setParameter("code", code)
            .getSingleResult();
    }
}
