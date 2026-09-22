package com.weaponsregistration.custody.controller;

import com.weaponsregistration.custody.service.CustodySchemaUpgrade;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

class CustodySchemaUpgradeTests {
    @Test
    void upgradesLegacyPersonRequirementWithoutLosingExistingRecipientsAndCanRunAgain() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:custody-upgrade;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("create table erp_custody (id bigint primary key, recipient_id bigint not null, recipient_unit_id bigint)");
        jdbc.update("insert into erp_custody values (1, 10, null)");
        var upgrade = new CustodySchemaUpgrade(jdbc);
        upgrade.run(null); upgrade.run(null);
        jdbc.update("insert into erp_custody values (2, null, 20)");
        assertEquals(10L, jdbc.queryForObject("select recipient_id from erp_custody where id=1", Long.class));
        assertEquals(20L, jdbc.queryForObject("select recipient_unit_id from erp_custody where id=2", Long.class));
    }
}
