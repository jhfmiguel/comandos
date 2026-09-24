package com.comandos.dashboard.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class DashboardService {

    private static final DateTimeFormatter MONTH_LABEL =
        DateTimeFormatter.ofPattern("MMM/yy", new Locale("pt", "BR"));

    private final EntityManager em;

    public DashboardService(EntityManager em) {
        this.em = em;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generatedAt", LocalDateTime.now().toString());
        result.put("kpis", kpis());
        result.put("trend", monthlyTrend());
        result.put("assetCategories", groupCount(
            "select a.model.category.name, count(a) from AssetItem a group by a.model.category.name order by count(a) desc"
        ));
        result.put("assetStatuses", groupCount(
            "select a.status, count(a) from AssetItem a group by a.status order by count(a) desc"
        ));
        result.put("categoryValues", groupDecimal(
            "select a.model.category.name, sum(a.currentValue) from AssetItem a group by a.model.category.name order by sum(a.currentValue) desc"
        ));
        result.put("stockComposition", stockComposition());
        result.put("locations", locations());
        result.put("operations", operations());
        result.put("lotExpiry", lotExpiry());
        return result;
    }

    private Map<String, Object> kpis() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("totalAssets", count("AssetItem"));
        values.put("availableAssets", countWhere("AssetItem", "status", "AVAILABLE"));
        values.put("activeCustodies", countWhere("Custody", "status", "ACTIVE"));
        values.put("openMaintenance", countWhere("WorkOrder", "status", "OPEN"));
        values.put("totalLots", count("StockLot"));
        values.put("stockAvailable", sum("select sum(b.available) from StockBalance b"));
        values.put("stockReserved", sum("select sum(b.reserved) from StockBalance b"));
        values.put("stockBlocked", sum("select sum(b.blocked) from StockBalance b"));
        return values;
    }

    private List<Map<String, Object>> monthlyTrend() {
        YearMonth firstMonth = YearMonth.now().minusMonths(11);
        LocalDateTime start = firstMonth.atDay(1).atStartOfDay();

        Map<YearMonth, Long> movements = new HashMap<>();
        em.createQuery(
                "select m.movedAt from StockMovement m where m.movedAt >= :start",
                LocalDateTime.class)
            .setParameter("start", start)
            .getResultList()
            .forEach(value -> movements.merge(YearMonth.from(value), 1L, Long::sum));

        Map<YearMonth, Long> custodies = new HashMap<>();
        em.createQuery(
                "select c.deliveredAt from Custody c where c.deliveredAt >= :start",
                LocalDateTime.class)
            .setParameter("start", start)
            .getResultList()
            .forEach(value -> custodies.merge(YearMonth.from(value), 1L, Long::sum));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth month = firstMonth.plusMonths(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", MONTH_LABEL.format(month.atDay(1)));
            row.put("movements", movements.getOrDefault(month, 0L));
            row.put("custodies", custodies.getOrDefault(month, 0L));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> stockComposition() {
        return List.of(
            metric("Disponível", sum("select sum(b.available) from StockBalance b")),
            metric("Reservado", sum("select sum(b.reserved) from StockBalance b")),
            metric("Bloqueado", sum("select sum(b.blocked) from StockBalance b"))
        );
    }

    private List<Map<String, Object>> locations() {
        Map<String, Map<String, Object>> rows = new LinkedHashMap<>();

        for (Object[] row : em.createQuery(
                "select a.location.name, count(a) from AssetItem a group by a.location.name order by count(a) desc",
                Object[].class).getResultList()) {
            String name = String.valueOf(row[0]);
            Map<String, Object> item = rows.computeIfAbsent(name, this::locationMetric);
            item.put("assets", ((Number) row[1]).longValue());
        }

        for (Object[] row : em.createQuery(
                "select b.location.name, sum(b.available) from StockBalance b group by b.location.name order by sum(b.available) desc",
                Object[].class).getResultList()) {
            String name = String.valueOf(row[0]);
            Map<String, Object> item = rows.computeIfAbsent(name, this::locationMetric);
            item.put("stockUnits", decimal(row[1]));
        }

        return new ArrayList<>(rows.values());
    }

    private Map<String, Object> locationMetric(String name) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("label", name);
        value.put("assets", 0L);
        value.put("stockUnits", BigDecimal.ZERO);
        return value;
    }

    private List<Map<String, Object>> operations() {
        return List.of(
            metric("Cautelas", count("Custody")),
            metric("Reservas", count("InventoryReservation")),
            metric("Transferências", count("InventoryTransfer")),
            metric("Consumos", count("AmmunitionConsumption")),
            metric("Compras", count("Purchase")),
            metric("Vendas", count("InventorySale")),
            metric("Doações", count("Donation")),
            metric("Desfazimentos", count("DisposalProcess")),
            metric("Manutenções", count("WorkOrder")),
            metric("Inventários", count("InventoryCount"))
        );
    }

    private List<Map<String, Object>> lotExpiry() {
        long expired = 0;
        long ninetyDays = 0;
        long oneYear = 0;
        long later = 0;
        long noExpiry = 0;

        LocalDate today = LocalDate.now();
        LocalDate ninety = today.plusDays(90);
        LocalDate year = today.plusYears(1);

        for (LocalDate value : em.createQuery("select l.validUntil from StockLot l", LocalDate.class).getResultList()) {
            if (value == null) noExpiry++;
            else if (value.isBefore(today)) expired++;
            else if (!value.isAfter(ninety)) ninetyDays++;
            else if (!value.isAfter(year)) oneYear++;
            else later++;
        }

        return List.of(
            metric("Vencidos", expired),
            metric("Até 90 dias", ninetyDays),
            metric("91 a 365 dias", oneYear),
            metric("Mais de 1 ano", later),
            metric("Sem validade", noExpiry)
        );
    }

    private List<Map<String, Object>> groupCount(String jpql) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : em.createQuery(jpql, Object[].class).getResultList()) {
            result.add(metric(String.valueOf(row[0]), ((Number) row[1]).longValue()));
        }
        return result;
    }

    private List<Map<String, Object>> groupDecimal(String jpql) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : em.createQuery(jpql, Object[].class).getResultList()) {
            result.add(metric(String.valueOf(row[0]), decimal(row[1])));
        }
        return result;
    }

    private long count(String entityName) {
        return em.createQuery("select count(e) from " + entityName + " e", Long.class).getSingleResult();
    }

    private long countWhere(String entityName, String field, String value) {
        return em.createQuery(
                "select count(e) from " + entityName + " e where e." + field + " = :value",
                Long.class)
            .setParameter("value", value)
            .getSingleResult();
    }

    private BigDecimal sum(String jpql) {
        Object value = em.createQuery(jpql).getSingleResult();
        return decimal(value);
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(String.valueOf(value));
    }

    private Map<String, Object> metric(String label, Object value) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("label", label);
        metric.put("value", value);
        return metric;
    }
}
