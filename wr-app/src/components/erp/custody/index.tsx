"use client";

import * as React from "react";
import axios from "axios";
import { useRouter } from "next/navigation";
import { Button } from "@primereact/ui/button";
import { Layout } from "components/layout";
import { Message } from "components/common/message";
import { useSession } from "components/auth/session-provider";
import { ReferenceField } from "components/erp/shared/record-workspace";
import { createErpService } from "api/services/erp.service";
import { custodyService as api } from "api/services/custody.service";
import type { ErpField, ErpValue } from "api/models/erp";
import type { Custody, CustodyEquipmentSetOption, CustodyIssueRequest, CustodyPage, CustodyStockOption } from "api/models/erp/custody";
import styles from "components/erp/shared/workspace.module.css";

const core = createErpService("core");
const inventory = createErpService("inventory");
const reference = (name: string, label: string, resource: string, required = true): ErpField =>
    ({ name, label, type: "reference", required, reference: resource, choices: [] });
const organizationField = reference("organizationId", "Organization", "organizations");
const unitField = reference("unitId", "Unit", "units", false);
const recipientField = reference("recipientId", "Recipient", "people");
const recipientUnitField = reference("recipientUnitId", "Receiving organizational unit", "units");
const authorizerField = reference("authorizerId", "Authorizer", "people");
const returnConditionField = reference("returnConditionTypeId", "Return condition", "custody-return-condition-types");
const errorText = (error: unknown) => axios.isAxiosError(error) && typeof error.response?.data?.detail === "string"
    ? error.response.data.detail : "Unable to complete the request. Check the API connection and try again.";

export function CustodyWorkspace() {
    const [generation, setGeneration] = React.useState(0);
    return <Layout title="Equipment custody"><CustodyForm key={generation} onNew={() => setGeneration(value => value + 1)} /></Layout>;
}

function CustodyForm({ onNew }: { onNew: () => void }) {
    const { can } = useSession();
    const [organization, setOrganization] = React.useState<ErpValue>(null);
    const [unit, setUnit] = React.useState<ErpValue>(null);
    const [recipient, setRecipient] = React.useState<ErpValue>(null);
    const [recipientType, setRecipientType] = React.useState<"PERSON" | "UNIT">("PERSON");
    const [recipientUnit, setRecipientUnit] = React.useState<ErpValue>(null);
    const [authorizer, setAuthorizer] = React.useState<ErpValue>(null);
    const [purpose, setPurpose] = React.useState("");
    const [dueAt, setDueAt] = React.useState("");
    const [selected, setSelected] = React.useState<CustodyStockOption[]>([]);
    const [selectedSets, setSelectedSets] = React.useState<CustodyEquipmentSetOption[]>([]);
    const [pending, setPending] = React.useState<CustodyIssueRequest | null>(null);
    const [completed, setCompleted] = React.useState<Custody | null>(null);
    const [busy, setBusy] = React.useState(false);
    const [error, setError] = React.useState("");
    const [refresh, setRefresh] = React.useState(0);
    const organizationId = organization ? Number(organization) : undefined;
    const unitId = unit ? Number(unit) : undefined;
    const canRead = can("custodies", "READ", organizationId, unitId);
    const canCreate = can("custodies", "CREATE", organizationId, unitId);
    const scopeKey = `${organizationId}:${unitId}`;

    async function issue(event: React.FormEvent) {
        event.preventDefault();
        if (!organizationId || !(recipientType === "PERSON" ? recipient : recipientUnit) || !authorizer
            || selected.length + selectedSets.length === 0 || !canCreate || busy || completed) return;
        const request = pending ?? { requestId: crypto.randomUUID(), organizationId, unitId,
            ...(recipientType === "PERSON" ? { recipientId: Number(recipient) } : { recipientUnitId: Number(recipientUnit) }),
            authorizerId: Number(authorizer), purpose, dueAt: dueAt || undefined,
            assetIds: selected.map(item => item.assetId), equipmentSetIds: selectedSets.map(item => item.equipmentSetId) };
        setBusy(true); setPending(request); setError("");
        try { setCompleted(await api.issue(request)); setPending(null); setRefresh(value => value + 1); }
        catch (error) {
            const status = axios.isAxiosError(error) ? error.response?.status : undefined;
            if (!pending && status && status >= 400 && status < 500) setPending(null);
            setError(errorText(error));
        } finally { setBusy(false); }
    }

    return <div className={styles.workspace}>
        <p className={styles.intro}>Issue available equipment to a person or an organizational unit and record each return with stock history.</p>
        {error && <Message type="error" text={error} onClose={() => setError("")} />}
        {pending && !busy && <Message type="warn" text="The result could not be confirmed. Retry with the same request to avoid issuing twice." />}
        {completed && <Message type="success" text={`Custody #${completed.id} issued successfully.`} />}
        <form data-comandos-erp-form="true" onSubmit={issue}>
            <fieldset className={styles.fields} disabled={busy || !!pending || !!completed}>
                <div className={styles.field}><label htmlFor="core-organizationId">Organization *</label>
                    <ReferenceField service={core} field={organizationField} value={organization} organizationId={null}
                        onChange={value => { setOrganization(value); setUnit(null); setRecipientUnit(null); setSelected([]); setSelectedSets([]); }} /></div>
                <div className={styles.field}><label htmlFor="core-unitId">Issuing unit</label>
                    <ReferenceField key={String(organization)} service={core} field={unitField} value={unit} organizationId={organization}
                        onChange={value => { setUnit(value); setSelected([]); setSelectedSets([]); }} /></div>
                <div className={styles.field}><label htmlFor="custody-recipient-type">Recipient type *</label>
                    <select id="custody-recipient-type" value={recipientType}
                        onChange={event => { setRecipientType(event.target.value as "PERSON" | "UNIT"); setRecipient(null); setRecipientUnit(null); }}>
                        <option value="PERSON">Person</option><option value="UNIT">Organizational unit</option>
                    </select></div>
                {recipientType === "PERSON"
                    ? <div className={styles.field}><label htmlFor="core-recipientId">Recipient *</label>
                        <ReferenceField service={core} field={recipientField} value={recipient} organizationId={null} onChange={setRecipient} /></div>
                    : <div className={styles.field}><label htmlFor="core-recipientUnitId">Receiving organizational unit *</label>
                        <ReferenceField key={String(organization)} service={core} field={recipientUnitField} value={recipientUnit}
                            organizationId={organization} onChange={setRecipientUnit} /></div>}
                <div className={styles.field}><label htmlFor="core-authorizerId">Authorizer *</label>
                    <ReferenceField service={core} field={authorizerField} value={authorizer} organizationId={null} onChange={setAuthorizer} /></div>
                <div className={styles.field}><label htmlFor="custody-purpose">Purpose *</label>
                    <input id="custody-purpose" required maxLength={255} value={purpose} onChange={event => setPurpose(event.target.value)} /></div>
                <div className={styles.field}><label htmlFor="custody-due">Due date and time</label>
                    <input id="custody-due" type="datetime-local" value={dueAt} onChange={event => setDueAt(event.target.value)} /></div>
            </fieldset>
            <fieldset disabled={busy || !!pending || !!completed}>
            {organizationId && canRead && canCreate && !completed && <StockPicker key={scopeKey} organizationId={organizationId} unitId={unitId}
                selected={selected} onAdd={item => setSelected([...selected, item])} />}
            {organizationId && canRead && canCreate && !completed && <EquipmentSetPicker key={`sets-${scopeKey}`} organizationId={organizationId}
                unitId={unitId} selected={selectedSets} onAdd={item => setSelectedSets([...selectedSets, item])} />}
            </fieldset>
            <div className={styles.tableContainer}><table><caption>Selected equipment sets</caption><thead><tr>
                <th>Code / name</th><th>Components</th><th>Actions</th></tr></thead><tbody>
                {selectedSets.map(item => <tr key={item.equipmentSetId}><td>{item.code}<br />{item.name}</td><td>{item.componentCount}</td>
                    <td><Button type="button" severity="secondary" disabled={busy || !!pending || !!completed}
                        onClick={() => setSelectedSets(selectedSets.filter(value => value.equipmentSetId !== item.equipmentSetId))}>Remove</Button></td></tr>)}
                {!selectedSets.length && <tr><td colSpan={3}>No equipment set selected.</td></tr>}
            </tbody></table></div>
            <div className={styles.tableContainer}><table><caption>Selected equipment</caption><thead><tr>
                <th>Asset / model</th><th>Serial number</th><th>Location</th><th>Actions</th></tr></thead><tbody>
                {selected.map(item => <tr key={item.assetId}><td>{item.assetCode}<br />{item.modelName}</td><td>{item.serialNumber || "—"}</td>
                    <td>{item.locationName}</td><td><Button type="button" severity="secondary" disabled={busy || !!pending || !!completed}
                        onClick={() => setSelected(selected.filter(value => value.assetId !== item.assetId))}>Remove</Button></td></tr>)}
                {!selected.length && <tr><td colSpan={4}>Select the organization and add available equipment.</td></tr>}
            </tbody></table></div>
            <div className={styles.actions}>{completed
                ? <Button type="button" className="registration-yellow-button" onClick={onNew}>+ Custody</Button>
                : <Button type="submit" className="registration-yellow-button" disabled={busy || selected.length + selectedSets.length === 0 || !canCreate}>
                    {busy ? "Issuing…" : pending ? "Retry issue" : "Issue custody"}</Button>}</div>
        </form>
        {!canCreate && <Message type="info" text="Select an organization and an authorized unit when required by your profile." />}
        {organizationId && canRead && <CustodyHistory key={scopeKey} organizationId={organizationId} unitId={unitId} refresh={refresh} />}
    </div>;
}

function EquipmentSetPicker({ organizationId, unitId, selected, onAdd }: { organizationId: number; unitId?: number;
    selected: CustodyEquipmentSetOption[]; onAdd: (item: CustodyEquipmentSetOption) => void }) {
    const [search, setSearch] = React.useState(""); const [page, setPage] = React.useState(0);
    const [result, setResult] = React.useState<CustodyPage<CustodyEquipmentSetOption> | null>(null); const [error, setError] = React.useState("");
    React.useEffect(() => { const controller = new AbortController(); const timer = setTimeout(() => api.equipmentSets(organizationId, unitId, search, page, controller.signal)
        .then(value => { if (!controller.signal.aborted) { setResult(value); setError(""); } })
        .catch(error => { if (!controller.signal.aborted) setError(errorText(error)); }), 250);
        return () => { clearTimeout(timer); controller.abort(); }; }, [organizationId, unitId, search, page]);
    return <section><div className={styles.toolbar}><h2>Available equipment sets</h2><div className={styles.field}>
        <label htmlFor="custody-set-search">Set code or name</label><input id="custody-set-search" type="search" value={search}
            onChange={event => { setSearch(event.target.value); setPage(0); setResult(null); }} /></div></div>
        {error && <Message type="error" text={error} />}{!result && !error && <p role="status">Loading equipment sets…</p>}
        {result && <><div className={styles.tableContainer}><table><thead><tr><th>Code / name</th><th>Components</th><th>Actions</th></tr></thead><tbody>
            {result.content.map(item => <tr key={item.equipmentSetId}><td>{item.code}<br />{item.name}</td><td>{item.componentCount}</td>
                <td><Button type="button" disabled={selected.some(value => value.equipmentSetId === item.equipmentSetId)} onClick={() => onAdd(item)}>Add set</Button></td></tr>)}
            {!result.content.length && <tr><td colSpan={3}>No complete equipment set is currently available.</td></tr>}</tbody></table></div>
            <Pagination page={page} result={result} onPage={value => { setPage(value); setResult(null); }} /></>}
    </section>;
}

function StockPicker({ organizationId, unitId, selected, onAdd }: { organizationId: number; unitId?: number;
    selected: CustodyStockOption[]; onAdd: (item: CustodyStockOption) => void }) {
    const [search, setSearch] = React.useState(""); const [page, setPage] = React.useState(0);
    const [result, setResult] = React.useState<CustodyPage<CustodyStockOption> | null>(null); const [error, setError] = React.useState("");
    React.useEffect(() => { const controller = new AbortController(); const timer = setTimeout(() => api.stock(organizationId, unitId, search, page, controller.signal)
        .then(value => { if (!controller.signal.aborted) { setResult(value); setError(""); } })
        .catch(error => { if (!controller.signal.aborted) setError(errorText(error)); }), 250);
        return () => { clearTimeout(timer); controller.abort(); }; }, [organizationId, unitId, search, page]);
    return <section><div className={styles.toolbar}><h2>Available equipment</h2><div className={styles.field}>
        <label htmlFor="custody-search">Asset code, serial number or model</label><input id="custody-search" type="search" value={search}
            onChange={event => { setSearch(event.target.value); setPage(0); setResult(null); }} /></div></div>
        {error && <Message type="error" text={error} />}{!result && !error && <p role="status">Loading equipment…</p>}
        {result && <><div className={styles.tableContainer}><table><thead><tr><th>Asset / model</th><th>Serial number</th><th>Location</th><th>Actions</th></tr></thead><tbody>
            {result.content.map(item => <tr key={item.assetId}><td>{item.assetCode}<br />{item.modelName}</td><td>{item.serialNumber || "—"}</td>
                <td>{item.locationName}</td><td><Button type="button" disabled={selected.length >= 100 || selected.some(value => value.assetId === item.assetId)}
                    onClick={() => onAdd(item)}>Add</Button></td></tr>)}
            {!result.content.length && <tr><td colSpan={4}>No available equipment matches your search.</td></tr>}</tbody></table></div>
            <Pagination page={page} result={result} onPage={value => { setPage(value); setResult(null); }} /></>}
    </section>;
}

function CustodyHistory({ organizationId, unitId, refresh }: { organizationId: number; unitId?: number; refresh: number }) {
    const router = useRouter();
    const { can } = useSession(); const [page, setPage] = React.useState(0);
    const [result, setResult] = React.useState<CustodyPage<Custody> | null>(null); const [error, setError] = React.useState("");
    const [returning, setReturning] = React.useState(false); const [revision, setRevision] = React.useState(0);
    const [returnCondition, setReturnCondition] = React.useState<ErpValue>(null); const [inspectionNotes, setInspectionNotes] = React.useState("");
    const [pendingReturn, setPendingReturn] = React.useState<{ custodyId: number; itemIds: number[]; requestId: string;
        conditionTypeId: number; inspectionNotes: string } | null>(null);
    const canReturn = can("custodies", "RETURN", organizationId, unitId);
    const canCreateMaintenance = can("maintenance", "CREATE", organizationId, unitId);
    function openMaintenance(custody: Custody, item: Custody["items"][number]) {
        const parameters = new URLSearchParams({ organizationId: String(custody.organizationId), assetId: String(item.assetId),
            custodyReturnItemId: String(item.returnInspectionId), reason: item.inspectionNotes || `Custody return inspection: ${item.returnConditionName}` });
        if (custody.unitId) parameters.set("unitId", String(custody.unitId));
        router.push(`/erp/maintenance?${parameters.toString()}`);
    }
    React.useEffect(() => { const controller = new AbortController(); api.list(organizationId, unitId, page, controller.signal)
        .then(value => { if (!controller.signal.aborted) { setResult(value); setError(""); } })
        .catch(error => { if (!controller.signal.aborted) setError(errorText(error)); }); return () => controller.abort();
    }, [organizationId, unitId, page, refresh, revision]);
    async function returnItems(custody: Custody, itemIds: number[]) {
        if (!returnCondition && !pendingReturn) return;
        const pending = pendingReturn ?? { custodyId: custody.id, itemIds, requestId: crypto.randomUUID(),
            conditionTypeId: Number(returnCondition), inspectionNotes };
        if (pending.custodyId !== custody.id || pending.itemIds.join(",") !== itemIds.join(",")) return;
        setReturning(true); setPendingReturn(pending); setError("");
        try { await api.returnItems(custody.id, itemIds, pending.requestId, pending.conditionTypeId, pending.inspectionNotes);
            setPendingReturn(null); setReturnCondition(null); setInspectionNotes(""); setRevision(value => value + 1); }
        catch (error) {
            const status = axios.isAxiosError(error) ? error.response?.status : undefined;
            if (status && status >= 400 && status < 500) setPendingReturn(null);
            setError(errorText(error));
        } finally { setReturning(false); }
    }
    return <section className="mt-8"><div className={styles.toolbar}><h2>Custody history</h2>
        <Button type="button" severity="secondary" onClick={() => setRevision(value => value + 1)}>Refresh history</Button></div>
        {error && <Message type="error" text={error} />}
        {pendingReturn && !returning && <div className={styles.actions}><Message type="warn" text="The return result could not be confirmed. Retry to recover it without a duplicate movement." />
            <Button type="button" onClick={() => { const custody = result?.content.find(value => value.id === pendingReturn.custodyId);
                if (custody) void returnItems(custody, pendingReturn.itemIds); }}>Retry pending return</Button></div>}
        {!result && !error && <p role="status">Loading custody history…</p>}
        {canReturn && <div className={styles.fields}><div className={styles.field}><label htmlFor="core-returnConditionTypeId">Return condition *</label>
            <ReferenceField service={inventory} field={returnConditionField} value={returnCondition} organizationId={null}
                optionFilter={record => record.active === true} onChange={setReturnCondition} /></div>
            <div className={styles.field}><label htmlFor="custody-inspection-notes">Inspection notes</label>
                <input id="custody-inspection-notes" maxLength={500} value={inspectionNotes} onChange={event => setInspectionNotes(event.target.value)} /></div></div>}
        {result?.content.map(custody => { const outstanding = custody.items.filter(item => !item.returnedAt); return <details key={custody.id}>
            <summary>Custody #{custody.id} · {custody.recipientName} · {custody.status}</summary>
            <div className={styles.tableContainer}><p>{custody.organizationName} · Issuing unit: {custody.unitName || "Organization-wide"}</p>
                <p>{custody.recipientType === "UNIT" ? "Receiving organizational unit" : "Recipient"}: {custody.recipientName}</p>
                <p>Purpose: {custody.purpose} · Delivered: {custody.deliveredAt.replace("T", " ")} · Due: {custody.dueAt?.replace("T", " ") || "Not set"}</p>
                <p>Authorized by: {custody.authorizerName} · Issued by: {custody.issuedByLogin || "Not recorded"}</p>
                <table><thead><tr><th>Equipment / model</th><th>Set / role</th><th>Quantity</th><th>Location</th><th>Return</th></tr></thead><tbody>
                    {custody.items.map(item => { const setItems = item.equipmentSetId ? outstanding.filter(value => value.equipmentSetId === item.equipmentSetId) : [];
                        return <tr key={item.id}><td>{item.assetCode}<br />{item.modelName}{item.serialNumber ? <><br />Serial: {item.serialNumber}</> : null}</td>
                        <td>{item.equipmentSetCode ? <>{item.equipmentSetCode}<br />{item.componentRole}</> : "Individual"}</td><td>{item.quantity}</td>
                        <td>{item.locationName}</td><td>{item.returnedAt ? <>Returned {item.returnedAt.replace("T", " ")}
                            {item.returnConditionName && <><br />Condition: {item.returnConditionName}</>}
                            {item.inspectionNotes && <><br />Notes: {item.inspectionNotes}</>}
                            {item.maintenanceWorkOrderId ? <><br />Work order #{item.maintenanceWorkOrderId}</>
                                : item.assetId && item.returnInspectionId && item.returnBlocksAvailability && canCreateMaintenance ? <><br /><Button type="button"
                                    onClick={() => openMaintenance(custody, item)}>Open maintenance</Button></> : null}</>
                            : canReturn ? <Button type="button" disabled={returning || !!pendingReturn || !returnCondition} onClick={() => void returnItems(custody,
                                item.equipmentSetId ? setItems.map(value => value.id) : [item.id])}>{item.equipmentSetId ? "Return set" : "Return"}</Button> : "Pending"}</td></tr>; })}</tbody></table>
                {canReturn && outstanding.length > 1 && <div className={styles.actions}><Button type="button" disabled={returning || !!pendingReturn || !returnCondition}
                    onClick={() => void returnItems(custody, outstanding.map(item => item.id))}>Return all pending</Button></div>}
            </div></details>; })}
        {result && !result.content.length && <p>No custody records in the selected scope.</p>}
        {result && <Pagination page={page} result={result} onPage={value => { setPage(value); setResult(null); }} />}</section>;
}

function Pagination({ page, result, onPage }: { page: number; result: { totalElements: number; size: number }; onPage: (page: number) => void }) {
    return <div className={styles.pagination}><span>{result.totalElements} records · Page {page + 1}</span>
        <Button type="button" severity="secondary" disabled={page === 0} onClick={() => onPage(page - 1)}>Previous</Button>
        <Button type="button" severity="secondary" disabled={(page + 1) * result.size >= result.totalElements} onClick={() => onPage(page + 1)}>Next</Button></div>;
}
