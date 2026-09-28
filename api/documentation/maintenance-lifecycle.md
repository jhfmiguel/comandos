# Maintenance lifecycle closure

## Scope

The COMANDOS maintenance domain covers preventive and corrective maintenance for serialized assets and preserves the complete operational trail from dispatch to return to availability.

Primary API: `/api/erp/maintenance`.

## Lifecycle

1. A maintenance plan may define organization/unit scope and periodicity.
2. An eligible asset is selected only when it is available or blocked and is not reserved or reported missing in an approved inventory count.
3. Opening a work order records organization/unit/asset snapshots, reason, maintenance type, workshop/gunsmith data and idempotency metadata.
4. Opening generates one `MAINTENANCE_ISSUE` stock movement with quantity `-1` and changes the asset to `IN_MAINTENANCE`.
5. Completion requires diagnosis, at least one executed service and a functional test.
6. Parts are optional, but when present require positive quantity and non-negative unit cost.
7. Total maintenance cost is the sum of executed-service costs plus part quantity × unit cost.
8. Functional test result is `APPROVED` or `REJECTED`.
9. Completion generates one `MAINTENANCE_RETURN` stock movement with quantity `+1` for the same asset and work order.
10. An approved test returns a non-expired asset to `AVAILABLE`; a rejected test or an expired asset returns it as `BLOCKED`.
11. The next maintenance date uses an explicit `nextMaintenanceDays` override when provided; otherwise it uses the linked plan periodicity.
12. Opening and completion are independently idempotent by request ID/fingerprint.

## Integrity invariants

The demo/Oracle regression now validates automatically that:

- every order retains organization, unit and asset provenance;
- maintenance type is preventive or corrective in the homologation data;
- planned maintenance references an active, in-scope plan with positive periodicity;
- each order has exactly one unique issue movement;
- open maintenance leaves the asset unavailable as `IN_MAINTENANCE`;
- completed maintenance has exactly one diagnosis and at least one executed service;
- diagnosis contains defect, cause and technical opinion;
- service and part costs are non-negative and part quantities are positive;
- recorded total cost equals services plus parts;
- completed maintenance has exactly one functional test using `APPROVED` or `REJECTED`;
- completion and return timestamps cannot precede opening;
- completed maintenance has exactly one unique return movement;
- issue (`-1`) plus return (`+1`) nets to zero units for the serialized asset;
- issue and return movements keep `MAINTENANCE` provenance and reference the same work order/asset;
- completed planned maintenance calculates the next maintenance date from plan periodicity in the demo scenario;
- opening and completion retain their idempotency metadata.

## Homologated capabilities retained

The closure does not replace the already homologated screens/API flows for preventive/corrective maintenance, plan, order, diagnosis, executed services, parts, costs, functional testing, asset unavailability, return, history, periodicity and attachments. It adds regression-level integrity around the lifecycle and normalizes the demo scenario to follow the same semantics as the production service.

## Demo normalization

The maintenance demo data now represents a coherent completed preventive maintenance:

- active preventive plan with 180-day periodicity;
- `MAINTENANCE_ISSUE = -1`;
- asset transitions through `IN_MAINTENANCE`;
- diagnosis, service and replacement part;
- total cost reconciled to the service and part values;
- functional test `APPROVED`;
- `MAINTENANCE_RETURN = +1`;
- next maintenance date calculated from completion + plan periodicity;
- final asset state `AVAILABLE` when not expired, otherwise `BLOCKED`.
