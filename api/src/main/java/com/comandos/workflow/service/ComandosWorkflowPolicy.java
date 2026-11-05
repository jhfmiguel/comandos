package com.comandos.workflow.service;
import com.fariamiguel.app.workflow.service.WorkflowPolicy;

import com.fariamiguel.security.api.CurrentActor;
import com.fariamiguel.workflow.api.WorkflowDefinition;
import com.fariamiguel.workflow.api.WorkflowEngine;
import com.fariamiguel.workflow.api.WorkflowTransition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Adds custody approval and evaluates the shared path using the product workflow engine. */
@Component
@Primary
public class ComandosWorkflowPolicy extends WorkflowPolicy {
    private final WorkflowEngine engine;
    private final WorkflowDefinition definition;
    private final Map<String, Rule> productRules;

    public ComandosWorkflowPolicy(WorkflowEngine engine) {
        this.engine = engine;
        var rules = new LinkedHashMap<>(super.rules());
        rules.put("CUSTODY", new Rule("CUSTODY", true, true));
        productRules = Map.copyOf(rules);
        var states = new HashSet<>(MAIN_PATH);
        states.add(CANCELLED);
        var transitions = new ArrayList<WorkflowTransition>();
        for (int i = 0; i < MAIN_PATH.size() - 1; i++) {
            String from = MAIN_PATH.get(i);
            String next = MAIN_PATH.get(i + 1);
            transitions.add(new WorkflowTransition(from, next, next, null));
            transitions.add(new WorkflowTransition(from, CANCELLED, CANCELLED, null));
        }
        definition = new WorkflowDefinition("COMANDOS_APPROVAL", REQUESTED, states, transitions);
    }

    @Override
    public Map<String, Rule> rules() {
        return productRules;
    }

    @Override
    public void requireTransition(String fromStatus, String targetStatus) {
        String from = normalizeStatus(fromStatus);
        String target = normalizeStatus(targetStatus);
        if (!engine.evaluate(definition, from, target, CurrentActor.anonymous()).allowed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Invalid workflow transition from " + from + " to " + target + ".");
        }
    }
}