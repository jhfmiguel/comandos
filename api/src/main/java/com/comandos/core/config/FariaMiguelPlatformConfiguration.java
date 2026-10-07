package com.comandos.core.config;

import com.fariamiguel.core.api.IdGenerator;
import com.fariamiguel.core.api.PlatformClock;
import com.fariamiguel.core.service.SystemPlatformClock;
import com.fariamiguel.core.service.UuidGenerator;
import com.fariamiguel.workflow.api.WorkflowEngine;
import com.fariamiguel.workflow.service.DefaultWorkflowEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Product wiring for canonical Faria Miguel Platform Core services.
 *
 * <p>COMANDOS owns only the Spring composition. Implementations remain in
 * jhfmiguel/faria-miguel and must not be copied back into the product.</p>
 */
@Configuration
public class FariaMiguelPlatformConfiguration {

    @Bean
    PlatformClock platformClock() {
        return new SystemPlatformClock();
    }

    @Bean
    IdGenerator idGenerator() {
        return new UuidGenerator();
    }

    @Bean
    WorkflowEngine workflowEngine() {
        return new DefaultWorkflowEngine();
    }

}
