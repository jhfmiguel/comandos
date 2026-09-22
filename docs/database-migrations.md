# Database migrations

COMANDOS now uses Flyway to introduce explicit, versioned database migrations.

## Transition strategy

The application still keeps `spring.jpa.hibernate.ddl-auto=update` temporarily
because the historical schema was created incrementally by Hibernate and does not
yet have a complete versioned baseline.

Flyway is enabled with `baseline-on-migrate=true` and baseline version `0`.
Existing non-empty PostgreSQL databases can therefore start applying migrations
from `V1` without recreating or deleting existing data.

The first migration fixes the compatibility issue found during the Armamento
integrated validation: legacy databases may not contain
`erp_stock_location.active`.

## Rules from this point forward

- Every intentional schema change must have a Flyway migration.
- Migrations must preserve existing data unless a destructive migration is
  explicitly reviewed and approved.
- Do not manually patch production schema as a substitute for a migration.
- Hibernate `ddl-auto=update` is transitional only.
- After the historical schema has a complete Flyway baseline and has been
  validated against existing databases, change Hibernate to `ddl-auto=validate`.
- H2 unit tests may continue using `create-drop`; PostgreSQL integration
  validation must execute Flyway.

## Current migrations

- `V1__stock_location_active.sql`: adds/backfills the active flag for stock
  locations and enforces a non-null default of `true`.


## Acquisition lifecycle

Acquisitions now enforce contracting state before receiving. Public onerous
acquisitions require a procurement process that reaches HOMOLOGATED or CONTRACTED
before authorization. Private or gratuitous acquisitions can be authorized
without forcing a public-procurement flow. Receiving accepts only AUTHORIZED,
ORDERED, or PARTIALLY_RECEIVED acquisitions; DRAFT and PROCUREMENT_IN_PROGRESS
records can no longer create stock intake accidentally.
