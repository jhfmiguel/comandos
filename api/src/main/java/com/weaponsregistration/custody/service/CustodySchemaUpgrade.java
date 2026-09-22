package com.weaponsregistration.custody.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Hibernate adds recipient_unit_id; older schemas also need the person requirement relaxed. */
@Component
public class CustodySchemaUpgrade implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public CustodySchemaUpgrade(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        Integer required = jdbc.queryForObject("select count(*) from information_schema.columns "
            + "where lower(table_schema) = 'public' and lower(table_name) = 'erp_custody' "
            + "and lower(column_name) = 'recipient_id' and is_nullable = 'NO'", Integer.class);
        if (required != null && required > 0)
            jdbc.execute("alter table erp_custody alter column recipient_id drop not null");
    }
}
