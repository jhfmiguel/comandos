# Disposal and physical destruction lifecycle

## Scope

COMANDOS treats logical disposal (baixa) and physical destruction as related but distinct events. Logical disposal is the irreversible inventory/accountability transition. Physical destruction is optional evidence describing how material that was logically disposed of was physically destroyed.

Primary API: `/api/erp/disposals`.

## Logical disposal

A disposal can only be finalized when all of the following are true:

- organization, process number and reason are provided;
- the caller has both `CREATE` and `APPROVE` permission in the selected scope;
- irreversible-disposal confirmation is explicit;
- the request ID is a valid idempotency key and has not been reused with another payload;
- the organization/unit and every stock location are in scope;
- there is no open/counted physical inventory locking the location;
- every line identifies exactly one serialized asset or one stock balance;
- serialized assets are `AVAILABLE` and use quantity `1`;
- lot balances have enough available stock and no active reservation;
- the same inventory selection is not duplicated inside the request.

Finalization creates one `DisposalProcess` in terminal `FINALIZED` state and one negative `DISPOSAL` stock movement for each item.

### Serialized assets

For an individual asset, logical disposal changes the asset to terminal `DISPOSED` and creates a `DISPOSAL = -1` movement. The asset cannot be made available again by custody, maintenance, transfer, inspection, recovery or exceptional-occurrence workflows. `DISPOSED`, `SOLD` and `DONATED` remain terminal lifecycle states.

### Lot-controlled stock

For a lot/balance, disposal subtracts the finalized quantity from both location availability and aggregate lot availability. The movement quantity is the exact negative disposed quantity. A disposal cannot overdraw stock or bypass active reservations.

## Physical destruction

Physical destruction is optional. When it is recorded, the three evidence fields are atomic and must be supplied together:

- destruction method;
- destruction date/time;
- destruction certificate/reference.

The destruction row is one-to-one with the disposal process, so a finalized process has at most one physical-destruction record. The physical record does not reactivate, replace or reverse the logical disposal.

## Idempotency and concurrency

`requestId` is globally unique for disposal finalization and is persisted with a request fingerprint. Repeating the same request returns the original process. Reusing the request ID with a different payload is rejected.

The service locks the inventory catalog and selected inventory rows before applying terminal state or quantity changes, preventing concurrent disposal from consuming the same availability twice.

## Audit and historical traceability

The finalized process preserves organization/unit snapshots, process number, reason, finalization timestamp and operator. Each item preserves model, SKU, stock code, location, unit of measure, quantity and its immutable stock movement. The finalization audit records the resulting disposal and the stock changes.

## Oracle/demo regression

The demo Oracle gate now validates that:

- at least one finalized disposal process exists;
- organization/process/reason/finalizer provenance is present;
- request IDs are valid UUIDs and unique;
- request fingerprints have the canonical 64-character representation;
- process numbers are unique inside an organization;
- every process contains 1 to 100 items;
- every item references exactly one asset or lot;
- item quantities are positive and inventory snapshots are preserved;
- every item has a `DISPOSAL` movement with reference type `DISPOSAL` and the process ID;
- movement quantity equals the exact negative disposed quantity;
- movement location/time/operator provenance is coherent with finalization;
- serialized assets use quantity one and remain terminal `DISPOSED`;
- lot aggregate availability never becomes negative;
- physical destruction, when present, has method, timestamp and certificate and is not future-dated.

The dedicated demo seed executes before the legacy disposition seed, so the homologation scenario uses the same disposal semantics as the production service instead of the older positive-movement demonstration data.

## Closure decision

The disposal lifecycle is considered functionally closed when logical terminal disposal, optional physical destruction evidence, authorization (`CREATE` + `APPROVE`), state incompatibilities, asset/lot handling, inventory locking, idempotency and Oracle regression all remain green together. Physical destruction is evidence after or alongside a logical write-off; it is never a substitute for the terminal inventory transition.
