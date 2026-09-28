# Uniform approval workflow policy

## Purpose

COMANDOS uses one approval state machine for sensitive operations. Domain modules keep their own business states, but authorization of a sensitive action follows a common lifecycle and audit history.

## Canonical lifecycle

`REQUESTED -> ANALYZED -> AUTHORIZED -> EXECUTED -> CONCLUDED`

A workflow may move to `CANCELLED` from any non-terminal state. `CONCLUDED` and `CANCELLED` are terminal.

No normal transition may skip analysis, authorization or execution.

## Transition permissions

- REQUESTED: CREATE on the workflow's business resource.
- ANALYZED: UPDATE on the workflow's business resource.
- AUTHORIZED: APPROVE on the workflow's business resource.
- EXECUTED: UPDATE on the workflow's business resource.
- CONCLUDED: UPDATE on the workflow's business resource.
- CANCELLED: UPDATE on the workflow's business resource.

The workflow service no longer authorizes every operation against `inventory/assets`. The permission resource persisted in the workflow is used for every read and transition, so approval remains scoped to the business domain, organization and optional unit.

## Sensitive operation matrix

The uniform policy registers these operation types:

- ACQUISITION
- RECEIVING
- INCORPORATION
- CUSTODY
- TRANSFER
- DONATION
- SALE
- CONSUMABLE_USAGE
- MAINTENANCE
- INSPECTION
- OCCURRENCE_RESOLUTION
- DISPOSAL
- STOCK_ADJUSTMENT
- INVENTORY_RECONCILIATION
- EQUIPMENT_SET_OPERATION

Every registered operation requires analysis and approval.

## Domain enforcement

`WorkflowApprovalGuard` is the common integration point for domain services.

Before performing a sensitive mutation, a domain service can call `requireAuthorized(...)`. Before a post-execution completion step it can call `requireExecuted(...)`, and downstream processes that depend on a fully completed authorization can call `requireConcluded(...)`.

The guard identifies a workflow by operation type, permission resource, business record, organization and optional unit. It rejects execution with HTTP 409 when the required approval state does not exist.

This keeps workflow authorization separate from each domain entity's own lifecycle while preventing each module from implementing a different approval lookup.

## Audit and immutable history

Every transition creates an `ApprovalWorkflowEvent` containing previous state, target state, justification, timestamp and actor. Workflow events are immutable and cannot be updated or deleted through JPA lifecycle operations.

The workflow aggregate is also recorded through the platform audit recorder on creation and on every transition, preserving before/after snapshots.

## Oracle regression

`WorkflowPolicyDemoVerifier` runs during demo/Oracle startup and checks:

- the sensitive-operation policy matrix;
- approval and analysis requirements;
- permission mapping, including APPROVE for authorization;
- workflow organization, resource, justification and request timestamp;
- event history beginning at REQUESTED;
- contiguous transition history with no skipped normal states;
- cancellation only from non-terminal states;
- persisted status matching the last event;
- authority evidence for authorized/executed/concluded workflows;
- execution, conclusion and cancellation timestamps;
- chronological request/authorization/execution/conclusion milestones.

A failure aborts the Oracle startup regression gate.
