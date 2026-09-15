"use client";

import * as React from "react";
import axios from "axios";
import { Button } from "@primereact/ui/button";

import { Layout } from "components/layout";
import { Message } from "components/common/message";
import { useComandosPreferences } from "components/settings/preferences-provider";
import { ReferenceField } from "components/erp/shared/record-workspace";
import { createErpService } from "api/services/erp.service";
import { purchaseService } from "api/services/purchase.service";
import type { ErpField, ErpValue } from "api/models/erp";
import type {
    BiddingModality,
    CreatePurchaseItemRequest,
    DirectContractingType,
    ItemModelOption,
    ProcurementMethod,
    PurchaseView
} from "api/models/erp/purchase";
import styles from "components/erp/shared/workspace.module.css";

const core = createErpService("core");
const inventory = createErpService("inventory");

const reference = (name: string, label: string, resource: string, required = true): ErpField => ({
    name,
    label,
    type: "reference",
    required,
    reference: resource,
    choices: []
});

const organizationField = reference("buyerOrganizationId", "Organization", "organizations");
const supplierField = reference("supplierOrganizationId", "Supplier", "organizations", false);
const itemModelField = reference("itemModelId", "Equipment / item", "item-models");

type PurchaseLine = {
    key: string;
    itemModelId: ErpValue;
    quantity: string;
    unitPrice: string;
    discount: string;
    notes: string;
};

const emptyLine = (): PurchaseLine => ({
    key: crypto.randomUUID(),
    itemModelId: null,
    quantity: "1",
    unitPrice: "0",
    discount: "0",
    notes: ""
});

function failure(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const data = error.response?.data;
        if (typeof data?.message === "string") return data.message;
        if (typeof data?.detail === "string") return data.detail;
    }
    return "Unable to complete the request. Check the API connection and try again.";
}

function money(value: number | string | undefined, locale: "pt-BR" | "en-US"): string {
    const parsed = Number(value ?? 0);
    return new Intl.NumberFormat(locale, {
        style: "currency",
        currency: "BRL"
    }).format(Number.isFinite(parsed) ? parsed : 0);
}

export function PurchasePanel() {
    const [generation, setGeneration] = React.useState(0);
    return <PurchaseForm key={generation} onNew={() => setGeneration(value => value + 1)} />;
}

export function PurchaseWorkspace() {
    return (
        <Layout title="Purchases">
            <PurchasePanel />
        </Layout>
    );
}

function PurchaseForm({ onNew }: { onNew: () => void }) {
    const [organization, setOrganization] = React.useState<ErpValue>(null);
    const [supplier, setSupplier] = React.useState<ErpValue>(null);
    const [purchaseNumber, setPurchaseNumber] = React.useState("");
    const [purchaseDate, setPurchaseDate] = React.useState(() => new Date().toISOString().slice(0, 10));
    const [notes, setNotes] = React.useState("");
    const [lines, setLines] = React.useState<PurchaseLine[]>([emptyLine()]);
    const [completed, setCompleted] = React.useState<PurchaseView | null>(null);
    const [busy, setBusy] = React.useState(false);
    const [error, setError] = React.useState("");
    const [refresh, setRefresh] = React.useState(0);

    const valid = Boolean(
        organization &&
        purchaseNumber.trim() &&
        purchaseDate &&
        lines.length &&
        lines.every(line =>
            line.itemModelId &&
            Number(line.quantity) > 0 &&
            Number(line.unitPrice) >= 0 &&
            Number(line.discount || 0) >= 0
        )
    );

    async function createPurchase(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!valid || busy) return;

        const items: CreatePurchaseItemRequest[] = lines.map(line => ({
            itemModelId: Number(line.itemModelId),
            quantity: Number(line.quantity),
            unitPrice: Number(line.unitPrice),
            discount: Number(line.discount || 0),
            notes: line.notes.trim() || undefined
        }));

        setBusy(true);
        setError("");

        try {
            const result = await purchaseService.create({
                buyerOrganizationId: Number(organization),
                supplierOrganizationId: supplier ? Number(supplier) : undefined,
                purchaseNumber: purchaseNumber.trim(),
                purchaseDate,
                notes: notes.trim() || undefined,
                items
            });
            setCompleted(result);
            setRefresh(value => value + 1);
        } catch (caught) {
            setError(failure(caught));
        } finally {
            setBusy(false);
        }
    }

    return (
        <div className={styles.workspace}>
            <p className={styles.intro}>
                Register equipment and material purchases. Public organizations can continue through
                bidding or direct contracting after the purchase is created.
            </p>

            {error && <Message type="error" text={error} onClose={() => setError("")} />}
            {completed && (
                <Message
                    type="success"
                    text={`Purchase #${completed.id} (${completed.purchaseNumber}) created successfully.`}
                />
            )}

            <form data-comandos-erp-form="true" onSubmit={createPurchase}>
                <fieldset className={styles.fields} disabled={busy || !!completed}>
                    <div className={styles.field}>
                        <label>Buyer organization *</label>
                        <ReferenceField
                            service={core}
                            field={organizationField}
                            value={organization}
                            organizationId={null}
                            onChange={setOrganization}
                        />
                    </div>

                    <div className={styles.field}>
                        <label>Supplier</label>
                        <ReferenceField
                            service={core}
                            field={supplierField}
                            value={supplier}
                            organizationId={null}
                            onChange={setSupplier}
                        />
                        <small>
                            The backend currently represents the supplier as an Organization.
                            Person/supplier roles can be generalized in the next backend evolution.
                        </small>
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="purchase-number">Purchase number *</label>
                        <input
                            id="purchase-number"
                            maxLength={100}
                            required
                            value={purchaseNumber}
                            onChange={event => setPurchaseNumber(event.target.value)}
                        />
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="purchase-date">Purchase date *</label>
                        <input
                            id="purchase-date"
                            type="date"
                            required
                            value={purchaseDate}
                            onChange={event => setPurchaseDate(event.target.value)}
                        />
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="purchase-notes">Notes</label>
                        <textarea
                            id="purchase-notes"
                            rows={3}
                            maxLength={4000}
                            value={notes}
                            onChange={event => setNotes(event.target.value)}
                        />
                    </div>
                </fieldset>

                {!completed && (
                    <PurchaseItems
                        lines={lines}
                        organization={organization}
                        disabled={busy}
                        onChange={setLines}
                    />
                )}

                {completed && <PurchaseSummary purchase={completed} />}

                <div className={styles.actions}>
                    {completed ? (
                        <Button
                            type="button"
                            className="registration-yellow-button"
                            onClick={onNew}
                        >
                            + Purchase
                        </Button>
                    ) : (
                        <Button
                            type="submit"
                            className="registration-yellow-button"
                            disabled={!valid || busy}
                        >
                            {busy ? "Saving…" : "Save purchase"}
                        </Button>
                    )}
                </div>
            </form>

            {completed && completed.buyerOrganizationId && (
                <ProcurementSection
                    purchase={completed}
                    onChange={setCompleted}
                />
            )}

            {organization && (
                <PurchaseHistory
                    organizationId={Number(organization)}
                    refresh={refresh}
                />
            )}
        </div>
    );
}

function PurchaseItems({
    lines,
    organization,
    disabled,
    onChange
}: {
    lines: PurchaseLine[];
    organization: ErpValue;
    disabled: boolean;
    onChange: (lines: PurchaseLine[]) => void;
}) {
    function update(key: string, patch: Partial<PurchaseLine>) {
        onChange(lines.map(line => line.key === key ? { ...line, ...patch } : line));
    }

    return (
        <section>
            <div className={styles.toolbar}>
                <h2>Purchase items</h2>
                <Button
                    type="button"
                    className="registration-yellow-button"
                    disabled={disabled || lines.length >= 100}
                    onClick={() => onChange([...lines, emptyLine()])}
                >
                    + Item
                </Button>
            </div>

            <div className={styles.tableContainer}>
                <table>
                    <thead>
                        <tr>
                            <th>Equipment / item</th>
                            <th>Quantity</th>
                            <th>Unit price</th>
                            <th>Discount</th>
                            <th>Notes</th>
                            <th className={styles.actionCell}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {lines.map(line => (
                            <tr key={line.key}>
                                <td>
                                    <ReferenceField
                                        service={inventory}
                                        field={itemModelField}
                                        value={line.itemModelId}
                                        organizationId={organization}
                                        onChange={value => update(line.key, { itemModelId: value })}
                                    />
                                </td>
                                <td>
                                    <input
                                        aria-label="Quantity"
                                        type="number"
                                        min="0.0001"
                                        step="0.0001"
                                        value={line.quantity}
                                        onChange={event => update(line.key, { quantity: event.target.value })}
                                    />
                                </td>
                                <td>
                                    <input
                                        aria-label="Unit price"
                                        type="number"
                                        min="0"
                                        step="0.0001"
                                        value={line.unitPrice}
                                        onChange={event => update(line.key, { unitPrice: event.target.value })}
                                    />
                                </td>
                                <td>
                                    <input
                                        aria-label="Discount"
                                        type="number"
                                        min="0"
                                        step="0.0001"
                                        value={line.discount}
                                        onChange={event => update(line.key, { discount: event.target.value })}
                                    />
                                </td>
                                <td>
                                    <input
                                        aria-label="Item notes"
                                        maxLength={2000}
                                        value={line.notes}
                                        onChange={event => update(line.key, { notes: event.target.value })}
                                    />
                                </td>
                                <td className={styles.actionCell}>
                                    <Button
                                        type="button"
                                        severity="secondary"
                                        disabled={disabled || lines.length === 1}
                                        onClick={() => onChange(lines.filter(value => value.key !== line.key))}
                                    >
                                        Remove
                                    </Button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </section>
    );
}

function ProcurementSection({
    purchase,
    onChange
}: {
    purchase: PurchaseView;
    onChange: (purchase: PurchaseView) => void;
}) {
    const [method, setMethod] = React.useState<ProcurementMethod>(
        purchase.procurement?.procurementMethod ?? "NOT_REQUIRED"
    );
    const [modality, setModality] = React.useState<BiddingModality>("PREGAO");
    const [directType, setDirectType] = React.useState<DirectContractingType>("DISPENSA");
    const [processNumber, setProcessNumber] = React.useState(purchase.procurement?.processNumber ?? "");
    const [objectDescription, setObjectDescription] = React.useState("");
    const [justification, setJustification] = React.useState("");
    const [legalBasis, setLegalBasis] = React.useState(purchase.procurement?.legalBasis ?? "");
    const [supplierChoiceReason, setSupplierChoiceReason] = React.useState("");
    const [priceJustification, setPriceJustification] = React.useState("");
    const [busy, setBusy] = React.useState(false);
    const [error, setError] = React.useState("");

    async function save(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setBusy(true);
        setError("");
        try {
            const result = await purchaseService.configureProcurement(purchase.id, {
                processNumber,
                objectDescription,
                justification: justification || undefined,
                procurementMethod: method,
                biddingModality: method === "BIDDING" ? modality : undefined,
                directContractingType: method === "DIRECT_CONTRACTING" ? directType : undefined,
                estimatedValue: purchase.total,
                legalBasis: legalBasis || undefined,
                supplierChoiceReason: supplierChoiceReason || undefined,
                priceJustification: priceJustification || undefined
            });
            onChange(result);
        } catch (caught) {
            setError(failure(caught));
        } finally {
            setBusy(false);
        }
    }

    return (
        <section className="mt-8">
            <h2>Procurement / contracting</h2>
            <p>
                For private organizations choose “Not required”. For organizations subject to
                public procurement, select bidding or direct contracting.
            </p>
            {error && <Message type="error" text={error} />}
            <form data-comandos-erp-form="true" onSubmit={save}>
                <fieldset className={styles.fields} disabled={busy}>
                    <div className={styles.field}>
                        <label htmlFor="procurement-method">Method *</label>
                        <select
                            id="procurement-method"
                            value={method}
                            onChange={event => setMethod(event.target.value as ProcurementMethod)}
                        >
                            <option value="NOT_REQUIRED">Not required</option>
                            <option value="BIDDING">Bidding</option>
                            <option value="DIRECT_CONTRACTING">Direct contracting</option>
                        </select>
                    </div>

                    {method !== "NOT_REQUIRED" && (
                        <>
                            <div className={styles.field}>
                                <label htmlFor="process-number">Process number *</label>
                                <input
                                    id="process-number"
                                    required
                                    value={processNumber}
                                    onChange={event => setProcessNumber(event.target.value)}
                                />
                            </div>
                            <div className={styles.field}>
                                <label htmlFor="object-description">Object *</label>
                                <textarea
                                    id="object-description"
                                    required
                                    rows={3}
                                    value={objectDescription}
                                    onChange={event => setObjectDescription(event.target.value)}
                                />
                            </div>
                        </>
                    )}

                    {method === "BIDDING" && (
                        <div className={styles.field}>
                            <label htmlFor="bidding-modality">Bidding modality *</label>
                            <select
                                id="bidding-modality"
                                value={modality}
                                onChange={event => setModality(event.target.value as BiddingModality)}
                            >
                                <option value="PREGAO">Pregão</option>
                                <option value="CONCORRENCIA">Concorrência</option>
                                <option value="CONCURSO">Concurso</option>
                                <option value="LEILAO">Leilão</option>
                                <option value="DIALOGO_COMPETITIVO">Diálogo competitivo</option>
                            </select>
                        </div>
                    )}

                    {method === "DIRECT_CONTRACTING" && (
                        <div className={styles.field}>
                            <label htmlFor="direct-type">Direct contracting *</label>
                            <select
                                id="direct-type"
                                value={directType}
                                onChange={event => setDirectType(event.target.value as DirectContractingType)}
                            >
                                <option value="DISPENSA">Dispensa</option>
                                <option value="INEXIGIBILIDADE">Inexigibilidade</option>
                            </select>
                        </div>
                    )}

                    {method !== "NOT_REQUIRED" && (
                        <>
                            <div className={styles.field}>
                                <label htmlFor="procurement-justification">Justification</label>
                                <textarea
                                    id="procurement-justification"
                                    rows={3}
                                    value={justification}
                                    onChange={event => setJustification(event.target.value)}
                                />
                            </div>
                            <div className={styles.field}>
                                <label htmlFor="legal-basis">Legal basis</label>
                                <textarea
                                    id="legal-basis"
                                    rows={2}
                                    value={legalBasis}
                                    onChange={event => setLegalBasis(event.target.value)}
                                />
                            </div>
                            <div className={styles.field}>
                                <label htmlFor="supplier-choice">Supplier choice reason</label>
                                <textarea
                                    id="supplier-choice"
                                    rows={2}
                                    value={supplierChoiceReason}
                                    onChange={event => setSupplierChoiceReason(event.target.value)}
                                />
                            </div>
                            <div className={styles.field}>
                                <label htmlFor="price-justification">Price justification</label>
                                <textarea
                                    id="price-justification"
                                    rows={2}
                                    value={priceJustification}
                                    onChange={event => setPriceJustification(event.target.value)}
                                />
                            </div>
                        </>
                    )}
                </fieldset>

                <div className={styles.actions}>
                    <Button
                        type="submit"
                        className="registration-yellow-button"
                        disabled={busy}
                    >
                        {busy ? "Saving…" : "Save procurement"}
                    </Button>
                </div>
            </form>
        </section>
    );
}

function PurchaseSummary({ purchase }: { purchase: PurchaseView }) {
    const { locale } = useComandosPreferences();
    return (
        <div className={styles.tableContainer}>
            <h2>Purchase #{purchase.id}</h2>
            <p>
                {purchase.purchaseNumber} · {purchase.purchaseDate} · {purchase.status}
            </p>
            <p>
                Buyer: {purchase.buyerName} · Supplier: {purchase.supplierName || "Not informed"}
            </p>
            <table>
                <thead>
                    <tr>
                        <th>Item</th>
                        <th>Quantity</th>
                        <th>Unit price</th>
                        <th>Discount</th>
                        <th>Total</th>
                    </tr>
                </thead>
                <tbody>
                    {purchase.items.map(item => (
                        <tr key={item.id}>
                            <td>{item.itemName}</td>
                            <td>{item.quantity}</td>
                            <td>{money(item.unitPrice, locale)}</td>
                            <td>{money(item.discount, locale)}</td>
                            <td>{money(item.total, locale)}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
            <p className={styles.pagination}>Total: {money(purchase.total, locale)}</p>
        </div>
    );
}

function PurchaseHistory({
    organizationId,
    refresh
}: {
    organizationId: number;
    refresh: number;
}) {
    const { locale } = useComandosPreferences();
    const [result, setResult] = React.useState<PurchaseView[] | null>(null);
    const [error, setError] = React.useState("");

    React.useEffect(() => {
        const controller = new AbortController();
        purchaseService.list(organizationId, controller.signal)
            .then(data => {
                if (!controller.signal.aborted) {
                    setResult(data);
                    setError("");
                }
            })
            .catch(caught => {
                if (!controller.signal.aborted) setError(failure(caught));
            });
        return () => controller.abort();
    }, [organizationId, refresh]);

    return (
        <section className="mt-8">
            <h2>Purchase history</h2>
            {error && <Message type="error" text={error} />}
            {!result && !error && <p role="status">Loading purchases…</p>}
            {result && (
                <div className={styles.tableContainer}>
                    <table>
                        <thead>
                            <tr>
                                <th>Number</th>
                                <th>Date</th>
                                <th>Supplier</th>
                                <th>Status</th>
                                <th>Procurement</th>
                                <th>Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            {result.map(purchase => (
                                <tr key={purchase.id}>
                                    <td>{purchase.purchaseNumber}</td>
                                    <td>{purchase.purchaseDate}</td>
                                    <td>{purchase.supplierName || "—"}</td>
                                    <td>{purchase.status}</td>
                                    <td>
                                        {purchase.procurement
                                            ? `${purchase.procurement.procurementMethod} · ${purchase.procurement.status}`
                                            : "—"}
                                    </td>
                                    <td>{money(purchase.total, locale)}</td>
                                </tr>
                            ))}
                            {!result.length && (
                                <tr>
                                    <td colSpan={6}>No purchases registered for this organization.</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    );
}