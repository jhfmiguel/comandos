package com.comandos.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fariamiguel.app.reservation.dto.ReservationContract.CreateRequest;
import com.fariamiguel.app.reservation.dto.ReservationContract.ReservationView;
import com.fariamiguel.app.reservation.service.ReservationService;
import com.fariamiguel.core.persistence.PlatformSettingStore;
import com.fariamiguel.enterprise.sales.SalesOperationsDirectory;
import com.fariamiguel.enterprise.sales.SalesOperationsService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class ComandosSalesOrderFulfillmentProviderTests {
    private final ReservationService reservations = mock(ReservationService.class);
    private final PlatformSettingStore settings = mock(PlatformSettingStore.class);
    private final ObjectProvider<SalesOperationsService> sales = mock(ObjectProvider.class);
    private final ComandosSalesOrderFulfillmentProvider provider = new ComandosSalesOrderFulfillmentProvider(
            reservations, mock(com.fariamiguel.app.sales.service.InventorySalesService.class), settings,
            sales, Duration.ofHours(24));

    @Test
    void reservesTypedStockInsideTheOrderOrganizationAndUnit() {
        when(settings.withLock(anyString(), any())).thenAnswer(invocation ->
                ((Supplier<?>) invocation.getArgument(1)).get());
        when(settings.get(anyString())).thenReturn(Optional.empty());
        when(reservations.create(any(CreateRequest.class))).thenReturn(new ReservationView(57, 12,
                "Organization", 4L, "Unit", "Sales order ORDER-57", "2026-11-06T12:00:00",
                "2026-11-07T12:00:00", "ACTIVE", "Active", "2026-11-06T12:00:00",
                null, "operator", List.of()));
        var order = new SalesOperationsDirectory.SalesDocument("order-57", "comandos", "ORDER-57",
                "customer-8", SalesOperationsDirectory.DocumentType.ORDER,
                SalesOperationsDirectory.DocumentStatus.APPROVED,
                List.of(new SalesOperationsDirectory.Line("ASSET:81", "Asset", BigDecimal.ONE,
                        new BigDecimal("125.00"), BigDecimal.ZERO)), "BRL", Instant.now(), Instant.now(),
                "actor-1", null, Map.of("comandos.organizationId", "12", "comandos.unitId", "4",
                        "comandos.buyerId", "8", "comandos.paymentMethod", "PIX",
                        "comandos.processNumber", "PROC-57", "comandos.legalBasis", "LEGAL-57"));

        var decision = provider.reserve("comandos", order, new BigDecimal("125.00"), "reserve-order-57");

        assertThat(decision.eligible()).isTrue();
        assertThat(decision.reservationReference()).isEqualTo("COMANDOS-ORDER:order-57");
        var request = org.mockito.ArgumentCaptor.forClass(CreateRequest.class);
        verify(reservations).create(request.capture());
        assertThat(request.getValue().organizationId()).isEqualTo(12L);
        assertThat(request.getValue().unitId()).isEqualTo(4L);
        assertThat(request.getValue().items()).singleElement().satisfies(line -> {
            assertThat(line.assetId()).isEqualTo(81L);
            assertThat(line.balanceId()).isNull();
        });
    }

    @Test
    void refusesARequestForAnotherProductTenant() {
        assertThrows(SecurityException.class, () -> provider.reserve("another-product", null,
                BigDecimal.ONE, "unsafe-tenant"));
    }
}
