# Equipment custody

The custody module controls delivery and return of individually tracked equipment and complete active equipment sets. It owns `custody/model`, `custody/dto`, `custody/service`, and `custody/controller` without replacing inventory or legacy records.

## Data model

- `Custody` stores organization/unit, recipient, authorizer, purpose, delivery, optional due date, completion, status, operator snapshots and request ID.
- `CustodyItem` stores an asset or set lot quantity, its set/role/model/code/serial/location snapshots, quantity, issue movement and return time.
- `CustodyReturn` stores every partial or final return, operator and request ID.
- `CustodyReturnItem` links returned items to positive stock movements.
- `CustodyReturnConditionType` is managed reference data. It defines whether a
  return result releases stock or sends it to blocked availability.

## HTTP API

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/api/erp/custodies/stock?organizationId=1&unitId=2&search=&page=0` | Available independent equipment |
| GET | `/api/erp/custodies/equipment-sets?organizationId=1&unitId=2&search=&page=0` | Complete active sets whose components are available |
| POST | `/api/erp/custodies` | Issue individual assets and/or sets, up to 100 expanded items |
| GET | `/api/erp/custodies?organizationId=1&unitId=2&page=0` | Scoped history |
| GET | `/api/erp/custodies/1` | Full record and returns |
| POST | `/api/erp/custodies/1/returns` | Return selected pending item IDs |

Any available and unexpired individual asset in the exact organization/unit can be issued, regardless of category. An asset assigned to an active set is issued only through that set. A set expands into all of its components in one transaction. Individual assets change to `CUSTODIED`; set lot quantities reduce the selected balance and global lot availability. Every component creates a negative `CUSTODY_ISSUE` movement.

A return requires an active inspection condition and accepts optional inspection notes. It creates a positive `CUSTODY_RETURN` movement. A nonblocking result returns valid assets to `AVAILABLE` and lot quantities to `available`. A blocking result sends assets to `BLOCKED` and lot quantities to `blocked`; expiry also blocks an asset. Every pending component of one set must be returned together. The return stores the condition, notes and inspecting operator for each component. The custody status progresses through `ACTIVE`, `PARTIALLY_RETURNED`, and `RETURNED`. Canonical UUID request IDs make retries idempotent, and concurrent requests cannot issue the same equipment twice.

For a blocking inspection on an individual asset, the history offers **Open maintenance**. It opens the maintenance workspace with organization, unit, asset, inspection notes and return inspection already filled. The resulting work order keeps a unique link to that inspection. A nonblocking inspection, a different asset or scope, and a second work order for the same inspection are rejected by the API. Quantity-controlled components remain blocked stock and do not open serialized-asset maintenance orders.

Permission resource `custodies` supports `READ`, `CREATE`, and `RETURN` with SYSTEM, ORGANIZATION, or exact UNIT scope. Person selection separately requires `core/people` READ.

The protected default return conditions are `GOOD`, `NEEDS_INSPECTION`, and
`DAMAGED`. Users may create other conditions and configure **Blocks availability**
under **Assets and inventory → Reference data**. Existing API clients that omit
the condition remain compatible and use `GOOD`.

Documents, signatures, overdue notifications, and approvals remain future extensions.
