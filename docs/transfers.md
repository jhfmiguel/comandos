# Inventory transfers

Inventory transfers use a destination-confirmation lifecycle instead of making
received stock immediately available.

## Lifecycle

1. The source finalizes the transfer.
2. Serialized assets move to the destination location with status
   `TRANSFER_PENDING`.
3. Lot quantities leave the source available balance and enter the destination
   `blocked` balance.
4. The destination explicitly accepts or rejects the transfer.
5. Acceptance releases the asset/quantity as available at the destination.
6. Rejection returns the asset/quantity to the source and records reversal stock
   movements without deleting the original transfer history.

States are `PENDING_ACCEPTANCE`, `ACCEPTED`, and `REJECTED`.

The transfer records organization, source and destination units, destination
location, purpose, transfer type, legal instrument/document reference, sender,
accepting or rejecting operator, timestamps, and rejection reason.

## API

- `GET /api/erp/transfers/stock`
- `POST /api/erp/transfers`
- `POST /api/erp/transfers/{id}/accept`
- `POST /api/erp/transfers/{id}/reject`
- `GET /api/erp/transfers`
- `GET /api/erp/transfers/{id}`

Finalization is atomic and uses a canonical UUID request id. Source and
destination stock are protected by pessimistic locking. Acceptance requires the
`ACCEPT` action in the destination scope; rejection requires `REJECT` in the
destination scope. History remains visible with `READ` to either participating
unit.

The frontend exposes destination acceptance/rejection from transfer history when
the signed-in profile has the corresponding permission.
