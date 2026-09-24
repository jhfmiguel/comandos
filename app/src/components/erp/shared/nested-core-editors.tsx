"use client";

import * as React from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";

import { Message } from "components/common/message";
import type { ErpRecord } from "api/models/erp";
import type { ErpService } from "api/services/erp.service";
import styles from "./workspace.module.css";

function errorMessage(error: unknown): string {
    if (typeof error === "object" && error !== null && "response" in error) {
        const response = (error as { response?: { data?: { detail?: unknown } } }).response;
        if (typeof response?.data?.detail === "string") return response.data.detail;
    }
    return "Não foi possível concluir a operação.";
}

export function RoleDetailsEditor({
    assignment,
    service
}: {
    assignment: ErpRecord;
    service: ErpService;
}) {
    const [items, setItems] = React.useState<ErpRecord[]>([]);
    const [revision, setRevision] = React.useState(0);
    const [loading, setLoading] = React.useState(true);
    const [busy, setBusy] = React.useState(false);
    const [error, setError] = React.useState("");
    const [editing, setEditing] = React.useState<ErpRecord | null>(null);
    const [adding, setAdding] = React.useState(false);
    const [key, setKey] = React.useState("");
    const [value, setValue] = React.useState("");

    React.useEffect(() => {
        const controller = new AbortController();
        setLoading(true);

        service.list(
            "role-data",
            "",
            0,
            controller.signal,
            undefined,
            { personRoleId: String(assignment.id) },
            100
        ).then(result => {
            if (!controller.signal.aborted) {
                setItems(result.content);
                setError("");
            }
        }).catch(error => {
            if (!controller.signal.aborted) setError(errorMessage(error));
        }).finally(() => {
            if (!controller.signal.aborted) setLoading(false);
        });

        return () => controller.abort();
    }, [assignment.id, revision, service]);

    const reset = () => {
        setEditing(null);
        setAdding(false);
        setKey("");
        setValue("");
        setError("");
    };

    const edit = (item: ErpRecord) => {
        setEditing(item);
        setAdding(true);
        setKey(String(item.key ?? ""));
        setValue(String(item.value ?? ""));
        setError("");
    };

    const save = async () => {
        if (busy || !key.trim() || !value.trim()) return;
        setBusy(true);
        setError("");

        try {
            await service.save(
                "role-data",
                {
                    personRoleId: assignment.id,
                    key: key.trim(),
                    value: value.trim(),
                    ...(editing ? { version: editing.version } : {})
                },
                editing?.id
            );
            reset();
            setRevision(current => current + 1);
        } catch (error) {
            setError(errorMessage(error));
        } finally {
            setBusy(false);
        }
    };

    const remove = async (item: ErpRecord) => {
        if (busy) return;
        setBusy(true);
        setError("");

        try {
            await service.remove("role-data", item);
            if (editing?.id === item.id) reset();
            setRevision(current => current + 1);
        } catch (error) {
            setError(errorMessage(error));
        } finally {
            setBusy(false);
        }
    };

    return (
        <section className={styles.nestedCollectionSection}>
            <div className={styles.contactHeader}>
                <div>
                    <h3>Dados do vínculo</h3>
                    <small>Informações adicionais específicas deste vínculo de papel.</small>
                </div>
                <button
                    type="button"
                    className="registration-yellow-button"
                    disabled={busy}
                    onClick={() => {
                        reset();
                        setAdding(true);
                    }}
                >
                    <Plus size={16} />
                    Adicionar dado
                </button>
            </div>

            {error && <Message type="error" text={error} onClose={() => setError("")} />}

            {adding && (
                <div className={styles.nestedCollectionEditor}>
                    <div className={styles.field}>
                        <label htmlFor="role-detail-key">Campo *</label>
                        <input
                            id="role-detail-key"
                            value={key}
                            maxLength={255}
                            disabled={Boolean(editing)}
                            onChange={event => setKey(event.target.value)}
                            placeholder="Ex.: matrícula funcional"
                        />
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="role-detail-value">Valor *</label>
                        <input
                            id="role-detail-value"
                            value={value}
                            maxLength={255}
                            onChange={event => setValue(event.target.value)}
                        />
                    </div>

                    <div className={styles.nestedCollectionActions}>
                        <button type="button" className="comandos-secondary-button" disabled={busy} onClick={reset}>
                            Cancelar
                        </button>
                        <button
                            type="button"
                            className="registration-yellow-button"
                            disabled={busy || !key.trim() || !value.trim()}
                            onClick={() => void save()}
                        >
                            {busy ? "Salvando..." : editing ? "Salvar dado" : "Adicionar dado"}
                        </button>
                    </div>
                </div>
            )}

            <div className={styles.tableContainer} aria-busy={loading}>
                <table>
                    <thead>
                        <tr>
                            <th>Campo</th>
                            <th>Valor</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody>
                        {!loading && items.length === 0 && (
                            <tr><td colSpan={3}>Nenhum dado adicional informado para este vínculo.</td></tr>
                        )}
                        {items.map(item => (
                            <tr key={item.id}>
                                <td>{String(item.key ?? "—")}</td>
                                <td>{String(item.value ?? "—")}</td>
                                <td className={styles.actionCell}>
                                    <div className={[styles.actions, styles.tableActions].join(" ")}>
                                        <button
                                            type="button"
                                            className="comandos-icon-button comandos-icon-button-edit"
                                            aria-label="Editar dado do vínculo"
                                            title="Editar"
                                            disabled={busy}
                                            onClick={() => edit(item)}
                                        >
                                            <Pencil size={18} />
                                        </button>
                                        <button
                                            type="button"
                                            className="comandos-icon-button comandos-icon-button-danger"
                                            data-comandos-table-action="delete"
                                            aria-label="Remover dado do vínculo"
                                            title="Remover"
                                            disabled={busy}
                                            onClick={() => void remove(item)}
                                        >
                                            <Trash2 size={18} />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </section>
    );
}
