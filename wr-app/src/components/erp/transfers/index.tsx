"use client";

import * as React from "react";
import axios from "axios";
import { Button } from "@primereact/ui/button";
import { Layout } from "components/layout";
import { Message } from "components/common/message";
import { useSession } from "components/auth/session-provider";
import { ReferenceField } from "components/erp/shared/record-workspace";
import { createErpService } from "api/services/erp.service";
import { transferService as api } from "api/services/transfer.service";
import type { ErpField, ErpValue } from "api/models/erp";
import type { InventoryTransfer, TransferPage, TransferRequest, TransferStockOption } from "api/models/erp/transfer";
import styles from "components/erp/shared/workspace.module.css";

const core = createErpService("core");
const inventory = createErpService("inventory");
const reference = (name: string, label: string, resource: string): ErpField =>
    ({ name, label, type: "reference", required: true, reference: resource, choices: [] });
const errorText = (error: unknown) => axios.isAxiosError(error) && typeof error.response?.data?.detail === "string"
    ? error.response.data.detail : "Unable to complete the request. Check the API connection and try again.";
type Selected = TransferStockOption & { quantity: string };

export function TransferWorkspace() {
    const [generation, setGeneration] = React.useState(0);
    return <Layout title="Inventory transfers">
        <TransferForm key={generation} onNew={() => setGeneration(value => value + 1)} />
    </Layout>;
}

function TransferForm({ onNew }: { onNew: () => void }) {
    const { can } = useSession();
    const [organization, setOrganization] = React.useState<ErpValue>(null);
    const [sourceUnit, setSourceUnit] = React.useState<ErpValue>(null);
    const [destinationUnit, setDestinationUnit] = React.useState<ErpValue>(null);
    const [destinationLocation, setDestinationLocation] = React.useState<ErpValue>(null);
    const [purpose, setPurpose] = React.useState("");
    const [selected, setSelected] = React.useState<Selected[]>([]);
    const [pending, setPending] = React.useState<TransferRequest | null>(null);
    const [completed, setCompleted] = React.useState<InventoryTransfer | null>(null);
    const [busy, setBusy] = React.useState(false);
    const [error, setError] = React.useState("");
    const [refresh, setRefresh] = React.useState(0);
    const submitting = React.useRef(false);
    const organizationId = organization ? Number(organization) : undefined;
    const sourceUnitId = sourceUnit ? Number(sourceUnit) : undefined;
    const destinationUnitId = destinationUnit ? Number(destinationUnit) : undefined;
    const canRead = !!organizationId && !!sourceUnitId && can("transfers", "READ", organizationId, sourceUnitId);
    const canCreate = !!organizationId && !!sourceUnitId && !!destinationUnitId
        && can("transfers", "CREATE", organizationId, sourceUnitId)
        && can("transfers", "CREATE", organizationId, destinationUnitId);
    const validItems = selected.length > 0 && selected.every(item => Number(item.quantity) > 0
        && Number(item.quantity) <= Number(item.available) && (item.kind === "LOT" || Number(item.quantity) === 1));

    async function finalize(event: React.FormEvent) {
        event.preventDefault();
        if (submitting.current || completed) return;
        if (!organizationId || !sourceUnitId || !destinationUnitId || !destinationLocation || !purpose.trim()
                || sourceUnitId === destinationUnitId || !validItems || !canCreate || busy) return;
        const request = pending ?? {
            requestId: crypto.randomUUID(), organizationId, sourceUnitId, destinationUnitId,
            destinationLocationId: Number(destinationLocation), purpose,
            items: selected.map(item => item.kind === "ASSET"
                ? { assetId: item.stockId, quantity: 1 }
                : { balanceId: item.stockId, quantity: Number(item.quantity) })
        };
        submitting.current = true;
        setBusy(true);
        setPending(request);
        setError("");
        try {
            setCompleted(await api.finalize(request));
            setPending(null);
            setRefresh(value => value + 1);
        } catch (caught) {
            const status = axios.isAxiosError(caught) ? caught.response?.status : undefined;
            if (!pending && status && status >= 400 && status < 500) setPending(null);
            setError(errorText(caught));
        } finally {
            submitting.current = false;
            setBusy(false);
        }
    }

    return <div className={styles.workspace}>
        <p className={styles.intro}>Move assets and lot-controlled stock between units in the same organization.</p>
        {error && <Message type="error" text={error} onClose={() => setError("")} />}
        {pending && !busy && <Message type="warn" text="The result could not be confirmed. Retry the same request to avoid transferring stock twice." />}
        {completed && <Message type="success" text={`Transfer #${completed.id} finalized successfully.`} />}
        <form onSubmit={finalize}>
            <fieldset className={styles.fields} disabled={busy || !!pending || !!completed}>
                <Field label="Organization *"><ReferenceField service={core} field={reference("organizationId", "Organization", "organizations")}
                    value={organization} organizationId={null} onChange={value => {
                        setOrganization(value); setSourceUnit(null); setDestinationUnit(null); setDestinationLocation(null); setSelected([]);
                    }} /></Field>
                <Field label="Source unit *"><ReferenceField key={`source-${organization}`} service={core}
                    field={reference("sourceUnitId", "Source unit", "units")} value={sourceUnit} organizationId={organization}
                    onChange={value => { setSourceUnit(value); setSelected([]); }} /></Field>
                <Field label="Destination unit *"><ReferenceField key={`destination-${organization}`} service={core}
                    field={reference("destinationUnitId", "Destination unit", "units")} value={destinationUnit}
                    organizationId={organization} onChange={value => { setDestinationUnit(value); setDestinationLocation(null); }} /></Field>
                <Field label="Destination location *"><ReferenceField key={`location-${organization}-${destinationUnit}`} service={inventory}
                    field={reference("destinationLocationId", "Destination location", "locations")} value={destinationLocation}
                    organizationId={organization} optionFilter={record => String(record.unitId) === String(destinationUnit)}
                    onChange={setDestinationLocation} /></Field>
                <div className={styles.field}><label htmlFor="transfer-purpose">Purpose *</label><input id="transfer-purpose"
                    required maxLength={255} value={purpose} onChange={event => setPurpose(event.target.value)} /></div>
            </fieldset>
            {organizationId && sourceUnitId && canRead && canCreate && !completed && <fieldset disabled={busy || !!pending}>
                <StockPicker organizationId={organizationId} sourceUnitId={sourceUnitId} selected={selected}
                    onAdd={item => setSelected([...selected, { ...item, quantity: "1" }])} />
            </fieldset>}
            <div className={styles.tableContainer}><table><caption>Transfer items</caption><thead><tr>
                <th>Item</th><th>Source location</th><th>Available</th><th>Quantity</th><th>Actions</th>
            </tr></thead><tbody>{selected.map(item => <tr key={`${item.kind}-${item.stockId}`}>
                <td>{item.code}<br />{item.modelName} · {item.sku}</td><td>{item.locationName}</td>
                <td>{item.available} {item.unitOfMeasure}</td><td><input aria-label={`Quantity for ${item.code}`} type="number"
                    min="0.0001" max={item.available} step="0.0001" value={item.quantity}
                    disabled={item.kind === "ASSET" || busy || !!pending || !!completed}
                    onChange={event => setSelected(selected.map(value => value.kind === item.kind && value.stockId === item.stockId
                        ? { ...value, quantity: event.target.value } : value))} /></td><td><Button type="button" severity="secondary"
                    disabled={busy || !!pending || !!completed} onClick={() => setSelected(selected.filter(value =>
                        value.kind !== item.kind || value.stockId !== item.stockId))}>Remove</Button></td>
            </tr>)}{!selected.length && <tr><td colSpan={5}>Select the source unit and add available stock.</td></tr>}</tbody></table></div>
            <div className={styles.actions}>{completed
                ? <Button type="button" className="registration-yellow-button" onClick={onNew}>+ Transfer</Button>
                : <Button type="submit" className="registration-yellow-button" disabled={busy || !canCreate || !validItems
                    || !purpose.trim() || !destinationLocation || sourceUnitId === destinationUnitId}>
                    {busy ? "Finalizing…" : pending ? "Retry finalize" : "Finalize transfer"}</Button>}</div>
        </form>
        {!canCreate && <Message type="info" text="Select different source and destination units authorized by your profile." />}
        {organizationId && can("transfers", "READ", organizationId, sourceUnitId) &&
            <History organizationId={organizationId} unitId={sourceUnitId} refresh={refresh} />}
    </div>;
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
    return <div className={styles.field}><label>{label}</label>{children}</div>;
}

function StockPicker({ organizationId, sourceUnitId, selected, onAdd }: {
    organizationId: number; sourceUnitId: number; selected: Selected[]; onAdd: (item: TransferStockOption) => void;
}) {
    const [kind, setKind] = React.useState<"ASSET" | "LOT">("ASSET");
    const [search, setSearch] = React.useState("");
    const [page, setPage] = React.useState(0);
    const [result, setResult] = React.useState<TransferPage<TransferStockOption> | null>(null);
    const [error, setError] = React.useState("");
    React.useEffect(() => {
        const controller = new AbortController();
        const timer = setTimeout(() => api.stock(organizationId, sourceUnitId, kind, search, page, controller.signal)
            .then(value => { if (!controller.signal.aborted) { setResult(value); setError(""); } })
            .catch(caught => { if (!controller.signal.aborted) setError(errorText(caught)); }), 250);
        return () => { clearTimeout(timer); controller.abort(); };
    }, [organizationId, sourceUnitId, kind, search, page]);
    return <section><div className={styles.toolbar}><h2>Available stock at source</h2>
        <div className={styles.field}><label htmlFor="transfer-kind">Stock type</label><select id="transfer-kind" value={kind}
            onChange={event => { setKind(event.target.value as "ASSET" | "LOT"); setPage(0); setResult(null); }}>
            <option value="ASSET">Individual assets</option><option value="LOT">Stock lots</option></select></div>
        <div className={styles.field}><label htmlFor="transfer-search">Code, model or SKU</label><input id="transfer-search"
            type="search" value={search} onChange={event => { setSearch(event.target.value); setPage(0); setResult(null); }} /></div>
    </div>{error && <Message type="error" text={error} />}{!result && !error && <p role="status">Loading stock…</p>}
        {result && <><div className={styles.tableContainer}><table><thead><tr><th>Code / model</th><th>Location</th>
            <th>Available</th><th>Actions</th></tr></thead><tbody>{result.content.map(item => <tr key={`${item.kind}-${item.stockId}`}>
                <td>{item.code}<br />{item.modelName} · {item.sku}</td><td>{item.locationName}</td>
                <td>{item.available} {item.unitOfMeasure}</td><td><Button type="button" disabled={selected.length >= 100
                    || selected.some(value => value.kind === item.kind && value.stockId === item.stockId)} onClick={() => onAdd(item)}>Add</Button></td>
            </tr>)}{!result.content.length && <tr><td colSpan={4}>No available stock matches your search.</td></tr>}</tbody></table></div>
            <Pagination page={page} result={result} onPage={value => { setPage(value); setResult(null); }} /></>}
    </section>;
}

function History({ organizationId, unitId, refresh }: { organizationId: number; unitId?: number; refresh: number }) {
    const [page, setPage] = React.useState(0);
    const [result, setResult] = React.useState<TransferPage<InventoryTransfer> | null>(null);
    const [error, setError] = React.useState("");
    React.useEffect(() => {
        const controller = new AbortController();
        api.list(organizationId, unitId, page, controller.signal)
            .then(value => { if (!controller.signal.aborted) { setResult(value); setError(""); } })
            .catch(caught => { if (!controller.signal.aborted) setError(errorText(caught)); });
        return () => controller.abort();
    }, [organizationId, unitId, page, refresh]);
    return <section className="mt-8"><h2>Transfer history</h2>{error && <Message type="error" text={error} />}
        {!result && !error && <p role="status">Loading history…</p>}{result?.content.map(transfer => <details key={transfer.id}>
            <summary>Transfer #{transfer.id} · {transfer.sourceUnitName} → {transfer.destinationUnitName} · {transfer.status}</summary>
            <p>{transfer.organizationName} · Destination: {transfer.destinationLocationName} · Purpose: {transfer.purpose}</p>
            <p>Finalized: {transfer.sentAt.replace("T", " ")} · Operator: {transfer.finalizedByLogin || "Not recorded"}</p>
            <div className={styles.tableContainer}><table><thead><tr><th>Item</th><th>Route</th><th>Quantity</th></tr></thead><tbody>
                {transfer.items.map(item => <tr key={item.id}><td>{item.stockCode} · {item.modelName}<br />{item.sku}</td>
                    <td>{item.sourceLocationName} → {item.destinationLocationName}</td><td>{item.quantity} {item.unitOfMeasure}</td></tr>)}
            </tbody></table></div></details>)}{result && !result.content.length && <p>No transfers in the selected scope.</p>}
        {result && <Pagination page={page} result={result} onPage={setPage} />}</section>;
}

function Pagination({ page, result, onPage }: { page: number; result: { totalElements: number; size: number }; onPage: (page: number) => void }) {
    return <div className={styles.pagination}><span>{result.totalElements} records · Page {page + 1}</span>
        <Button type="button" severity="secondary" disabled={page === 0} onClick={() => onPage(page - 1)}>Previous</Button>
        <Button type="button" severity="secondary" disabled={(page + 1) * result.size >= result.totalElements}
            onClick={() => onPage(page + 1)}>Next</Button></div>;
}
