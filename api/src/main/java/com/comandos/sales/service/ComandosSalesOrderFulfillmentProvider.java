package com.comandos.sales.service;

import com.fariamiguel.app.model.PaymentMethod;
import com.fariamiguel.app.reservation.dto.ReservationContract.CreateRequest;
import com.fariamiguel.app.reservation.dto.ReservationContract.LineRequest;
import com.fariamiguel.app.reservation.service.ReservationService;
import com.fariamiguel.app.sales.dto.SalesContract.FinalizeRequest;
import com.fariamiguel.app.sales.service.InventorySalesService;
import com.fariamiguel.core.persistence.PlatformSettingStore;
import com.fariamiguel.enterprise.sales.SalesOperationsDirectory;
import com.fariamiguel.enterprise.sales.SalesOperationsService;
import com.fariamiguel.enterprise.sales.SalesOrderFulfillmentProvider;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bridges generic sales orders to COMANDOS' scoped stock reservation and asset-sale services. */
@Service
public class ComandosSalesOrderFulfillmentProvider implements SalesOrderFulfillmentProvider {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ComandosSalesOrderFulfillmentProvider.class);
    private static final String TENANT = "comandos";
    private static final String RESERVATION_PREFIX = "COMANDOS_SALES_RESERVATION_";
    private final ReservationService reservations;
    private final InventorySalesService inventorySales;
    private final PlatformSettingStore settings;
    private final ObjectProvider<SalesOperationsService> salesOperations;
    private final Clock clock;
    private final Duration reservationTtl;

    public ComandosSalesOrderFulfillmentProvider(ReservationService reservations,
            InventorySalesService inventorySales, PlatformSettingStore settings,
            @Lazy ObjectProvider<SalesOperationsService> salesOperations,
            @Value("${comandos.sales.reservation-ttl:PT24H}") Duration reservationTtl) {
        this.reservations = Objects.requireNonNull(reservations);
        this.inventorySales = Objects.requireNonNull(inventorySales);
        this.settings = Objects.requireNonNull(settings);
        this.salesOperations = Objects.requireNonNull(salesOperations);
        this.clock = Clock.systemUTC();
        this.reservationTtl = Objects.requireNonNull(reservationTtl);
        if (reservationTtl.isNegative() || reservationTtl.isZero())
            throw new IllegalArgumentException("sales reservation TTL must be positive");
    }

    @Override
    @Transactional
    public Decision reserve(String tenant, SalesOperationsDirectory.SalesDocument order,
            BigDecimal payableAmount, String idempotencyKey) {
        requireTenant(tenant);
        Objects.requireNonNull(order, "order");
        if (!order.tenant().equals(tenant)) throw new SecurityException("order tenant mismatch");
        return settings.withLock(lockKey(order.id()), () -> {
            String current = settings.get(reservationKey(order.id())).orElse(null);
            if (current != null) return new Decision(true, virtualReference(order.id()), null);
            try {
                requireSaleTerms(order, payableAmount);
                var view = reservations.create(reservationRequest(order, order.lines(), idempotencyKey));
                settings.put(reservationKey(order.id()), Long.toString(view.id()), "COMANDOS_SALES_RESERVATION");
                return new Decision(true, virtualReference(order.id()), null);
            } catch (org.springframework.web.server.ResponseStatusException | IllegalArgumentException rejected) {
                return new Decision(false, null, safeReason(rejected));
            }
        });
    }

    @Override
    @Transactional
    public void adjust(String tenant, String orderId, String reservationReference,
            Map<String, BigDecimal> remainingQuantities, String idempotencyKey) {
        requireTenant(tenant);
        requireReference(orderId, reservationReference);
        settings.withLock(lockKey(orderId), () -> {
            var order = order(orderId);
            String oldReservation = settings.get(reservationKey(orderId)).orElseThrow(
                    () -> new IllegalStateException("COMANDOS order reservation is missing"));
            reservations.release(Long.parseLong(oldReservation), "CANCELLED");
            List<SalesOperationsDirectory.Line> remaining = selectLines(order, remainingQuantities);
            if (remaining.isEmpty()) {
                settings.delete(reservationKey(orderId));
            } else {
                var view = reservations.create(reservationRequest(order, remaining, idempotencyKey));
                settings.put(reservationKey(orderId), Long.toString(view.id()), "COMANDOS_SALES_RESERVATION");
            }
            return Boolean.TRUE;
        });
    }

    @Override
    @Transactional
    public void fulfill(String tenant, String orderId, String reservationReference,
            List<SalesOperationsDirectory.FulfilledLine> fulfilledLines, String idempotencyKey) {
        requireTenant(tenant);
        requireReference(orderId, reservationReference);
        try { settings.withLock(lockKey(orderId), () -> {
            var order = order(orderId);
            String current = settings.get(reservationKey(orderId)).orElse(null);
            if (current != null) reservations.release(Long.parseLong(current), "CANCELLED");
            List<SalesOperationsDirectory.Line> delivered = selectLines(order, fulfilledLines);
            if (delivered.isEmpty()) throw new IllegalArgumentException("fulfillment requires at least one line");
            requireSaleTerms(order, order.total().setScale(currencyScale(order.currency()),
                    java.math.RoundingMode.HALF_EVEN));
            if (delivered.stream().anyMatch(line -> line.discount().signum() != 0))
                throw new IllegalArgumentException("COMANDOS fulfillment does not accept discounted lines");
            var request = saleRequest(order, delivered, idempotencyKey);
            var completedSale = inventorySales.finalizeSale(request);
            BigDecimal inventoryTotal = new BigDecimal(completedSale.total());
            BigDecimal customerPayable = delivered.stream().map(SalesOperationsDirectory.Line::total)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(currencyScale(order.currency()),
                            java.math.RoundingMode.HALF_EVEN);
            BigDecimal roundingAdjustment = customerPayable.subtract(inventoryTotal);
            if (roundingAdjustment.signum() != 0) {
                String adjustmentKey = "COMANDOS_SALES_ROUNDING_" + deriveKey(idempotencyKey);
                String adjustment = "order=" + orderId + ";sale=" + completedSale.id() + ";payable="
                        + customerPayable.toPlainString() + ";inventory=" + inventoryTotal.toPlainString()
                        + ";adjustment=" + roundingAdjustment.toPlainString();
                settings.put(adjustmentKey, adjustment, "SALES_ROUNDING_ADJUSTMENT");
            }

            Map<String, BigDecimal> remaining = quantities(order.lines());
            for (var line : delivered) remaining.merge(line.itemId(), line.quantity().negate(), BigDecimal::add);
            for (var previous : salesOperations.getObject().deliveries(tenant, orderId))
                for (var line : previous.lines()) remaining.merge(line.itemId(), line.quantity().negate(), BigDecimal::add);
            for (var previous : salesOperations.getObject().orderCancellations(tenant, orderId))
                for (var line : previous.lines()) remaining.merge(line.itemId(), line.quantity().negate(), BigDecimal::add);
            remaining.replaceAll((item, quantity) -> quantity.max(BigDecimal.ZERO));
            if (remaining.values().stream().anyMatch(quantity -> quantity.signum() > 0)) {
                List<SalesOperationsDirectory.Line> outstanding = selectLines(order, remaining);
                var view = reservations.create(reservationRequest(order, outstanding,
                        deriveKey(idempotencyKey + "|remaining")));
                settings.put(reservationKey(orderId), Long.toString(view.id()), "COMANDOS_SALES_RESERVATION");
            } else {
                settings.delete(reservationKey(orderId));
            }
            return Boolean.TRUE;
        }); } catch (RuntimeException failure) { LOG.error("COMANDOS order fulfillment failed", failure); throw failure; }
    }

    @Override
    @Transactional
    public void release(String tenant, String orderId, String reservationReference, String idempotencyKey) {
        requireTenant(tenant);
        requireReference(orderId, reservationReference);
        settings.withLock(lockKey(orderId), () -> {
            settings.get(reservationKey(orderId)).ifPresent(id -> reservations.release(Long.parseLong(id), "CANCELLED"));
            settings.delete(reservationKey(orderId));
            return Boolean.TRUE;
        });
    }

    private CreateRequest reservationRequest(SalesOperationsDirectory.SalesDocument order,
            List<SalesOperationsDirectory.Line> lines, String idempotencyKey) {
        var scope = scope(order);
        LocalDateTime start = LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(clock.instant().plus(reservationTtl), ZoneId.systemDefault());
        return new CreateRequest(deriveKey(idempotencyKey), scope.organizationId(), scope.unitId(),
                "Sales order " + order.reference(), start.toString(), end.toString(), lines.stream()
                    .map(line -> inventoryLine(line.itemId(), line.quantity())).toList());
    }

    private FinalizeRequest saleRequest(SalesOperationsDirectory.SalesDocument order,
            List<SalesOperationsDirectory.Line> lines, String idempotencyKey) {
        var scope = scope(order);
        PaymentMethod payment = PaymentMethod.valueOf(metadata(order, "comandos.paymentMethod").toUpperCase(Locale.ROOT));
        return new FinalizeRequest(deriveKey(idempotencyKey), scope.organizationId(),
                positiveLong(order.metadata().get("comandos.buyerId"), "comandos.buyerId"), payment,
                lines.stream().map(line -> saleLine(line.itemId(), line.quantity(), line.unitPrice())).toList(),
                scope.unitId(), metadata(order, "comandos.processNumber"), metadata(order, "comandos.legalBasis"),
                order.metadata().getOrDefault("comandos.documentReference", order.reference()));
    }

    private static LineRequest inventoryLine(String itemId, BigDecimal quantity) {
        StockReference ref = stockReference(itemId);
        return new LineRequest(ref.assetId(), ref.balanceId(), quantity);
    }

    private static com.fariamiguel.app.sales.dto.SalesContract.LineRequest saleLine(
            String itemId, BigDecimal quantity, BigDecimal price) {
        StockReference ref = stockReference(itemId);
        return new com.fariamiguel.app.sales.dto.SalesContract.LineRequest(
                ref.assetId(), ref.balanceId(), quantity, price);
    }

    private static StockReference stockReference(String value) {
        if (value == null) throw new IllegalArgumentException("sale itemId must use ASSET:<id> or BALANCE:<id>");
        String[] parts = value.split(":", -1);
        if (parts.length != 2) throw new IllegalArgumentException("sale itemId must use ASSET:<id> or BALANCE:<id>");
        long id = positiveLong(parts[1], "stock item id");
        return switch (parts[0].toUpperCase(Locale.ROOT)) {
            case "ASSET" -> new StockReference(id, null);
            case "BALANCE" -> new StockReference(null, id);
            default -> throw new IllegalArgumentException("sale itemId must use ASSET:<id> or BALANCE:<id>");
        };
    }

    private static List<SalesOperationsDirectory.Line> selectLines(SalesOperationsDirectory.SalesDocument order,
            Map<String, BigDecimal> requested) {
        List<SalesOperationsDirectory.Line> result = new ArrayList<>();
        Map<String, BigDecimal> available = quantities(order.lines());
        requested.forEach((item, quantity) -> {
            if (!available.containsKey(item) || quantity == null || quantity.signum() < 0
                    || quantity.compareTo(available.get(item)) > 0)
                throw new IllegalArgumentException("requested fulfillment is outside the sales order");
            if (quantity.signum() > 0) {
                var source = order.lines().stream().filter(line -> line.itemId().equals(item)).findFirst().orElseThrow();
                result.add(new SalesOperationsDirectory.Line(item, source.description(), quantity,
                        source.unitPrice(), BigDecimal.ZERO));
            }
        });
        return List.copyOf(result);
    }

    private static List<SalesOperationsDirectory.Line> selectLines(SalesOperationsDirectory.SalesDocument order,
            List<SalesOperationsDirectory.FulfilledLine> requested) {
        Map<String, BigDecimal> quantities = new HashMap<>();
        for (var line : requested) quantities.merge(line.itemId(), line.quantity(), BigDecimal::add);
        return selectLines(order, quantities);
    }

    private static Map<String, BigDecimal> quantities(List<SalesOperationsDirectory.Line> lines) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (var line : lines) result.merge(line.itemId(), line.quantity(), BigDecimal::add);
        return result;
    }

    private void requireSaleTerms(SalesOperationsDirectory.SalesDocument order, BigDecimal amount) {
        scope(order);
        positiveLong(order.metadata().get("comandos.buyerId"), "comandos.buyerId");
        PaymentMethod.valueOf(metadata(order, "comandos.paymentMethod").toUpperCase(Locale.ROOT));
        metadata(order, "comandos.processNumber");
        metadata(order, "comandos.legalBasis");
        if (order.metadata().getOrDefault("comandos.documentReference", order.reference()).isBlank())
            throw new IllegalArgumentException("comandos.documentReference cannot be blank");
        if (!"BRL".equals(order.currency())) throw new IllegalArgumentException("COMANDOS inventory sales currently settle in BRL");
        BigDecimal lineTotal = BigDecimal.ZERO;
        for (var line : order.lines()) {
            if (line.discount().signum() != 0)
                throw new IllegalArgumentException("COMANDOS inventory sales do not accept generic discounts");
            stockReference(line.itemId());
            lineTotal = lineTotal.add(line.total());
        }
        if (lineTotal.compareTo(order.total()) != 0 || amount.compareTo(lineTotal.setScale(
                currencyScale(order.currency()), java.math.RoundingMode.HALF_EVEN)) != 0)
            throw new IllegalArgumentException("COMANDOS payable amount must match the currency-rounded undiscounted order total");
    }

    private SalesOperationsDirectory.SalesDocument order(String orderId) {
        return salesOperations.getObject().findDocument(TENANT, orderId)
                .filter(document -> document.type() == SalesOperationsDirectory.DocumentType.ORDER)
                .orElseThrow(() -> new IllegalArgumentException("COMANDOS sales order does not exist"));
    }

    private Scope scope(SalesOperationsDirectory.SalesDocument order) {
        long organization = positiveLong(order.metadata().get("comandos.organizationId"), "comandos.organizationId");
        Long unit = optionalPositiveLong(order.metadata().get("comandos.unitId"), "comandos.unitId");
        return new Scope(organization, unit);
    }

    private static String metadata(SalesOperationsDirectory.SalesDocument order, String key) {
        String value = order.metadata().get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required");
        return value.trim();
    }

    private static long positiveLong(String value, String name) {
        try {
            long parsed = Long.parseLong(Objects.requireNonNull(value, name));
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException(name + " must be a positive integer");
        }
    }

    private static Long optionalPositiveLong(String value, String name) {
        return value == null || value.isBlank() ? null : positiveLong(value, name);
    }

    private static void requireTenant(String tenant) {
        if (!TENANT.equals(tenant)) throw new SecurityException("COMANDOS sales adapter only serves its configured product tenant");
    }

    private static void requireReference(String orderId, String reference) {
        if (!virtualReference(orderId).equals(reference)) throw new SecurityException("sales reservation reference mismatch");
    }

    private static String virtualReference(String orderId) { return "COMANDOS-ORDER:" + orderId; }
    private static String reservationKey(String orderId) { return RESERVATION_PREFIX + orderId; }
    private static String lockKey(String orderId) { return RESERVATION_PREFIX + "LOCK_" + orderId; }
    private static String deriveKey(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }
    private static int currencyScale(String currency) {
        int scale = java.util.Currency.getInstance(currency).getDefaultFractionDigits();
        return scale < 0 ? 2 : scale;
    }
    private static String safeReason(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return "COMANDOS inventory validation rejected this order";
        return message.length() > 240 ? message.substring(0, 240) : message;
    }

    private record StockReference(Long assetId, Long balanceId) { }
    private record Scope(long organizationId, Long unitId) { }
}
