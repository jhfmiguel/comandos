package com.comandos.workflow.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class WorkflowPolicy {
    public static final String REQUESTED = "REQUESTED";
    public static final String ANALYZED = "ANALYZED";
    public static final String AUTHORIZED = "AUTHORIZED";
    public static final String EXECUTED = "EXECUTED";
    public static final String CONCLUDED = "CONCLUDED";
    public static final String CANCELLED = "CANCELLED";

    public record Rule(String operationType, boolean approvalRequired, boolean analysisRequired) {}

    private static final Map<String, Rule> RULES = rules();
    private static final Set<String> TERMINAL = Set.of(CONCLUDED, CANCELLED);

    public Rule rule(String operationType) {
        String type = normalize(operationType);
        Rule rule = RULES.get(type);
        if (rule == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Operation type is not registered in the uniform workflow policy: " + type);
        }
        return rule;
    }

    public Map<String, Rule> rules() {
        return RULES;
    }

    public boolean terminal(String status) {
        return TERMINAL.contains(status);
    }

    public String permissionForTransition(String targetStatus) {
        return switch (targetStatus) {
            case REQUESTED -> "CREATE";
            case ANALYZED -> "UPDATE";
            case AUTHORIZED -> "APPROVE";
            case EXECUTED -> "UPDATE";
            case CONCLUDED -> "UPDATE";
            case CANCELLED -> "UPDATE";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported workflow state: " + targetStatus);
        };
    }

    public String expectedPrevious(String targetStatus) {
        return switch (targetStatus) {
            case ANALYZED -> REQUESTED;
            case AUTHORIZED -> ANALYZED;
            case EXECUTED -> AUTHORIZED;
            case CONCLUDED -> EXECUTED;
            default -> null;
        };
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operation type is required.");
        }
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static Map<String, Rule> rules() {
        Map<String, Rule> values = new LinkedHashMap<>();
        sensitive(values, "ACQUISITION");
        sensitive(values, "RECEIVING");
        sensitive(values, "INCORPORATION");
        sensitive(values, "CUSTODY");
        sensitive(values, "TRANSFER");
        sensitive(values, "DONATION");
        sensitive(values, "SALE");
        sensitive(values, "CONSUMABLE_USAGE");
        sensitive(values, "MAINTENANCE");
        sensitive(values, "INSPECTION");
        sensitive(values, "OCCURRENCE_RESOLUTION");
        sensitive(values, "DISPOSAL");
        sensitive(values, "STOCK_ADJUSTMENT");
        sensitive(values, "INVENTORY_RECONCILIATION");
        sensitive(values, "EQUIPMENT_SET_OPERATION");
        return Map.copyOf(values);
    }

    private static void sensitive(Map<String, Rule> values, String type) {
        values.put(type, new Rule(type, true, true));
    }
}
