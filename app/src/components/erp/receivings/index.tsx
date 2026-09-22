"use client"

import * as React from "react"
import { Button } from "@primereact/ui/button"
import { useComandosPreferences } from "components/settings/preferences-provider"
import { purchaseService } from "api/services/purchase.service"
import { receivingService } from "api/services/receiving.service"
import type { PurchaseItemView, PurchaseView } from "api/models/erp/purchase"
import type {
    ReceivingInput,
    ReceivingItemInput,
    ReceivingSourceType,
    ReceivingView
} from "api/models/erp/receiving"

type DraftItem = {
    acquisitionItemId: string
    itemModelId: string
    quantity: string
    lotNumber: string
    conditionDescription: string
    serials: string
}

const emptyItem = (): DraftItem => ({
    acquisitionItemId: "",
    itemModelId: "",
    quantity: "1",
    lotNumber: "",
    conditionDescription: "",
    serials: ""
})

export const ReceivingWorkspace: React.FC = () => {
    const { tr } = useComandosPreferences()
    const [sourceType, setSourceType] = React.useState<ReceivingSourceType>("ACQUISITION")
    const [acquisitionId, setAcquisitionId] = React.useState("")
    const [acquisition, setAcquisition] = React.useState<PurchaseView | null>(null)
    const [documentNumber, setDocumentNumber] = React.useState("")
    const [invoiceNumber, setInvoiceNumber] = React.useState("")
    const [location, setLocation] = React.useState("")
    const [receivedBy, setReceivedBy] = React.useState("")
    const [physicalChecked, setPhysicalChecked] = React.useState(false)
    const [documentsChecked, setDocumentsChecked] = React.useState(false)
    const [items, setItems] = React.useState<DraftItem[]>([emptyItem()])
    const [receivings, setReceivings] = React.useState<ReceivingView[]>([])
    const [saving, setSaving] = React.useState(false)
    const [loadingAcquisition, setLoadingAcquisition] = React.useState(false)
    const [message, setMessage] = React.useState("")

    const loadHistory = React.useCallback(async (id?: number) => {
        setReceivings(await receivingService.list(id))
    }, [])

    React.useEffect(() => {
        const timer = window.setTimeout(() => {
            void loadHistory()
        }, 0)

        return () => window.clearTimeout(timer)
    }, [loadHistory])

    const loadAcquisition = async () => {
        if (!acquisitionId) return
        setLoadingAcquisition(true)
        setMessage("")
        try {
            const value = await purchaseService.get(Number(acquisitionId))
            setAcquisition(value)
            const pending = value.items.filter(
                (item) => Number(item.quantity) - Number(item.receivedQuantity) > 0
            )
            setItems(pending.length
                ? pending.map((item) => ({
                    ...emptyItem(),
                    acquisitionItemId: String(item.id),
                    itemModelId: String(item.itemModelId),
                    quantity: String(Number(item.quantity) - Number(item.receivedQuantity))
                }))
                : [emptyItem()])
            await loadHistory(value.id)
        } catch {
            setAcquisition(null)
            setMessage(tr("Acquisition not found or unavailable for receiving."))
        } finally {
            setLoadingAcquisition(false)
        }
    }

    const pendingFor = (draft: DraftItem): PurchaseItemView | undefined =>
        acquisition?.items.find((item) => item.id === Number(draft.acquisitionItemId))

    const updateItem = (index: number, patch: Partial<DraftItem>) => {
        setItems((current) => current.map(
            (item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item
        ))
    }

    const removeItem = (index: number) => {
        setItems((current) => current.length === 1
            ? current
            : current.filter((_, itemIndex) => itemIndex !== index))
    }

    const save = async () => {
        setSaving(true)
        setMessage("")
        try {
            const payloadItems: ReceivingItemInput[] = items.map((item) => ({
                acquisitionItemId: sourceType === "ACQUISITION"
                    ? Number(item.acquisitionItemId) : undefined,
                itemModelId: item.itemModelId ? Number(item.itemModelId) : undefined,
                receivedQuantity: Number(item.quantity),
                lotNumber: item.lotNumber || undefined,
                conditionDescription: item.conditionDescription || undefined,
                serialNumbers: item.serials
                    .split(/\r?\n|,/)
                    .map((value) => value.trim())
                    .filter(Boolean)
            }))

            const payload: ReceivingInput = {
                sourceType,
                acquisitionId: sourceType === "ACQUISITION"
                    ? Number(acquisitionId) : undefined,
                receivingLocation: location || undefined,
                deliveryDocumentNumber: documentNumber || undefined,
                invoiceNumber: invoiceNumber || undefined,
                receivedBy: receivedBy || undefined,
                physicalChecked,
                documentsChecked,
                items: payloadItems
            }

            const created = await receivingService.create(payload)
            setMessage(`${tr("Receiving")} #${created.id} - ${tr(created.status)}`)
            await loadHistory(
                sourceType === "ACQUISITION" ? Number(acquisitionId) : undefined
            )
            if (sourceType === "ACQUISITION") {
                await loadAcquisition()
            }
        } catch (error) {
            setMessage(error instanceof Error ? error.message : tr("Unable to save receiving."))
        } finally {
            setSaving(false)
        }
    }

    const valid = items.length > 0
        && items.every((item) =>
            Number(item.quantity) > 0
            && (sourceType !== "ACQUISITION" || Boolean(item.acquisitionItemId))
            && (sourceType === "ACQUISITION" || Boolean(item.itemModelId))
        )
        && (sourceType !== "ACQUISITION" || Boolean(acquisition))

    return (
        <section className="registration-page">
            <div className="registration-header">
                <div>
                    <h1>{tr("Receiving")}</h1>
                    <p>{tr("Register partial or total deliveries linked to the acquired items.")}</p>
                </div>
            </div>

            {message && <p>{message}</p>}

            <div className="registration-form-grid">
                <div className="registration-field">
                    <label>{tr("Receiving source")}</label>
                    <select
                        className="registration-input"
                        value={sourceType}
                        onChange={(event) => {
                            setSourceType(event.target.value as ReceivingSourceType)
                            setAcquisition(null)
                            setItems([emptyItem()])
                        }}
                    >
                        <option value="ACQUISITION">{tr("Acquisition")}</option>
                        <option value="TRANSFER">{tr("Transfer")}</option>
                        <option value="RETURN">{tr("Return")}</option>
                        <option value="LOAN">{tr("Loan")}</option>
                        <option value="DONATION">{tr("Donation")}</option>
                        <option value="MAINTENANCE_RETURN">{tr("Maintenance return")}</option>
                        <option value="OTHER">{tr("Other")}</option>
                    </select>
                </div>

                {sourceType === "ACQUISITION" && (
                    <div className="registration-field">
                        <label>{tr("Acquisition")}</label>
                        <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                            <input
                                className="registration-input"
                                inputMode="numeric"
                                value={acquisitionId}
                                onChange={(event) =>
                                    setAcquisitionId(event.target.value.replace(/\D/g, ""))}
                            />
                            <Button onClick={loadAcquisition} disabled={!acquisitionId || loadingAcquisition}>
                                {loadingAcquisition ? tr("Loading...") : tr("Load")}
                            </Button>
                        </div>
                    </div>
                )}

                <div className="registration-field">
                    <label>{tr("Delivery document")}</label>
                    <input className="registration-input" value={documentNumber}
                        onChange={(event) => setDocumentNumber(event.target.value)} />
                </div>
                <div className="registration-field">
                    <label>{tr("Invoice")}</label>
                    <input className="registration-input" value={invoiceNumber}
                        onChange={(event) => setInvoiceNumber(event.target.value)} />
                </div>
                <div className="registration-field">
                    <label>{tr("Receiving location")}</label>
                    <input className="registration-input" value={location}
                        onChange={(event) => setLocation(event.target.value)} />
                </div>
                <div className="registration-field">
                    <label>{tr("Received by")}</label>
                    <input className="registration-input" value={receivedBy}
                        onChange={(event) => setReceivedBy(event.target.value)} />
                </div>

                <label className="registration-field">
                    <span>{tr("Physical check completed")}</span>
                    <input type="checkbox" checked={physicalChecked}
                        onChange={(event) => setPhysicalChecked(event.target.checked)} />
                </label>
                <label className="registration-field">
                    <span>{tr("Document check completed")}</span>
                    <input type="checkbox" checked={documentsChecked}
                        onChange={(event) => setDocumentsChecked(event.target.checked)} />
                </label>
            </div>

            {acquisition && (
                <p>
                    {tr("Acquisition")}: {acquisition.purchaseNumber} · {tr("Origin")}:{" "}
                    {acquisition.originPersonName}
                </p>
            )}

            <h2>{tr("Received items")}</h2>
            {items.map((item, index) => {
                const acquired = pendingFor(item)
                const pending = acquired
                    ? Number(acquired.quantity) - Number(acquired.receivedQuantity)
                    : undefined

                return (
                    <div className="registration-form-grid" key={`${index}-${item.acquisitionItemId}`}>
                        {sourceType === "ACQUISITION" ? (
                            <div className="registration-field">
                                <label>{tr("Acquisition item")}</label>
                                <select
                                    className="registration-input"
                                    value={item.acquisitionItemId}
                                    onChange={(event) => {
                                        const selected = acquisition?.items.find(
                                            (value) => value.id === Number(event.target.value)
                                        )
                                        updateItem(index, {
                                            acquisitionItemId: event.target.value,
                                            itemModelId: selected ? String(selected.itemModelId) : "",
                                            quantity: selected
                                                ? String(Number(selected.quantity) - Number(selected.receivedQuantity))
                                                : "1"
                                        })
                                    }}
                                >
                                    <option value="">{tr("Select")}</option>
                                    {acquisition?.items
                                        .filter((value) =>
                                            Number(value.quantity) - Number(value.receivedQuantity) > 0)
                                        .map((value) => (
                                            <option key={value.id} value={value.id}>
                                                {value.itemName} · {tr("Pending")}:{" "}
                                                {Number(value.quantity) - Number(value.receivedQuantity)}
                                            </option>
                                        ))}
                                </select>
                            </div>
                        ) : (
                            <div className="registration-field">
                                <label>{tr("Item model ID")}</label>
                                <input className="registration-input" inputMode="numeric"
                                    value={item.itemModelId}
                                    onChange={(event) => updateItem(index, {
                                        itemModelId: event.target.value.replace(/\D/g, "")
                                    })} />
                            </div>
                        )}

                        <div className="registration-field">
                            <label>{tr("Received quantity")}</label>
                            <input
                                className="registration-input"
                                inputMode="decimal"
                                value={item.quantity}
                                onChange={(event) => updateItem(index, {
                                    quantity: event.target.value
                                })}
                            />
                            {pending !== undefined && (
                                <small>{tr("Pending before this receiving")}: {pending}</small>
                            )}
                        </div>

                        <div className="registration-field">
                            <label>{tr("Lot number")}</label>
                            <input className="registration-input" value={item.lotNumber}
                                onChange={(event) => updateItem(index, {
                                    lotNumber: event.target.value
                                })} />
                        </div>

                        <div className="registration-field">
                            <label>{tr("Condition")}</label>
                            <input className="registration-input" value={item.conditionDescription}
                                onChange={(event) => updateItem(index, {
                                    conditionDescription: event.target.value
                                })} />
                        </div>

                        <div className="registration-field registration-field-full">
                            <label>{tr("Serial numbers")}</label>
                            <textarea
                                className="registration-input"
                                rows={4}
                                value={item.serials}
                                onChange={(event) => updateItem(index, {
                                    serials: event.target.value
                                })}
                                placeholder={tr("One serial number per line or separated by commas")}
                            />
                        </div>

                        <div className="registration-field registration-field-full">
                            <Button onClick={() => removeItem(index)} disabled={items.length === 1}>
                                {tr("Remove item")}
                            </Button>
                        </div>
                    </div>
                )
            })}

            {sourceType !== "ACQUISITION" && (
                <Button onClick={() => setItems((current) => [...current, emptyItem()])}>
                    {tr("Add item")}
                </Button>
            )}

            <div style={{ marginTop: "1rem" }}>
                <Button className="registration-yellow-button" onClick={save}
                    disabled={saving || !valid}>
                    {saving ? tr("Saving...") : tr("Save")}
                </Button>
            </div>

            <h2>{tr("Receiving history")}</h2>
            <div style={{ overflowX: "auto" }}>
                <table style={{ width: "100%", minWidth: 760 }}>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>{tr("Acquisition")}</th>
                            <th>{tr("Received at")}</th>
                            <th>{tr("Physical check")}</th>
                            <th>{tr("Document check")}</th>
                            <th>{tr("Status")}</th>
                            <th>{tr("Items")}</th>
                        </tr>
                    </thead>
                    <tbody>
                        {receivings.map((receiving) => (
                            <tr key={receiving.id}>
                                <td>{receiving.id}</td>
                                <td>{receiving.acquisitionNumber || "-"}</td>
                                <td>{receiving.receivedAt || "-"}</td>
                                <td>{receiving.physicalChecked ? tr("Yes") : tr("No")}</td>
                                <td>{receiving.documentsChecked ? tr("Yes") : tr("No")}</td>
                                <td>{tr(receiving.status)}</td>
                                <td>{receiving.items.length}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </section>
    )
}
