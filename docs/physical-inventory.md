# Physical inventory and reconciliation

Physical inventory compares the system snapshot with equipment and lot quantities found at one stock location. It is generic across all inventory families.

## Workflow

1. Open a count for an organization, optional unit, stock location, and purpose.
2. The system snapshots every `AVAILABLE` or `BLOCKED` individual asset and every positive lot balance at that location.
3. Enter a counted quantity and optional notes for every line. Individual assets accept zero or one.
4. Save the count to calculate `MATCH`, `SHORTAGE`, or `SURPLUS`.
5. An authorized approver applies all adjustments atomically.

Lot differences change the location balance, the lot physical total, and create `INVENTORY_ADJUSTMENT` movements. An absent individual asset becomes `BLOCKED` and receives a negative adjustment movement. An unidentified individual surplus must first be registered before approval. Reserved or blocked lot quantity cannot be silently consumed by a shortage.

Only one unfinished count may exist per location. Approved counts and their adjustment links preserve history. The `inventory-counts` resource uses `READ`, `CREATE`, `COUNT`, `APPROVE`, and `CANCEL` permissions.

`InventoryCountStatusType` and `InventoryCountResultType` are managed reference registrations. Protected defaults are `OPEN`, `COUNTED`, `APPROVED`, `CANCELLED`, `MATCH`, `SHORTAGE`, and `SURPLUS`.
