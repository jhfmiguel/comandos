package com.comandos.workflow.api;

import com.comandos.core.api.PlatformPage;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class WorkflowApiContractsTests {

    @Test
    void requestKeepsReusableWorkflowInput() {
        var request = new WorkflowRequest(
            10L,
            20L,
            "TRANSFER",
            "inventory/assets",
            30L,
            "Operational need"
        );

        assertEquals(10L, request.organizationId());
        assertEquals(20L, request.unitId());
        assertEquals("TRANSFER", request.operationType());
        assertEquals("inventory/assets", request.resource());
        assertEquals(30L, request.recordId());
        assertEquals("Operational need", request.justification());
    }

    @Test
    void transitionKeepsJustification() {
        var transition = new WorkflowTransition("Approved after analysis");

        assertEquals("Approved after analysis", transition.justification());
    }

    @Test
    void viewExposesWorkflowTimelineWithoutInternalEntities() {
        var event = new WorkflowEventView(
            1L,
            "REQUESTED",
            "ANALYZED",
            "Reviewed",
            "2026-09-23T07:00:00",
            "operator"
        );

        var view = new WorkflowView(
            99L,
            10L,
            20L,
            "TRANSFER",
            "inventory/assets",
            30L,
            "ANALYZED",
            "Operational need",
            "2026-09-23T06:50:00",
            "requester",
            null,
            null,
            null,
            null,
            null,
            List.of(event)
        );

        assertEquals("ANALYZED", view.status());
        assertEquals(1, view.events().size());
        assertEquals("operator", view.events().getFirst().actorLogin());
    }

    @Test
    void pageKeepsPaginationMetadata() {
        var page = new PlatformPage<>(
            List.of("one", "two"),
            12,
            1,
            20
        );

        assertEquals(List.of("one", "two"), page.content());
        assertEquals(12, page.totalElements());
        assertEquals(1, page.page());
        assertEquals(20, page.size());
    }
}
