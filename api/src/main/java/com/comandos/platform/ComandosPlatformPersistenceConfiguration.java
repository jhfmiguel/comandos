package com.comandos.platform;

import com.fariamiguel.core.persistence.JdbcPlatformSettingStore;
import com.fariamiguel.core.persistence.PlatformSettingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** Supplies the shared Faria Miguel JDBC settings store when platform auto-configuration is absent. */
@Configuration(proxyBeanMethods = false)
public class ComandosPlatformPersistenceConfiguration {
    @Bean
    @ConditionalOnMissingBean(PlatformSettingStore.class)
    PlatformSettingStore comandosPlatformSettingStore(
            JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        return new JdbcPlatformSettingStore(jdbc, transactionManager);
    }
}
