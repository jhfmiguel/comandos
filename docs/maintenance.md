# Maintenance and inspection

The maintenance module implements MaintenancePlan, WorkOrder, Diagnosis, ExecutedService and FunctionalTest for serialized assets. Plans belong to an organization and optional unit and define a name, type, periodicity in days and active state.

Opening a work order accepts an AVAILABLE or BLOCKED asset, snapshots its scope and description, changes it to IN_MAINTENANCE and creates a negative MAINTENANCE_ISSUE movement. Completion requires a diagnosis, one or more executed services with non-negative cost, and an APPROVED or REJECTED functional test.

An approved, unexpired asset returns to AVAILABLE; a rejected or expired asset returns as BLOCKED. Completion creates a positive MAINTENANCE_RETURN movement. Opening and completion use separate canonical UUID fingerprints for safe retries. Both operations are atomic, audited and protected by pessimistic locks.

The maintenance resource supports READ, CREATE and COMPLETE at SYSTEM, ORGANIZATION and UNIT scope. The English interface is available at /erp/maintenance.

A work order may originate from one blocking custody return inspection. The link is optional for ordinary work orders and unique when supplied. Opening validates the returned individual asset, exact organization/unit scope and blocking condition. The custody screen provides the explicit action and pre-fills the maintenance form; the operator still reviews the reason and chooses an optional plan before opening the order. Custody history then displays the generated work-order number.

Periodic inspection history refreshes after every recorded or approved inspection, including consecutive submissions that display the same success message. Approving the inspection record does not release an asset whose work order is still open. Maintenance and inspection evidence can be uploaded and downloaded from their history entries.

Task 014 browser/PostgreSQL validation and reproduction commands are recorded in [armamento-014-validation.md](armamento-014-validation.md).
