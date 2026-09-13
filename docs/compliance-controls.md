# Expiration, certification and recall controls

The inventory Compliance group contains four additive resources: expiration controls, certifications, recalls and recall items. Expiration and certification records target exactly one serialized asset or stock lot and preserve organization and optional unit ownership.

An expiration marked EXPIRED blocks an AVAILABLE asset. An expired certification cannot remain ACTIVE, and a certification marked EXPIRED also blocks an AVAILABLE asset. Recall items target exactly one asset or lot, belong to an OPEN or IN_PROGRESS recall, and block AVAILABLE assets immediately.

Compliance history cannot be deleted. Mutations use the existing optimistic locking, scoped permissions and audit snapshots of the generic inventory service. Lot recalls are registered for traceability; quantity quarantine will be completed by the reservation and blocked-balance workflow.
