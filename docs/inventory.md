# Asset and inventory foundation

This module continues the official [ERP architecture](arquitetura-erp-seguranca.md)
without changing existing User, Weapon, or Sale workflows.

## Start

Start the configured development PostgreSQL database, then run:

```powershell
# From wr-api
.\mvnw.cmd spring-boot:run

# From wr-app, in another terminal
npm.cmd run dev
```

Open `http://localhost:3000/erp/inventory` or **Assets and inventory** in the menu.

## Firearm specifications

Models whose category family is `FIREARM` can receive one dedicated specification
through **Firearm specifications** in the inventory workspace. It stores caliber,
operating mechanism, positive whole-number capacity and positive barrel length in
millimeters. Supported mechanisms are SINGLE_SHOT, BOLT_ACTION, LEVER_ACTION,
PUMP_ACTION, SEMI_AUTOMATIC, AUTOMATIC and REVOLVER.

The API resource is `/api/erp/inventory/firearm-specifications`. Its permission
key is `inventory/firearm-specifications`. Each model can have only one record.
Once the specification exists, its model cannot be replaced and the model category
or unit of measure cannot change. Create, update and delete operations use the
existing optimistic locking, permission and audit behavior. API restart creates
`erp_firearm_specification` through the development schema update.

## Ammunition specifications

**Ammunition specifications** stores the dedicated technical identity of each `AMMUNITION` model: caliber, lethality classification (`LETHAL` or `LESS_LETHAL`), projectile type, case type and primer type. Caliber and lethality are independent, allowing both lethal and less-lethal `12 GA` models. A model accepts only one specification. Once it has a specification, its category and unit of measure cannot change. The resource uses the shared catalog scope, optimistic locking and the standard transactional audit trail.

## Grenade specifications

**Grenade specifications** stores grenade type, agent, delay time in seconds and safety radius in meters for models in the `GRENADE` family. Delay and radius must be positive. Each model accepts one specification, and its category and unit of measure become protected while that specification exists.

## Spray specifications

**Spray specifications** stores the agent, concentration percentage, container volume in milliliters and effective range in meters for models in the `SPRAY` family. Concentration must be above zero and at most 100; volume and range must be positive. Each model accepts one specification and receives the same catalog permission, optimistic-locking and audit behavior as the other controlled-equipment specifications.

## Ballistic protection specifications

**Ballistic protection specifications** records protection type, declared protection level, material and certification for models in the `BALLISTIC_PROTECTION` family. Each model accepts one specification. Its category and unit of measure remain protected while the specification exists, and all changes use the standard catalog authorization, optimistic locking and audit trail.

## Electrical device specifications

**Electrical device specifications** records positive voltage, a positive whole number of supported cycles and cartridge type for models in the `ELECTRICAL_DEVICE` family. Each model accepts one specification and receives the standard model protection, catalog authorization, optimistic locking and audit history.

## Optical specifications

**Optical specifications** records optical type, positive maximum magnification and whether the model supports night vision or thermal vision. It accepts only `OPTICAL` models, one specification per model, with protected classification, catalog authorization, optimistic locking and audit history.

## Regulatory control

**Regulatory controls** associate an individual FIREARM asset with an external
registry. Each record contains externalSystem, registrationNumber, status and an
optional validUntil date. Status accepts PENDING, ACTIVE, SUSPENDED, CANCELLED or
EXPIRED. An expired date cannot be ACTIVE, and sold assets reject new changes.

The API resource is `/api/erp/inventory/regulatory-controls`; its permission key
is `inventory/regulatory-controls`. An asset has at most one record in each system,
and a system/registration-number pair identifies only one asset. The asset and
external system are immutable after creation. Visibility follows the asset's
current stock-location organization and unit. Generic create/update/delete,
optimistic locking and audit apply. API restart creates `erp_regulatory_control`.
The existing development Hibernate schema-update setting creates the new tables.
No data reset or historical migration is required by this implementation.

## Registration sequence

1. Create an organization and, optionally, an organizational unit in the core.
2. Create an item category. Set serialized for individual serial-number tracking
   or lotControlled for quantity-based stock. Consumables cannot be serialized.
3. Create a brand and an item model, including category, unit of measure, SKU,
   and commercial list price.
4. Configure technical characteristics, their data types, and category bindings.
   A binding specifies whether the value is required and belongs to the model
   or an individual item. Bindings apply directly to that category.
5. Add required model characteristic values.
6. Create a stock location owned by the organization, optionally linked to its unit.
7. For serialized items, register an individual asset with an asset code and
   serial number. Use DRAFT when required per-item values must be added first;
   then edit the asset to AVAILABLE once complete.
8. For quantity-controlled stock, register a lot with its opening location,
   lot number, and a positive opening quantity.
9. Inspect **Stock balances** and **Stock movements**. A lot opening creates both
   records atomically. Asset registration creates one OPENING movement with
   quantity one. Balances and movements cannot be edited directly.

The initial stock operation records the registration itself. It does not invent
a purchase, supplier, receipt document, or historical stock movement.

## API

Base path: `/api/erp/inventory`.

| Resource | Description | Writes |
| --- | --- | --- |
| categories | Hierarchical item categories and tracking rules | CRUD |
| brands | Brand and manufacturer | CRUD |
| models | Item catalog, SKU, unit, list price | CRUD |
| characteristics | Typed technical characteristic definitions | CRUD |
| category-characteristics | Category requirements and value scope | Validated CRUD |
| model-values | Values attached to models | Validated CRUD |
| locations | Organization/unit stock locations | Validated CRUD |
| assets | Individual physical assets | Create/update; no generic deletion |
| item-values | Values attached to assets | Validated CRUD |
| lots | Initial quantity-controlled lots | Create/update; no generic deletion |
| balances | Available/reserved/blocked quantity per lot and location | Read-only |
| movements | Immutable opening movement records | Read-only |
| expirations | Asset or lot expiration controls | Validated CRUD; no deletion |
| certifications | Asset or lot certificates and validity | Validated CRUD; no deletion |
| recalls | Organization recall processes | Validated CRUD; no deletion |
| recall-items | Assets or lots affected by recall | Validated CRUD; no deletion |

GET `/catalog` returns field metadata, choices, reference resources, and
readOnly/createOnly flags. Cross-module references use `core/organizations` and
`core/units`; inventory references use local resource names.

Registration requests use reference IDs and decimal strings. For example:

```json
{
  "modelId": 1,
  "openingLocationId": 2,
  "lotNumber": "LOT-001",
  "initialQuantity": "100.2500",
  "validUntil": "2028-12-31"
}
```

`initialQuantity`, model, lot number, and opening location cannot be changed
after registration. `availableQuantity` is calculated by the server and must not
be sent in write requests. The opening location is historical; future transfers
will use balances at other locations rather than rewriting that field.

PUT requests contain all editable fields plus the current `version`. GET
responses include IDs, reference labels, timestamps, and versions. Decimal
responses use strings so the frontend can preserve their precision. Values have
at most 15 integer digits and four fractional digits.

Lists support `search`, `page`, and `size` (maximum 100). Locations, assets, lots,
balances, and movements also support `organizationId` filtering. These filters
are query features, not authorization boundaries.

## Verification

```powershell
# wr-api: isolated H2 databases
.\mvnw.cmd test

# wr-app
npx.cmd tsc --noEmit
npx.cmd eslint src/components/erp src/api/services/erp.service.ts src/api/services/core.service.ts src/api/models/erp src/app/erp
npm.cmd run build
```

Inventory HTTP integration tests cover all resource searches, category cycles,
tracking-mode changes, required/duplicate serial numbers, initial movements,
exact lot quantities, duplicate-opening rollback, read-only stock fields,
immutable registration fields, optimistic locking, required typed values,
per-item draft/availability transitions, expiry on registration, and cross-module
unit ownership protection. These tests use H2 and do not certify PostgreSQL-specific
deployment behavior or browser interactions.

## Current boundaries

There are no purchase receipts, separate shipment workflows,
reservations, counts/reconciliation, disposal, or reversals yet. Each will have
its own business entity and transaction workflow. Generic stock deletion is
intentionally unavailable because it would discard the opening history.

Category family is a classification field; dedicated firearm/ammunition/etc.
specification tables remain to be implemented. Parent-category characteristics
are not inherited automatically. Changes to required specifications after stock
exists require a future schema-evolution workflow.

The availability states now include DRAFT, AVAILABLE, BLOCKED, CUSTODIED, SOLD,
DONATED and DISPOSED. Workflow states are changed only by their corresponding operations. These do
not replace the planned configurable states
and state history. Expiration is checked when an available asset/lot is saved;
scheduled expiration handling remains to be added. Sale finalization now checks
expiration again and fulfills stock in the same transaction.

The core access configuration still does not enforce authentication, permissions,
or tenant isolation. The new [Inventory sales](sales.md) module connects Person
buyers to these assets and lots through `/erp/sales`. The legacy sales screen
continues independently. Remaining requirements stay in the official architecture.
# Donation movements

Finalizing a donation creates negative `DONATION` movements. Individual assets become `DONATED`; lot-controlled quantities are deducted from the location balance and lot total in the same transaction. The generic asset editor cannot enter or leave `DONATED` status.

# Ammunition consumption movements

Finalizing ammunition use creates a negative `AMMUNITION_CONSUMPTION` movement for every consumed lot and reduces both the location balance and the lot's global available quantity in the same transaction. Expired lots and categories that are not lot-controlled, non-serialized consumable ammunition are rejected.

# Inventory transfer movements

Finalizing a transfer creates paired `TRANSFER_OUT` and `TRANSFER_IN` movements for every item. Serialized assets change location while remaining `AVAILABLE`. Lot quantities move between location balances, creating the destination balance when required. `StockLot.availableQuantity` remains unchanged because a transfer does not add or remove organizational stock.

# Asset disposal movements

Finalizing disposal creates a negative `DISPOSAL` movement for every item. Individual assets enter the terminal `DISPOSED` status. Lot-controlled items reduce the selected location balance and the lot's global available quantity. Generic inventory editing cannot enter or leave `DISPOSED`.

# Maintenance movements

Opening maintenance creates a negative `MAINTENANCE_ISSUE` movement and changes the asset to `IN_MAINTENANCE`. Completing the work creates a positive `MAINTENANCE_RETURN`; an approved, valid asset becomes `AVAILABLE`, while a rejected or expired asset becomes `BLOCKED`. Generic inventory editing cannot enter or leave `IN_MAINTENANCE`.

# Compliance controls

Expiration controls and certifications reference exactly one asset or lot. Past dates cannot remain valid or active. Recalls contain affected assets or lots and their required action. Expired and recalled AVAILABLE assets are changed to BLOCKED. Compliance records remain immutable against deletion for traceability.

# Reservations

Reservation status types are managed under the inventory reference-data catalog. The protected defaults are ACTIVE, CANCELLED and EXPIRED; additional options may be registered for later workflow expansion.

An active reservation blocks an individual asset or moves a lot quantity from `StockBalance.available` to `StockBalance.reserved`. It does not create a stock movement because custody and location do not change. Cancellation and elapsed expiration release the allocation. See [Inventory reservations](reservations.md).

# Physical inventory

Physical counts snapshot countable stock by location, record a physical quantity for every line, classify differences through configurable result registrations, and require a separate approval before adjustment. Approved differences create `INVENTORY_ADJUSTMENT` movements. See [Physical inventory](physical-inventory.md).

# Equipment sets

`Equipment sets` group individually tracked assets and quantities from a specific
stock balance under one organization-owned code. Create the set as inactive, add
its records through `Equipment set components`, and then activate the set.

Each component selects exactly one `Individual asset` or one `Stock balance`.
Individual assets always use quantity one. Lot quantities must be positive and
cannot exceed the balance's physical total (`available + reserved + blocked`). A
unit-owned set accepts only components stored in that exact unit; an
organization-wide set accepts components from any of its units.

An active set must contain at least one component. Its composition is locked
until the set is deactivated, and one individual asset cannot belong to two
active sets. Composition does not reserve or move stock. Future custody support
will use the set as the source for one atomic issue and return operation.
