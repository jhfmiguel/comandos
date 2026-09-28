# Transfer transit lifecycle

The COMANDOS transfer flow separates the existing workflow status from the physical/logistical state of the material.

## Compatibility workflow status

Existing clients may continue to use:

- `PENDING_ACCEPTANCE`
- `ACCEPTED`
- `REJECTED`

The legacy `SENT` value is interpreted as an in-transit historical record.

## Physical transit state

The API now exposes `transitState`:

| Workflow status | Transit state | Meaning |
| --- | --- | --- |
| `PENDING_ACCEPTANCE` / legacy `SENT` | `IN_TRANSIT` | Material has left the source custody and cannot be used there or at destination yet. |
| `ACCEPTED` | `RECEIVED` | Destination confirmed receipt and stock becomes available according to the existing inventory rules. |
| `REJECTED` | `RETURNED_TO_SOURCE` | Destination rejected the transfer and the material was restored to the source flow. |

## Explicit operations

Preferred API operations:

- `POST /api/erp/transfers/dispatch` — dispatches the transfer and starts `IN_TRANSIT`.
- `POST /api/erp/transfers/{id}/receive` — confirms physical receipt and moves to `RECEIVED`.
- `POST /api/erp/transfers/{id}/reject` — rejects the receipt and closes transit as `RETURNED_TO_SOURCE`.

Compatibility endpoints remain available:

- `POST /api/erp/transfers`
- `POST /api/erp/transfers/{id}/accept`

## Traceability

A transfer preserves:

- organization;
- historical source unit name;
- destination unit and location;
- dispatch timestamp and actor;
- receipt timestamp and actor, when received;
- rejection timestamp, actor and reason, when rejected;
- transit close timestamp;
- `TRANSFER_OUT` and `TRANSFER_IN` stock movements for every operational transfer line;
- stock movement reference back to the transfer.

Individual assets remain `TRANSFER_PENDING` while the transfer is in transit. Lot quantities remain unavailable to normal destination use until receipt confirmation through the existing blocked-balance mechanism.

## Invariants

1. Source and destination units are different.
2. A new dispatch starts as `PENDING_ACCEPTANCE + IN_TRANSIT`.
3. Only an in-transit transfer can be received or rejected.
4. Receipt closes transit as `ACCEPTED + RECEIVED`.
5. Rejection closes transit as `REJECTED + RETURNED_TO_SOURCE`.
6. Dispatch, receipt and rejection actors/timestamps remain recorded.
7. Historical source unit/location data remains available even after the item is physically at the destination.
8. The Oracle demo regression fails startup when the transfer lifecycle or its movement references are inconsistent.
