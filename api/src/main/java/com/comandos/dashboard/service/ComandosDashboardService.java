package com.comandos.dashboard.service;
import com.fariamiguel.app.dashboard.service.DashboardService;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@Transactional
public class ComandosDashboardService extends DashboardService {
    public ComandosDashboardService(EntityManager em) { super(em); }

    @Override
    protected Map<String, Object> kpis() {
        var values = super.kpis();
        values.put("activeCustodies", countWhere("Custody", "status", "ACTIVE"));
        return values;
    }

    @Override
    protected String consumptionEntity() { return "AmmunitionConsumption"; }

    @Override
    protected List<Map<String, Object>> operations() {
        var rows = new ArrayList<>(super.operations());
        rows.addFirst(metric("Cautelas", count("Custody")));
        return rows;
    }

    @Override
    protected void enrichMonthlyTrend(YearMonth firstMonth, List<Map<String, Object>> rows) {
        Map<YearMonth, Long> custodies = new HashMap<>();
        em.createQuery("select c.deliveredAt from Custody c where c.deliveredAt >= :start", LocalDateTime.class)
            .setParameter("start", firstMonth.atDay(1).atStartOfDay()).getResultList()
            .forEach(value -> custodies.merge(YearMonth.from(value), 1L, Long::sum));
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).put("custodies", custodies.getOrDefault(firstMonth.plusMonths(i), 0L));
        }
    }
}