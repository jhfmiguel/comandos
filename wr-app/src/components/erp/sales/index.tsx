"use client";

import * as React from "react";
import axios from "axios";
import { Button } from "@primereact/ui/button";
import { Layout } from "components/layout";
import { Message } from "components/common/message";
import { useSession } from "components/auth/session-provider";
import { ReferenceField } from "../shared/record-workspace";
import { createErpService } from "api/services/erp.service";
import { inventorySalesService as sales } from "api/services/inventory-sales.service";
import type { ErpField, ErpValue } from "api/models/erp";
import type { InventorySale, SaleRequest, SaleReturnRequest, SalesPage, StockOption } from "api/models/erp/sales";
import styles from "../shared/workspace.module.css";
import { QuantityInput } from "components/erp/shared/quantity-input";

const core = createErpService("core");
const inventory = createErpService("inventory");
const reference = (name: string, label: string, resource: string): ErpField =>
    ({ name, label, type: "reference", required: true, reference: resource, choices: [] });
const organizationField = reference("organizationId", "Organization", "organizations");
const unitField = { ...reference("unitId", "Unit", "units"), required: false };
const buyerField = reference("buyerId", "Buyer", "people");
const returnReasonField = reference("reasonId", "Return reason", "sale-return-reason-types");
const paymentMethods = ["CASH", "PIX", "CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER"];
type CartLine = StockOption & { quantity: string };
const keyOf = (item: StockOption) => `${item.kind}:${item.stockId}`;
const failure = (error: unknown) => axios.isAxiosError(error) && typeof error.response?.data?.detail === "string"
    ? error.response.data.detail : "Unable to complete the request. Check the API connection and try again.";

// Match backend decimal arithmetic: four decimal places, HALF_UP per line.
function units(value: string): bigint {
    if (!/^\d{1,15}(\.\d{0,4})?$/.test(value)) return 0n;
    const [whole, fraction = ""] = value.split(".");
    return BigInt(whole) * 10000n + BigInt(fraction.padEnd(4, "0"));
}
function amount(value: bigint): string { return `${value / 10000n}.${(value % 10000n).toString().padStart(4, "0")}`; }
function subtotal(item: CartLine): bigint { return (units(item.quantity) * units(item.unitPrice) + 5000n) / 10000n; }

export function InventorySalesWorkspace() {
    const [generation, setGeneration] = React.useState(0);
    return <Layout title="Inventory sales"><SalesForm key={generation} onNewSale={() => setGeneration(value => value + 1)} /></Layout>;
}

function SalesForm({ onNewSale }: { onNewSale: () => void }) {
    const { can } = useSession();
    const [organization, setOrganization] = React.useState<ErpValue>(null);
    const [unit, setUnit] = React.useState<ErpValue>(null);
    const [buyer, setBuyer] = React.useState<ErpValue>(null);
    const [payment, setPayment] = React.useState("");
    const [cart, setCart] = React.useState<CartLine[]>([]);
    const [busy, setBusy] = React.useState(false);
    const [pending, setPending] = React.useState<SaleRequest | null>(null);
    const [completed, setCompleted] = React.useState<InventorySale | null>(null);
    const [error, setError] = React.useState("");
    const [refresh, setRefresh] = React.useState(0);
    const submitting = React.useRef(false);
    const organizationId = organization ? Number(organization) : undefined;
    const unitId = unit ? Number(unit) : undefined;
    const canCreate = can("sales", "CREATE", organizationId, unitId);
    const canRead = can("sales", "READ", organizationId, unitId);
    const scopeKey = `${organizationId}:${unitId}`;
    const locked = busy || !!pending || !!completed;

    async function finalize(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (submitting.current || completed || !canCreate) return;
        if (!cart.length) { setError("Add at least one stock item."); return; }
        if (cart.some(item => units(item.quantity) <= 0n || units(item.quantity) > units(item.available))) {
            setError("Each quantity must be positive and no greater than the available stock."); return;
        }
        const request = pending ?? {
            requestId: crypto.randomUUID(), organizationId: Number(organization), buyerId: Number(buyer), paymentMethod: payment,
            unitId,
            items: cart.map(item => ({ ...(item.kind === "ASSET" ? { assetId: item.stockId } : { balanceId: item.stockId }),
                quantity: item.quantity, expectedUnitPrice: item.unitPrice }))
        };
        submitting.current = true;
        setBusy(true); setError(""); setPending(request);
        try { setCompleted(await sales.finalize(request)); setPending(null); setRefresh(value => value + 1); }
        catch (error) {
            const status = axios.isAxiosError(error) ? error.response?.status : undefined;
            if (!pending && status && status >= 400 && status < 500) setPending(null);
            setError(failure(error));
        } finally { submitting.current = false; setBusy(false); }
    }

    return <div className={styles.workspace}>
        <p className={styles.intro}>Sell available assets or stock lots. Prices and stock are checked when you finalize.</p>
        {error && <Message type="error" text={error} onClose={() => setError("")} />}
        {pending && !busy && <Message type="warn" text="The result could not be confirmed. Retry this sale to recover its result without a second stock deduction." />}
        {completed && <Message type="success" text={`Sale #${completed.id} finalized successfully.`} />}
        <form data-comandos-erp-form="true" onSubmit={finalize}>
            <fieldset className={styles.fields} disabled={locked}>
                <div className={styles.field}>
                    <label htmlFor="core-organizationId">Organization *</label>
                    <ReferenceField service={core} field={organizationField} value={organization} organizationId={null}
                        onChange={value => { setOrganization(value); setUnit(null); setCart([]); }} />
                </div>
                <div className={styles.field}>
                    <label htmlFor="core-unitId">Unit</label>
                    <ReferenceField key={String(organization)} service={core} field={unitField} value={unit} organizationId={organization}
                        onChange={value => { setUnit(value); setCart([]); }} />
                    <small>Select a unit to restrict stock and history. Leaving it empty requires organization-wide sales access.</small>
                </div>
                <div className={styles.field}>
                    <label htmlFor="core-buyerId">Buyer *</label>
                    <ReferenceField service={core} field={buyerField} value={buyer} organizationId={null} onChange={setBuyer} />
                </div>
                <div className={styles.field}>
                    <label htmlFor="sale-payment">Payment method *</label>
                    <select id="sale-payment" required value={payment} onChange={event => setPayment(event.target.value)}>
                        <option value="">Select a payment method</option>
                        {paymentMethods.map(method => <option key={method} value={method}>{method.replaceAll("_", " ")}</option>)}
                    </select>
                </div>
            </fieldset>
            {!!organization && !completed && canCreate && canRead && <fieldset disabled={locked} className={styles.field}>
                <StockPicker key={scopeKey} organizationId={Number(organization)} unitId={unitId} cart={cart} onAdd={item => {
                    if (!cart.some(line => keyOf(line) === keyOf(item))) setCart([...cart, { ...item, quantity: "1" }]);
                }} />
            </fieldset>}
            {!completed && <div className={styles.tableContainer}>
                <table><caption>Sale items</caption><thead><tr><th>Item / code</th><th>Location</th><th>Quantity</th><th>Unit price</th><th>Subtotal</th><th>Actions</th></tr></thead>
                    <tbody>{cart.map(item => <tr key={keyOf(item)}>
                        <td>{item.modelName}<br />{item.code}</td><td>{item.locationName}</td>
                        <td><div className={styles.field}><QuantityInput code={item.code} max={item.available} value={item.quantity}
                            disabled={locked} individual={item.kind === "ASSET"}
                            onChange={quantity => setCart(cart.map(line => keyOf(line) === keyOf(item) ? { ...line, quantity } : line))} /></div>{item.unitOfMeasure}</td>
                        <td>{item.unitPrice}</td><td>{amount(subtotal(item))}</td>
                        <td><Button type="button" severity="secondary" disabled={locked} aria-label={`Remove ${item.code}`}
                            onClick={() => setCart(cart.filter(line => keyOf(line) !== keyOf(item)))}>Remove</Button></td>
                    </tr>)}{!cart.length && <tr><td colSpan={6}>Select an organization and add stock items.</td></tr>}</tbody>
                </table>
                <p className={styles.pagination}>Total: {amount(cart.reduce((total, item) => total + subtotal(item), 0n))}</p>
            </div>}
            {completed && <SaleReceipt sale={completed} />}
            <div className={styles.actions}>
                {completed ? <Button type="button" className="registration-yellow-button" onClick={onNewSale}>+ Sale</Button>
                    : <Button type="submit" className="registration-yellow-button" disabled={busy || !cart.length || !canCreate}>{busy ? "Finalizing…" : pending ? "Retry finalization" : "Finalize"}</Button>}
            </div>
        </form>
        {!canCreate && <Message type="info" text="Select an organization and, if required by your profile, an authorized unit to finalize sales." />}
        {!!organization && canRead && <SaleHistory key={scopeKey} organizationId={Number(organization)} unitId={unitId} refresh={refresh} />}
    </div>;
}

function StockPicker({ organizationId, unitId, cart, onAdd }: { organizationId: number; unitId?: number; cart: CartLine[]; onAdd: (item: StockOption) => void }) {
    const [kind, setKind] = React.useState("ASSET");
    const [search, setSearch] = React.useState("");
    const [page, setPage] = React.useState(0);
    const [result, setResult] = React.useState<SalesPage<StockOption> | null>(null);
    const [error, setError] = React.useState("");
    const [retry, setRetry] = React.useState(0);
    React.useEffect(() => {
        const controller = new AbortController();
        const timer = setTimeout(() => sales.stock(organizationId, kind, search, page, controller.signal, unitId)
            .then(data => { if (!controller.signal.aborted) { setResult(data); setError(""); } })
            .catch(error => { if (!controller.signal.aborted) setError(failure(error)); }), 250);
        return () => { clearTimeout(timer); controller.abort(); };
    }, [organizationId, unitId, kind, search, page, retry]);
    function reset() { setPage(0); setResult(null); setError(""); }
    return <section>
        <div className={styles.toolbar}>
            <h2>Available stock</h2>
            <div className={styles.field}><label htmlFor="stock-kind">Stock type</label><select id="stock-kind" value={kind}
                onChange={event => { setKind(event.target.value); reset(); }}><option value="ASSET">Individual assets</option><option value="LOT">Stock lots</option></select></div>
            <div className={styles.field}><label htmlFor="stock-search">Code, SKU or model</label><input id="stock-search" type="search" value={search}
                onChange={event => { setSearch(event.target.value); reset(); }} /></div>
            <Button type="button" severity="secondary" onClick={() => { reset(); setRetry(value => value + 1); }}>Refresh stock</Button>
        </div>
        {error && <Message type="error" text={error} />}
        {!result && !error && <p role="status">Loading stock…</p>}
        {result && <><div className={styles.tableContainer}><table><thead><tr><th>Code / model</th><th>Location</th><th>Available</th><th>Unit price</th><th>Actions</th></tr></thead>
            <tbody>{result.content.map(item => <tr key={keyOf(item)}><td>{item.code}<br />{item.modelName} · {item.sku}</td><td>{item.locationName}</td>
                <td>{item.available} {item.unitOfMeasure}</td><td>{item.unitPrice}</td><td><Button type="button" disabled={cart.length >= 100 || cart.some(line => keyOf(line) === keyOf(item))}
                    onClick={() => onAdd(item)}>Add</Button></td></tr>)}{!result.content.length && <tr><td colSpan={5}>No available stock matches your search.</td></tr>}</tbody></table></div>
            <Pagination page={page} result={result} onPage={value => { setResult(null); setPage(value); }} /></>}
    </section>;
}

function SaleReceipt({ sale }: { sale: InventorySale }) {
    return <div className={styles.tableContainer}>
        <p>Sale #{sale.id} · {sale.status} · {sale.finalizedAt.replace("T", " ")}</p>
        <p>Finalized by: {sale.finalizedByLogin || "Not recorded"}</p>
        <p>Unit: {sale.unitName || "Organization-wide sale"}</p>
        <p>{sale.organizationName} · Buyer: {sale.buyerName} · {sale.paymentMethod.replaceAll("_", " ")}</p>
        <table><thead><tr><th>Item / code</th><th>Location</th><th>Quantity</th><th>Unit price</th><th>Subtotal</th></tr></thead>
            <tbody>{sale.items.map(item => <tr key={item.id}><td>{item.modelName}<br />{item.stockCode}</td><td>{item.locationName}</td><td>{item.quantity} {item.unitOfMeasure}</td>
                <td>{item.unitPrice}</td><td>{item.subtotal}</td></tr>)}</tbody></table>
        <p className={styles.pagination}>Total: {sale.total}</p>
        {!!sale.returns?.length && <div><h3>Returns and cancellations</h3>{sale.returns.map(entry => <p key={entry.id}>#{entry.id} · {entry.cancellation ? "Cancellation" : "Return"} · {entry.reasonName} · Refund {entry.refundAmount}</p>)}</div>}
    </div>;
}

function SaleHistory({ organizationId, unitId, refresh }: { organizationId: number; unitId?: number; refresh: number }) {
    const [page, setPage] = React.useState(0);
    const [result, setResult] = React.useState<SalesPage<InventorySale> | null>(null);
    const [error, setError] = React.useState("");
    const [retry, setRetry] = React.useState(0);
    React.useEffect(() => {
        const controller = new AbortController();
        sales.list(organizationId, page, controller.signal, unitId).then(data => { if (!controller.signal.aborted) { setResult(data); setError(""); } })
            .catch(error => { if (!controller.signal.aborted) setError(failure(error)); });
        return () => controller.abort();
    }, [organizationId, unitId, page, refresh, retry]);
    return <section className="mt-8"><div className={styles.toolbar}><h2>Sales history</h2><Button type="button" severity="secondary" onClick={() => setRetry(value => value + 1)}>Refresh history</Button></div>
        {error && <Message type="error" text={error} />}
        {!result && !error && <p role="status">Loading sales…</p>}
        {result?.content.map(sale => <details key={sale.id}><summary>Sale #{sale.id} · {sale.buyerName} · {sale.total}</summary><SaleReceipt sale={sale} /><SaleReturnForm sale={sale} onDone={() => setRetry(value => value + 1)} /></details>)}
        {result && !result.content.length && <p>No sales recorded for the selected scope.</p>}
        {result && <Pagination page={page} result={result} onPage={value => { setResult(null); setPage(value); }} />}
    </section>;
}

function SaleReturnForm({ sale, onDone }: { sale: InventorySale; onDone: () => void }) {
    const { can } = useSession();
    const [reason, setReason] = React.useState<ErpValue>(null);
    const [notes, setNotes] = React.useState("");
    const [refundReference, setRefundReference] = React.useState("");
    const [selected, setSelected] = React.useState<Record<number, string>>({});
    const [busy, setBusy] = React.useState(false);
    const [error, setError] = React.useState("");
    const [pending, setPending] = React.useState<SaleReturnRequest | null>(null);
    const submitting = React.useRef(false);
    const locked = busy || !!pending;
    const reasonField = { ...returnReasonField, name: `sale-return-reason-${sale.id}` };
    const returned = new Map<number, bigint>();
    sale.returns?.forEach(operation => operation.items.forEach(item => returned.set(item.saleItemId, (returned.get(item.saleItemId) ?? 0n) + units(item.quantity))));
    const remaining = sale.items.map(item => ({ item, units: units(item.quantity) - (returned.get(item.id) ?? 0n) })).filter(entry => entry.units > 0n);
    const canReturn = can("sales", "RETURN", sale.organizationId, sale.unitId ?? undefined);
    const canCancel = can("sales", "CANCEL", sale.organizationId, sale.unitId ?? undefined);
    async function send(cancellation: boolean) {
        if (submitting.current || (cancellation ? !canCancel : !canReturn) || pending && pending.cancellation !== cancellation) return;
        let request = pending;
        if (!request) {
            if (!reason || !notes.trim()) { setError("Return reason and notes are required."); return; }
            const items = cancellation ? remaining.map(entry => ({ saleItemId: entry.item.id, quantity: amount(entry.units) }))
                : remaining.filter(entry => units(selected[entry.item.id] ?? "0") > 0n).map(entry => ({ saleItemId: entry.item.id, quantity: selected[entry.item.id] }));
            if (!items.length) { setError("Select at least one quantity to return."); return; }
            request = { requestId: crypto.randomUUID(), reasonId: Number(reason), notes,
                refundReference: refundReference || null, cancellation, items };
        }
        submitting.current = true;
        setBusy(true); setError(""); setPending(request);
        try {
            await sales.returnItems(sale.id, request);
            setPending(null); setSelected({}); onDone();
        } catch (error) {
            const status = axios.isAxiosError(error) ? error.response?.status : undefined;
            // A rejected retry does not prove that the original uncertain operation failed.
            if (!pending && status && status >= 400 && status < 500) setPending(null);
            setError(failure(error));
        } finally { submitting.current = false; setBusy(false); }
    }
    if (!remaining.length && !pending) return <p>All sale items have been returned.</p>;
    if (!canReturn && !canCancel) return null;
    return <section>
        <h3>Return or cancel sale</h3>
        {error && <Message type="error" text={error} onClose={() => setError("")} />}
        {pending && !busy && <Message type="warn" text="The result could not be confirmed. Retry this operation to recover it without returning stock twice." />}
        <fieldset className={styles.fields} disabled={locked}>
            <div className={styles.field}>
                <label htmlFor={`core-${reasonField.name}`}>Return reason *</label>
                <ReferenceField service={inventory} field={reasonField} value={reason} organizationId={null} onChange={setReason} />
            </div>
            <div className={styles.field}>
                <label htmlFor={`return-notes-${sale.id}`}>Notes *</label>
                <input id={`return-notes-${sale.id}`} value={notes} onChange={event => setNotes(event.target.value)} />
            </div>
            <div className={styles.field}>
                <label htmlFor={`refund-reference-${sale.id}`}>Refund reference</label>
                <input id={`refund-reference-${sale.id}`} value={refundReference} onChange={event => setRefundReference(event.target.value)} />
            </div>
        </fieldset>
        <div className={styles.tableContainer}><table>
            <thead><tr><th>Item</th><th>Remaining</th><th>Return quantity</th></tr></thead>
            <tbody>{remaining.map(({ item, units: available }) => <tr key={item.id}>
                <td>{item.stockCode} · {item.modelName}</td><td>{amount(available)} {item.unitOfMeasure}</td>
                <td><input type="number" min="0" max={amount(available)} step={item.assetId ? 1 : ".0001"}
                    aria-label={`Return quantity for ${item.stockCode}`} disabled={locked || !canReturn}
                    value={selected[item.id] ?? "0"} onChange={event => setSelected({ ...selected, [item.id]: event.target.value })} /></td>
            </tr>)}</tbody>
        </table></div>
        <div className={styles.actions}>
            {canReturn && <Button type="button" disabled={busy || !!pending?.cancellation} onClick={() => void send(false)}>
                {pending && !pending.cancellation ? "Retry return" : "Return selected items"}
            </Button>}
            {canCancel && <Button type="button" className="registration-yellow-button" disabled={busy || !!pending && !pending.cancellation}
                onClick={() => void send(true)}>{pending?.cancellation ? "Retry cancellation" : "Cancel remaining sale"}</Button>}
        </div>
    </section>;
}

function Pagination({ page, result, onPage }: { page: number; result: { totalElements: number; size: number }; onPage: (page: number) => void }) {
    return <div className={styles.pagination}><span>{result.totalElements} records · Page {page + 1}</span>
        <Button type="button" severity="secondary" disabled={page === 0} onClick={() => onPage(page - 1)}>Previous</Button>
        <Button type="button" severity="secondary" disabled={(page + 1) * result.size >= result.totalElements} onClick={() => onPage(page + 1)}>Next</Button></div>;
}
