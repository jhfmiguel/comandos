"use client";

import * as React from "react";
import axios from "axios";
import { Button } from "@primereact/ui/button";
import { Layout } from "components/layout";
import { Message } from "components/common/message";
import { useSession } from "components/auth/session-provider";
import { auditService, type AuditPage, type AuditDetail, type AuditFilters } from "api/services/audit.service";
import styles from "../shared/workspace.module.css";

const emptyFilters: AuditFilters = { resource: "", recordId: "", action: "", actor: "", from: "", until: "" };
const failure = (error: unknown) => axios.isAxiosError(error) && typeof error.response?.data?.detail === "string"
    ? error.response.data.detail : "Unable to load audit records. Check the connection and try again.";

export function AuditWorkspace() {
    const { can } = useSession();
    return <Layout title="Audit history">{can("audit", "READ") ? <AuditHistory /> : <Message type="info" text="Your profile cannot view audit history." />}</Layout>;
}

function AuditHistory() {
    const [filters, setFilters] = React.useState(emptyFilters);
    const [page, setPage] = React.useState(0);
    const [revision, setRevision] = React.useState(0);
    const [result, setResult] = React.useState<AuditPage | null>(null);
    const [selected, setSelected] = React.useState<number | null>(null);
    const [detail, setDetail] = React.useState<AuditDetail | null>(null);
    const [error, setError] = React.useState("");
    const [detailError, setDetailError] = React.useState("");
    React.useEffect(() => {
        const controller = new AbortController();
        const timer = setTimeout(() => {
            auditService.list(filters, page, controller.signal).then(data => {
                if (!controller.signal.aborted) { setResult(data); setError(""); }
            }).catch(error => { if (!controller.signal.aborted) setError(failure(error)); });
        }, 250);
        return () => { clearTimeout(timer); controller.abort(); };
    }, [filters, page, revision]);
    React.useEffect(() => {
        if (selected === null) return;
        const controller = new AbortController();
        auditService.get(selected, controller.signal).then(data => {
            if (!controller.signal.aborted) setDetail(data);
        }).catch(error => { if (!controller.signal.aborted) setDetailError(failure(error)); });
        return () => controller.abort();
    }, [selected, revision]);
    function changeFilters(next: AuditFilters) {
        setFilters(next); setPage(0); setResult(null); setError(""); setSelected(null); setDetail(null);
    }
    return <div className={styles.workspace}>
        <p className={styles.intro}>Review recorded changes to ERP records and finalized sales. Audit entries cannot be edited or deleted here.</p>
        <div className={styles.fields}>
            <div className={styles.field}><label htmlFor="audit-resource">Resource</label>
                <input id="audit-resource" value={filters.resource} placeholder="For example: core/people or sales" onChange={event => changeFilters({ ...filters, resource: event.target.value })} /></div>
            <div className={styles.field}><label htmlFor="audit-record">Record ID</label>
                <input id="audit-record" type="number" min="1" step="1" value={filters.recordId} onChange={event => changeFilters({ ...filters, recordId: event.target.value })} /></div>
            <div className={styles.field}><label htmlFor="audit-action">Operation</label>
                <select id="audit-action" value={filters.action} onChange={event => changeFilters({ ...filters, action: event.target.value })}>
                    <option value="">All operations</option>{["CREATE", "UPDATE", "DELETE", "FINALIZE"].map(action => <option key={action}>{action}</option>)}
                </select></div>
            <div className={styles.field}><label htmlFor="audit-actor">Account login</label>
                <input id="audit-actor" value={filters.actor} onChange={event => changeFilters({ ...filters, actor: event.target.value })} /></div>
            <div className={styles.field}><label htmlFor="audit-from">From (local time)</label>
                <input id="audit-from" type="datetime-local" value={filters.from} onChange={event => changeFilters({ ...filters, from: event.target.value })} /></div>
            <div className={styles.field}><label htmlFor="audit-until">Until (local time)</label>
                <input id="audit-until" type="datetime-local" value={filters.until} onChange={event => changeFilters({ ...filters, until: event.target.value })} /></div>
        </div>
        <div className={styles.toolbar}>
            <Button type="button" severity="secondary" onClick={() => changeFilters(emptyFilters)}>Clear filters</Button>
            <Button type="button" severity="secondary" onClick={() => { setResult(null); setError(""); setDetailError(""); setRevision(value => value + 1); }}>Refresh</Button>
        </div>
        {error && <Message type="error" text={error} />}
        {!result && !error && <p role="status">Loading audit history…</p>}
        {result && <>
            <div className={styles.tableContainer}><table><thead><tr><th>Event</th><th>Time</th><th>Account</th><th>Operation</th><th>Resource</th><th>Record</th><th>Details</th></tr></thead>
                <tbody>{result.content.map(event => <tr key={event.id}><td>{event.id}</td><td>{new Date(event.occurredAt).toLocaleString()}</td>
                    <td>{event.actorLogin || "Unauthenticated"}</td><td>{event.action}</td><td>{event.resource}</td><td>{event.recordId}</td>
                    <td><Button type="button" variant="text" aria-label={`View audit event ${event.id}`}
                        onClick={() => { if (selected !== event.id) { setDetail(null); setDetailError(""); setSelected(event.id); } }}>View</Button></td></tr>)}
                    {!result.content.length && <tr><td colSpan={7}>No audit records match these filters.</td></tr>}</tbody></table></div>
            <div className={styles.pagination}><span>{result.totalElements} records · Page {page + 1}</span>
                <Button type="button" severity="secondary" disabled={page === 0} onClick={() => { setResult(null); setPage(page - 1); }}>Previous</Button>
                <Button type="button" severity="secondary" disabled={(page + 1) * result.size >= result.totalElements} onClick={() => { setResult(null); setPage(page + 1); }}>Next</Button></div>
        </>}
        {selected !== null && <section className="mt-6" aria-label="Audit event details">
            <div className={styles.toolbar}><h2>Audit event #{selected}</h2><Button type="button" severity="secondary" onClick={() => { setSelected(null); setDetail(null); }}>Close details</Button></div>
            {detailError && <Message type="error" text={detailError} />}
            {!detail && !detailError && <p role="status">Loading event details…</p>}
            {detail && <div className={styles.fields}>{(["before", "after"] as const).map(key => <div key={key} className={styles.field}>
                <h3>{key === "before" ? "Before" : "After"}</h3><pre className="max-h-96 overflow-auto p-3 border-round surface-ground"
                    style={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere" }}>{detail[key] === null ? "No snapshot" : JSON.stringify(detail[key], null, 2)}</pre>
            </div>)}</div>}
        </section>}
    </div>;
}
