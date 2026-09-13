# ERP audit history

The audit module records successful core/inventory registration changes and ERP
sale finalization. It follows `audit/model`, `audit/service`, and `audit/controller`.
It implements the RegistroAuditoria architectural concept as AuditRecord in
`erp_audit_record` and adds operator snapshots to `erp_sale`.

## Recorded information

Each event has an identity ID, UTC occurrence time, resource code, target record
ID, operation, actor ID/login/type and before/after JSON snapshots. AuditRecord is
an immutable event, so it does not inherit the mutable record's version/update
fields. Normal JPA updates are disabled and the API provides no write endpoints.

- CREATE, UPDATE and DELETE are recorded for institutional core and inventory
  registration resources, including access configuration.
- CREATE has no previous snapshot; DELETE has no resulting snapshot. UPDATE
  preserves both values. Snapshots use server-produced record views, not raw
  request bodies or JPA entity serialization.
- System-user snapshots omit passwords and hashes. Password assignment/change is
  indicated only by `passwordChanged: true`. The serializer also removes named
  credential/token fields recursively as a second precaution.
- The actor comes from the authenticated session, never a request field. Requests
  without an authenticated account are marked UNAUTHENTICATED with null actor
  ID/login. This identifies setup operations without inventing a system operator.
- Actor ID/login and target record IDs are historical scalar snapshots, not
  foreign keys. Account renaming/deletion and target deletion cannot rewrite or
  cascade-delete the history. Resource plus record ID identifies the target.

Audit persistence joins the caller's transaction and requires one. Business
changes and events commit or roll back together. An audit-write failure fails the
operation. Rejected or rolled-back operations produce no successful-change event.

## Sales

ERP sale finalization records one FINALIZE event containing the receipt and stock
effects: asset state, location-balance quantity and lot total before/after changes.
The receipt includes its stock movement IDs. Identical idempotent retries return
the existing receipt and produce no additional finalization event.

Sales now store `finalizedById` and `finalizedByLogin` from the session. The receipt
shows the operator login. These nullable columns preserve compatibility with old
sales and setup operations; the UI displays **Not recorded** when absent. No
historical operator is inferred or backfilled.

Opening asset/lot registration is covered by the parent CREATE event and existing
OPENING movements. Generated opening balances/movements are not separate audit
events in this delivery.

## Read API and interface

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/erp/audit` | Filtered summaries, latest event IDs first, 20 per page |
| GET | `/api/erp/audit/{id}` | Event summary and before/after snapshots |

List filters: `resource` (exact code), `recordId`, `action`, `actor` (login search),
`from` and `until` (inclusive ISO instants), and zero-based `page`. Invalid page or
reversed time ranges return 400. POST/PUT/DELETE are not available, including to
administrators.

When permission enforcement is enabled, the API requires a SYSTEM profile grant
for resource `audit`, action `READ` (or the global administrator wildcard).
ORGANIZATION/UNIT audit grants grant nothing because snapshots can include shared
records and cross-resource data. Access-administration permission alone does not
grant audit viewing. With enforcement disabled, the existing setup-mode access
behavior applies.

Open **Audit history** in the sidebar (`/erp/audit`). Filters use local date/time
inputs converted to UTC instants. **View** loads snapshots on demand. The menu and
page honor the audit permission; backend checks remain authoritative.

The default development `ddl-auto=update` creates the table and nullable sale
columns on API restart. No existing data is reset or reconstructed.

## Verification and remaining scope

Backend tests cover core and inventory snapshots, failed-write/transaction
rollback, authenticated actor attribution, unauthenticated setup, secret omission,
history surviving deletion, read-only endpoints, filters, global audit permission,
sale stock effects and idempotent retries. Frontend checks use TypeScript,
targeted ESLint and a production build. Browser interactions and production
PostgreSQL deployment are not certified by these checks.

This covers successful ERP business changes. Legacy User/Weapon/Sale endpoints,
login/logout/security failures, read-access logging, denied operations and direct
SQL/import changes are not audited yet. Tamper-evident storage, retention,
archival, external audit export and access to history scoped by organization/unit
remain future work. Database administrators can still modify storage directly;
application read-only access is not tamper-proof retention.
