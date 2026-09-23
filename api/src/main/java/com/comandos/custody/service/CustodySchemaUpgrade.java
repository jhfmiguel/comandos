package com.comandos.custody.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Hibernate adds recipient_unit_id; older Oracle schemas also need the person requirement relaxed. */
@Component
public class CustodySchemaUpgrade implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public CustodySchemaUpgrade(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        Integer required = jdbc.queryForObject(
            "select count(*) from user_tab_columns "
                + "where table_name = 'ERP_CUSTODY' "
                + "and column_name = 'RECIPIENT_ID' "
                + "and nullable = 'N'",
            Integer.class);
        if (required != null && required > 0)
            jdbc.execute("alter table erp_custody modify recipient_id null");
    }
}
