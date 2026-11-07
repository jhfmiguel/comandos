"use client";

import * as React from "react";
import axios from "axios";
import { Layout } from "components/layout";
import { Message } from "components/common/message";
import { useSession } from "components/auth/session-provider";
import { ReferenceField } from "components/erp/shared/record-workspace";
import { createErpService } from "api/services/erp.service";
import { logisticsService } from "api/services/logistics.service";
import type { ErpField, ErpValue } from "api/models/erp";
import type { ImportExpense, ImportFile, ImportLine, ImportLineCost, ImportProcess, ImportStatusTransition, InventoryBalance, InventoryLot, InventoryMovement, InventoryReservation, InventoryTransfer, InventoryValuation, Shipment, ShipmentItem, WarehouseBin } from "api/models/erp/logistics";
import styles from "components/erp/shared/workspace.module.css";

const core = createErpService("core");
const organizationField: ErpField = { name: "organizationId", label: "Organization", type: "reference", required: true, reference: "organizations", choices: [] };
const unitField: ErpField = { name: "unitId", label: "Unit", type: "reference", required: false, reference: "units", choices: [] };
const failure = (error: unknown) => axios.isAxiosError(error) && typeof error.response?.data?.detail === "string"
    ? error.response.data.detail : "The operation could not be completed. Check access and API connectivity.";

export function LogisticsWorkspace() { return <Layout title="Stock logistics"><LogisticsContent /></Layout>; }

function LogisticsContent() {
    const { can } = useSession();
    const [organization, setOrganization] = React.useState<ErpValue>(null);
    const [unit, setUnit] = React.useState<ErpValue>(null);
    const [bins, setBins] = React.useState<WarehouseBin[]>([]);
    const [lots, setLots] = React.useState<InventoryLot[]>([]);
    const [shipments, setShipments] = React.useState<Shipment[]>([]);
    const [imports, setImports] = React.useState<ImportProcess[]>([]);
    const [shipmentItems, setShipmentItems] = React.useState<ShipmentItem[]>([]);
    const [importExpenses, setImportExpenses] = React.useState<ImportExpense[]>([]);
    const [importDocuments, setImportDocuments] = React.useState<ImportFile[]>([]);
    const [importHistory, setImportHistory] = React.useState<ImportStatusTransition[]>([]);
    const [importLines, setImportLines] = React.useState<ImportLine[]>([]);
    const [importLineCosts, setImportLineCosts] = React.useState<ImportLineCost[]>([]);
    const [balances, setBalances] = React.useState<InventoryBalance[]>([]);
    const [valuations, setValuations] = React.useState<InventoryValuation[]>([]);
    const [movements, setMovements] = React.useState<InventoryMovement[]>([]);
    const [transfers, setTransfers] = React.useState<InventoryTransfer[]>([]);
    const [reservations, setReservations] = React.useState<InventoryReservation[]>([]);
    const [selectedBin, setSelectedBin] = React.useState("");
    const [productId, setProductId] = React.useState("");
    const [costingMethod, setCostingMethod] = React.useState<InventoryValuation["method"]>("WEIGHTED_AVERAGE");
    const [error, setError] = React.useState("");
    const [success, setSuccess] = React.useState("");
    const organizationId = organization ? Number(organization) : undefined;
    const unitId = unit ? Number(unit) : undefined;
    const canRead = can("inventory/logistics", "READ", organizationId, unitId);
    const canCreate = can("inventory/logistics", "CREATE", organizationId, unitId);
    const canUpdate = can("inventory/logistics", "UPDATE", organizationId, unitId);

    const refresh = React.useCallback(async () => {
        if (!organizationId || !canRead) { setBins([]); setLots([]); return; }
        try {
            const [nextBins, nextLots, nextShipments, nextImports, nextTransfers] = await Promise.all([
                logisticsService.bins(organizationId, unitId), logisticsService.lots(organizationId, unitId, productId || undefined),
                logisticsService.shipments(organizationId, unitId), logisticsService.imports(organizationId, unitId), logisticsService.transfers(organizationId, unitId, productId || undefined)
            ]);
            setBins(nextBins); setLots(nextLots); setShipments(nextShipments); setImports(nextImports); setTransfers(nextTransfers); setError("");
            if (!selectedBin && nextBins.length) setSelectedBin(nextBins[0].id);
        } catch (reason) { setError(failure(reason)); }
    }, [organizationId, unitId, canRead, productId, selectedBin]);
    React.useEffect(() => {
        if (!organizationId || !canRead) return;
        let active = true;
        Promise.all([logisticsService.bins(organizationId, unitId), logisticsService.lots(organizationId, unitId, productId || undefined), logisticsService.shipments(organizationId, unitId), logisticsService.imports(organizationId, unitId), logisticsService.transfers(organizationId, unitId, productId || undefined)])
            .then(([nextBins, nextLots, nextShipments, nextImports, nextTransfers]) => { if (active) { setBins(nextBins); setLots(nextLots); setShipments(nextShipments); setImports(nextImports); setTransfers(nextTransfers); setError(""); if (!selectedBin && nextBins.length) setSelectedBin(nextBins[0].id); } })
            .catch(reason => { if (active) setError(failure(reason)); });
        return () => { active = false; };
    }, [organizationId, unitId, canRead, productId, selectedBin]);

    async function run(action: () => Promise<unknown>, message: string): Promise<boolean> {
        setError(""); setSuccess("");
        try { await action(); setSuccess(message); await refresh(); return true; }
        catch (reason) { setError(failure(reason)); return false; }
    }
    async function createBin(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId) return;
        const form = event.currentTarget; const data = new FormData(form);
        if (await run(() => logisticsService.createBin({ id: crypto.randomUUID(), organizationId, unitId,
            warehouseId: String(data.get("warehouseId")), code: String(data.get("code")), name: String(data.get("name")),
            unitOfMeasure: String(data.get("unitOfMeasure")), parentBinId: String(data.get("parentBinId") || "") || undefined,
            capacity: String(data.get("capacity") || "") || undefined }), "Warehouse bin created.")) form.reset();
    }
    async function createLot(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId) return;
        const form = event.currentTarget; const data = new FormData(form);
        if (await run(() => logisticsService.createLot({ id: crypto.randomUUID(), organizationId, unitId,
            productId: String(data.get("productId")), lotNumber: String(data.get("lotNumber")),
            manufacturedOn: String(data.get("manufacturedOn") || "") || undefined, expiresOn: String(data.get("expiresOn") || "") || undefined,
            supplierId: String(data.get("supplierId") || "") || undefined }), "Inventory lot registered.")) form.reset();
    }
    async function updateLotStatus(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId) return;
        const form = event.currentTarget; const data = new FormData(form);
        if (await run(() => logisticsService.updateLotStatus(String(data.get("lotId")), organizationId, unitId,
            String(data.get("status")) as InventoryLot["status"]), "Lot status updated.")) form.reset();
    }
    async function receive(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId || !selectedBin) return;
        const form = event.currentTarget; const data = new FormData(form);
        if (await run(() => logisticsService.receive(selectedBin, organizationId, unitId, { id: crypto.randomUUID(),
            productId: String(data.get("productId")), lotId: String(data.get("lotId") || "") || undefined,
            quantity: String(data.get("quantity")), unitOfMeasure: String(data.get("unitOfMeasure")),
            referenceType: "RECEIPT", referenceId: String(data.get("referenceId") || "") || undefined,
            unitCost: String(data.get("unitCost") || "") || undefined, currency: String(data.get("currency") || "") || undefined }), "Stock receipt recorded.")) form.reset();
    }
    async function adjust(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId || !selectedBin) return;
        const form = event.currentTarget; const data = new FormData(form);
        if (await run(() => logisticsService.adjust(selectedBin, organizationId, unitId, { id: crypto.randomUUID(),
            productId: String(data.get("productId")), lotId: String(data.get("lotId") || "") || undefined,
            type: String(data.get("type")) as "ADJUSTMENT_IN" | "ADJUSTMENT_OUT", quantity: String(data.get("quantity")),
            unitOfMeasure: String(data.get("unitOfMeasure")), referenceId: String(data.get("referenceId") || "") || undefined
        }), "Inventory count adjustment recorded.")) form.reset();
    }
    async function reserve(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId || !selectedBin) return;
        const form = event.currentTarget; const data = new FormData(form);
        if (await run(() => logisticsService.reserveFefo(selectedBin, organizationId, unitId, { id: crypto.randomUUID(),
            productId: String(data.get("productId")),
            quantity: String(data.get("quantity")), unitOfMeasure: String(data.get("unitOfMeasure")),
            referenceType: "ORDER", referenceId: String(data.get("referenceId") || "") || undefined }), "Stock reserved.")) form.reset();
    }
    async function transfer(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId) return;
        const form = event.currentTarget; const data = new FormData(form);
        if (await run(() => logisticsService.transfer({ id: crypto.randomUUID(), organizationId, unitId,
            productId: String(data.get("productId")), lotId: String(data.get("lotId") || "") || undefined,
            sourceLocationId: String(data.get("source")), destinationLocationId: String(data.get("destination")),
            quantity: String(data.get("quantity")), unitOfMeasure: String(data.get("unitOfMeasure")),
            referenceType: "TRANSFER", referenceId: String(data.get("referenceId") || "") || undefined }), "Stock transfer recorded.")) form.reset();
    }
    async function createShipment(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId) return; const form=event.currentTarget; const data=new FormData(form);
        if (await run(() => logisticsService.createShipment({ id: crypto.randomUUID(), organizationId, unitId,
            reference: String(data.get("reference")), originLocationId: String(data.get("origin")), destinationLocationId: String(data.get("destination")),
            carrier: String(data.get("carrier") || "") || undefined, trackingNumber: String(data.get("trackingNumber") || "") || undefined,
            recipient: String(data.get("recipient") || "") || undefined }), "Shipment created.")) form.reset();
    }
    async function recordShipmentEvent(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId) return; const form=event.currentTarget; const data=new FormData(form);
        if (await run(() => logisticsService.recordShipmentEvent(String(data.get("shipmentId")), organizationId, unitId, {
            id: crypto.randomUUID(), type: String(data.get("type")) as "PICKED" | "DISPATCHED" | "DELIVERED" | "OCCURRENCE" | "PROOF_ATTACHED",
            locationId: String(data.get("locationId") || "") || undefined, description: String(data.get("description")),
            evidenceReference: String(data.get("evidenceReference") || "") || undefined }), "Shipment event recorded.")) form.reset();
    }
    async function createImport(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId || !selectedBin) return; const form=event.currentTarget; const data=new FormData(form);
        if (await run(() => logisticsService.createImport({ id: crypto.randomUUID(), organizationId, unitId,
            reference: String(data.get("reference")), supplierId: String(data.get("supplierId")), supplierCountry: String(data.get("supplierCountry")),
            purchaseReference: String(data.get("purchaseReference") || "") || undefined, currency: String(data.get("currency")),
            exchangeRate: Number(data.get("exchangeRate")), orderedOn: String(data.get("orderedOn")),
            estimatedArrival: String(data.get("estimatedArrival") || "") || undefined, destinationLocationId: selectedBin }), "Import process created.")) form.reset();
    }
    async function addImportExpense(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId) return; const form=event.currentTarget; const data=new FormData(form);
        if (await run(() => logisticsService.addImportExpense(String(data.get("importId")), organizationId, unitId, {
            id: crypto.randomUUID(), category: String(data.get("category")), amount: Number(data.get("amount")),
            currency: String(data.get("currency")), exchangeRate: Number(data.get("exchangeRate")), baseCurrencyAmount: Number(data.get("baseCurrencyAmount")),
            incurredOn: String(data.get("incurredOn")), documentReference: String(data.get("documentReference") || "") || undefined,
            metadata: { baseCurrency: String(data.get("baseCurrency")).toUpperCase() } }), "Import expense registered.")) form.reset();
    }
    async function addImportLine(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId) return; const form=event.currentTarget; const data=new FormData(form);
        if (await run(() => logisticsService.addImportLine(String(data.get("importId")),organizationId,unitId,{ id: crypto.randomUUID(),
            productId: String(data.get("productId")), lotId: String(data.get("lotId")||"")||undefined, quantity: Number(data.get("quantity")),
            unitOfMeasure: String(data.get("unitOfMeasure")), unitPrice: Number(data.get("unitPrice")), currency: String(data.get("currency")).toUpperCase(),
            exchangeRate: Number(data.get("exchangeRate")), baseUnitPrice: Number(data.get("baseUnitPrice")), baseCurrency: String(data.get("baseCurrency")).toUpperCase(),
            supplierLineReference: String(data.get("supplierLineReference")||"")||undefined }), "Import item registered.")) form.reset();
    }
    async function receiveImportLine(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId) return; const form=event.currentTarget; const data=new FormData(form); const value=String(data.get("lineKey")).split("|");
        if (await run(() => logisticsService.receiveImportLine(value[0],value[1],organizationId,unitId,{ receiptId: crypto.randomUUID(), quantity: Number(data.get("quantity")) }), "Import line received into inventory with landed cost.")) form.reset();
    }
    async function addShipmentItem(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId) return; const form=event.currentTarget; const data=new FormData(form);
        if (await run(() => logisticsService.addShipmentItem(String(data.get("shipmentId")), organizationId, unitId, {
            id: crypto.randomUUID(), productId: String(data.get("productId")), lotId: String(data.get("lotId") || "") || undefined,
            serialNumber: String(data.get("serialNumber") || "") || undefined, quantity: Number(data.get("quantity")), unitOfMeasure: String(data.get("unitOfMeasure"))
        }), "Shipment item added.")) form.reset();
    }
    async function addImportDocument(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId) return; const form=event.currentTarget; const data=new FormData(form);
        if (await run(() => logisticsService.addImportDocument(String(data.get("importId")), organizationId, unitId, {
            id: crypto.randomUUID(), type: String(data.get("type")), documentReference: String(data.get("documentReference")),
            fileName: String(data.get("fileName")), checksum: String(data.get("checksum") || "") || undefined
        }), "Import document reference registered.")) form.reset();
    }
    async function updateImportStatus(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault(); if (!organizationId) return; const form=event.currentTarget; const data=new FormData(form);
        if (await run(() => logisticsService.updateImportStatus(String(data.get("importId")), organizationId, unitId, {
            status: String(data.get("status")) as ImportProcess["status"], arrivedOn: String(data.get("arrivedOn") || "") || undefined,
            clearedOn: String(data.get("clearedOn") || "") || undefined, customsReference: String(data.get("customsReference") || "") || undefined
        }), "Import status updated.")) form.reset();
    }
    async function loadShipmentItems(shipmentId: string) {
        if (!organizationId) return;
        try { setShipmentItems(await logisticsService.shipmentItems(shipmentId, organizationId, unitId)); setError(""); }
        catch (reason) { setError(failure(reason)); }
    }
    async function loadImportDetails(importId: string) {
        if (!organizationId) return;
        try { const [expenses, documents, history, lines] = await Promise.all([logisticsService.importExpenses(importId, organizationId, unitId), logisticsService.importDocuments(importId, organizationId, unitId), logisticsService.importHistory(importId, organizationId, unitId), logisticsService.importLines(importId,organizationId,unitId)]); const costs=await Promise.all(lines.map(line=>logisticsService.importLineCost(importId,line.id,organizationId,unitId))); setImportExpenses(expenses); setImportDocuments(documents); setImportHistory(history); setImportLines(lines); setImportLineCosts(costs); setError(""); }
        catch (reason) { setError(failure(reason)); }
    }
    async function loadBalance() {
        if (!organizationId || !selectedBin || !productId) return;
        try { const [nextBalances,nextReservations,nextMovements]=await Promise.all([logisticsService.balances(selectedBin, organizationId, unitId, productId, "EA"),logisticsService.reservations(selectedBin,organizationId,unitId,productId),logisticsService.movements(selectedBin,organizationId,unitId,productId)]);const nextValuations=await Promise.all(nextBalances.map(balance=>logisticsService.valuation(selectedBin,organizationId,unitId,productId,balance.lotId??null,balance.unitOfMeasure,costingMethod)));setBalances(nextBalances);setReservations(nextReservations);setMovements(nextMovements);setValuations(nextValuations);setError(""); }
        catch (reason) { setError(failure(reason)); }
    }
    async function finishReservation(id: string, fulfill: boolean) {
        if (!organizationId) return;
        const ok=await run(()=>fulfill?logisticsService.fulfillReservation(id,organizationId,unitId):logisticsService.releaseReservation(id,organizationId,unitId),fulfill?"Reservation fulfilled and stock issued.":"Reservation released.");
        if(ok&&selectedBin&&productId){try{const [nextBalances,nextReservations,nextMovements]=await Promise.all([logisticsService.balances(selectedBin,organizationId,unitId,productId,"EA"),logisticsService.reservations(selectedBin,organizationId,unitId,productId),logisticsService.movements(selectedBin,organizationId,unitId,productId)]);const nextValuations=await Promise.all(nextBalances.map(balance=>logisticsService.valuation(selectedBin,organizationId,unitId,productId,balance.lotId??null,balance.unitOfMeasure,costingMethod)));setBalances(nextBalances);setReservations(nextReservations);setMovements(nextMovements);setValuations(nextValuations);}catch(reason){setError(failure(reason));}}
    }

    return <main className={styles.panel}>
        <div className={styles.fields}>
            <ReferenceField service={core} field={organizationField} value={organization} organizationId={null} onChange={value => { setOrganization(value); setUnit(null); setSelectedBin(""); }} />
            <ReferenceField service={core} field={unitField} value={unit} organizationId={organization} onChange={setUnit} />
        </div>
        {error && <Message type="error" text={error} onClose={() => setError("")} />}
        {success && <Message type="success" text={success} onClose={() => setSuccess("")} />}
        {!organizationId && <Message type="info" text="Choose an organization and unit to view stock logistics." />}
        {organizationId && !canRead && <Message type="warn" text="Your profile cannot read logistics records in this scope." />}
        {organizationId && canRead && <>
            <section>
                <h2>Warehouse positions</h2>
                {canCreate && <form className={styles.fields} onSubmit={createBin}>
                    <label className={styles.field}>Warehouse ID<input name="warehouseId" required /></label>
                    <label className={styles.field}>Code<input name="code" required /></label>
                    <label className={styles.field}>Name<input name="name" required /></label>
                    <label className={styles.field}>Unit<input name="unitOfMeasure" defaultValue="EA" required /></label>
                    <label className={styles.field}>Parent bin ID<input name="parentBinId" /></label>
                    <label className={styles.field}>Capacity (optional)<input name="capacity" type="number" min="0.0001" step="any" /></label>
                    <button type="submit">Add position</button>
                </form>}
                <div className={styles.fields}>
                    <label className={styles.field}>Selected position<select value={selectedBin} onChange={event => setSelectedBin(event.target.value)}><option value="">Select</option>{bins.map(bin => <option key={bin.id} value={bin.id}>{bin.code} — {bin.name}</option>)}</select></label>
                    <label className={styles.field}>Product ID<input value={productId} onChange={event => setProductId(event.target.value)} /></label>
                    <label className={styles.field}>Costing method<select value={costingMethod} onChange={event => setCostingMethod(event.target.value as InventoryValuation["method"])}><option value="WEIGHTED_AVERAGE">Weighted average</option><option value="FIFO">FIFO</option></select></label>
                    <button type="button" onClick={loadBalance} disabled={!selectedBin || !productId}>View balance</button>
                </div>
                <div className={styles.tableContainer}><table><thead><tr><th>Warehouse</th><th>Position</th><th>Unit</th><th>Active</th></tr></thead><tbody>{bins.map(bin => <tr key={bin.id}><td>{bin.warehouseId}</td><td>{bin.code} — {bin.name}</td><td>{bin.unitOfMeasure}</td><td>{bin.active ? "Yes" : "No"}</td></tr>)}</tbody></table></div>
                {balances.length > 0 && <div className={styles.tableContainer}><table><thead><tr><th>Lot</th><th>On hand</th><th>Reserved</th><th>Available</th><th>Unit</th><th>{costingMethod === "FIFO" ? "FIFO value" : "Weighted value"}</th><th>Cost status</th></tr></thead><tbody>{balances.map(balance => { const valuation=valuations.find(value=>value.lotId===balance.lotId); return <tr key={balance.lotId ?? `${balance.productId}-${balance.locationId}-untracked`}><td>{lots.find(lot => lot.id === balance.lotId)?.lotNumber ?? balance.lotId ?? "Untracked"}</td><td>{balance.onHand}</td><td>{balance.reserved}</td><td>{balance.available}</td><td>{balance.unitOfMeasure}</td><td>{valuation?.complete ? `${valuation.onHandValue ?? 0} ${valuation.currency ?? ""}` : "—"}</td><td>{valuation?.complete ? "Complete" : valuation?.incompleteReason === "MULTIPLE_CURRENCIES_REQUIRE_CONVERSION" ? "Convert currencies" : "Missing cost"}</td></tr>; })}</tbody></table></div>}
                {reservations.length > 0 && <div className={styles.tableContainer}><table><thead><tr><th>Order</th><th>Lot</th><th>Quantity</th><th>Status</th><th>Actor</th><th>Actions</th></tr></thead><tbody>{reservations.map(reservation=><tr key={reservation.id}><td>{reservation.referenceId}</td><td>{lots.find(lot=>lot.id===reservation.lotId)?.lotNumber ?? reservation.lotId}</td><td>{reservation.quantity} {reservation.unitOfMeasure}</td><td>{reservation.status}</td><td>{reservation.actor}</td><td>{reservation.status==="ACTIVE"&&canUpdate&&<><button type="button" onClick={()=>{void finishReservation(reservation.id,false);}}>Release</button><button type="button" onClick={()=>{void finishReservation(reservation.id,true);}}>Fulfill</button></>}</td></tr>)}</tbody></table></div>}
                {movements.length > 0 && <div className={styles.tableContainer}><table><thead><tr><th>Movement</th><th>Type</th><th>Quantity</th><th>Cost / unit</th><th>Reference</th><th>When</th><th>Actor</th></tr></thead><tbody>{movements.map(movement=><tr key={movement.id}><td>{movement.id}</td><td>{movement.type}</td><td>{movement.quantity} {movement.unitOfMeasure}</td><td>{movement.unitCost ?? "—"} {movement.currency ?? ""}</td><td>{movement.referenceType ?? ""} {movement.referenceId ?? ""}</td><td>{movement.occurredAt}</td><td>{movement.actor}</td></tr>)}</tbody></table></div>}
                {transfers.length > 0 && <div className={styles.tableContainer}><table><thead><tr><th>Transfer</th><th>Product</th><th>Lot</th><th>From</th><th>To</th><th>Quantity</th><th>When</th><th>Actor</th></tr></thead><tbody>{transfers.map(transfer=><tr key={transfer.id}><td>{transfer.id}</td><td>{transfer.productId}</td><td>{lots.find(lot=>lot.id===transfer.lotId)?.lotNumber ?? transfer.lotId ?? "Untracked"}</td><td>{bins.find(bin=>bin.id===transfer.sourceLocationId)?.code ?? transfer.sourceLocationId}</td><td>{bins.find(bin=>bin.id===transfer.destinationLocationId)?.code ?? transfer.destinationLocationId}</td><td>{transfer.quantity} {transfer.unitOfMeasure}</td><td>{transfer.occurredAt}</td><td>{transfer.actor}</td></tr>)}</tbody></table></div>}
            </section>
            <section>
                <h2>Lots and expiry</h2>
                {canCreate && <form className={styles.fields} onSubmit={createLot}>
                    <label className={styles.field}>Product ID<input name="productId" required /></label>
                    <label className={styles.field}>Lot number<input name="lotNumber" required /></label>
                    <label className={styles.field}>Manufactured on<input type="date" name="manufacturedOn" /></label>
                    <label className={styles.field}>Expires on<input type="date" name="expiresOn" /></label>
                    <label className={styles.field}>Supplier ID<input name="supplierId" /></label>
                    <button type="submit">Register lot</button>
                </form>}
                {canUpdate && lots.length > 0 && <form className={styles.fields} onSubmit={updateLotStatus}>
                    <h3>Quarantine or recall a lot</h3>
                    <label className={styles.field}>Lot<select name="lotId" required>{lots.map(lot => <option key={lot.id} value={lot.id}>{lot.productId} — {lot.lotNumber} ({lot.status})</option>)}</select></label>
                    <label className={styles.field}>Status<select name="status"><option value="ACTIVE">Active</option><option value="QUARANTINED">Quarantined</option><option value="RECALLED">Recalled</option></select></label>
                    <button type="submit">Update lot status</button>
                </form>}
                <div className={styles.tableContainer}><table><thead><tr><th>Product</th><th>Lot</th><th>Expiry</th><th>Status</th></tr></thead><tbody>{lots.map(lot => <tr key={lot.id}><td>{lot.productId}</td><td>{lot.lotNumber}</td><td>{lot.expiresOn || "No expiry"}</td><td>{lot.status}</td></tr>)}</tbody></table></div>
            </section>
            {canCreate && <section>
                <h2>Receive, reserve, transfer</h2>
                <div className={styles.fields}>
                    <form className={styles.fields} onSubmit={receive}><h3>Receipt</h3><label className={styles.field}>Product ID<input name="productId" required /></label><label className={styles.field}>Lot<select name="lotId"><option value="">Untracked</option>{lots.map(lot => <option key={lot.id} value={lot.id}>{lot.lotNumber} ({lot.status})</option>)}</select></label><label className={styles.field}>Quantity<input name="quantity" type="number" min="0.0001" step="any" required /></label><label className={styles.field}>Unit<input name="unitOfMeasure" defaultValue="EA" required /></label><label className={styles.field}>Unit cost<input name="unitCost" type="number" min="0" step="any" /></label><label className={styles.field}>Cost currency<input name="currency" defaultValue="BRL" maxLength={3} /></label><label className={styles.field}>Reference<input name="referenceId" /></label><button disabled={!selectedBin}>Record receipt at selected position</button></form>
                    {canUpdate && <form className={styles.fields} onSubmit={adjust}><h3>Count adjustment</h3><label className={styles.field}>Product ID<input name="productId" required /></label><label className={styles.field}>Lot<select name="lotId"><option value="">Untracked</option>{lots.map(lot => <option key={lot.id} value={lot.id}>{lot.lotNumber}</option>)}</select></label><label className={styles.field}>Direction<select name="type"><option value="ADJUSTMENT_IN">Increase</option><option value="ADJUSTMENT_OUT">Decrease</option></select></label><label className={styles.field}>Quantity<input name="quantity" type="number" min="0.0001" step="any" required /></label><label className={styles.field}>Unit<input name="unitOfMeasure" defaultValue="EA" required /></label><label className={styles.field}>Count reference<input name="referenceId" required /></label><button disabled={!selectedBin}>Record count adjustment</button></form>}
                    {canUpdate && <form className={styles.fields} onSubmit={reserve}><h3>Reservation (FEFO)</h3><label className={styles.field}>Product ID<input name="productId" required /></label><label className={styles.field}>Quantity<input name="quantity" type="number" min="0.0001" step="any" required /></label><label className={styles.field}>Unit<input name="unitOfMeasure" defaultValue="EA" required /></label><label className={styles.field}>Order reference<input name="referenceId" /></label><small>Allocates the earliest non-expired lots first.</small><button disabled={!selectedBin}>Reserve using FEFO</button></form>}
                    {canUpdate && <form className={styles.fields} onSubmit={transfer}><h3>Transfer</h3><label className={styles.field}>Product ID<input name="productId" required /></label><label className={styles.field}>Lot<select name="lotId"><option value="">Untracked</option>{lots.map(lot => <option key={lot.id} value={lot.id}>{lot.lotNumber}</option>)}</select></label><label className={styles.field}>From<select name="source" required>{bins.map(bin => <option key={bin.id} value={bin.id}>{bin.code} — {bin.name}</option>)}</select></label><label className={styles.field}>To<select name="destination" required>{bins.map(bin => <option key={bin.id} value={bin.id}>{bin.code} — {bin.name}</option>)}</select></label><label className={styles.field}>Quantity<input name="quantity" type="number" min="0.0001" step="any" required /></label><label className={styles.field}>Unit<input name="unitOfMeasure" defaultValue="EA" required /></label><label className={styles.field}>Reference<input name="referenceId" /></label><button disabled={bins.length < 2}>Transfer stock</button></form>}
                </div>
            </section>}
            {(canCreate || canUpdate) && <section>
                <h2>Shipping and international imports</h2>
                {canCreate && <form className={styles.fields} onSubmit={createShipment}><h3>New shipment</h3><label className={styles.field}>Reference<input name="reference" required /></label><label className={styles.field}>Origin<select name="origin" required>{bins.map(bin=><option key={bin.id} value={bin.id}>{bin.code} — {bin.name}</option>)}</select></label><label className={styles.field}>Destination<select name="destination" required>{bins.map(bin=><option key={bin.id} value={bin.id}>{bin.code} — {bin.name}</option>)}</select></label><label className={styles.field}>Carrier<input name="carrier" /></label><label className={styles.field}>Tracking number<input name="trackingNumber" /></label><label className={styles.field}>Recipient<input name="recipient" /></label><button disabled={bins.length < 2}>Create shipment</button></form>}
                <div className={styles.tableContainer}><table><thead><tr><th>Reference</th><th>Carrier</th><th>Tracking</th><th>Status</th><th>Origin → destination</th></tr></thead><tbody>{shipments.map(shipment=><tr key={shipment.id}><td>{shipment.reference}</td><td>{shipment.carrier}</td><td>{shipment.trackingNumber}</td><td>{shipment.status}</td><td>{bins.find(bin=>bin.id===shipment.originLocationId)?.code ?? shipment.originLocationId} → {bins.find(bin=>bin.id===shipment.destinationLocationId)?.code ?? shipment.destinationLocationId}</td></tr>)}</tbody></table></div>
                {canUpdate && shipments.length>0 && <><form className={styles.fields} onSubmit={addShipmentItem}><h3>Shipment contents</h3><label className={styles.field}>Shipment<select name="shipmentId" required>{shipments.map(shipment=><option key={shipment.id} value={shipment.id}>{shipment.reference} — {shipment.status}</option>)}</select></label><label className={styles.field}>Product ID<input name="productId" required /></label><label className={styles.field}>Lot<select name="lotId"><option value="">Untracked</option>{lots.map(lot=><option key={lot.id} value={lot.id}>{lot.lotNumber}</option>)}</select></label><label className={styles.field}>Serial number<input name="serialNumber" /></label><label className={styles.field}>Quantity<input name="quantity" type="number" min="0.0001" step="any" defaultValue="1" required /></label><label className={styles.field}>Unit<input name="unitOfMeasure" defaultValue="EA" required /></label><button>Add shipment item</button></form><form className={styles.fields} onSubmit={recordShipmentEvent}><h3>Shipment event</h3><label className={styles.field}>Shipment<select name="shipmentId" required>{shipments.map(shipment=><option key={shipment.id} value={shipment.id}>{shipment.reference} — {shipment.status}</option>)}</select></label><label className={styles.field}>Event<select name="type"><option value="PICKED">Picked</option><option value="DISPATCHED">Dispatched</option><option value="ROUTE_STOP">Route stop</option><option value="DELAYED">Delayed</option><option value="DELIVERED">Delivered</option><option value="PROOF_ATTACHED">Proof attached</option><option value="OCCURRENCE">Occurrence</option></select></label><label className={styles.field}>Location ID<input name="locationId" /></label><label className={styles.field}>Description<input name="description" required /></label><label className={styles.field}>GED evidence reference<input name="evidenceReference" /></label><button>Record event</button></form><div className={styles.fields}><label className={styles.field}>Inspect shipment contents<select onChange={event=>{void loadShipmentItems(event.target.value);}}><option value="">Choose shipment</option>{shipments.map(shipment=><option key={shipment.id} value={shipment.id}>{shipment.reference}</option>)}</select></label><div className={styles.tableContainer}><table><thead><tr><th>Product</th><th>Lot</th><th>Serial</th><th>Quantity</th><th>Unit</th></tr></thead><tbody>{shipmentItems.map(item=><tr key={item.id}><td>{item.productId}</td><td>{lots.find(lot=>lot.id===item.lotId)?.lotNumber ?? item.lotId}</td><td>{item.serialNumber}</td><td>{item.quantity}</td><td>{item.unit}</td></tr>)}</tbody></table></div></div></>}
                {canCreate && <form className={styles.fields} onSubmit={createImport}><h3>International import</h3><label className={styles.field}>Reference<input name="reference" required /></label><label className={styles.field}>Supplier ID<input name="supplierId" required /></label><label className={styles.field}>Supplier country<input name="supplierCountry" required /></label><label className={styles.field}>Purchase reference<input name="purchaseReference" /></label><label className={styles.field}>Currency<input name="currency" defaultValue="USD" required /></label><label className={styles.field}>Exchange rate<input name="exchangeRate" type="number" min="0.000001" step="any" required /></label><label className={styles.field}>Ordered on<input name="orderedOn" type="date" required /></label><label className={styles.field}>Estimated arrival<input name="estimatedArrival" type="date" /></label><button disabled={!selectedBin}>Create import at selected position</button></form>}
                <div className={styles.tableContainer}><table><thead><tr><th>Reference</th><th>Supplier</th><th>Origin</th><th>Currency / rate</th><th>Status</th><th>Destination</th></tr></thead><tbody>{imports.map(value=><tr key={value.id}><td>{value.reference}</td><td>{value.supplierId}</td><td>{value.supplierCountry}</td><td>{value.currency} / {value.exchangeRate}</td><td>{value.status}</td><td>{bins.find(bin=>bin.id===value.destinationLocationId)?.code ?? value.destinationLocationId}</td></tr>)}</tbody></table></div>
                {canUpdate && imports.length>0 && <><form className={styles.fields} onSubmit={updateImportStatus}><h3>Customs and receiving status</h3><label className={styles.field}>Import<select name="importId">{imports.map(value=><option key={value.id} value={value.id}>{value.reference} — {value.status}</option>)}</select></label><label className={styles.field}>Status<select name="status"><option value="IN_TRANSIT">In transit</option><option value="ARRIVED">Arrived</option><option value="CUSTOMS_HOLD">Customs hold</option><option value="CLEARED">Cleared</option><option value="RECEIVED">Received</option><option value="CANCELLED">Cancelled</option></select></label><label className={styles.field}>Arrived on<input name="arrivedOn" type="date" /></label><label className={styles.field}>Cleared on<input name="clearedOn" type="date" /></label><label className={styles.field}>Customs reference<input name="customsReference" /></label><button>Update status</button></form><form className={styles.fields} onSubmit={addImportExpense}><h3>Import expense</h3><label className={styles.field}>Import<select name="importId">{imports.map(value=><option key={value.id} value={value.id}>{value.reference}</option>)}</select></label><label className={styles.field}>Category<input name="category" required /></label><label className={styles.field}>Amount<input name="amount" type="number" min="0" step="any" required /></label><label className={styles.field}>Currency<input name="currency" defaultValue="USD" required /></label><label className={styles.field}>Exchange rate<input name="exchangeRate" type="number" min="0.000001" step="any" required /></label><label className={styles.field}>Base currency amount<input name="baseCurrencyAmount" type="number" min="0" step="any" required /></label><label className={styles.field}>Base currency<input name="baseCurrency" defaultValue="BRL" maxLength={3} required /></label><label className={styles.field}>Incurred on<input name="incurredOn" type="date" required /></label><label className={styles.field}>Document reference<input name="documentReference" /></label><button>Record expense</button></form><form className={styles.fields} onSubmit={addImportLine}><h3>Import item and customs value</h3><label className={styles.field}>Import<select name="importId" required>{imports.map(value=><option key={value.id} value={value.id}>{value.reference}</option>)}</select></label><label className={styles.field}>Product ID<input name="productId" required /></label><label className={styles.field}>Lot<select name="lotId"><option value="">Untracked</option>{lots.map(lot=><option key={lot.id} value={lot.id}>{lot.lotNumber}</option>)}</select></label><label className={styles.field}>Supplier line reference<input name="supplierLineReference" /></label><label className={styles.field}>Quantity<input name="quantity" type="number" min="0.0001" step="any" required /></label><label className={styles.field}>Unit<input name="unitOfMeasure" defaultValue="EA" required /></label><label className={styles.field}>Unit price<input name="unitPrice" type="number" min="0" step="any" required /></label><label className={styles.field}>Supplier currency<input name="currency" defaultValue="USD" maxLength={3} required /></label><label className={styles.field}>Exchange rate<input name="exchangeRate" type="number" min="0.000001" step="any" required /></label><label className={styles.field}>Base unit price<input name="baseUnitPrice" type="number" min="0" step="any" required /></label><label className={styles.field}>Base currency<input name="baseCurrency" defaultValue="BRL" maxLength={3} required /></label><button>Add import item</button></form><form className={styles.fields} onSubmit={receiveImportLine}><h3>Receive import item into inventory</h3><label className={styles.field}>Import line<select name="lineKey" required>{importLines.map(line=><option key={`${line.importId}|${line.id}`} value={`${line.importId}|${line.id}`}>{imports.find(value=>value.id===line.importId)?.reference ?? line.importId} — {line.supplierLineReference ?? line.productId}</option>)}</select></label><label className={styles.field}>Quantity received<input name="quantity" type="number" min="0.0001" step="any" required /></label><button disabled={!importLines.length}>Receive at landed cost</button></form><form className={styles.fields} onSubmit={addImportDocument}><h3>Import document reference</h3><label className={styles.field}>Import<select name="importId">{imports.map(value=><option key={value.id} value={value.id}>{value.reference}</option>)}</select></label><label className={styles.field}>Document type<select name="type"><option value="COMMERCIAL_INVOICE">Commercial invoice</option><option value="PACKING_LIST">Packing list</option><option value="BILL_OF_LADING">Bill of lading</option><option value="AIR_WAYBILL">Air waybill</option><option value="CERTIFICATE_OF_ORIGIN">Certificate of origin</option><option value="CUSTOMS_DECLARATION">Customs declaration</option><option value="OTHER">Other</option></select></label><label className={styles.field}>GED reference<input name="documentReference" required /></label><label className={styles.field}>File name<input name="fileName" required /></label><label className={styles.field}>Checksum<input name="checksum" /></label><button>Register document</button></form><div className={styles.fields}><label className={styles.field}>Inspect import records<select onChange={event=>{void loadImportDetails(event.target.value);}}><option value="">Choose import</option>{imports.map(value=><option key={value.id} value={value.id}>{value.reference}</option>)}</select></label><div className={styles.tableContainer}><table><thead><tr><th>From</th><th>To</th><th>When</th><th>Actor</th></tr></thead><tbody>{importHistory.map(row=><tr key={row.id}><td>{row.from || "Initial"}</td><td>{row.to}</td><td>{row.changedAt}</td><td>{row.actor}</td></tr>)}</tbody></table></div><div className={styles.tableContainer}><table><thead><tr><th>Expense</th><th>Amount</th><th>Currency</th><th>Base amount</th><th>Document</th></tr></thead><tbody>{importExpenses.map(item=><tr key={item.id}><td>{item.category}</td><td>{item.amount}</td><td>{item.currency}</td><td>{item.baseCurrencyAmount}</td><td>{item.documentReference}</td></tr>)}</tbody></table></div><div className={styles.tableContainer}><table><thead><tr><th>Import item</th><th>Quantity</th><th>Base goods value</th><th>Allocated expenses</th><th>Landed unit cost</th><th>Currency</th></tr></thead><tbody>{importLineCosts.map(cost=><tr key={cost.lineId}><td>{importLines.find(line=>line.id===cost.lineId)?.supplierLineReference ?? cost.lineId}</td><td>{importLines.find(line=>line.id===cost.lineId)?.quantity}</td><td>{cost.baseGoodsValue}</td><td>{cost.allocatedImportExpenses}</td><td>{cost.landedUnitCost}</td><td>{cost.baseCurrency}</td></tr>)}</tbody></table></div><div className={styles.tableContainer}><table><thead><tr><th>Document</th><th>Type</th><th>GED reference</th><th>File</th><th>Checksum</th></tr></thead><tbody>{importDocuments.map(doc=><tr key={doc.id}><td>{doc.id}</td><td>{doc.type}</td><td>{doc.documentReference}</td><td>{doc.fileName}</td><td>{doc.checksum}</td></tr>)}</tbody></table></div></div></>}
            </section>}
        </>}
    </main>;
}
