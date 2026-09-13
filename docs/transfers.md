# Inventory transfers

The inventory transfer module moves controlled items between two distinct organizational units in the same organization. It supports serialized `AssetItem` records and quantities held in a `StockBalance` without changing or duplicating the underlying item model.

Finalization is atomic. An asset retains `AVAILABLE` status and moves to the selected destination location. A lot quantity is deducted from its source balance and added to the balance for the same lot at the destination location; that destination balance is created when necessary. The lot's global `availableQuantity` does not change because the stock remains inside the organization.

Each item creates a negative `TRANSFER_OUT` movement at the source and a positive `TRANSFER_IN` movement at the destination. The transfer and its items preserve organization, unit, location, model, code, quantity, operator and purpose snapshots. Expired, unavailable, cross-organization, cross-unit and insufficient stock rejects the whole transaction.

Canonical UUID request identifiers and payload fingerprints make retries idempotent. Ordered pessimistic locks protect concurrent transfers. The `transfers` permission resource supports `READ` and `CREATE` at SYSTEM, ORGANIZATION and UNIT scope. Creation requires access to both units. A unit may read history where it is either the source or destination.

API endpoints are `GET /api/erp/transfers/stock`, `POST /api/erp/transfers`, `GET /api/erp/transfers` and `GET /api/erp/transfers/{id}`. The English frontend is available at `/erp/transfers`.
