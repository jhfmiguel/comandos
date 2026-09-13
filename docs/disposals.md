# Asset disposal

The disposal module permanently removes serialized assets or lot-controlled quantities through a formal `DisposalProcess`. Every process records its organization, optional exact unit, process number, reason, operator, items and immutable stock snapshots. A process may also contain one `Destruction` record with method, date and certificate.

Finalization is atomic. Individual assets change from `AVAILABLE` to `DISPOSED`. Lot quantities reduce both `StockBalance.available` and `StockLot.availableQuantity`. Every item creates one negative `DISPOSAL` stock movement. Destruction fields are optional as a group and must all be supplied when physical destruction is recorded.

Canonical UUID request identifiers provide idempotent retries, pessimistic locks protect concurrent operations, and failures roll back the complete process. Process numbers are unique within an organization. The `disposals` resource supports `READ` and `CREATE` at SYSTEM, ORGANIZATION and UNIT scope.

API endpoints are `GET /api/erp/disposals/stock`, `POST /api/erp/disposals`, `GET /api/erp/disposals` and `GET /api/erp/disposals/{id}`. The English frontend is available at `/erp/disposals`.
