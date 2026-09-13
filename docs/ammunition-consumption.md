# Ammunition consumption

The ammunition consumption module records the authorized use or deflagration of ammunition and performs the related inventory deduction in one database transaction.

## Domain model

- `AmmunitionConsumption` identifies the organization, optional unit, responsible person, authorizer, purpose, finalization time, operator and idempotency key.
- `AmmunitionConsumptionItem` identifies the stock balance, lot, location, quantity, result and generated stock movement. It also stores descriptive snapshots for permanent history.
- A finalized item generates a negative `StockMovement` with nature `AMMUNITION_CONSUMPTION` and deducts the exact decimal quantity from both `StockBalance.available` and `StockLot.availableQuantity`.

Only active, unexpired stock classified as `AMMUNITION`, lot controlled, non-serialized and consumable is accepted. Every line must belong to the selected organization and, when supplied, unit. Pessimistic locks serialize competing deductions; failure on any line rolls back the complete operation.

`POST /api/erp/ammunition-consumptions` requires a canonical UUID in `requestId`. Repeating the same payload returns the original operation without another deduction. Reusing that UUID for different data returns conflict.

## API

- `GET /api/erp/ammunition-consumptions/stock`: paginated eligible balances for an organization and optional unit.
- `POST /api/erp/ammunition-consumptions`: finalize a consumption.
- `GET /api/erp/ammunition-consumptions`: paginated history for an organization and optional unit.
- `GET /api/erp/ammunition-consumptions/{id}`: operation details.

The resource key is `ammunition-consumptions`, with `READ` and `CREATE` actions and organization/unit scope. Finalization writes one immutable `FINALIZE` audit entry containing the operation and stock changes.
