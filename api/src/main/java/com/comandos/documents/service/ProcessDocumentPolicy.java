package com.comandos.documents.service;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProcessDocumentPolicy {
    public static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf", "image/jpeg", "image/png", "image/webp",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    public record Rule(String permissionResource, Set<String> allowedTypes, Set<String> requiredTypes) {}

    private final Map<String, Rule> rules = Map.ofEntries(
        Map.entry("ACQUISITION", rule("purchases", set("PROCESS","NOTICE","CONTRACT","COMMITMENT","INVOICE","TERM","REPORT","OTHER"), set("PROCESS"))),
        Map.entry("RECEIVING", rule("purchases", set("INVOICE","RECEIPT_TERM","INSPECTION_REPORT","DIVERGENCE_REPORT","PHOTO","OTHER"), Set.of())),
        Map.entry("INCORPORATION", rule("purchases", set("INCORPORATION_TERM","PATRIMONY_RECORD","INSPECTION_REPORT","PHOTO","OTHER"), Set.of())),
        Map.entry("CUSTODY", rule("custodies", set("CUSTODY_TERM","RESPONSIBILITY_TERM","RETURN_TERM","DAMAGE_REPORT","PHOTO","OTHER"), Set.of())),
        Map.entry("TRANSFER", rule("transfers", set("TRANSFER_TERM","DISPATCH_TERM","RECEIPT_TERM","REJECTION_TERM","PHOTO","OTHER"), Set.of())),
        Map.entry("DONATION", rule("donations", set("DONATION_TERM","RECEIPT_TERM","LEGAL_INSTRUMENT","PHOTO","OTHER"), set("DONATION_TERM"))),
        Map.entry("SALE", rule("sales", set("PROCESS","LEGAL_BASIS","SALE_TERM","INVOICE","RECEIPT","RETURN_TERM","CANCELLATION_TERM","OTHER"), set("PROCESS"))),
        Map.entry("CONSUMABLE_USAGE", rule("ammunition-consumptions", set("DELIVERY_TERM","USAGE_REPORT","RETURN_TERM","OPERATION_REPORT","PHOTO","OTHER"), Set.of())),
        Map.entry("MAINTENANCE", rule("maintenance", set("WORK_ORDER","DIAGNOSIS_REPORT","SERVICE_REPORT","PART_INVOICE","FUNCTIONAL_TEST","CERTIFICATE","PHOTO","OTHER"), Set.of())),
        Map.entry("INSPECTION", rule("inspections", set("CHECKLIST","INSPECTION_REPORT","PHOTO","CERTIFICATE","OTHER"), Set.of())),
        Map.entry("OCCURRENCE", rule("occurrences", set("OCCURRENCE_REPORT","POLICE_REPORT","INVESTIGATION_REPORT","RECOVERY_TERM","SEIZURE_TERM","PHOTO","OTHER"), Set.of())),
        Map.entry("DISPOSAL", rule("disposals", set("PROCESS","APPROVAL_TERM","DESTRUCTION_CERTIFICATE","DISPOSAL_CERTIFICATE","PHOTO","OTHER"), set("PROCESS"))),
        Map.entry("EQUIPMENT_SET_OPERATION", rule("inventory", set("AGGREGATE_OPERATION_TERM","COMPONENT_LIST","PHOTO","OTHER"), Set.of()))
    );

    public Rule rule(String processType) {
        String normalized = normalize(processType);
        Rule rule = rules.get(normalized);
        if (rule == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported process type for attachments: " + processType);
        return rule;
    }

    public Set<String> processTypes() { return Collections.unmodifiableSet(rules.keySet()); }

    public String validateDocumentType(String processType, String documentType) {
        if (documentType == null || documentType.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document type is required.");
        String normalized = normalize(documentType);
        if (!rule(processType).allowedTypes().contains(normalized))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document type is not allowed for this process.");
        return normalized;
    }

    public void validateFile(String contentType, long size) {
        if (size <= 0 || size > MAX_FILE_SIZE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment size must be between 1 byte and 20 MB.");
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported attachment content type.");
    }

    private static Rule rule(String permission, Set<String> allowed, Set<String> required) {
        return new Rule(permission, Collections.unmodifiableSet(allowed), Collections.unmodifiableSet(required));
    }
    private static Set<String> set(String... values) { return Set.of(values); }
    public static String normalize(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }
}
