# Permissions and organizational scope

This stage connects AccessProfile, Permission, ProfilePermission and UserProfile
to backend authorization. It reuses existing tables and adds no default grants.
The cumulative reference remains [ERP architecture](arquitetura-erp-seguranca.md).

## Enable after provisioning

1. In setup mode, create an active organization, an active person, and a system
   user as described in [Authentication](authentication.md).
2. Create an access profile with level `SYSTEM`.
3. Create a permission with resource `*` and action `*`, and link it to the profile
   through **Profile permissions**. This is full system administrator access.
4. Assign this profile to the initial user through **User profiles**, selecting
   an active organization and leaving unit empty. SYSTEM scope covers all
   organizations; the assignment's organization must remain active.
5. Configure narrower profiles for other accounts before enabling enforcement.
6. Restart the API from `wr-api` with both settings enabled:

   ```powershell
   $env:ERP_REQUIRE_LOGIN = 'true'
   $env:ERP_ENFORCE_PERMISSIONS = 'true'
   .\mvnw.cmd spring-boot:run
   ```

Both flags default to false to preserve initial setup. Login-only mode continues
to work when only ERP_REQUIRE_LOGIN is true. Enabling permissions without login
causes startup to fail. No existing profile names/levels or database records are
rewritten. Unknown levels grant no access and must be explicitly configured.

## Grant semantics

| Profile level | Assignment | Effective scope |
| --- | --- | --- |
| SYSTEM | Active organization, no unit | All records of the permitted resource, across organizations |
| ORGANIZATION | Active organization, no unit | Records in that organization |
| UNIT | Active organization and a unit | Records in that exact unit; no automatic child-unit inheritance |

SYSTEM/ORGANIZATION assignments with a unit and UNIT assignments without a unit
grant nothing. Grants require an active person, an unblocked account and an
active assignment organization. Multiple valid assignments combine their access.
Grants are read from the database during operations; removing a link or disabling
the organization takes effect on subsequent requests without another login.

Resources and actions are case-sensitive. The whole value `*` matches all
resources/actions; partial patterns such as `inventory/*` are not supported.
HTTP actions map to READ (GET/HEAD), CREATE (POST), UPDATE (PUT), DELETE (DELETE).

| Resource | Scope supported |
| --- | --- |
| `core/organizations` | SYSTEM or ORGANIZATION |
| `core/units`, `core/person-roles`, `core/role-data` | SYSTEM, ORGANIZATION or UNIT |
| `core/people`, `core/roles`, `core/credentials`, `core/qualifications` | SYSTEM: these are shared records without organization ownership |
| `inventory/locations`, `inventory/assets`, `inventory/item-values`, `inventory/lots`, `inventory/balances`, `inventory/movements`, `inventory/regulatory-controls`, `inventory/expirations`, `inventory/certifications`, `inventory/recalls`, `inventory/recall-items` | SYSTEM, ORGANIZATION or UNIT |
| `inventory/categories`, `inventory/brands`, `inventory/models`, `inventory/characteristics`, `inventory/category-characteristics`, `inventory/model-values`, `inventory/firearm-specifications` | SYSTEM: shared catalog and equipment specifications |
| `sales` | SYSTEM, ORGANIZATION or UNIT (explicit receipt unit required); READ, CREATE, RETURN and CANCEL |
| `custodies` | SYSTEM, ORGANIZATION or UNIT; UNIT requires an explicit custody unit |
| `reservations` | SYSTEM, ORGANIZATION or UNIT; UNIT requires an explicit reservation unit |
| `inventory-counts` | SYSTEM, ORGANIZATION or UNIT; UNIT requires the exact count location unit |
| `transfers` | SYSTEM, ORGANIZATION or UNIT; CREATE requires both exact units |
| `disposals` | SYSTEM, ORGANIZATION or UNIT; UNIT requires the exact disposal unit |
| `maintenance` | SYSTEM, ORGANIZATION or UNIT; READ, CREATE and COMPLETE |
| `legacy/users`, `legacy/weapons`, `legacy/sales` | SYSTEM: legacy tables have no ERP organization ownership |
| `security/access` with action `MANAGE` | SYSTEM: administer users, profiles, permissions and both access-assignment resources |
| `audit` with action `READ` | SYSTEM: view global ERP change history and snapshots |

The five administrative resources cannot be granted through ordinary
`core/users`/`core/profiles`/etc. actions. They always require the separate global
`security/access` + `MANAGE` permission (or the SYSTEM `*` + `*` administrator).
This prevents organizational grants or regular registration rights from becoming
access-administration privileges.

Shared Person/catalog permissions are deliberately global, not a claim of complete
tenant isolation. Grant their READ access separately when an operator needs those
references. Granting READ to a record also reveals its normal relationship labels.

## Example sales operator

Assign these permissions through profiles:

- ORGANIZATION: `sales` READ and CREATE; `core/organizations` READ for the assigned
  organization. Sales history and available-stock search require READ.
- SYSTEM: `core/people` READ to select the buyer from the shared person directory.

For a unit-limited sales operator, use UNIT `sales` READ and CREATE plus UNIT
`core/units` READ, assigned to the exact organization/unit. Keep ORGANIZATION
`core/organizations` READ for the organization selector and SYSTEM `core/people`
READ for buyer selection. An organization-wide operator can optionally receive
ORGANIZATION `core/units` READ to select individual units.

No access to system-user administration is needed. Sales fulfillment does not
require generic stock UPDATE rights. The backend validates the sale permission,
buyer reference, organization and inventory business rules as one operation.

For an inventory registrar, grant the desired inventory CRUD actions in the
organization/unit and READ access to referenced shared models and allowed
locations. A create/update action never bypasses relationship scope checks.

## Enforcement and frontend

Core and inventory services apply scope predicates to both list queries and
counts before pagination. Search and client-supplied organization filters cannot
widen access. Detail, update and delete operations validate the actual record;
updates validate both the old and proposed scope. Creates validate the target
scope. Form references require READ permission for the referenced record.

Sales stock/history/finalization check the requested organization and optional unit.
UNIT grants require a matching explicit `unitId`; omitting it returns 403.
Direct receipt IDs and idempotent retries check stored organization/unit ownership.
Unit sales accept only stock in that exact unit. Earlier receipts and new sales
without a unit remain organization-wide and cannot be exposed by UNIT grants,
even if every line happens to refer to the same unit. Child units are not inherited.
Lot catalog scope follows its opening location; each current quantity follows its
location balance. Transfers authorize and move those balances by source and destination unit.

The legacy APIs also enforce their explicit global permissions when enabled.
Permission failures return 403. Existing business validation, optimistic locking,
read-only inventory history and sold-asset restrictions continue to apply even
to administrators.

Catalog endpoints return only readable resources and include an `actions` array.
The workspace selects the first available resource if its default is unavailable,
shows an explicit empty-access state, hides creation without CREATE and disables
editing/deletion without their actions. Action metadata indicates that an action
is allowed in at least one scope; the backend still checks each target record.
The sales page enables finalization and history for the selected organization/unit.
Session responses include current grants. The frontend refreshes them on window
focus or a rejected operation, and reloads catalog metadata when they change.

## Verification and remaining scope

Run `./mvnw.cmd test` in `wr-api`. Authorization HTTP tests use an isolated H2
database and cover default denial, scope-filtered rows/counts/search, direct IDs,
cross-organization references, old/new update scope, action restrictions, access
administration, revocation, inactive organizations, sales and legacy routes.
Unit sales tests include cross-unit and unassigned stock rejection with rollback,
scoped stock/history counts, direct receipt denial, retry authorization and audit.
Existing authentication and business suites remain in setup/login-only modes to
verify compatibility. TypeScript, targeted ESLint and a production build validate
the frontend changes; browser interaction is not covered by these checks.

Remaining architecture work includes ownership rules
for currently shared records, administrative delegation, protection against
removing the last administrator, broader audit coverage, login throttling,
MFA, recovery and distributed sessions. This stage does not claim those controls.

Successful ERP mutations and sale operator attribution are now covered by the
subsequent [audit module](audit.md); its own delivery boundaries still apply.
# Donation permissions

The `donations` resource supports `READ` and `CREATE` at SYSTEM, ORGANIZATION and UNIT scope. `READ` controls available-stock search and history. `CREATE` is checked for the selected operation scope and again for every stock location before finalization.

# Ammunition consumption permissions

The `ammunition-consumptions` resource supports `READ` and `CREATE`. Both permissions follow organization and unit scope. `READ` controls eligible-stock searches and history; `CREATE` controls finalization and is checked again against every selected stock location. Operational profiles normally need both actions. A unit grant can operate only on stock in that exact unit; an organization grant can operate across that organization.

# Inventory transfer permissions

The `transfers` resource supports `READ` and `CREATE`. Finalization requires `CREATE` for both the source and destination unit, including every source location. `READ` controls source-stock search and history. A UNIT grant sees transfers where that unit is either the source or destination; organization and system grants retain their broader scope.

# Asset disposal permissions

The `disposals` resource supports `READ` and `CREATE` at SYSTEM, ORGANIZATION and UNIT scope. `READ` controls eligible-stock search and immutable history. `CREATE` is validated for the process scope and every selected stock location. UNIT grants require an explicit matching unit.

# Maintenance permissions

The `maintenance` resource uses `READ` for plans, eligible assets and work-order history, `CREATE` for plans and work-order opening, and `COMPLETE` for diagnosis, service and functional-test completion. Organization and exact-unit ownership are checked against both the order and asset location.

# Inventory reservation permissions

The `reservations` resource uses `READ` for eligible-stock search and history, `CREATE` for atomic allocation, and `CANCEL` or `EXPIRE` for release. Organization and exact-unit ownership are checked for the reservation and every selected stock location. `inventory/reservation-status-types` is a shared SYSTEM catalog registration.

# Physical inventory permissions

The `inventory-counts` resource uses `READ` for history, `CREATE` to open a snapshot, `COUNT` to enter physical quantities, `APPROVE` to apply adjustments, and `CANCEL` to close an unfinished count. Exact organization, unit, and location ownership are validated. The two inventory-count type registrations are shared SYSTEM catalog resources.
