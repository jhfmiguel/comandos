package com.comandos.workflow.service;

import java.util.LinkedHashMap;
import java.util.List;
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

    public static final List<String> MAIN_PATH = List.of(
        REQUESTED,
        ANALYZED,
        AUTHORIZED,
        EXECUTED,
        CONCLUDED
    );

    public record Rule(String operationType, boolean approvalRequired, boolean analysisRequired) {}

    private static final Map<String, Rule> RULES = buildRules();
    private static final Set<String> TERMINAL = Set.of(CONCLUDED, CANCELLED);
    private static final Map<String, String> EXPECTED_PREVIOUS = Map.of(
        ANALYZED, REQUESTED,
        AUTHORIZED, ANALYZED,
        EXECUTED, AUTHORIZED,
        CONCLUDED, EXECUTED
    );

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
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unsupported workflow state: " + targetStatus);
        };
    }

    public String expectedPrevious(String targetStatus) {
        return EXPECTED_PREVIOUS.get(targetStatus);
    }

    public void requireTransition(String fromStatus, String targetStatus) {
        String from = normalizeStatus(fromStatus);
        String target = normalizeStatus(targetStatus);

        if (CANCELLED.equals(target)) {
            if (terminal(from)) {
                invalidTransition(from, target);
            }
            return;
        }

        String expected = expectedPrevious(target);
        if (expected == null || !expected.equals(from)) {
            invalidTransition(from, target);
        }
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operation type is required.");
        }
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    public static String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow state is required.");
        }
        String status = value.trim().toUpperCase(Locale.ROOT);
        if (!MAIN_PATH.contains(status) && !CANCELLED.equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported workflow state: " + status);
        }
        return status;
    }

    public static String normalizeResource(String value) {
        String resource = value == null ? "" : value.trim();
        if (resource.isBlank() || resource.length() > 100 || resource.startsWith("/")
                || resource.endsWith("/") || resource.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A valid permission resource is required.");
        }
        return resource;
    }

    private static void invalidTransition(String from, String target) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Invalid workflow transition from " + from + " to " + target + ".");
    }

    private static Map<String, Rule> buildRules() {
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
