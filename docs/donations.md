# Donations

The donations module records a permanent, authorized transfer of available inventory to a donee. A donation belongs to an organization and optional unit, identifies the donor representative and donee as `Person` records, stores the formal term and contains one or more serialized assets or lot quantities.

Finalization is atomic. Serialized assets change from `AVAILABLE` to `DONATED`; lot items reduce `StockBalance.available` and `StockLot.availableQuantity`. Every item creates a negative `DONATION` stock movement. Expired, unavailable, cross-scope or insufficient stock rejects and rolls back the complete operation.

Canonical UUID request identifiers make retries idempotent. Pessimistic locks protect concurrent finalizations. The `donations` resource supports `READ` and `CREATE` with organization and exact-unit scope, and each successful finalization creates one audit event containing stock changes.

API endpoints are `GET /api/erp/donations/stock`, `POST /api/erp/donations`, `GET /api/erp/donations` and `GET /api/erp/donations/{id}`. The English frontend is available at `/erp/donations`.
