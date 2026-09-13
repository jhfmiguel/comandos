# Institutional core

## Scope

This module implements the institutional registration foundation of the official
[ERP architecture](arquitetura-erp-seguranca.md). It adds 13 relational entities,
CRUD endpoints, and a frontend workspace. It does not migrate test data or replace
the existing user, weapon, or sale endpoints.

The institutional module lives in `wr-api/src/main/java/com/weaponsregistration/core`.
Entities live in `core/model`; business services live in `core/service`; HTTP
controllers and error handling live in `core/controller`. The shared field catalog explicitly lists allowed resources and
editable fields; it cannot expose arbitrary JPA entities or accept arbitrary
entity properties.

## Run locally

1. Start the development PostgreSQL database configured in
   `wr-api/src/main/resources/application.properties`.
2. From `wr-api`, run `./mvnw.cmd spring-boot:run` on Windows.
   Hibernate creates the `erp_*` tables and their foreign keys using the existing
   development `ddl-auto=update` setting.
3. From `wr-app`, run `npm.cmd run dev`.
4. Open `http://localhost:3000/erp/core`, or select **Institutional core** in the
   sidebar. The API defaults to port 8080. Set `erp.allowed-origin` when the UI
   uses a different origin.

These commands do not erase the development database. SQL backfills, legacy ID
maps, and production schema-version management are not part of this iteration.

## API contract

Base path: `/api/erp/core`.

| Method | Path | Behavior |
| --- | --- | --- |
| GET | /catalog | Resource names and editable field metadata |
| GET | /{resource}?search=&page=0&size=20 | Paged search; maximum size 100 |
| GET | /{resource}/{id} | Record with reference labels and version |
| POST | /{resource} | Create a record; HTTP 201 |
| PUT | /{resource}/{id} | Replace editable fields; requires current version |
| DELETE | /{resource}/{id}?version=0 | Delete an unreferenced record; HTTP 204 |

Resources: `organizations`, `units`, `people`, `roles`, `person-roles`,
`role-data`, `users`, `profiles`, `permissions`, `user-profiles`,
`profile-permissions`, `credentials`, and `qualifications`.

The units list also accepts `organizationId` to filter the relationship selector
before pagination. Search accepts names/codes or IDs. Assignment lists also
search their main related names. SQL wildcard characters in search are treated
literally.

Write requests use foreign-key IDs, not nested persistence objects. Example:

```json
{
  "personId": 1,
  "roleId": 2,
  "organizationId": 1,
  "unitId": null,
  "startDate": "2026-09-10",
  "endDate": null,
  "status": "ACTIVE"
}
```

For PUT, send all editable fields plus the `version` returned by GET. Do not send
response-only properties such as `id`, `label`, `referenceLabels`, `createdAt`,
or `updatedAt`. Failed optimistic-lock checks return HTTP 409.

Invalid input returns HTTP 400, missing resources return HTTP 404, and duplicate
values or referenced-record deletion return HTTP 409. Error bodies use Problem
Detail with an English `detail` message. The frontend displays that message.

System users require a password of at least 12 characters and at most 72 UTF-8
bytes on creation. The API stores only a BCrypt hash and never returns the
password or hash. On update, an omitted or empty password retains the hash.
Login is normalized to lowercase and unique. Creating a person does not create
a system account or grant a role/profile automatically.

## Manual verification

1. Create two organizations and a unit in the first organization.
2. Create a child unit, then try making its parent a child of that unit. Saving
   must fail with a hierarchy error.
3. Create a person and two distinct person roles. Assign both roles to that
   person with dates and organization context.
4. Try assigning the unit from the first organization to a role assignment in
   the second. Saving must fail.
5. Add role details, a credential, and a qualification for the person.
6. Create a system user for that person, an access profile, and a permission.
   Link the permission to the profile and the profile to the user in an organization.
7. Edit the account with a blank password. Other edits must save while retaining
   the original password hash.
8. Open the same record for editing in two tabs. Save one tab, then save the
   other. The stale edit must be rejected and can be retried after refreshing.
9. Try deleting the organization or person while references exist. Deletion
   must fail. An unreferenced test record can be deleted after confirmation.
10. Confirm existing user registration, weapon registration, and sales screens
    still load. They remain independent until the next module integration.

## Automated checks

```powershell
# wr-api: all tests use isolated in-memory H2 databases, not development PostgreSQL
.\mvnw.cmd test

# wr-app
npx.cmd tsc --noEmit
npx.cmd eslint src/components/erp src/api/services/core.service.ts src/api/services/erp.service.ts src/api/models/erp src/app/erp
npm.cmd run build
```

The integration tests exercise actual HTTP requests and database persistence,
including resource schemas, CRUD, relationship integrity, multiple person roles,
duplicate assignments, password hashing, optimistic locking, and concurrent
hierarchy edits. H2 tests do not replace PostgreSQL-specific deployment testing.

## Remaining architecture work

System-user records and profile/permission assignments are configuration only.
This iteration does not provide login, sessions, MFA, authorization enforcement,
or tenant-filtered data visibility. These controls still require implementation
before this administration API is used as a secure multi-organization system.

The asset catalog and opening-stock foundation are now implemented in the
[inventory module](inventory.md). Access enforcement, auditing, transactional
stock operations, and sales integration remain pending. The complete
architecture remains authoritative for procurement, maintenance, fleet,
operations, intelligence, investigations, documents, workflows, and training.
