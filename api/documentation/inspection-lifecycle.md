# Inspection lifecycle closure

## Scope

COMANDOS periodic inspections preserve the complete inspection trail for serialized assets: inspection, structured checklist, photographic evidence, approval/history and automatic corrective maintenance when a nonconformity requires it.

Primary API: `/api/erp/lifecycle`.

## Final checklist model

The legacy `PeriodicInspection.checklist` text remains as an immutable-friendly historical snapshot and compatibility field. The final model adds `PeriodicInspectionItem`, one row per inspected criterion.

Each item records:

- positive display order;
- unique code inside the inspection;
- human-readable label;
- required/optional flag;
- result `APPROVED`, `REPROVED` or `NOT_APPLICABLE`;
- technical observation;
- zero or more photographic evidences linked directly to the item.

Required items cannot be `NOT_APPLICABLE`.

## Photographic evidence

General inspection attachments remain supported. Photos that document a checklist criterion are stored through `OperationAttachment` with:

- resource `periodic-inspections`;
- inspection record id;
- direct `inspectionItem` relation;
- image content type;
- file metadata, uploader and timestamp.

The final approval rule requires at least one photo for every reproved checklist item. Photos are optional for approved items.

## Approval rules

Before an inspection can be approved:

1. at least one structured checklist item must exist;
2. required items must be complete;
3. required items cannot be marked `NOT_APPLICABLE`;
4. every reproved item must have photographic evidence;
5. a reproved item makes the overall inspection `REPROVED`;
6. failed/reproved/maintenance-required inspections block the asset;
7. if corrective maintenance is required and no work order exists yet, approval automatically creates one with maintenance type `CORRECTIVE`;
8. once approved, checklist and checklist-photo evidence become immutable through the structured inspection endpoints.

## API additions

- `PUT /api/erp/lifecycle/inspections/{id}/checklist` creates or updates structured checklist items before approval.
- `GET /api/erp/lifecycle/inspections/{id}/checklist` returns items and photo counts.
- `POST /api/erp/lifecycle/inspections/{id}/checklist/{itemId}/photos` uploads photographic evidence for one checklist item.
- `POST /api/erp/lifecycle/inspections/{id}/approve` now uses the final checklist/evidence validation before recording approval.

The original inspection creation/listing and generic attachment endpoints remain compatible.

## Regression coverage

The demo/Oracle regression validates:

- organization/asset provenance;
- inspection result vocabulary;
- responsibility and approval history;
- structured checklist presence;
- unique item codes and ordering;
- required-item rules;
- consistency between item failures and overall inspection result;
- photo resource, inspection and checklist-item provenance;
- image metadata and non-empty file content;
- unavailability of assets after failed inspections;
- generated corrective work order belongs to the inspected asset and is `CORRECTIVE`;
- at least one item-linked photo exists in the homologation scenario.

## Final modeling decision

The inspection record is the historical event; checklist items are the auditable criteria evaluated in that event; photos are evidence attached to a specific criterion when appropriate. This avoids encoding the operational checklist as an opaque free-text field while retaining the legacy text snapshot for compatibility and historical readability.
