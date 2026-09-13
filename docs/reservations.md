# Inventory reservations

Reservations temporarily commit individual equipment or a quantity from a stock lot to a future purpose without recording a physical stock movement.

## Data model

- `ReservationStatusType` is a managed registration table. The application seeds protected `ACTIVE`, `CANCELLED`, and `EXPIRED` records; administrators may add and deactivate other status options.
- `InventoryReservation` stores scope, purpose, period, status, operator and idempotency data.
- `ReservationItem` identifies either one individual asset or one lot balance and stores descriptive snapshots and quantity.

An active reservation changes an individual asset from `AVAILABLE` to `BLOCKED`. For lots, it moves quantity from `StockBalance.available` to `StockBalance.reserved`; `StockLot.availableQuantity` remains the physical total. Cancellation or expiration releases the commitment. An individual asset only returns to `AVAILABLE` when no expiration or recall rule requires it to remain blocked.

## HTTP API

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/api/erp/reservations/stock` | Search reservable assets or lot balances |
| POST | `/api/erp/reservations` | Create an atomic reservation with 1–100 items |
| GET | `/api/erp/reservations` | List scoped reservation history |
| POST | `/api/erp/reservations/{id}/cancel` | Cancel and release an active reservation |
| POST | `/api/erp/reservations/{id}/expire` | Expire and release after the end time |

Creation uses a canonical UUID request ID and fingerprint for idempotency. Locks protect assets, balances, and status rows against concurrent allocation. Reserved assets are excluded from custody, sales, transfers, donation, disposal, and maintenance eligibility.

The `/erp/reservations` page supports scope selection, purpose and period, asset/lot search, an item cart, history, cancellation, and expiration. The status registration is maintained under **Inventory > Reference data > Reservation status types**.
