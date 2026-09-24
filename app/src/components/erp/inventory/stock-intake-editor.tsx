"use client";

import * as React from "react";
import axios from "axios";
import { Message } from "components/common/message";
import { ComandosSelectField } from "components/common/select-field";
import { ReferenceSelectField } from "components/erp/shared/record-workspace";
import { httpClient } from "api/http";
import type { ErpResource, ErpValue } from "api/models/erp";
import type { ErpService } from "api/services/erp.service";
import styles from "components/erp/shared/workspace.module.css";

type RowResult = { row: number; status: string; errors: string[] };
type BatchResult = { quantity: string; rows?: RowResult[] };
type Pair = { first: string; second: string };
type Payload = { requestId: string; [key: string]: unknown };
const emptyRow = (): Pair => ({ first: "", second: "" });
const whole = /^[1-9][0-9]{0,14}$/;

export function StockIntakeEditor({ resource, service, onCancel, onSaved }: {
    resource: ErpResource; service: ErpService; onCancel: () => void; onSaved: (quantity: string) => void;
}) {
    const assets = resource.key === "assets";
    const fields = resource.fields.filter(field => !field.readOnly && !(assets
        ? ["assetCode", "serialNumber", "internalCode"].includes(field.name) : field.name === "initialQuantity"));
    const [values, setValues] = React.useState<Record<string, ErpValue>>({ condition: "GOOD", status: "AVAILABLE", currentValue: "0" });
    const [rows, setRows] = React.useState<Pair[]>([emptyRow()]);
    const [paste, setPaste] = React.useState("");
    const [looseUnits, setLooseUnits] = React.useState("0");
    const [error, setError] = React.useState("");
    const [busy, setBusy] = React.useState(false);
    const [pending, setPending] = React.useState<Payload | null>(null);
    const [reviewed, setReviewed] = React.useState(false);
    const [results, setResults] = React.useState<RowResult[]>([]);
    const [completed, setCompleted] = React.useState<string | null>(null);
    const saving = React.useRef(false);
    const locked = busy || !!pending || reviewed || completed !== null;
    const normalized = rows.map(row => ({ first: row.first.trim(), second: row.second.trim() }));
    const serials = normalized.map(row => row.second).filter(Boolean);
    const codes = normalized.map(row => row.first).filter(Boolean);
    const duplicate = assets && (new Set(serials).size !== serials.length || new Set(codes).size !== codes.length);
    const validRows = (!assets || normalized.length > 0) && normalized.length <= 1000 && normalized.every(row => assets
        ? row.first.length > 0 && row.first.length <= 255 && row.second.length > 0 && row.second.length <= 255
        : whole.test(row.first) && whole.test(row.second));
    const validLoose = /^(0|[1-9][0-9]{0,14})$/.test(looseUnits);
    const total = assets ? String(rows.length) : validRows && validLoose
        ? normalized.reduce((sum, row) => sum + BigInt(row.first) * BigInt(row.second), BigInt(looseUnits)).toString() : "0";
    const valid = validRows && !duplicate && (assets || validLoose && total !== "0" && total.length <= 15);

    function update(index: number, column: keyof Pair, value: string) {
        setResults([]);
        setRows(current => current.map((row, i) => i === index ? { ...row, [column]: value } : row));
    }
    function importPairs() {
        const lines = paste.trim().split(/\r?\n/).filter(line => line.trim());
        const parsed = lines.map(line => line.split(/\t|;/));
        if (!paste.trim() || parsed.some(row => row.length !== 2 || row.some(cell => !cell.trim()))) {
            setError("Paste two columns per line: asset code and serial number, separated by a tab or semicolon."); return;
        }
        const existing = rows.filter(row => row.first.trim() || row.second.trim());
        if (existing.length + parsed.length > 1000) { setError("An entry can contain at most 1000 assets."); return; }
        setRows([...existing, ...parsed.map(([first, second]) => ({ first: first.trim(), second: second.trim() }))]);
        setPaste(""); setError(""); setResults([]);
    }
    async function submit(event: React.FormEvent) {
        event.preventDefault();
        if (saving.current || !valid || completed !== null) return;
        const common = Object.fromEntries(fields.map(field => [field.name, values[field.name] ?? ""]));
        const payload = pending ?? (assets
            ? { requestId: crypto.randomUUID(), common, items: normalized.map(row => ({ assetCode: row.first, serialNumber: row.second })) }
            : { requestId: crypto.randomUUID(), ...common, looseUnits, boxes: normalized.map(row => ({ boxes: row.first, roundsPerBox: row.second })) });
        const review = assets && !reviewed && !pending;
        saving.current = true; setBusy(true); if (!review) setPending(payload); setError("");
        try {
            const result = await httpClient.post<BatchResult>(`/api/erp/inventory/${assets ? review ? "assets/batch/review" : "assets/batch" : "lots/from-boxes"}`, payload);
            setResults(result.data.rows ?? []);
            if (review) { setReviewed(true); return; }
            if (assets) { setPending(null); setReviewed(false); setCompleted(result.data.quantity); return; }
            onSaved(result.data.quantity);
        } catch (failure) {
            if (axios.isAxiosError(failure) && Array.isArray(failure.response?.data?.rows)) {
                setResults(failure.response.data.rows); setReviewed(false);
            }
            const status = axios.isAxiosError(failure) ? failure.response?.status : undefined;
            if (!pending && status && status >= 400 && status < 500) { setPending(null); setReviewed(false); }
            setError(axios.isAxiosError(failure) && typeof failure.response?.data?.detail === "string"
                ? failure.response.data.detail : "The entry result could not be confirmed. Retry to recover the same entry.");
        } finally { saving.current = false; setBusy(false); }
    }
    React.useEffect(() => {
        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape" && !locked) onCancel();
        };
        document.addEventListener("keydown", onKeyDown);
        return () => document.removeEventListener("keydown", onKeyDown);
    }, [locked, onCancel]);

    return <div className="comandos-dialog-layer">
        <button
            type="button"
            className="comandos-dialog-backdrop"
            aria-label="Close dialog"
            disabled={locked}
            onClick={() => { if (!locked) onCancel(); }}
        />
        <div role="dialog" aria-modal="true" className={`comandos-native-dialog ${styles.dialog}`}>
            <div className="comandos-native-dialog-header">
                <h2>{assets ? "Register individual assets" : "Receive ammunition boxes"}</h2>
            </div>
            <div className="comandos-native-dialog-content"><form data-comandos-erp-form="true" className={styles.form} onSubmit={submit}>
                <p>{assets ? "Choose the common model and location, then enter one asset code / serial number pair per unit."
                    : "Choose the ammunition model and lot. Enter boxes, loose rounds or both. Remove box rows to receive only loose rounds."}</p>
                {assets && <Message type="info" text="Atomic batch: all rows are saved together. If any row is rejected, no assets are created. Review the pairs before confirming." />}
                {reviewed && <Message type="info" text="All rows validated. Check the model, location and pairs below, then confirm registration." />}
                {!assets && <Message type="info" text="This entry creates one aggregate lot balance in rounds. Box quantities are recorded as opening packaging only; individual boxes do not have identifiers or separate balances. Use this entry only for aggregate stock." />}
                {error && <Message type="error" text={error} />}
                {completed !== null && <Message type="success" text={`${completed} individual assets registered successfully.`} />}
                {pending && !busy && <Message type="warn" text="Retry this entry to confirm its result without registering the stock twice." />}
                <fieldset className={styles.fields} disabled={locked}>
                    {fields.map(field => <div className={styles.field} data-comandos-field="true" key={field.name}>
                        {field.type !== "reference" && field.type !== "choice" && (
                            <label htmlFor={`core-${field.name}`}>{field.label}{field.required ? " *" : ""}</label>
                        )}
                        {field.type === "reference" ? <ReferenceSelectField
                            service={service}
                            field={field}
                            label={field.label}
                            required={field.required}
                            value={values[field.name] ?? null}
                            organizationId={null}
                            modelFamily={!assets && field.name === "modelId" ? "AMMUNITION" : undefined}
                            onChange={value => setValues(current => ({ ...current, [field.name]: value }))}
                        />
                            : field.type === "choice" ? <ComandosSelectField
                                id={`core-${field.name}`}
                                label={field.label}
                                required={field.required}
                                value={String(values[field.name] ?? "")}
                                options={field.choices
                                    .filter(choice => field.name !== "status" || ["DRAFT", "AVAILABLE", "BLOCKED"].includes(choice))
                                    .map(choice => ({
                                        value: choice,
                                        label: choice.replaceAll("_", " ")
                                    }))}
                                onChange={value => setValues(current => ({ ...current, [field.name]: value }))}
                            />
                                : <input id={`core-${field.name}`} required={field.required} type={field.type === "decimal" ? "number" : field.type}
                                    min={field.type === "decimal" ? "0" : undefined} step={field.type === "decimal" ? "0.0001" : undefined}
                                    maxLength={255} value={String(values[field.name] ?? "")}
                                    onChange={event => setValues(current => ({ ...current, [field.name]: event.target.value }))} />}
                    </div>)}
                </fieldset>
                <fieldset disabled={locked}>
                    {assets && <details><summary>Paste a list from a spreadsheet</summary>
                        <label htmlFor="asset-pairs-paste">Asset code and serial number (two columns, no header)</label>
                        <textarea id="asset-pairs-paste" rows={5} value={paste} onChange={event => setPaste(event.target.value)} />
                        <button type="button" className="registration-yellow-button" onClick={importPairs}>Add pasted rows</button></details>}
                    <div className={styles.tableContainer}><table><caption>{assets ? "Asset code / serial number pairs" : "Boxes and rounds"}</caption>
                        <thead><tr><th>#</th><th>{assets ? "Asset code" : "Number of boxes"}</th><th>{assets ? "Serial number" : "Rounds per box"}</th><th className={styles.actionCell}>Actions</th>{assets && <th>Validation</th>}</tr></thead>
                        <tbody>{rows.map((row, index) => <tr key={index}><td>{index + 1}</td>
                            {(["first", "second"] as const).map(column => <td key={column}><input required
                                aria-label={`${assets ? column === "first" ? "Asset code" : "Serial number" : column === "first" ? "Number of boxes" : "Rounds per box"} ${index + 1}`}
                                type="text" inputMode={assets ? "text" : "numeric"} maxLength={assets ? 255 : 15}
                                value={row[column]} onChange={event => update(index, column, event.target.value)} /></td>)}
                            <td className={styles.actionCell}><button type="button" className="registration-yellow-button" aria-label={`Remove row ${index + 1}`}
                                onClick={() => { setResults([]); setRows(current => current.filter((_, i) => i !== index)); }}>Remove</button></td>{assets && <td aria-live="polite">{results[index]?.errors.length ? results[index].errors.join(" ") : normalized[index].first === "" || normalized[index].second === "" ? "Asset code and serial number are required." : normalized.some((other, i) => i !== index && (other.first === normalized[index].first || other.second === normalized[index].second)) ? "Duplicate asset code or serial number." : results[index]?.status === "ACCEPTED" ? "Accepted; saved" : results[index]?.status === "VALID" ? "Valid; not saved yet" : ""}</td>}</tr>)}</tbody></table></div>
                    <button type="button" className="registration-yellow-button" disabled={rows.length >= 1000} onClick={() => setRows(current => [...current, emptyRow()])}>Add row</button>
                    {!assets && <div className={styles.field}><label htmlFor="intake-loose-units">Loose rounds (without a box)</label>
                        <input id="intake-loose-units" type="text" inputMode="numeric" maxLength={15} required value={looseUnits}
                            onChange={event => setLooseUnits(event.target.value)} /></div>}
                </fieldset>
                <p role="status">{assets ? "Quantity (serial numbers)" : "Total rounds"}: <strong>{total}</strong></p>
                {duplicate && <Message type="error" text="Each asset code and serial number must be unique in this list." />}
                {!assets && (!validRows || total.length > 15) && <small>Enter positive whole quantities; the total can contain at most 15 digits.</small>}
                {reviewed && !pending && <button type="button" className="registration-yellow-button" disabled={busy} onClick={() => { setReviewed(false); setResults([]); }}>Edit batch</button>}
                {completed !== null ? <button type="button" className="registration-yellow-button" onClick={() => onSaved(completed)}>Done</button> : <div className={styles.actions}><button type="button" className="registration-yellow-button" disabled={locked} onClick={onCancel}>Cancel</button>
                    <button type="submit" className="registration-yellow-button" disabled={busy || !valid}>
                        {busy ? "Processing..." : pending ? "Retry entry" : assets ? reviewed ? "Confirm registration" : "Register assets" : "Receive boxes"}</button></div>}
            </form></div>
        </div>
    </div>;
}
