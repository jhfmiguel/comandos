"use client";

import * as React from "react";
import axios from "axios";
import { Button } from "@primereact/ui/button";
import { Layout } from "components/layout";
import { Message } from "components/common/message";
import { useSession } from "components/auth/session-provider";
import { ReferenceField } from "components/erp/shared/record-workspace";
import { createErpService } from "api/services/erp.service";
import { ammunitionConsumptionService as api } from "api/services/ammunition-consumption.service";
import type { ErpField, ErpValue } from "api/models/erp";
import type { AmmunitionConsumption, AmmunitionConsumptionPage, AmmunitionConsumptionRequest, AmmunitionStockOption } from "api/models/erp/ammunition-consumption";
import styles from "components/erp/shared/workspace.module.css";

const core = createErpService("core");
const ref = (name: string, label: string, resource: string, required = true): ErpField =>
    ({ name, label, type: "reference", required, reference: resource, choices: [] });
const errorText = (error: unknown) => axios.isAxiosError(error) && typeof error.response?.data?.detail === "string"
    ? error.response.data.detail : "Unable to complete the request. Check the API connection and try again.";
type Selected = AmmunitionStockOption & { quantity: string; result: string };

export function AmmunitionConsumptionWorkspace() {
    const [generation, setGeneration] = React.useState(0);
    return <Layout title="Ammunition consumption"><ConsumptionForm key={generation} onNew={() => setGeneration(value => value + 1)} /></Layout>;
}

function ConsumptionForm({ onNew }: { onNew: () => void }) {
    const { can } = useSession();
    const [organization, setOrganization] = React.useState<ErpValue>(null); const [unit, setUnit] = React.useState<ErpValue>(null);
    const [responsible, setResponsible] = React.useState<ErpValue>(null); const [authorizer, setAuthorizer] = React.useState<ErpValue>(null);
    const [purpose, setPurpose] = React.useState(""); const [selected, setSelected] = React.useState<Selected[]>([]);
    const [pending, setPending] = React.useState<AmmunitionConsumptionRequest | null>(null);
    const [completed, setCompleted] = React.useState<AmmunitionConsumption | null>(null);
    const [busy, setBusy] = React.useState(false); const [error, setError] = React.useState(""); const [refresh, setRefresh] = React.useState(0);
    const organizationId = organization ? Number(organization) : undefined; const unitId = unit ? Number(unit) : undefined;
    const canRead = can("ammunition-consumptions", "READ", organizationId, unitId);
    const canCreate = can("ammunition-consumptions", "CREATE", organizationId, unitId);
    const validItems = selected.length > 0 && selected.every(item => Number(item.quantity) > 0 && Number(item.quantity) <= Number(item.available) && item.result.trim());
    async function finalize(event: React.FormEvent) {
        event.preventDefault(); if (!organizationId || !responsible || !authorizer || !validItems || !canCreate || busy) return;
        const request = pending ?? { requestId: crypto.randomUUID(), organizationId, unitId, responsibleId: Number(responsible),
            authorizerId: Number(authorizer), purpose, items: selected.map(item => ({ balanceId: item.balanceId, quantity: Number(item.quantity), result: item.result })) };
        setBusy(true); setPending(request); setError("");
        try { setCompleted(await api.finalize(request)); setPending(null); setRefresh(value => value + 1); }
        catch (caught) { const status = axios.isAxiosError(caught) ? caught.response?.status : undefined;
            if (status && status >= 400 && status < 500) setPending(null); setError(errorText(caught)); }
        finally { setBusy(false); }
    }
    return <div className={styles.workspace}><p className={styles.intro}>Record authorized ammunition use and deduct each lot from inventory.</p>
        {error && <Message type="error" text={error} onClose={() => setError("")} />}
        {pending && !busy && <Message type="warn" text="The result could not be confirmed. Retry the same request to avoid consuming stock twice." />}
        {completed && <Message type="success" text={`Consumption #${completed.id} finalized successfully.`} />}
        <form data-comandos-erp-form="true" onSubmit={finalize}><fieldset className={styles.fields} disabled={busy || !!pending || !!completed}>
            <Field label="Organization *"><ReferenceField service={core} field={ref("organizationId", "Organization", "organizations")} value={organization} organizationId={null}
                onChange={value => { setOrganization(value); setUnit(null); setSelected([]); }} /></Field>
            <Field label="Unit"><ReferenceField key={String(organization)} service={core} field={ref("unitId", "Unit", "units", false)} value={unit} organizationId={organization}
                onChange={value => { setUnit(value); setSelected([]); }} /></Field>
            <Field label="Responsible person *"><ReferenceField service={core} field={ref("responsibleId", "Responsible person", "people")} value={responsible} organizationId={null} onChange={setResponsible} /></Field>
            <Field label="Authorizer *"><ReferenceField service={core} field={ref("authorizerId", "Authorizer", "people")} value={authorizer} organizationId={null} onChange={setAuthorizer} /></Field>
            <div className={styles.field}><label htmlFor="consumption-purpose">Purpose *</label><input id="consumption-purpose" required maxLength={255} value={purpose} onChange={event => setPurpose(event.target.value)} /></div>
        </fieldset>
        {organizationId && canRead && canCreate && !completed && <StockPicker organizationId={organizationId} unitId={unitId} selected={selected}
            onAdd={item => setSelected([...selected, { ...item, quantity: "1", result: "Consumed" }])} />}
        <div className={styles.tableContainer}><table><caption>Consumption items</caption><thead><tr><th>Ammunition / lot</th><th>Available</th><th>Quantity *</th><th>Result *</th><th>Actions</th></tr></thead><tbody>
            {selected.map(item => <tr key={item.balanceId}><td>{item.sku} · {item.modelName}<br />Lot {item.lotNumber} · {item.locationName}</td><td>{item.available} {item.unitOfMeasure}</td>
                <td><input aria-label={`Quantity for ${item.lotNumber}`} type="number" min="0.0001" max={item.available} step="0.0001" required value={item.quantity}
                    disabled={busy || !!pending || !!completed} onChange={event => setSelected(selected.map(value => value.balanceId === item.balanceId ? { ...value, quantity: event.target.value } : value))} /></td>
                <td><input aria-label={`Result for ${item.lotNumber}`} required maxLength={255} value={item.result} disabled={busy || !!pending || !!completed}
                    onChange={event => setSelected(selected.map(value => value.balanceId === item.balanceId ? { ...value, result: event.target.value } : value))} /></td>
                <td><Button type="button" severity="secondary" disabled={busy || !!pending || !!completed} onClick={() => setSelected(selected.filter(value => value.balanceId !== item.balanceId))}>Remove</Button></td></tr>)}
            {!selected.length && <tr><td colSpan={5}>Select the organization and add an available ammunition lot.</td></tr>}
        </tbody></table></div><div className={styles.actions}>{completed ? <Button type="button" className="registration-yellow-button" onClick={onNew}>+ Consumption</Button>
            : <Button type="submit" className="registration-yellow-button" disabled={busy || !validItems || !canCreate}>{busy ? "Finalizing…" : pending ? "Retry finalize" : "Finalize consumption"}</Button>}</div></form>
        {!canCreate && <Message type="info" text="Select an organization and an authorized unit when required by your profile." />}
        {organizationId && canRead && <History organizationId={organizationId} unitId={unitId} refresh={refresh} />}</div>;
}
function Field({ label, children }: { label: string; children: React.ReactNode }) { return <div className={styles.field}><label>{label}</label>{children}</div>; }

function StockPicker({ organizationId, unitId, selected, onAdd }: { organizationId: number; unitId?: number; selected: Selected[]; onAdd: (item: AmmunitionStockOption) => void }) {
    const [search, setSearch] = React.useState(""); const [page, setPage] = React.useState(0); const [result, setResult] = React.useState<AmmunitionConsumptionPage<AmmunitionStockOption> | null>(null); const [error, setError] = React.useState("");
    React.useEffect(() => { const controller = new AbortController(); const timer = setTimeout(() => api.stock(organizationId, unitId, search, page, controller.signal)
        .then(value => { if (!controller.signal.aborted) { setResult(value); setError(""); } }).catch(caught => { if (!controller.signal.aborted) setError(errorText(caught)); }), 250);
        return () => { clearTimeout(timer); controller.abort(); }; }, [organizationId, unitId, search, page]);
    return <section><div className={styles.toolbar}><h2>Available ammunition</h2><div className={styles.field}><label htmlFor="ammunition-search">SKU, model, lot or location</label>
        <input id="ammunition-search" type="search" value={search} onChange={event => { setSearch(event.target.value); setPage(0); setResult(null); }} /></div></div>
        {error && <Message type="error" text={error} />}{!result && !error && <p role="status">Loading ammunition…</p>}
        {result && <><div className={styles.tableContainer}><table><thead><tr><th>SKU / model</th><th>Lot</th><th>Location</th><th>Available</th><th>Actions</th></tr></thead><tbody>
            {result.content.map(item => <tr key={item.balanceId}><td>{item.sku}<br />{item.modelName}</td><td>{item.lotNumber}<br />Expires: {item.validUntil || "Not set"}</td><td>{item.locationName}</td><td>{item.available} {item.unitOfMeasure}</td>
                <td><Button type="button" disabled={selected.length >= 100 || selected.some(value => value.balanceId === item.balanceId)} onClick={() => onAdd(item)}>Add</Button></td></tr>)}
            {!result.content.length && <tr><td colSpan={5}>No available ammunition matches your search.</td></tr>}</tbody></table></div><Pagination page={page} result={result} onPage={setPage} /></>}
    </section>;
}
function History({ organizationId, unitId, refresh }: { organizationId: number; unitId?: number; refresh: number }) {
    const [page, setPage] = React.useState(0); const [result, setResult] = React.useState<AmmunitionConsumptionPage<AmmunitionConsumption> | null>(null); const [error, setError] = React.useState("");
    React.useEffect(() => { const controller = new AbortController(); api.list(organizationId, unitId, page, controller.signal).then(value => { if (!controller.signal.aborted) { setResult(value); setError(""); } })
        .catch(caught => { if (!controller.signal.aborted) setError(errorText(caught)); }); return () => controller.abort(); }, [organizationId, unitId, page, refresh]);
    return <section className="mt-8"><h2>Consumption history</h2>{error && <Message type="error" text={error} />}{!result && !error && <p role="status">Loading history…</p>}
        {result?.content.map(value => <details key={value.id}><summary>Consumption #{value.id} · {value.responsibleName} · {value.status}</summary><p>{value.organizationName} · Unit: {value.unitName || "Organization-wide"}</p>
            <p>Purpose: {value.purpose} · Date: {value.consumedAt.replace("T", " ")} · Authorized by: {value.authorizerName} · Finalized by: {value.finalizedByLogin || "Not recorded"}</p>
            <div className={styles.tableContainer}><table><thead><tr><th>Ammunition / lot</th><th>Location</th><th>Quantity</th><th>Result</th></tr></thead><tbody>{value.items.map(item => <tr key={item.id}>
                <td>{item.sku} · {item.modelName}<br />Lot {item.lotNumber}</td><td>{item.locationName}</td><td>{item.quantity} {item.unitOfMeasure}</td><td>{item.result}</td></tr>)}</tbody></table></div></details>)}
        {result && !result.content.length && <p>No consumption records in the selected scope.</p>}{result && <Pagination page={page} result={result} onPage={setPage} />}</section>;
}
function Pagination({ page, result, onPage }: { page: number; result: { totalElements: number; size: number }; onPage: (page: number) => void }) {
    return <div className={styles.pagination}><span>{result.totalElements} records · Page {page + 1}</span><Button type="button" severity="secondary" disabled={page === 0} onClick={() => onPage(page - 1)}>Previous</Button>
        <Button type="button" severity="secondary" disabled={(page + 1) * result.size >= result.totalElements} onClick={() => onPage(page + 1)}>Next</Button></div>;
}
