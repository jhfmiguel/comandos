"use client";

import * as React from "react";
import axios from "axios";
import { Button } from "components/common/button";
import { ComandosSelectField } from "components/common/select-field";
import { Layout } from "components/layout";
import { Message } from "components/common/message";
import { useSession } from "components/auth/session-provider";
import { ReferenceField } from "components/erp/shared/record-workspace";
import { createErpService } from "api/services/erp.service";
import { inventorySalesService } from "api/services/inventory-sales.service";
import { commercialSalesService } from "api/services/commercial-sales.service";
import type { ErpField, ErpValue } from "api/models/erp";
import type { StockOption } from "api/models/erp/sales";
import type { CommercialSalesDocument } from "api/models/erp/commercial-sales";
import styles from "components/erp/shared/workspace.module.css";
import { formatBRLValue } from "utils/money";

const core = createErpService("core");
const reference = (name: string, label: string, resource: string): ErpField =>
    ({ name, label, type: "reference", required: true, reference: resource, choices: [] });
const organizationField = reference("organizationId", "Organization", "organizations");
const unitField = { ...reference("unitId", "Unit", "units"), required: false };
const buyerField = reference("buyerId", "Buyer", "people");
type CartLine = StockOption & { quantity: string };
const lineKey = (item: StockOption) => `${item.kind}:${item.stockId}`;
const paymentMethods = ["CASH", "PIX", "CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER"];
const failure = (error: unknown) => axios.isAxiosError(error) && typeof error.response?.data?.detail === "string"
    ? error.response.data.detail : "Unable to complete the request. Check access and the API connection.";

export function CommercialSalesOrdersPage() {
    return <Layout title="Commercial sales orders"><CommercialSalesOrders /></Layout>;
}

function CommercialSalesOrders() {
    const { can } = useSession();
    const [organization, setOrganization] = React.useState<ErpValue>(null);
    const [unit, setUnit] = React.useState<ErpValue>(null);
    const [buyer, setBuyer] = React.useState<ErpValue>(null);
    const [payment, setPayment] = React.useState("PIX");
    const [processNumber, setProcessNumber] = React.useState("");
    const [legalBasis, setLegalBasis] = React.useState("");
    const [documentReference, setDocumentReference] = React.useState("");
    const [cart, setCart] = React.useState<CartLine[]>([]);
    const [orders, setOrders] = React.useState<CommercialSalesDocument[]>([]);
    const [selected, setSelected] = React.useState<CommercialSalesDocument | null>(null);
    const [busy, setBusy] = React.useState(false);
    const [error, setError] = React.useState("");
    const [createReference, setCreateReference] = React.useState(() => crypto.randomUUID());
    const [deliveryReference, setDeliveryReference] = React.useState(() => crypto.randomUUID());
    const orgId = organization ? Number(organization) : undefined;
    const unitId = unit ? Number(unit) : undefined;
    const canRead = can("sales", "READ", orgId, unitId);
    const canCreate = can("sales", "CREATE", orgId, unitId);
    const canUpdate = can("sales", "UPDATE", orgId, unitId);
    const canApprove = can("sales", "APPROVE", orgId, unitId);
    const canConfirm = can("sales", "CONFIRM", orgId, unitId);
    const canInvoice = can("sales", "INVOICE", orgId, unitId);

    const refresh = React.useCallback(async () => {
        if (!orgId || !canRead) { setOrders([]); return; }
        try { setOrders(await commercialSalesService.list(orgId, unitId)); setError(""); }
        catch (failureReason) { setError(failure(failureReason)); }
    }, [orgId, unitId, canRead]);
    React.useEffect(() => {
        if (!orgId || !canRead) return;
        let active = true;
        commercialSalesService.list(orgId, unitId).then(result => {
            if (active) { setOrders(result); setError(""); }
        }).catch(reason => { if (active) setError(failure(reason)); });
        return () => { active = false; };
    }, [orgId, unitId, canRead]);

    async function create(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!orgId || !buyer || !cart.length || !canCreate || busy) return;
        setBusy(true); setError("");
        try {
            const order = await commercialSalesService.create({
                reference: createReference,
                customerId: String(buyer),
                organizationId: orgId,
                unitId,
                buyerId: Number(buyer),
                paymentMethod: payment,
                processNumber,
                legalBasis,
                documentReference: documentReference || createReference,
                lines: cart.map(item => ({
                    itemId: `${item.kind === "ASSET" ? "ASSET" : "BALANCE"}:${item.stockId}`,
                    description: `${item.modelName} (${item.code})`,
                    quantity: item.quantity,
                    unitPrice: item.unitPrice
                }))
            });
            setSelected(order); setCart([]); setCreateReference(crypto.randomUUID()); await refresh();
        } catch (reason) { setError(failure(reason)); }
        finally { setBusy(false); }
    }

    async function act(action: () => Promise<unknown>, reload = false) {
        if (!selected || busy) return;
        setBusy(true); setError("");
        try {
            await action();
            setSelected(await commercialSalesService.get(selected.id));
            if (reload) await refresh();
            if (selected.status === "INVOICED" || selected.status === "PARTIALLY_DELIVERED")
                setDeliveryReference(crypto.randomUUID());
        } catch (reason) { setError(failure(reason)); }
        finally { setBusy(false); }
    }

    const outstanding = selected?.lines ?? [];
    return <div className={styles.workspace}>
        <p className={styles.intro}>Build a scoped sales order from available stock, then submit it for approval, reserve stock, invoice and record delivery.</p>
        <p><a href="/erp/sales">Open the specialized inventory sale screen</a></p>
        {error && <Message type="error" text={error} onClose={() => setError("")} />}
        <form className={styles.fields} onSubmit={create}>
            <div className={styles.field}><label>Organization *</label>
                <ReferenceField service={core} field={organizationField} value={organization} organizationId={null}
                    onChange={value => { setOrganization(value); setUnit(null); setCart([]); setSelected(null); }} />
            </div>
            <div className={styles.field}><label>Unit</label>
                <ReferenceField key={String(organization)} service={core} field={unitField} value={unit} organizationId={organization}
                    onChange={value => { setUnit(value); setCart([]); setSelected(null); }} />
            </div>
            <div className={styles.field}><label>Buyer *</label>
                <ReferenceField service={core} field={buyerField} value={buyer} organizationId={null} onChange={setBuyer} />
            </div>
            <div className={styles.field}><label htmlFor="commercial-process">Process number *</label>
                <input id="commercial-process" required value={processNumber} onChange={event => setProcessNumber(event.target.value)} />
            </div>
            <div className={styles.field}><label htmlFor="commercial-legal">Legal basis *</label>
                <input id="commercial-legal" required value={legalBasis} onChange={event => setLegalBasis(event.target.value)} />
            </div>
            <div className={styles.field}><label htmlFor="commercial-reference">Document reference</label>
                <input id="commercial-reference" value={documentReference} onChange={event => setDocumentReference(event.target.value)} />
            </div>
            <div className={styles.field}><ComandosSelectField id="commercial-payment" label="Payment method" required value={payment}
                options={paymentMethods.map(value => ({ value, label: value.replaceAll("_", " ") }))} onChange={setPayment} /></div>
            {!!orgId && canRead && canCreate && !selected && <div className={styles.field}>
                <CommercialStockPicker organizationId={orgId} unitId={unitId} cart={cart} onAdd={item => {
                    if (!cart.some(line => lineKey(line) === lineKey(item))) setCart(current => [...current, { ...item, quantity: "1" }]);
                }} />
            </div>}
            {!!cart.length && !selected && <div className={styles.tableContainer}><table><caption>Order items</caption><thead><tr><th>Item</th><th>Location</th><th>Qty</th><th>Unit price</th><th>Action</th></tr></thead>
                <tbody>{cart.map(item => <tr key={lineKey(item)}><td>{item.modelName}<br />{item.code}</td><td>{item.locationName}</td>
                    <td><input aria-label={`Quantity ${item.code}`} type="number" min="0.0001" max={item.available} step="0.0001" value={item.quantity}
                        disabled={item.kind === "ASSET"} onChange={event => setCart(current => current.map(line => lineKey(line) === lineKey(item)
                            ? { ...line, quantity: event.target.value } : line))} /></td>
                    <td>{formatBRLValue(item.unitPrice)}</td><td><Button type="button" severity="secondary"
                        onClick={() => setCart(current => current.filter(line => lineKey(line) !== lineKey(item)))}>Remove</Button></td></tr>)}</tbody></table></div>}
            {!selected && <Button type="submit" disabled={busy || !canCreate || !cart.length}>Create draft order</Button>}
        </form>
        {!canCreate && <Message type="info" text="Select an organization and unit covered by your sales permission." />}
        {selected && <section aria-label="Selected sales order" className={styles.tableContainer}>
            <h2>Order {selected.reference} — {selected.status}</h2>
            <p>Total: {formatBRLValue(String(selected.lines.reduce((sum, line) => sum + Number(line.quantity) * Number(line.unitPrice), 0)))}</p>
            <div className={styles.actions}>
                {selected.status === "DRAFT" && canUpdate && <Button disabled={busy} onClick={() => void act(() => commercialSalesService.submit(selected.id), true)}>Submit for approval</Button>}
                {selected.status === "SUBMITTED" && canApprove && <><p>Approval must be completed by a different user than the requester.</p><Button disabled={busy} onClick={() => void act(() => commercialSalesService.approve(selected.id), true)}>Approve</Button></>}
                {selected.status === "APPROVED" && canConfirm && <Button disabled={busy} onClick={() => void act(() => commercialSalesService.confirm(selected.id), true)}>Confirm and reserve stock</Button>}
                {selected.status === "CONFIRMED" && canInvoice && <Button disabled={busy} onClick={() => void act(() => commercialSalesService.invoice(selected.id,
                    new Date().toISOString().slice(0, 10), new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10)), true)}>Invoice</Button>}
                {(selected.status === "INVOICED" || selected.status === "PARTIALLY_DELIVERED") && canUpdate && <Button disabled={busy} onClick={() => void act(() =>
                    commercialSalesService.deliver(selected.id, deliveryReference, outstanding.map(line => ({ itemId: line.itemId, quantity: line.quantity }))), true)}>Record delivery</Button>}
                {["DRAFT", "SUBMITTED", "APPROVED", "CONFIRMED"].includes(selected.status) && canUpdate && <Button severity="secondary" disabled={busy}
                    onClick={() => void act(() => commercialSalesService.cancel(selected.id, "Cancelled by authorized COMANDOS user"), true)}>Cancel order</Button>}
                <Button severity="secondary" disabled={busy} onClick={() => { setSelected(null); void refresh(); }}>Close order</Button>
            </div>
            <table><caption>Order lines</caption><thead><tr><th>Item</th><th>Quantity</th><th>Unit price</th></tr></thead>
                <tbody>{selected.lines.map(line => <tr key={line.itemId}><td>{line.description}</td><td>{line.quantity}</td><td>{formatBRLValue(line.unitPrice)}</td></tr>)}</tbody></table>
        </section>}
        {!!orgId && canRead && !selected && <section className={styles.tableContainer}>
            <h2>Orders for selected scope</h2>
            <table><thead><tr><th>Reference</th><th>Status</th><th>Amount</th><th>Action</th></tr></thead><tbody>
                {orders.map(order => <tr key={order.id}><td>{order.reference}</td><td>{order.status}</td>
                    <td>{formatBRLValue(String(order.lines.reduce((sum, line) => sum + Number(line.quantity) * Number(line.unitPrice), 0)))}</td>
                    <td><Button severity="secondary" onClick={() => setSelected(order)}>Open</Button></td></tr>)}
                {!orders.length && <tr><td colSpan={4}>No orders in this scope.</td></tr>}
            </tbody></table>
        </section>}
    </div>;
}

function CommercialStockPicker({ organizationId, unitId, cart, onAdd }: {
    organizationId: number; unitId?: number; cart: CartLine[]; onAdd: (stock: StockOption) => void;
}) {
    const [kind, setKind] = React.useState("ASSET");
    const [search, setSearch] = React.useState("");
    const [stock, setStock] = React.useState<StockOption[]>([]);
    const [error, setError] = React.useState("");
    React.useEffect(() => {
        const controller = new AbortController();
        const timer = setTimeout(() => void inventorySalesService.stock(organizationId, kind, search, 0, controller.signal, unitId)
            .then(result => { setStock(result.content); setError(""); })
            .catch(reason => { if (!controller.signal.aborted) setError(failure(reason)); }), 250);
        return () => { clearTimeout(timer); controller.abort(); };
    }, [organizationId, unitId, kind, search]);
    return <div><strong>Select stock to include</strong>
        <div className={styles.actions}>
            <ComandosSelectField id="commercial-stock-kind" label="Stock type" value={kind}
                options={[{ value: "ASSET", label: "Individual assets" }, { value: "LOT", label: "Stock lots" }]}
                onChange={setKind} />
            <label htmlFor="commercial-stock-search">Search</label>
            <input id="commercial-stock-search" value={search} onChange={event => setSearch(event.target.value)} />
        </div>
        {error && <Message type="error" text={error} onClose={() => setError("")} />}
        <table><thead><tr><th>Item</th><th>Available</th><th>Price</th><th>Action</th></tr></thead><tbody>
            {stock.map(item => <tr key={lineKey(item)}><td>{item.modelName} ({item.code})</td><td>{item.available}</td>
                <td>{formatBRLValue(item.unitPrice)}</td><td><Button type="button" severity="secondary"
                    disabled={cart.some(line => lineKey(line) === lineKey(item))} onClick={() => onAdd(item)}>Add</Button></td></tr>)}
            {!stock.length && <tr><td colSpan={4}>No available stock matched this search.</td></tr>}
        </tbody></table>
    </div>;
}
