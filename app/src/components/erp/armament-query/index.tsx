"use client";

import * as React from "react";
import axios from "axios";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { httpClient } from "api/http";
import type { ErpPage, ErpRecord } from "api/models/erp";
import type { Custody, CustodyPage } from "api/models/erp/custody";
import { auditService } from "api/services/audit.service";
import { createErpService } from "api/services/erp.service";
import { useSession } from "components/auth/session-provider";
import { Layout } from "components/layout";
import { Message } from "components/common/message";
import styles from "components/erp/shared/workspace.module.css";

const inventory = createErpService("inventory");
const fields = [
    ["assetCode", "Patrimônio"], ["serialNumber", "Número de série"],
    ["modelId", "Modelo (nome, código ou ID)"], ["unit", "Unidade (nome ou código)"],
    ["locationId", "Localização (nome, código ou ID)"]
] as const;
const statuses = ["DRAFT", "AVAILABLE", "BLOCKED", "CUSTODIED", "IN_MAINTENANCE", "SOLD", "DONATED", "DISPOSED"];
const value = (record: ErpRecord, field: string) => record.referenceLabels[field] || String(record[field] ?? "—");

function errorText(error: unknown) {
    if (axios.isAxiosError(error)) {
        if (error.response?.status === 403) return "Sem autorização para consultar estes dados.";
        if (error.response?.status === 401) return "Sua sessão expirou. Entre novamente.";
        if (error.response?.status === 404) return "Armamento não encontrado ou indisponível.";
    }
    return "Não foi possível consultar os dados. Tente novamente.";
}

// Each query is mounted with its URL/access key, so old records never flash on a new scope.
function Remote<T>({ load, children }: { load: (signal: AbortSignal) => Promise<T>; children: (data: T) => React.ReactNode }) {
    const [data, setData] = React.useState<T>();
    const [error, setError] = React.useState("");
    const [retry, setRetry] = React.useState(0);
    React.useEffect(() => {
        const controller = new AbortController();
        load(controller.signal).then(result => {
            if (!controller.signal.aborted) setData(result);
        }).catch(reason => { if (!controller.signal.aborted) setError(errorText(reason)); });
        return () => controller.abort();
    }, [load, retry]);
    if (error) return <><Message type="error" text={error} /><button onClick={() => { setError(""); setRetry(n => n + 1); }}>Tentar novamente</button></>;
    if (data === undefined) return <p role="status">Carregando…</p>;
    return <>{children(data)}</>;
}

function Pagination({ page, size, total, change }: { page: number; size: number; total: number; change: (page: number) => void }) {
    return <nav aria-label="Paginação" className={styles.actions}>
        <button disabled={page === 0} onClick={() => change(page - 1)}>Anterior</button>
        <span>Página {page + 1} · {total} registros</span>
        <button disabled={(page + 1) * size >= total || page >= 100000} onClick={() => change(page + 1)}>Próxima</button>
    </nav>;
}

function History<T>({ title, allowed, load, render }: {
    title: string; allowed: boolean;
    load: (page: number, signal: AbortSignal) => Promise<{ content: T[]; totalElements: number; page: number; size: number }>;
    render: (record: T) => React.ReactNode;
}) {
    const [page, setPage] = React.useState(0);
    const fetchPage = React.useCallback((signal: AbortSignal) => load(page, signal), [load, page]);
    return <section><h3>{title}</h3>{!allowed ? <p>Sem permissão para consultar esta seção.</p> :
        <Remote key={page} load={fetchPage}>{data => <>
            {!data.content.length ? <p>Nenhum registro encontrado.</p> : <ul>{data.content.map((record, index) => <li key={index}>{render(record)}</li>)}</ul>}
            <Pagination page={data.page} size={data.size} total={data.totalElements} change={setPage} />
        </>}</Remote>}
    </section>;
}

function AssetDetail({ asset, allowed }: { asset: ErpRecord; allowed: (resource: string) => boolean }) {
    const custody = React.useCallback((page: number, signal: AbortSignal) =>
        httpClient.get<CustodyPage<Custody>>(`/api/erp/custodies/by-asset/${asset.id}`, { params: { page }, signal }).then(r => r.data), [asset.id]);
    const movements = React.useCallback((page: number, signal: AbortSignal) =>
        httpClient.get<ErpPage>("/api/erp/inventory/movements", { params: { assetId: asset.id, page, size: 10 }, signal }).then(r => r.data), [asset.id]);
    const audit = React.useCallback((page: number, signal: AbortSignal) => auditService.list({
        resource: "inventory/assets", recordId: String(asset.id), action: "", actor: "", from: "", until: ""
    }, page, signal), [asset.id]);
    return <section aria-label="Detalhe do armamento"><h2>Armamento {value(asset, "assetCode")}</h2>
        <dl>{[["serialNumber", "Série"], ["modelId", "Modelo"], ["status", "Status"], ["condition", "Condição"],
            ["organizationId", "Organização"], ["unitId", "Unidade"], ["locationId", "Localização"]].map(([field, label]) =>
            <React.Fragment key={field}><dt>{label}</dt><dd>{value(asset, field)}</dd></React.Fragment>)}</dl>
        <History title="Custódia e cautelas" allowed={allowed("custodies")} load={custody} render={record => <>
            #{record.id} · {record.recipientName} · {record.status} · Entrega: {record.deliveredAt} · Prazo: {record.dueAt || "—"}
            {record.items.filter(item => item.assetId === asset.id).map(item => <p key={item.id}>
                {item.returnedAt ? `Devolvido: ${item.returnedAt} · ${item.returnConditionName || "—"}` : "Em custódia"}
            </p>)}
        </>} />
        <History title="Histórico de movimentações" allowed={allowed("inventory/movements")} load={movements}
            render={record => <>{value(record, "movedAt")} · {value(record, "nature")} · {value(record, "locationId")}</>} />
        <History title="Auditoria" allowed={allowed("audit")} load={audit}
            render={record => <>{record.occurredAt} · {record.action} · {record.actorLogin || record.actorType}</>} />
    </section>;
}

function Query() {
    const params = useSearchParams();
    const router = useRouter();
    const pathname = usePathname();
    const { session } = useSession();
    const query = params.toString();
    const pageValue = Number(params.get("page") || 0);
    const page = Number.isInteger(pageValue) && pageValue >= 0 && pageValue <= 100000 ? pageValue : 0;
    const selected = params.get("asset");
    const assetId = selected && /^\d+$/.test(selected) && Number.isSafeInteger(Number(selected)) && Number(selected) > 0 ? Number(selected) : null;
    const accessKey = JSON.stringify(session?.access);
    const allowed = (resource: string) => !!session && (!session.access?.enforced || session.access.grants.some(grant =>
        (grant.resource === resource || grant.resource === "*") && (grant.action === "READ" || grant.action === "*")));
    const navigate = (next: URLSearchParams) => router.push(`${pathname}?${next}`, { scroll: false });
    const load = React.useCallback((signal: AbortSignal) => {
        const filters = Object.fromEntries([...fields.map(([key]) => key), "status"].map(key => [key, new URLSearchParams(query).get(key) || ""]));
        return inventory.list("assets", "", page, signal, undefined, filters);
    }, [query, page]);
    const detail = React.useCallback((signal: AbortSignal) => httpClient.get<ErpRecord>(`/api/erp/inventory/assets/${assetId}`, { signal }).then(r => r.data), [assetId]);
    return <Layout title="Consulta de armamento"><div className={styles.workspace}>
        <p className={styles.intro}>Consulte patrimônio, série, modelo, status, unidade e localização. Os filtros são combinados no servidor.</p>
        {!allowed("inventory/assets") ? <Message type="info" text="Sem autorização para consultar armamento." /> : <>
            <form key={query} onSubmit={event => {
                event.preventDefault();
                const next = new URLSearchParams();
                new FormData(event.currentTarget).forEach((entry, key) => { if (String(entry).trim()) next.set(key, String(entry).trim()); });
                navigate(next);
            }}><div className={styles.fields}>
                {fields.map(([key, label]) => <div className={styles.field} key={key}><label htmlFor={`query-${key}`}>{label}</label>
                    <input id={`query-${key}`} name={key} defaultValue={params.get(key) || ""} maxLength={255} /></div>)}
                <div className={styles.field}><label htmlFor="query-status">Status</label><select id="query-status" name="status" defaultValue={params.get("status") || ""}>
                    <option value="">Todos</option>{statuses.map(status => <option key={status}>{status}</option>)}
                </select></div>
            </div><div className={styles.actions}><button type="submit">Consultar</button><button type="button" onClick={() => navigate(new URLSearchParams())}>Limpar filtros</button></div></form>
            <Remote key={query + accessKey} load={load}>{data => <>
                {!data.content.length ? <p>Nenhum armamento encontrado.</p> : <div className={styles.tableContainer}><table>
                    <thead><tr>{["Patrimônio", "Série", "Modelo", "Status", "Unidade", "Localização", "Detalhe"].map(label => <th key={label}>{label}</th>)}</tr></thead>
                    <tbody>{data.content.map(record => <tr key={record.id}>
                        {["assetCode", "serialNumber", "modelId", "status", "unitId", "locationId"].map(field => <td key={field}>{value(record, field)}</td>)}
                        <td><button onClick={() => { const next = new URLSearchParams(query); next.set("asset", String(record.id)); navigate(next); }}>Ver detalhe</button></td>
                    </tr>)}</tbody></table></div>}
                <Pagination page={data.page} size={data.size} total={data.totalElements} change={nextPage => { const next = new URLSearchParams(query); next.set("page", String(nextPage)); next.delete("asset"); navigate(next); }} />
            </>}</Remote>
            {selected && !assetId && <Message type="error" text="Identificador de armamento inválido." />}
            {assetId && <Remote key={`${assetId}-${accessKey}`} load={detail}>{asset => <AssetDetail asset={asset} allowed={allowed} />}</Remote>}
        </>}
    </div></Layout>;
}

export function ArmamentQuery() {
    return <React.Suspense fallback={<p role="status">Carregando consulta…</p>}><Query /></React.Suspense>;
}
