"use client";

import * as React from "react";
import axios from "axios";
import { Button } from "components/common/button";
import { ComandosSelectField } from "components/common/select-field";

import { Layout } from "components/layout";
import { Message } from "components/common/message";
import { useComandosPreferences } from "components/settings/preferences-provider";
import { ReferenceField } from "components/erp/shared/record-workspace";
import { createErpService } from "api/services/erp.service";
import { purchaseService } from "api/services/purchase.service";
import type { ErpField, ErpValue } from "api/models/erp";
import type {
    AcquisitionDocumentRequest,
    AcquisitionDocumentType,
    AcquisitionType,
    BiddingModality,
    CreatePurchaseItemRequest,
    DirectContractingType,
    ProcurementMethod,
    ProcurementStatus,
    PurchaseView
} from "api/models/erp/purchase";
import styles from "components/erp/shared/workspace.module.css";
import { formatBRLValue, maskBRLInput, parseBRLValue } from "utils/money";

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
const originPersonField = reference("originPersonId", "Acquisition origin", "people");
const itemModelField = reference("itemModelId", "Equipment / item", "item-models");

type PurchaseLine = {
    key: string;
    itemModelId: ErpValue;
    quantity: string;
    unitPrice: string;
    discount: string;
    conditionDescription: string;
    notes: string;
};

const emptyLine = (): PurchaseLine => ({
    key: crypto.randomUUID(),
    itemModelId: null,
    quantity: "1",
    unitPrice: formatBRLValue(0),
    discount: formatBRLValue(0),
    conditionDescription: "",
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

function money(value: number | string | undefined, _locale: "pt-BR" | "en-US"): string {
    return formatBRLValue(value ?? 0);
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
    const [originPerson, setOriginPerson] = React.useState<ErpValue>(null);
    const [acquisitionType, setAcquisitionType] = React.useState<AcquisitionType>("ONEROUS");
    const [originDescription, setOriginDescription] = React.useState("");
    const [freight, setFreight] = React.useState(formatBRLValue(0));
    const [taxes, setTaxes] = React.useState(formatBRLValue(0));
    const [otherCosts, setOtherCosts] = React.useState(formatBRLValue(0));
    const [paymentConditions, setPaymentConditions] = React.useState("");
    const [deliveryConditions, setDeliveryConditions] = React.useState("");
    const [warrantyConditions, setWarrantyConditions] = React.useState("");
    const [documents, setDocuments] = React.useState<AcquisitionDocumentRequest[]>([]);
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
        originPerson &&
        purchaseNumber.trim() &&
        purchaseDate &&
        lines.length &&
        lines.every(line =>
            line.itemModelId &&
            Number(line.quantity) > 0 &&
            parseBRLValue(line.unitPrice) >= 0 &&
            parseBRLValue(line.discount || 0) >= 0
        )
    );

    async function createPurchase(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!valid || busy) return;

        const items: CreatePurchaseItemRequest[] = lines.map(line => ({
            itemModelId: Number(line.itemModelId),
            quantity: Number(line.quantity),
            unitPrice: parseBRLValue(line.unitPrice),
            discount: parseBRLValue(line.discount || 0),
            conditionDescription: line.conditionDescription.trim() || undefined,
            notes: line.notes.trim() || undefined
        }));

        setBusy(true);
        setError("");

        try {
            const result = await purchaseService.create({
                buyerOrganizationId: Number(organization),
                originPersonId: Number(originPerson),
                acquisitionType,
                originDescription: originDescription.trim() || undefined,
                purchaseNumber: purchaseNumber.trim(),
                purchaseDate,
                notes: notes.trim() || undefined,
                freight: parseBRLValue(freight),
                taxes: parseBRLValue(taxes),
                otherCosts: parseBRLValue(otherCosts),
                paymentConditions: paymentConditions.trim() || undefined,
                deliveryConditions: deliveryConditions.trim() || undefined,
                warrantyConditions: warrantyConditions.trim() || undefined,
                items,
                documents            });
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
                        <label>Acquisition origin *</label>
                        <ReferenceField
                            service={core}
                            field={originPersonField}
                            value={originPerson}
                            organizationId={null}
                            onChange={setOriginPerson}
                        />
                        <small>
                            Select a registered natural person or legal entity as supplier/origin.
                        </small>
                    </div>
                    <div className={styles.field}>
                        <ComandosSelectField
                            id="acquisition-type"
                            label="Tipo de aquisição"
                            required
                            value={acquisitionType}
                            options={[
                                { value: "ONEROUS", label: "Onerosa / compra" },
                                { value: "FREE", label: "Gratuita" }
                            ]}
                            onChange={value => setAcquisitionType(value as AcquisitionType)}
                        />
                    </div>
                    <div className={styles.field}>
                        <label htmlFor="origin-description">Origin complement</label>
                        <input id="origin-description" maxLength={1000} value={originDescription} onChange={event => setOriginDescription(event.target.value)} />
                    </div>
                    <div className={styles.field}>
                        <label htmlFor="purchase-freight">Frete</label>
                        <input id="purchase-freight" type="text" inputMode="numeric" disabled={acquisitionType === "FREE"} value={freight} onChange={event => setFreight(maskBRLInput(event.target.value))} />
                    </div>
                    <div className={styles.field}>
                        <label htmlFor="purchase-taxes">Tributos</label>
                        <input id="purchase-taxes" type="text" inputMode="numeric" disabled={acquisitionType === "FREE"} value={taxes} onChange={event => setTaxes(maskBRLInput(event.target.value))} />
                    </div>
                    <div className={styles.field}>
                        <label htmlFor="purchase-other-costs">Outros custos</label>
                        <input id="purchase-other-costs" type="text" inputMode="numeric" disabled={acquisitionType === "FREE"} value={otherCosts} onChange={event => setOtherCosts(maskBRLInput(event.target.value))} />
                    </div>
                    <div className={styles.field}><label>Condições de pagamento</label><textarea value={paymentConditions} onChange={event => setPaymentConditions(event.target.value)} /></div>
                    <div className={styles.field}><label>Condições de entrega</label><textarea value={deliveryConditions} onChange={event => setDeliveryConditions(event.target.value)} /></div>
                    <div className={styles.field}><label>Garantia / condições</label><textarea value={warrantyConditions} onChange={event => setWarrantyConditions(event.target.value)} /></div>

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
                <section>
                    <div className={styles.toolbar}>
                        <h2>Documentos da aquisição</h2>
                        <Button
                            type="button"
                            className="comandos-accent-button"
                            disabled={busy || !!completed}
                            onClick={() => setDocuments([...documents, { documentType: "OTHER" }])}
                        >
                            + Documento
                        </Button>
                    </div>
                    <div className={styles.tableContainer}>
                        <table>
                            <thead>
                                <tr>
                                    <th>Tipo</th>
                                    <th>Número</th>
                                    <th>Data</th>
                                    <th>Emissor</th>
                                    <th>Valor</th>
                                    <th>Arquivo / referência</th>
                                    <th>Ações</th>
                                </tr>
                            </thead>
                            <tbody>
                                {documents.map((doc, index) => (
                                    <tr key={index}>
                                        <td>
                                            <ComandosSelectField
                                                id={`purchase-document-type-${index}`}
                                                label="Tipo de documento"
                                                required
                                                value={doc.documentType}
                                                options={[
                                                    { value: "CONTRACT", label: "Contrato" },
                                                    { value: "BUDGET_COMMITMENT", label: "Empenho" },
                                                    { value: "INVOICE", label: "Nota fiscal" },
                                                    { value: "PROCUREMENT_PROCESS", label: "Processo" },
                                                    { value: "BIDDING_NOTICE", label: "Edital" },
                                                    { value: "DIRECT_CONTRACTING_ACT", label: "Ato de contratação direta" },
                                                    { value: "FREE_ACQUISITION_TERM", label: "Termo de aquisição gratuita" },
                                                    { value: "DONATION_TERM", label: "Termo de doação" },
                                                    { value: "TRANSFER_TERM", label: "Termo de transferência" },
                                                    { value: "AUTHORIZATION", label: "Autorização" },
                                                    { value: "DELIVERY_DOCUMENT", label: "Documento de entrega" },
                                                    { value: "OTHER", label: "Outro" }
                                                ]}
                                                onChange={value =>
                                                    setDocuments(documents.map((item, itemIndex) =>
                                                        itemIndex === index
                                                            ? { ...item, documentType: value as AcquisitionDocumentType }
                                                            : item
                                                    ))
                                                }
                                            />
                                        </td>
                                        <td>
                                            <input
                                                value={doc.documentNumber ?? ""}
                                                onChange={event =>
                                                    setDocuments(documents.map((item, itemIndex) =>
                                                        itemIndex === index
                                                            ? { ...item, documentNumber: event.target.value }
                                                            : item
                                                    ))
                                                }
                                            />
                                        </td>
                                        <td>
                                            <input
                                                type="date"
                                                value={doc.issueDate ?? ""}
                                                onChange={event =>
                                                    setDocuments(documents.map((item, itemIndex) =>
                                                        itemIndex === index
                                                            ? { ...item, issueDate: event.target.value }
                                                            : item
                                                    ))
                                                }
                                            />
                                        </td>
                                        <td>
                                            <input
                                                value={doc.issuer ?? ""}
                                                onChange={event =>
                                                    setDocuments(documents.map((item, itemIndex) =>
                                                        itemIndex === index
                                                            ? { ...item, issuer: event.target.value }
                                                            : item
                                                    ))
                                                }
                                            />
                                        </td>
                                        <td>
                                            <input
                                                type="text"
                                                inputMode="numeric"
                                                value={doc.amount == null ? "" : formatBRLValue(doc.amount)}
                                                onChange={event =>
                                                    setDocuments(documents.map((item, itemIndex) =>
                                                        itemIndex === index
                                                            ? {
                                                                ...item,
                                                                amount: event.target.value
                                                                    ? parseBRLValue(maskBRLInput(event.target.value))
                                                                    : undefined
                                                            }
                                                            : item
                                                    ))
                                                }
                                            />
                                        </td>
                                        <td>
                                            <input
                                                value={doc.storageReference ?? ""}
                                                onChange={event =>
                                                    setDocuments(documents.map((item, itemIndex) =>
                                                        itemIndex === index
                                                            ? { ...item, storageReference: event.target.value }
                                                            : item
                                                    ))
                                                }
                                            />
                                        </td>
                                        <td>
                                            <Button
                                                type="button"
                                                severity="secondary"
                                                disabled={busy || !!completed}
                                                onClick={() =>
                                                    setDocuments(documents.filter((_, itemIndex) => itemIndex !== index))
                                                }
                                            >
                                                Remover
                                            </Button>
                                        </td>
                                    </tr>
                                ))}
                                {!documents.length && (
                                    <tr>
                                        <td colSpan={7}>Nenhum documento adicionado.</td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </section>
<div className={styles.actions}>
                    {completed ? (
                        <Button
                            type="button"
                            className="comandos-accent-button"
                            onClick={onNew}
                        >
                            + Purchase
                        </Button>
                    ) : (
                        <Button
                            type="submit"
                            className="comandos-accent-button"
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
                    className="comandos-accent-button"
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
                            <th>Condição</th>
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
                                        type="text"
                                        inputMode="numeric"
                                        value={line.unitPrice}
                                        onChange={event => update(line.key, { unitPrice: maskBRLInput(event.target.value) })}
                                    />
                                </td>
                                <td>
                                    <input
                                        aria-label="Discount"
                                        type="text"
                                        inputMode="numeric"
                                        value={line.discount}
                                        onChange={event => update(line.key, { discount: maskBRLInput(event.target.value) })}
                                    />
                                </td>
                                <td>
                                    <input aria-label="Condição do item" maxLength={1000} value={line.conditionDescription} onChange={event => update(line.key, { conditionDescription: event.target.value })} />
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
    const [statusBusy, setStatusBusy] = React.useState(false);

    async function advanceStatus(status: ProcurementStatus) {
        setStatusBusy(true); setError("");
        try { onChange(await purchaseService.updateProcurementStatus(purchase.id, status)); }
        catch (caught) { setError(failure(caught)); }
        finally { setStatusBusy(false); }
    }

    async function authorize() {
        setStatusBusy(true); setError("");
        try { onChange(await purchaseService.authorize(purchase.id)); }
        catch (caught) { setError(failure(caught)); }
        finally { setStatusBusy(false); }
    }

    async function order() {
        setStatusBusy(true); setError("");
        try { onChange(await purchaseService.order(purchase.id)); }
        catch (caught) { setError(failure(caught)); }
        finally { setStatusBusy(false); }
    }

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
                        <ComandosSelectField
                            id="procurement-method"
                            label="Method"
                            required
                            value={method}
                            options={[
                                { value: "NOT_REQUIRED", label: "Not required" },
                                { value: "BIDDING", label: "Bidding" },
                                { value: "DIRECT_CONTRACTING", label: "Direct contracting" }
                            ]}
                            onChange={value => setMethod(value as ProcurementMethod)}
                        />
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
                            <ComandosSelectField
                                id="bidding-modality"
                                label="Bidding modality"
                                required
                                value={modality}
                                options={[
                                    { value: "PREGAO", label: "Pregão" },
                                    { value: "CONCORRENCIA", label: "Concorrência" },
                                    { value: "CONCURSO", label: "Concurso" },
                                    { value: "LEILAO", label: "Leilão" },
                                    { value: "DIALOGO_COMPETITIVO", label: "Diálogo competitivo" }
                                ]}
                                onChange={value => setModality(value as BiddingModality)}
                            />
                        </div>
                    )}

                    {method === "DIRECT_CONTRACTING" && (
                        <div className={styles.field}>
                            <ComandosSelectField
                                id="direct-type"
                                label="Direct contracting"
                                required
                                value={directType}
                                options={[
                                    { value: "DISPENSA", label: "Dispensa" },
                                    { value: "INEXIGIBILIDADE", label: "Inexigibilidade" }
                                ]}
                                onChange={value => setDirectType(value as DirectContractingType)}
                            />
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
                        className="comandos-accent-button"
                        disabled={busy}
                    >
                        {busy ? "Saving…" : "Save procurement"}
                    </Button>
                </div>
            </form>
            <div className={styles.actions}>
                {purchase.procurement && !["HOMOLOGATED","CONTRACTED","CANCELLED","FAILED"].includes(purchase.procurement.status) && (
                    <select aria-label="Next procurement status" disabled={statusBusy}
                        defaultValue="" onChange={event => {
                            if (event.target.value) void advanceStatus(event.target.value as ProcurementStatus);
                            event.currentTarget.value = "";
                        }}>
                        <option value="">Advance procurement…</option>
                        <option value="PLANNING">Planning</option>
                        <option value="UNDER_REVIEW">Under review</option>
                        <option value="AUTHORIZED">Authorized</option>
                        <option value="PUBLISHED">Published</option>
                        <option value="PROPOSAL_PHASE">Proposal phase</option>
                        <option value="QUALIFICATION_PHASE">Qualification phase</option>
                        <option value="JUDGMENT_PHASE">Judgment phase</option>
                        <option value="APPEAL_PHASE">Appeal phase</option>
                        <option value="AWARDED">Awarded</option>
                        <option value="HOMOLOGATED">Homologated</option>
                        <option value="CONTRACTED">Contracted</option>
                    </select>
                )}
                {!["AUTHORIZED","ORDERED","PARTIALLY_RECEIVED","RECEIVED","CANCELLED"].includes(purchase.status) && (
                    <Button type="button" disabled={statusBusy} onClick={() => void authorize()}>Authorize acquisition</Button>
                )}
                {purchase.status === "AUTHORIZED" && (
                    <Button type="button" disabled={statusBusy} onClick={() => void order()}>Issue order</Button>
                )}
            </div>
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
                Buyer: {purchase.buyerName} · Origin: {purchase.originPersonName} ({purchase.originPersonType})
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