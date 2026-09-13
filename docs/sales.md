# Inventory sales

The new module connects core Person buyers and Organization ownership to individual
assets and stock lots. Code, API fields, table names and interface text use English.
The official cumulative specification remains [ERP architecture](arquitetura-erp-seguranca.md).

## Use the module

1. Start the API using `wr-api/mvnw.cmd spring-boot:run` and the frontend using
   `npm.cmd run dev` from `wr-app`. Development JPA `ddl-auto=update` creates the
   new `erp_sale` and `erp_sale_item` tables; no database reset is needed.
2. In **Institutional core**, register an active organization and an active person.
3. In **Assets and inventory**, register a category, brand, model with list price,
   location in that organization, and an AVAILABLE asset or a stock lot.
4. Open **Inventory sales** (`/erp/sales`), choose the organization, optional unit, buyer and
   payment method, then search for stock by code, SKU or model name.
5. Add assets or lots. Asset quantity is one; lot quantities may have four decimal
   places. Review quantities and totals, then click **Finalize**.
6. The receipt shows the recorded values. Stock movements contain negative SALE
   quantities, assets become SOLD, and lot/location availability decreases.
7. Expand a sale in **Sales history** to review its receipt. **+ Sale** clears
   every form field and stock search by mounting a new form.

## HTTP contract

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/api/erp/sales/stock?organizationId=1&kind=ASSET&search=&page=0` | Available assets; use `kind=LOT` for lot balances |
| POST | `/api/erp/sales` | Finalize a sale; return its receipt, or the same receipt for an identical retry |
| GET | `/api/erp/sales?organizationId=1&page=0` | Latest sales first, 20 per page |
| GET | `/api/erp/sales/1` | Historical receipt |

Stock and history accept an optional `unitId` query parameter. Finalization accepts
the same optional field in its JSON body. A unit must belong to the selected
organization. Every selected asset/location balance must belong to that exact
unit; stock without a unit or in sibling/child units is excluded. The receipt
returns nullable `unitId` and a historical `unitName` snapshot.

Without `unitId`, a sale belongs to the organization and may contain stock from
multiple units. Organization-wide history includes both kinds of receipt. Unit
history includes only receipts explicitly recorded for that unit, including when
the viewer has SYSTEM or ORGANIZATION access. Earlier sales stay organization-wide;
their unit is not inferred from their items. Existing request fingerprints remain
valid; adding, removing or changing the unit on a retry returns a conflict when
the caller has access to both scopes.

Changing organization clears the unit and cart; changing unit clears the cart and
reloads stock/history. **+ Sale** also clears the unit. Unit-limited operators must
select their unit before stock, history or finalization become available.
The development schema update adds nullable `unit_id` (foreign key) and `unit_name`
columns to `erp_sale` on API restart. There is no backfill or database reset.

Example request; replace all IDs and prices with actual selected records:

```json
{
  "requestId": "ef58c49c-dbec-4f08-9fb9-7286cfde58f0",
  "organizationId": 1,
  "buyerId": 2,
  "paymentMethod": "PIX",
  "items": [
    { "assetId": 3, "quantity": "1", "expectedUnitPrice": "150.0000" },
    { "balanceId": 4, "quantity": "2.125", "expectedUnitPrice": "12.3456" }
  ]
}
```

`stockId` in an ASSET option is an AssetItem ID; in a LOT option it is a
StockBalance ID, identifying both lot and location. Each request line must supply
exactly one of assetId/balanceId. Prices are authoritative in the catalog; the
request price only detects stale selections. There are no discounts/taxes in this
stage. Subtotals use HALF_UP to four decimal places. Responses encode decimals as
strings; the UI uses integer decimal arithmetic for its estimate.

Payment methods: CASH, PIX, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER. Payment method
registration does not execute a payment. Request validation returns 400; stale
prices, unavailable stock and mismatched retries return 409. No finalized-sale
update/delete endpoints exist.

One UUID identifies one exact ordered request. Retry unchanged contents with the
same UUID following an uncertain response; never generate another ID for that
retry. The page locks editing during uncertainty and offers **Retry finalization**.
The pending request is retained in component memory, not persisted across page
reload/navigation. Consult history after leaving the page with an uncertain result.

## Validation

- `wr-api`: `./mvnw.cmd test` — core, inventory, sales and authentication tests.
- Sales HTTP tests verify mixed asset/lot fulfillment, exact amounts and snapshots,
  rollback on a later-line failure, required fields, invalid quantities, duplicate
  selections, organization boundaries, expired stock, changed prices, sold-state
  protection, retries and simultaneous requests competing for stock. Unit tests
  also cover asset/lot filtering, mismatched unit rejection, receipt snapshots,
  request fingerprint conflicts and prevention of unit ownership changes after a sale.
- Browser integration checks cover CORS preflight for stock search/finalization,
  actual history requests and rejection of other origins. Sales use the same
  `erp.allowed-origin` setting as core/inventory (default `http://localhost:3000`).
- `wr-app`: `npx.cmd tsc --noEmit`, targeted ESLint, and `npm.cmd run build`.
- API tests use H2 in PostgreSQL mode. A separate browser suite now validates
  mixed sales, partial returns, cancellation, response-loss recovery and scoped
  access with a real PostgreSQL validation database. See [Validation](validation.md)
  for commands and the boundary between tested development behavior and production.

## Delivery boundary

The original legacy sale API and form remain available and do not consume ERP
stock. New stock-aware sales use `/erp/sales`. This stage does not copy test data,
merge legacy records or remove previous functionality.

The [authentication foundation](authentication.md) can require login for the sales
API. The [authorization module](authorization.md) can additionally enforce sales
actions and organization/unit scope. UNIT grants require an explicit matching
unit; they cannot read or create organization-wide receipts. Historical sales
prevent moving their unit to another organization, even if no stock location
still references it. Renaming the unit preserves the receipt's original name.
The [audit module](audit.md) records successful finalization and stock effects;
receipts now include the authenticated operator when available.
## Returns, cancellation and refund records

The original finalized receipt remains immutable. `SaleReturn` and `SaleReturnItem` preserve each partial return or full remaining-sale cancellation, the registered reason, notes, refund reference, operator, quantities, calculated refund values, and positive stock movements.

- **Return selected items** accepts any positive quantity still outstanding on a sale line. Individual assets require quantity one.
- **Cancel remaining sale** requires every outstanding line in full. Earlier partial returns remain separate history.
- Returned lot quantities are added to the original location balance and physical lot total. A valid individual asset returns to `AVAILABLE`; expiration, recall, or an approved missing-inventory record keeps it `BLOCKED`.
- Refund value uses the immutable sold unit price. The optional refund reference records the external payment or accounting transaction; this module does not contact a payment processor.
- Canonical request UUIDs make retries idempotent. `sales` permissions use `RETURN` and `CANCEL` in the receipt scope.
- The return form retains the exact request after an uncertain response and
  offers **Retry return** or **Retry cancellation**. Fields and the alternative
  operation stay disabled until confirmation. A definite initial 4xx rejection
  unlocks correction; a rejected retry preserves the already uncertain request.
  The same preservation rule applies to finalization retries. Pending requests
  live in component memory: consult history after leaving or reloading the page.
- Successful returns clear the entered quantities. Return controls have labels
  and identifiers unique to each receipt in the history.

Return reasons are managed in **Assets and inventory → Reference data → Sale return reason types**. Protected defaults are `CUSTOMER_RETURN`, `ORDER_ERROR`, and `DEFECTIVE_ITEM`.

Broader audit coverage, approvals, documents, payment gateway integration and accounting reconciliation remain future architecture requirements. Sold items cannot be manually reactivated through the registration API.
