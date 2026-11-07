package com.comandos.sales.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.comandos.security.service.ComandosAccessPolicy;
import com.fariamiguel.app.audit.service.AuditService;
import com.fariamiguel.enterprise.sales.SalesOperationsDirectory;
import com.fariamiguel.enterprise.sales.SalesOperationsService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ComandosCommercialSalesControllerTests {
    private final SalesOperationsService sales = mock(SalesOperationsService.class);
    private final ComandosAccessPolicy access = mock(ComandosAccessPolicy.class);
    private final AuditService audit = mock(AuditService.class);
    private final ComandosCommercialSalesController controller = new ComandosCommercialSalesController(
            sales, access, audit);

    @Test
    void createsGenericOrderWithinAuthorizedOrganizationAndUnit() {
        when(audit.actor()).thenReturn(new AuditService.Actor(7L, "seller", "ACCOUNT"));
        var request = new ComandosCommercialSalesController.CreateOrder("ORDER-7", "7", 10L, 3L,
                7L, "PIX", "PROC-7", "LEGAL-7", "DOC-7",
                List.of(new ComandosCommercialSalesController.OrderLine("ASSET:91", "Rifle", BigDecimal.ONE,
                        new BigDecimal("1800.00"))));
        when(sales.createDocument(eq("comandos"), eq("ORDER-7"), eq("7"),
                eq(SalesOperationsDirectory.DocumentType.ORDER), org.mockito.ArgumentMatchers.anyList(),
                eq("BRL"), eq("7"), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(new SalesOperationsDirectory.SalesDocument("doc-7", "comandos", "ORDER-7", "7",
                        SalesOperationsDirectory.DocumentType.ORDER, SalesOperationsDirectory.DocumentStatus.DRAFT,
                        List.of(new SalesOperationsDirectory.Line("ASSET:91", "Rifle", BigDecimal.ONE,
                                new BigDecimal("1800.00"), BigDecimal.ZERO)), "BRL", Instant.now(), Instant.now(),
                        "7", null, Map.of("comandos.organizationId", "10", "comandos.unitId", "3")));

        var created = controller.create(request);

        assertThat(created.id()).isEqualTo("doc-7");
        verify(access).requireScope("sales", "CREATE", 10L, 3L);
    }

    @Test
    void filtersReadsByRequestedOrganizationAndUnitBeforeReturning() {
        var accessible = new SalesOperationsDirectory.SalesDocument("in-scope", "comandos", "A", "7",
                SalesOperationsDirectory.DocumentType.ORDER, SalesOperationsDirectory.DocumentStatus.DRAFT,
                List.of(new SalesOperationsDirectory.Line("ASSET:91", "Rifle", BigDecimal.ONE,
                        new BigDecimal("1800.00"), BigDecimal.ZERO)), "BRL", Instant.now(), Instant.now(),
                "7", null, Map.of("comandos.organizationId", "10", "comandos.unitId", "3"));
        var other = new SalesOperationsDirectory.SalesDocument("out-of-scope", "comandos", "B", "7",
                SalesOperationsDirectory.DocumentType.ORDER, SalesOperationsDirectory.DocumentStatus.DRAFT,
                List.of(new SalesOperationsDirectory.Line("ASSET:92", "Pistol", BigDecimal.ONE,
                        new BigDecimal("700.00"), BigDecimal.ZERO)), "BRL", Instant.now(), Instant.now(),
                "7", null, Map.of("comandos.organizationId", "11", "comandos.unitId", "3"));
        when(sales.listDocuments("comandos", null)).thenReturn(List.of(accessible, other));

        var result = controller.list(null, 10L, 3L);

        assertThat(result).containsExactly(accessible);
        verify(access).requireScope("sales", "READ", 10L, 3L);
    }
}
