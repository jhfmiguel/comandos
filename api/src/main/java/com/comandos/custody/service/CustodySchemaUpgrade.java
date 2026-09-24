package com.comandos.custody.service;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate adds recipient_unit_id; older schemas can still have recipient_id
 * marked NOT NULL. Detect the column through JDBC metadata so this startup
 * compatibility step works on Oracle, PostgreSQL and H2.
 */
@Component
public class CustodySchemaUpgrade implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public CustodySchemaUpgrade(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        Boolean required = jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, null, null)) {
                while (columns.next()) {
                    String table = columns.getString("TABLE_NAME");
                    String column = columns.getString("COLUMN_NAME");
                    if ("ERP_CUSTODY".equalsIgnoreCase(table)
                            && "RECIPIENT_ID".equalsIgnoreCase(column)) {
                        return columns.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls;
                    }
                }
            }
            return false;
        });

        if (!Boolean.TRUE.equals(required)) return;

        String product = jdbc.execute((ConnectionCallback<String>) connection ->
            connection.getMetaData().getDatabaseProductName());

        if (product != null && product.toLowerCase().contains("oracle")) {
            jdbc.execute("alter table erp_custody modify recipient_id null");
        } else {
            jdbc.execute("alter table erp_custody alter column recipient_id drop not null");
        }
    }
}
