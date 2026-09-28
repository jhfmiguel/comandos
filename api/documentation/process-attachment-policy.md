# Uniform process attachment policy

## Purpose

COMANDOS uses one attachment policy for business processes while preserving domain-specific structured documents. The binary attachment layer is transversal and does not replace records such as acquisition contracts, invoices or other business entities that carry structured fields.

## Covered processes

The policy applies to:

- ACQUISITION
- RECEIVING
- INCORPORATION
- CUSTODY
- TRANSFER
- DONATION
- SALE
- CONSUMABLE_USAGE
- MAINTENANCE
- INSPECTION
- OCCURRENCE
- DISPOSAL
- EQUIPMENT_SET_OPERATION

Every attachment is linked by `processType + recordId`, organization and optional unit.

## File policy

All processes use the same physical-file policy:

- maximum size: 20 MB;
- supported MIME types: PDF, JPEG, PNG, WebP, DOCX and XLSX;
- binary content is stored through `DocumentStorage`;
- Oracle storage uses immutable BLOB rows;
- SHA-256 is calculated when the bytes are stored;
- metadata records the file name, content type, byte size, checksum, uploader and upload timestamp.

A read operation recalculates SHA-256 and rejects corrupted content if the stored bytes no longer match the original checksum.

## Versioning

An attachment is never overwritten in place.

Replacement creates a new `ProcessAttachment` with an incremented `versionNumber` and `supersedes` reference. The previous row becomes `currentVersion=false` and receives `retiredAt`. The binary object belonging to the previous version remains immutable and downloadable when history is requested.

This preserves the document that supported a decision at the time it was made even if a newer version is later supplied.

## Document classification matrix

### Acquisition

Allowed: PROCESS, NOTICE, CONTRACT, COMMITMENT, INVOICE, TERM, REPORT, OTHER.
Required for document completeness: PROCESS.

### Receiving

Allowed: INVOICE, RECEIPT_TERM, INSPECTION_REPORT, DIVERGENCE_REPORT, PHOTO, OTHER.

### Incorporation

Allowed: INCORPORATION_TERM, PATRIMONY_RECORD, INSPECTION_REPORT, PHOTO, OTHER.

### Custody

Allowed: CUSTODY_TERM, RESPONSIBILITY_TERM, RETURN_TERM, DAMAGE_REPORT, PHOTO, OTHER.

### Transfer

Allowed: TRANSFER_TERM, DISPATCH_TERM, RECEIPT_TERM, REJECTION_TERM, PHOTO, OTHER.

### Donation

Allowed: DONATION_TERM, RECEIPT_TERM, LEGAL_INSTRUMENT, PHOTO, OTHER.
Required for document completeness: DONATION_TERM.

### Sale / alienation

Allowed: PROCESS, LEGAL_BASIS, SALE_TERM, INVOICE, RECEIPT, RETURN_TERM, CANCELLATION_TERM, OTHER.
Required for document completeness: PROCESS.

### Consumable usage

Allowed: DELIVERY_TERM, USAGE_REPORT, RETURN_TERM, OPERATION_REPORT, PHOTO, OTHER.

### Maintenance

Allowed: WORK_ORDER, DIAGNOSIS_REPORT, SERVICE_REPORT, PART_INVOICE, FUNCTIONAL_TEST, CERTIFICATE, PHOTO, OTHER.

### Inspection

Allowed: CHECKLIST, INSPECTION_REPORT, PHOTO, CERTIFICATE, OTHER.

### Exceptional occurrence

Allowed: OCCURRENCE_REPORT, POLICE_REPORT, INVESTIGATION_REPORT, RECOVERY_TERM, SEIZURE_TERM, PHOTO, OTHER.

### Disposal

Allowed: PROCESS, APPROVAL_TERM, DESTRUCTION_CERTIFICATE, DISPOSAL_CERTIFICATE, PHOTO, OTHER.
Required for document completeness: PROCESS.

### Equipment-set aggregate operation

Allowed: AGGREGATE_OPERATION_TERM, COMPONENT_LIST, PHOTO, OTHER.

## API

Policy:

`GET /api/erp/documents/policy/{processType}`

Current attachments or complete version history:

`GET /api/erp/documents/{processType}/{recordId}?organizationId={id}&unitId={id}&includeHistory={true|false}`

Upload:

`POST /api/erp/documents/{processType}/{recordId}` using multipart/form-data with organizationId, optional unitId, documentType, title and file.

Create a new version:

`POST /api/erp/documents/attachments/{attachmentId}/versions` using multipart/form-data with optional title and file.

Download:

`GET /api/erp/documents/attachments/{attachmentId}/download`

Document completeness:

`GET /api/erp/documents/{processType}/{recordId}/compliance?organizationId={id}&unitId={id}`

The completeness endpoint returns required, present and missing document types. A process-specific domain service may use that information as a prerequisite when the business rule requires documentary completion; the generic attachment service itself does not silently change the finalization rules of unrelated domains.

## Authorization and audit

Each process type maps to its existing permission resource. Upload/replacement uses CREATE permission for that resource. Listing, policy and download use READ permission. Organization and unit scope are checked through the existing `AccessPolicy`.

Upload and replacement are written to the audit trail with the attachment identifier and immutable metadata. Previous versions remain available for historical reconstruction.

## Compatibility

Acquisition keeps its typed business documents. Maintenance and inspection can keep their existing specialized endpoints during migration. The uniform layer provides the common binary evidence policy and can be adopted by all screens without removing existing domain records.

## Oracle regression

`ProcessAttachmentPolicyDemoVerifier` checks the complete process matrix, allowed/required document types, MIME/size limits, SHA-256 metadata, storage references, immutable version chains and binary-length integrity during the demo/Oracle startup gate.
