# Donation lifecycle and title transfer

The donation domain has two explicit directions.

## Outgoing donation (realized)

- Endpoint: `POST /api/erp/donations`.
- Direction: `OUTGOING`.
- Event type: `REALIZED`.
- The selected stock must already belong to the selected COMANDOS organization/unit.
- Serialized assets become `DONATED`.
- Lot balances are reduced by the donated quantity.
- Stock movement nature is `DONATION_OUT` and the movement quantity is negative.
- Historical ownership snapshot: `ORGANIZATION -> DONEE`.
- Title transfer state: `TRANSFERRED_TO_DONEE`.
- `realizedAt` and `titleTransferredAt` identify the effective transfer instant.

The legacy `POST /api/erp/donations` contract remains available to the existing frontend, but it no longer accepts `INCOMING`; received donations must use the receipt endpoint.

## Incoming donation (received)

- Endpoint: `POST /api/erp/donations/receive`.
- Direction: `INCOMING`.
- Event type: `RECEIVED`.
- A term and a document reference are mandatory.
- The destination location must belong to the selected organization/unit.
- Serialized catalog families create an individual `AssetItem` with globally checked asset code, serial and internal code.
- Non-serialized catalog families create a `StockLot` plus its destination `StockBalance`.
- Stock movement nature is `DONATION_IN` and the movement quantity is positive.
- Historical ownership snapshot: `DONOR -> ORGANIZATION`.
- Title transfer state: `TRANSFERRED_TO_ORGANIZATION`.
- `receivedAt` and `titleTransferredAt` identify the effective transfer instant.

## Term and documentary instrument

`term` is mandatory in both directions and is persisted with `termConfirmed=true` only when non-blank. Received donations additionally require `documentReference`, because incorporation of externally owned material must keep the documentary source that supports the title transfer.

## Compatibility and audit

Existing donation records are normalized at persistence time: outgoing records receive `REALIZED`/`TRANSFERRED_TO_DONEE`, incoming records receive `RECEIVED`/`TRANSFERRED_TO_ORGANIZATION`, and ownership snapshots on each item are filled from the donation parties when older code paths do not provide them directly.

`GET /api/erp/donations/{id}/lifecycle` exposes direction, event type, title-transfer state, term confirmation and the effective timestamps without changing the older donation detail response used by the frontend.

The Oracle demo gate runs `DonationLifecycleDemoVerifier`, which rejects contradictory direction/event/title combinations, missing ownership snapshots, missing terms, and invalid incoming movement direction.
