# Validity and compliance lifecycle

## Scope

COMANDOS consolidates preventive compliance for active inventory by combining validity, regulatory controls, recalls, periodic inspections and preventive maintenance into one current-state view.

Primary API: `/api/erp/compliance`.

## Compliance policy

Preventive thresholds are organization/unit scoped through `CompliancePolicy` rather than being hard-coded in the frontend.

The policy defines:

- expiration warning window;
- maintenance warning window;
- regulatory-validity warning window;
- maximum periodic-inspection interval;
- active/inactive state.

A unit-specific active policy overrides the organization policy. If neither exists, the safe defaults are 30 days for validity/maintenance/regulatory warnings and 180 days for periodic inspection.

## Preventive alerts

Alerts are calculated from live domain state so they cannot become stale after a maintenance, inspection, recall or validity change.

Severity rules:

- `CRITICAL`: already overdue/expired, due in at most seven days, active recall, invalid regulatory state;
- `WARNING`: inside the configured preventive window but not yet critical;
- `INFO`: reserved for non-blocking informational compliance signals.

Sources include:

- `AssetItem.validUntil`;
- `StockLot.validUntil`;
- `ExpirationRecord`;
- `RegulatoryControl`;
- active `RecallItem` records;
- `WorkOrder.nextMaintenanceAt`;
- latest `PeriodicInspection` and configured inspection interval.

## Consolidated compliance view

`GET /api/erp/compliance/summary?organizationId=...&unitId=...` returns:

- policy in force;
- total operational assets/lots;
- compliant and non-compliant totals;
- critical/warning/informational alert totals;
- ordered alert list;
- item-by-item compliance state;
- reasons for non-compliance;
- validity;
- next maintenance;
- latest inspection and result;
- active recall count;
- active alert count.

Terminal assets (`SOLD`, `DONATED`, `DISPOSED`) are excluded from active operational compliance.

## Compliance rules

An item becomes non-compliant when applicable rules detect conditions such as:

- expired validity;
- expired explicit expiration control;
- invalid/expired regulatory control;
- active recall;
- blocked, missing or restricted asset state;
- overdue preventive maintenance;
- missing/overdue inspection history;
- unresolved inspection nonconformity.

A failed/reproved inspection stops being treated as an unresolved inspection nonconformity when a maintenance order has been completed after that inspection. This avoids keeping a repaired asset permanently non-compliant because of historical inspection data.

## Recall and availability

Active recalls are critical compliance findings. Recall targets remain traceable to their asset or lot and must not remain operationally available while the recall is active.

## Validity and regulatory controls

Expired inventory cannot remain operationally available. Expiration dates from the main asset/lot records and explicit `ExpirationRecord` rows are both considered. Regulatory controls are evaluated independently so an asset can be physically valid but still non-compliant because its external registration/certification is expired or invalid.

## Inspection and maintenance interaction

The consolidated view uses the latest inspection as the current inspection evidence and the latest maintenance order carrying `nextMaintenanceAt` as the preventive-maintenance source. The policy determines when each date enters the warning window.

## API

- `GET /api/erp/compliance/summary` returns the consolidated view and preventive alerts.
- `GET /api/erp/compliance/policy` returns the effective organization/unit policy.
- `PUT /api/erp/compliance/policy` creates or updates preventive thresholds for the selected scope.

The policy endpoints use the existing inventory authorization scope; compliance does not bypass inventory security.

## Oracle/demo regression

The demo stack seeds an explicit organization compliance policy and verifies:

- valid policy intervals and scope;
- expired asset/lot availability restrictions;
- expiration-record provenance;
- regulatory-control integrity;
- recall target exclusivity and active-recall blocking;
- preventive-maintenance date consistency;
- periodic-inspection date/history consistency.

This regression complements the existing maintenance, inspection, recall and inventory tests rather than duplicating their transaction flows.
