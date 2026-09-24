"use client"

import * as React from "react"
import axios from "axios"
import { Eye } from "lucide-react"

import { Layout } from "components/layout"
import { Message } from "components/common/message"
import { useSession } from "components/auth/session-provider"
import { Pagination } from "platform/components/pagination"
import {
    auditService,
    type AuditDetail,
    type AuditFilters,
    type AuditPage
} from "api/services/audit.service"
import styles from "../shared/workspace.module.css"

const EMPTY_FILTERS: AuditFilters = {
    resource: "",
    recordId: "",
    action: "",
    actor: "",
    from: "",
    until: ""
}

const failure = (error: unknown): string =>
    axios.isAxiosError(error) &&
    typeof error.response?.data?.detail === "string"
        ? error.response.data.detail
        : "Unable to load audit records. Check the connection and try again."

export function AuditWorkspace(): React.JSX.Element {
    const { can } = useSession()

    return (
        <Layout title="Audit history">
            {can("audit", "READ")
                ? <AuditHistory />
                : <Message type="info" text="Your profile cannot view audit history." />
            }
        </Layout>
    )
}

function AuditHistory(): React.JSX.Element {
    const [filters, setFilters] = React.useState<AuditFilters>(EMPTY_FILTERS)
    const [page, setPage] = React.useState(0)
    const rows = 10
    const [revision, setRevision] = React.useState(0)
    const [result, setResult] = React.useState<AuditPage | null>(null)
    const [loading, setLoading] = React.useState(true)
    const [selected, setSelected] = React.useState<number | null>(null)
    const [detail, setDetail] = React.useState<AuditDetail | null>(null)
    const [error, setError] = React.useState("")
    const [detailError, setDetailError] = React.useState("")

    React.useEffect(() => {
        const controller = new AbortController()
        const timer = setTimeout(() => {
            setLoading(true)
            auditService.list(filters, page, controller.signal, rows)
                .then(data => {
                    if (controller.signal.aborted) return
                    setResult(data)
                    setError("")
                })
                .catch((requestError: unknown) => {
                    if (!controller.signal.aborted) setError(failure(requestError))
                })
                .finally(() => {
                    if (!controller.signal.aborted) setLoading(false)
                })
        }, 250)

        return () => {
            clearTimeout(timer)
            controller.abort()
        }
    }, [filters, page, revision])

    React.useEffect(() => {
        if (selected === null) return
        const controller = new AbortController()

        auditService.get(selected, controller.signal)
            .then(data => {
                if (!controller.signal.aborted) {
                    setDetail(data)
                    setDetailError("")
                }
            })
            .catch((requestError: unknown) => {
                if (!controller.signal.aborted) setDetailError(failure(requestError))
            })

        return () => controller.abort()
    }, [selected, revision])

    const changeFilter = (field: keyof AuditFilters, value: string) => {
        setFilters(current => ({ ...current, [field]: field === "action" ? value.toUpperCase() : value }))
        setPage(0)
        setResult(null)
        setSelected(null)
        setDetail(null)
        setError("")
    }

    const clearFilters = () => {
        setFilters(EMPTY_FILTERS)
        setPage(0)
        setResult(null)
        setError("")
        setSelected(null)
        setDetail(null)
    }

    const refresh = () => {
        setResult(null)
        setError("")
        setDetailError("")
        setLoading(true)
        setRevision(value => value + 1)
    }

    const total = result?.totalElements ?? 0

    return (
        <div className={styles.workspace}>
            <p className={styles.intro}>
                Review recorded changes to ERP records and finalized sales.
                Audit entries cannot be edited or deleted here.
            </p>

            <div className={styles.toolbar}>
                <div className={styles.fields}>
                    <div className={styles.field}>
                        <label htmlFor="audit-from">From (local time)</label>
                        <input
                            id="audit-from"
                            className="comandos-input"
                            type="datetime-local"
                            value={filters.from}
                            onChange={event => changeFilter("from", event.target.value)}
                        />
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="audit-until">Until (local time)</label>
                        <input
                            id="audit-until"
                            className="comandos-input"
                            type="datetime-local"
                            value={filters.until}
                            onChange={event => changeFilter("until", event.target.value)}
                        />
                    </div>
                </div>

                <div className="comandos-audit-actions">
                    <button type="button" className="comandos-secondary-button" onClick={clearFilters}>
                        Clear filters
                    </button>
                    <button type="button" className="comandos-secondary-button" onClick={refresh} disabled={loading}>
                        Refresh
                    </button>
                </div>
            </div>

            {error && <Message type="error" text={error} />}

            <div className="comandos-native-table-container">
                <table className="comandos-native-table comandos-audit-table">
                    <thead>
                        <tr>
                            <th>Event</th>
                            <th>Time</th>
                            <th>Account</th>
                            <th>Operation</th>
                            <th>Resource</th>
                            <th>Record</th>
                            <th>Details</th>
                        </tr>
                        <tr className="comandos-filter-row">
                            <th />
                            <th />
                            <th><input className="comandos-input" placeholder="Search account..." value={filters.actor} onChange={e=>changeFilter("actor",e.target.value)} /></th>
                            <th><input className="comandos-input" placeholder="Search operation..." value={filters.action} onChange={e=>changeFilter("action",e.target.value)} /></th>
                            <th><input className="comandos-input" placeholder="Search resource..." value={filters.resource} onChange={e=>changeFilter("resource",e.target.value)} /></th>
                            <th><input className="comandos-input" inputMode="numeric" placeholder="Search ID..." value={filters.recordId} onChange={e=>changeFilter("recordId",e.target.value.replace(/\D/g,""))} /></th>
                            <th />
                        </tr>
                    </thead>
                    <tbody>
                        {(result?.content ?? []).map(event => (
                            <tr key={event.id}>
                                <td>{event.id}</td>
                                <td>{new Date(event.occurredAt).toLocaleString()}</td>
                                <td>{event.actorLogin || "Unauthenticated"}</td>
                                <td>{event.action}</td>
                                <td>{event.resource}</td>
                                <td>{event.recordId}</td>
                                <td className="text-center">
                                    <button
                                        type="button"
                                        className="comandos-icon-button"
                                        data-comandos-table-action="view"
                                        aria-label={`Ver evento de auditoria ${event.id}`}
                                        title="Ver detalhes"
                                        onClick={() => {
                                            if (selected !== event.id) {
                                                setDetail(null)
                                                setDetailError("")
                                                setSelected(event.id)
                                            }
                                        }}
                                    >
                                        <Eye size={18} />
                                    </button>
                                </td>
                            </tr>
                        ))}
                        {!loading && !error && (result?.content.length ?? 0) === 0 && (
                            <tr><td colSpan={7} className="text-center">No audit records match these filters.</td></tr>
                        )}
                    </tbody>
                </table>
            </div>

            <Pagination
                page={page}
                totalElements={total}
                pageSize={rows}
                onPageChange={(nextPage) => {
                    setLoading(true)
                    setResult(null)
                    setPage(nextPage)
                }}
                labels={{
                    totalRecords: "registros",
                    first: "Primeira página",
                    previous: "Página anterior",
                    next: "Próxima página",
                    last: "Última página"
                }}
            />

            {loading && <p role="status">Loading audit history…</p>}

            {selected !== null && (
                <section className="mt-6" aria-label="Audit event details">
                    <div className={styles.toolbar}>
                        <h2>Audit event #{selected}</h2>
                        <button
                            type="button"
                            className="comandos-secondary-button"
                            onClick={() => {
                                setSelected(null)
                                setDetail(null)
                            }}
                        >
                            Close details
                        </button>
                    </div>

                    {detailError && <Message type="error" text={detailError} />}
                    {!detail && !detailError && <p role="status">Loading event details…</p>}

                    {detail && (
                        <div className={styles.fields}>
                            {(["before", "after"] as const).map(key => (
                                <div key={key} className={styles.field}>
                                    <h3>{key === "before" ? "Before" : "After"}</h3>
                                    <pre className="comandos-audit-json">
                                        {detail[key] === null
                                            ? "No snapshot"
                                            : JSON.stringify(detail[key], null, 2)}
                                    </pre>
                                </div>
                            ))}
                        </div>
                    )}
                </section>
            )}
        </div>
    )
}
