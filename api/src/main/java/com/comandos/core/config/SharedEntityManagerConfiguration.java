package com.comandos.core.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

/**
 * Exposes a transaction-aware EntityManager for COMANDOS services that use
 * constructor injection. The proxy delegates to the transaction-bound manager;
 * it neither creates a new persistence unit nor owns the underlying lifecycle.
 */
@Configuration(proxyBeanMethods = false)
public class SharedEntityManagerConfiguration {

    @Bean
    @Lazy
    public EntityManager comandosEntityManager(EntityManagerFactory entityManagerFactory) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }
}
