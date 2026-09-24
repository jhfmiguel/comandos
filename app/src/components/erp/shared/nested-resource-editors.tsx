"use client";

import * as React from "react";
import { Pencil, Plus } from "lucide-react";
import { Trash } from "@primeicons/react";

import { Message } from "components/common/message";
import { ComandosSelectField, type ComandosSelectOption } from "components/common/select-field";
import type { ErpField, ErpRecord, ErpValue } from "api/models/erp";
import type { ErpService } from "api/services/erp.service";
import styles from "./workspace.module.css";

function errorMessage(error: unknown): string {
    if (typeof error === "object" && error !== null && "response" in error) {
        const response = (error as { response?: { data?: { detail?: unknown } } }).response;
        if (typeof response?.data?.detail === "string") return response.data.detail;
    }
    return "Não foi possível concluir a operação.";
}

function labelOf(record: ErpRecord): string {
    return String(record.label ?? ("#" + record.id));
}

function useResourceOptions(
    service: ErpService,
    resource: string,
    organizationId?: ErpValue,
    filters: Record<string, string> = {}
) {
    const filterKey = JSON.stringify(filters);
    const [options, setOptions] = React.useState<ErpRecord[]>([]);
    const [error, setError] = React.useState("");

    React.useEffect(() => {
        const controller = new AbortController();

        service.list(
            resource,
            "",
            0,
            controller.signal,
            organizationId,
            filters,
            100
        ).then(result => {
            if (!controller.signal.aborted) {
                setOptions(result.content);
                setError("");
            }
        }).catch(error => {
            if (!controller.signal.aborted) setError(errorMessage(error));
        });

        return () => controller.abort();
    }, [service, resource, organizationId, filterKey]);

    return { options, error };
}

function toOptions(records: ErpRecord[]): ComandosSelectOption[] {
    return records.map(record => ({
        value: String(record.id),
        label: labelOf(record)
    }));
}

export function EquipmentSetComponentsEditor({
    equipmentSet,
    service
}: {
    equipmentSet: ErpRecord;
    service: ErpService;
}) {
    const organizationId = equipmentSet.organizationId as ErpValue;
    const [items, setItems] = React.useState<ErpRecord[]>([]);
    const [revision, setRevision] = React.useState(0);
    const [loading, setLoading] = React.useState(true);
    const [busy, setBusy] = React.useState(false);
    const [error, setError] = React.useState("");
    const [editing, setEditing] = React.useState<ErpRecord | null>(null);
    const [adding, setAdding] = React.useState(false);
    const [targetKind, setTargetKind] = React.useState<"asset" | "balance">("asset");
    const [targetId, setTargetId] = React.useState("");
    const [role, setRole] = React.useState("");
    const [quantity, setQuantity] = React.useState("1");

    const assets = useResourceOptions(service, "assets", organizationId);
    const balances = useResourceOptions(service, "balances", organizationId);
    const editable = equipmentSet.active !== true;

    React.useEffect(() => {
        const controller = new AbortController();
        setLoading(true);

        service.list(
            "equipment-set-components",
            "",
            0,
            controller.signal,
            organizationId,
            { equipmentSetId: String(equipmentSet.id) },
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
    }, [equipmentSet.id, organizationId, revision, service]);

    const reset = () => {
        setEditing(null);
        setAdding(false);
        setTargetKind("asset");
        setTargetId("");
        setRole("");
        setQuantity("1");
        setError("");
    };

    const edit = (item: ErpRecord) => {
        const asset = Boolean(item.assetId);
        setEditing(item);
        setAdding(true);
        setTargetKind(asset ? "asset" : "balance");
        setTargetId(String(asset ? item.assetId ?? "" : item.balanceId ?? ""));
        setRole(String(item.role ?? ""));
        setQuantity(String(item.quantity ?? "1"));
        setError("");
    };

    const save = async () => {
        if (!editable || busy || !targetId || !role.trim() || !quantity.trim()) return;

        setBusy(true);
        setError("");

        try {
            await service.save(
                "equipment-set-components",
                {
                    equipmentSetId: equipmentSet.id,
                    assetId: targetKind === "asset" ? Number(targetId) : null,
                    balanceId: targetKind === "balance" ? Number(targetId) : null,
                    role: role.trim(),
                    quantity,
                    ...(editing ? { version: editing.version } : {})
                },
                editing?.id
            );
            reset();
            setRevision(value => value + 1);
        } catch (error) {
            setError(errorMessage(error));
        } finally {
            setBusy(false);
        }
    };

    const remove = async (item: ErpRecord) => {
        if (!editable || busy) return;
        setBusy(true);
        setError("");

        try {
            await service.remove("equipment-set-components", item);
            if (editing?.id === item.id) reset();
            setRevision(value => value + 1);
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
                    <h3>Componentes do conjunto</h3>
                    <small>Inclua bens individuais ou saldos de estoque que compõem este conjunto.</small>
                </div>
                <button
                    type="button"
                    className="registration-yellow-button"
                    disabled={busy || !editable}
                    onClick={() => {
                        reset();
                        setAdding(true);
                    }}
                >
                    <Plus size={16} />
                    Adicionar componente
                </button>
            </div>

            {equipmentSet.active === true && (
                <Message
                    type="info"
                    text="Desative o conjunto para alterar seus componentes. Conjuntos ativos permanecem protegidos contra mudanças estruturais."
                />
            )}
            {(error || assets.error || balances.error) && (
                <Message type="error" text={error || assets.error || balances.error} onClose={() => setError("")} />
            )}

            {adding && editable && (
                <div className={styles.nestedCollectionEditor}>
                    <ComandosSelectField
                        id="equipment-set-component-kind"
                        label="Origem do componente"
                        value={targetKind}
                        options={[
                            { value: "asset", label: "Bem individual" },
                            { value: "balance", label: "Saldo de estoque" }
                        ]}
                        required
                        onChange={value => {
                            setTargetKind(value === "balance" ? "balance" : "asset");
                            setTargetId("");
                        }}
                    />

                    <ComandosSelectField
                        id="equipment-set-component-target"
                        label={targetKind === "asset" ? "Bem individual" : "Saldo de estoque"}
                        value={targetId}
                        options={toOptions(targetKind === "asset" ? assets.options : balances.options)}
                        required
                        disabled={Boolean(editing)}
                        onChange={setTargetId}
                    />

                    <div className={styles.field}>
                        <label htmlFor="equipment-set-component-role">Função no conjunto *</label>
                        <input
                            id="equipment-set-component-role"
                            value={role}
                            maxLength={255}
                            onChange={event => setRole(event.target.value)}
                            placeholder="Ex.: Armamento principal"
                        />
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="equipment-set-component-quantity">Quantidade *</label>
                        <input
                            id="equipment-set-component-quantity"
                            type="number"
                            min="0.0001"
                            step="0.0001"
                            value={quantity}
                            onChange={event => setQuantity(event.target.value)}
                        />
                    </div>

                    <div className={styles.nestedCollectionActions}>
                        <button type="button" className="comandos-secondary-button" disabled={busy} onClick={reset}>
                            Cancelar
                        </button>
                        <button
                            type="button"
                            className="registration-yellow-button"
                            disabled={busy || !targetId || !role.trim() || !quantity.trim()}
                            onClick={() => void save()}
                        >
                            {busy ? "Salvando..." : editing ? "Salvar componente" : "Adicionar ao conjunto"}
                        </button>
                    </div>
                </div>
            )}

            <div className={styles.tableContainer} aria-busy={loading}>
                <table>
                    <thead>
                        <tr>
                            <th>Tipo</th>
                            <th>Componente</th>
                            <th>Função</th>
                            <th>Quantidade</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody>
                        {!loading && items.length === 0 && (
                            <tr><td colSpan={5}>Nenhum componente incluído neste conjunto.</td></tr>
                        )}
                        {items.map(item => {
                            const isAsset = Boolean(item.assetId);
                            const referenceName = isAsset ? "assetId" : "balanceId";
                            const referenceValue = isAsset ? item.assetId : item.balanceId;

                            return (
                                <tr key={item.id}>
                                    <td>{isAsset ? "Bem individual" : "Saldo de estoque"}</td>
                                    <td>{item.referenceLabels[referenceName] || ("#" + String(referenceValue ?? ""))}</td>
                                    <td>{String(item.role ?? "—")}</td>
                                    <td>{String(item.quantity ?? "—")}</td>
                                    <td className={styles.actionCell}>
                                        <div className={[styles.actions, styles.tableActions].join(" ")}>
                                            <button
                                                type="button"
                                                className="comandos-icon-button comandos-icon-button-edit"
                                                aria-label="Editar componente"
                                                title="Editar"
                                                disabled={busy || !editable}
                                                onClick={() => edit(item)}
                                            >
                                                <Pencil size={18} />
                                            </button>
                                            <button
                                                type="button"
                                                className="comandos-icon-button comandos-icon-button-danger"
                                                data-comandos-table-action="delete"
                                                aria-label="Remover componente"
                                                title="Remover"
                                                disabled={busy || !editable}
                                                onClick={() => void remove(item)}
                                            >
                                                <Trash />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </section>
    );
}

export function CategoryCharacteristicsEditor({
    category,
    service
}: {
    category: ErpRecord;
    service: ErpService;
}) {
    const [items, setItems] = React.useState<ErpRecord[]>([]);
    const [revision, setRevision] = React.useState(0);
    const [loading, setLoading] = React.useState(true);
    const [busy, setBusy] = React.useState(false);
    const [error, setError] = React.useState("");
    const [editing, setEditing] = React.useState<ErpRecord | null>(null);
    const [adding, setAdding] = React.useState(false);
    const [characteristicId, setCharacteristicId] = React.useState("");
    const [requiredValue, setRequiredValue] = React.useState(false);
    const [perItem, setPerItem] = React.useState(false);

    const characteristics = useResourceOptions(service, "characteristics");

    React.useEffect(() => {
        const controller = new AbortController();
        setLoading(true);

        service.list(
            "category-characteristics",
            "",
            0,
            controller.signal,
            undefined,
            { categoryId: String(category.id) },
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
    }, [category.id, revision, service]);

    const reset = () => {
        setEditing(null);
        setAdding(false);
        setCharacteristicId("");
        setRequiredValue(false);
        setPerItem(false);
        setError("");
    };

    const edit = (item: ErpRecord) => {
        setEditing(item);
        setAdding(true);
        setCharacteristicId(String(item.characteristicId ?? ""));
        setRequiredValue(Boolean(item.requiredValue));
        setPerItem(Boolean(item.perItem));
        setError("");
    };

    const save = async () => {
        if (busy || !characteristicId) return;
        setBusy(true);
        setError("");

        try {
            await service.save(
                "category-characteristics",
                {
                    categoryId: category.id,
                    characteristicId: Number(characteristicId),
                    requiredValue,
                    perItem,
                    ...(editing ? { version: editing.version } : {})
                },
                editing?.id
            );
            reset();
            setRevision(value => value + 1);
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
            await service.remove("category-characteristics", item);
            if (editing?.id === item.id) reset();
            setRevision(value => value + 1);
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
                    <h3>Características aplicáveis</h3>
                    <small>Defina quais características técnicas pertencem à categoria e onde seus valores são informados.</small>
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
                    Adicionar característica
                </button>
            </div>

            {(error || characteristics.error) && (
                <Message type="error" text={error || characteristics.error} onClose={() => setError("")} />
            )}

            {adding && (
                <div className={styles.nestedCollectionEditor}>
                    <ComandosSelectField
                        id="category-characteristic"
                        label="Característica"
                        value={characteristicId}
                        options={toOptions(characteristics.options)}
                        required
                        disabled={Boolean(editing)}
                        onChange={setCharacteristicId}
                    />

                    <label className={styles.nestedBooleanField}>
                        <span>Obrigatória</span>
                        <input
                            type="checkbox"
                            checked={requiredValue}
                            onChange={event => setRequiredValue(event.target.checked)}
                        />
                    </label>

                    <label className={styles.nestedBooleanField}>
                        <span>Valor por bem individual</span>
                        <input
                            type="checkbox"
                            checked={perItem}
                            disabled={category.serialized !== true}
                            onChange={event => setPerItem(event.target.checked)}
                        />
                    </label>

                    <div className={styles.nestedCollectionActions}>
                        <button type="button" className="comandos-secondary-button" disabled={busy} onClick={reset}>
                            Cancelar
                        </button>
                        <button
                            type="button"
                            className="registration-yellow-button"
                            disabled={busy || !characteristicId}
                            onClick={() => void save()}
                        >
                            {busy ? "Salvando..." : editing ? "Salvar característica" : "Adicionar à categoria"}
                        </button>
                    </div>
                </div>
            )}

            <div className={styles.tableContainer} aria-busy={loading}>
                <table>
                    <thead>
                        <tr>
                            <th>Característica</th>
                            <th>Obrigatória</th>
                            <th>Valor informado em</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody>
                        {!loading && items.length === 0 && (
                            <tr><td colSpan={4}>Nenhuma característica foi vinculada a esta categoria.</td></tr>
                        )}
                        {items.map(item => (
                            <tr key={item.id}>
                                <td>{item.referenceLabels.characteristicId || ("#" + String(item.characteristicId ?? ""))}</td>
                                <td>{item.requiredValue ? "Sim" : "Não"}</td>
                                <td>{item.perItem ? "Bem individual" : "Modelo"}</td>
                                <td className={styles.actionCell}>
                                    <div className={[styles.actions, styles.tableActions].join(" ")}>
                                        <button
                                            type="button"
                                            className="comandos-icon-button comandos-icon-button-edit"
                                            aria-label="Editar característica da categoria"
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
                                            aria-label="Remover característica da categoria"
                                            title="Remover"
                                            disabled={busy}
                                            onClick={() => void remove(item)}
                                        >
                                            <Trash />
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

export function CharacteristicValuesEditor({
    owner,
    ownerType,
    service
}: {
    owner: ErpRecord;
    ownerType: "model" | "asset";
    service: ErpService;
}) {
    const [items, setItems] = React.useState<ErpRecord[]>([]);
    const [bindings, setBindings] = React.useState<ErpRecord[]>([]);
    const [revision, setRevision] = React.useState(0);
    const [loading, setLoading] = React.useState(true);
    const [busy, setBusy] = React.useState(false);
    const [error, setError] = React.useState("");
    const [editing, setEditing] = React.useState<ErpRecord | null>(null);
    const [adding, setAdding] = React.useState(false);
    const [characteristicId, setCharacteristicId] = React.useState("");
    const [value, setValue] = React.useState("");

    const childResource = ownerType === "model" ? "model-values" : "item-values";
    const parentField = ownerType === "model" ? "modelId" : "assetId";

    React.useEffect(() => {
        const controller = new AbortController();
        setLoading(true);

        const load = async () => {
            let categoryId = ownerType === "model" ? owner.categoryId : null;

            if (ownerType === "asset") {
                const modelId = owner.modelId;
                if (!modelId) {
                    setBindings([]);
                    return;
                }

                const models = await service.list(
                    "models",
                    "",
                    0,
                    controller.signal,
                    undefined,
                    { id: String(modelId) },
                    100
                );
                categoryId = models.content[0]?.categoryId ?? null;
            }

            const [valuesResult, bindingResult] = await Promise.all([
                service.list(
                    childResource,
                    "",
                    0,
                    controller.signal,
                    undefined,
                    { [parentField]: String(owner.id) },
                    100
                ),
                categoryId
                    ? service.list(
                        "category-characteristics",
                        "",
                        0,
                        controller.signal,
                        undefined,
                        { categoryId: String(categoryId) },
                        100
                    )
                    : Promise.resolve({ content: [], totalElements: 0, page: 0, size: 100 })
            ]);

            if (controller.signal.aborted) return;

            setItems(valuesResult.content);
            setBindings(
                bindingResult.content.filter(binding =>
                    ownerType === "asset" ? binding.perItem === true : binding.perItem !== true
                )
            );
            setError("");
        };

        load().catch(error => {
            if (!controller.signal.aborted) setError(errorMessage(error));
        }).finally(() => {
            if (!controller.signal.aborted) setLoading(false);
        });

        return () => controller.abort();
    }, [childResource, owner.categoryId, owner.id, owner.modelId, ownerType, parentField, revision, service]);

    const reset = () => {
        setEditing(null);
        setAdding(false);
        setCharacteristicId("");
        setValue("");
        setError("");
    };

    const edit = (item: ErpRecord) => {
        setEditing(item);
        setAdding(true);
        setCharacteristicId(String(item.characteristicId ?? ""));
        setValue(String(item.value ?? ""));
        setError("");
    };

    const save = async () => {
        if (busy || !characteristicId || !value.trim()) return;
        setBusy(true);
        setError("");

        try {
            await service.save(
                childResource,
                {
                    [parentField]: owner.id,
                    characteristicId: Number(characteristicId),
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
            await service.remove(childResource, item);
            if (editing?.id === item.id) reset();
            setRevision(current => current + 1);
        } catch (error) {
            setError(errorMessage(error));
        } finally {
            setBusy(false);
        }
    };

    const availableBindings = bindings.filter(binding =>
        editing?.characteristicId === binding.characteristicId
        || !items.some(item => item.characteristicId === binding.characteristicId)
    );

    const characteristicOptions: ComandosSelectOption[] = availableBindings.map(binding => ({
        value: String(binding.characteristicId ?? ""),
        label: binding.referenceLabels.characteristicId || ("#" + String(binding.characteristicId ?? ""))
    }));

    return (
        <section className={styles.nestedCollectionSection}>
            <div className={styles.contactHeader}>
                <div>
                    <h3>{ownerType === "model" ? "Características do modelo" : "Características do bem"}</h3>
                    <small>
                        {ownerType === "model"
                            ? "Preencha os valores definidos pela categoria para este modelo."
                            : "Preencha apenas as características configuradas para cada bem individual."}
                    </small>
                </div>
                <button
                    type="button"
                    className="registration-yellow-button"
                    disabled={busy || availableBindings.length === 0}
                    onClick={() => {
                        reset();
                        setAdding(true);
                    }}
                >
                    <Plus size={16} />
                    Adicionar valor
                </button>
            </div>

            {error && <Message type="error" text={error} onClose={() => setError("")} />}
            {!loading && bindings.length === 0 && (
                <Message
                    type="info"
                    text={
                        ownerType === "model"
                            ? "A categoria deste modelo não possui características configuradas para preenchimento no modelo."
                            : "A categoria deste bem não possui características configuradas para preenchimento por item."
                    }
                />
            )}

            {adding && (
                <div className={styles.nestedCollectionEditor}>
                    <ComandosSelectField
                        id={ownerType + "-characteristic-value"}
                        label="Característica"
                        value={characteristicId}
                        options={characteristicOptions}
                        required
                        disabled={Boolean(editing)}
                        onChange={setCharacteristicId}
                    />

                    <div className={styles.field}>
                        <label htmlFor={ownerType + "-characteristic-value-input"}>Valor *</label>
                        <input
                            id={ownerType + "-characteristic-value-input"}
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
                            disabled={busy || !characteristicId || !value.trim()}
                            onClick={() => void save()}
                        >
                            {busy ? "Salvando..." : editing ? "Salvar valor" : "Adicionar valor"}
                        </button>
                    </div>
                </div>
            )}

            <div className={styles.tableContainer} aria-busy={loading}>
                <table>
                    <thead>
                        <tr>
                            <th>Característica</th>
                            <th>Valor</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody>
                        {!loading && items.length === 0 && (
                            <tr><td colSpan={3}>Nenhum valor de característica foi informado.</td></tr>
                        )}
                        {items.map(item => (
                            <tr key={item.id}>
                                <td>{item.referenceLabels.characteristicId || ("#" + String(item.characteristicId ?? ""))}</td>
                                <td>{String(item.value ?? "—")}</td>
                                <td className={styles.actionCell}>
                                    <div className={[styles.actions, styles.tableActions].join(" ")}>
                                        <button
                                            type="button"
                                            className="comandos-icon-button comandos-icon-button-edit"
                                            aria-label="Editar valor da característica"
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
                                            aria-label="Remover valor da característica"
                                            title="Remover"
                                            disabled={busy}
                                            onClick={() => void remove(item)}
                                        >
                                            <Trash />
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
