package com.comandos.documents.service;
import com.fariamiguel.app.documents.service.ProcessDocumentPolicy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Regras documentais exclusivas do COMANDOS.
 *
 * Regras comuns permanecem no FARIA MIGUEL.
 */
@Primary
@Component
public class ComandosProcessDocumentPolicy extends ProcessDocumentPolicy {

    private static final Rule CUSTODY_RULE = new Rule(
        "custodies",
        Set.of(
            "CUSTODY_TERM",
            "RESPONSIBILITY_TERM",
            "RETURN_TERM",
            "DAMAGE_REPORT",
            "PHOTO",
            "OTHER"
        ),
        Set.of()
    );

    private static final Rule AMMUNITION_RULE = new Rule(
        "ammunition-consumptions",
        Set.of(
            "DELIVERY_TERM",
            "USAGE_REPORT",
            "RETURN_TERM",
            "OPERATION_REPORT",
            "PHOTO",
            "OTHER"
        ),
        Set.of()
    );

    @Override
    public Rule rule(String processType) {

        String normalized = normalize(processType);

        if ("CUSTODY".equals(normalized)) {
            return CUSTODY_RULE;
        }

        if ("CONSUMABLE_USAGE".equals(normalized)) {
            return AMMUNITION_RULE;
        }

        return super.rule(processType);
    }

    @Override
    public Set<String> processTypes() {

        Set<String> values = new LinkedHashSet<>(
            super.processTypes()
        );

        values.add("CUSTODY");

        return Collections.unmodifiableSet(values);
    }
}