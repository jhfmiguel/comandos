"use client"

import * as React from "react"
import { Button } from "@primereact/ui/button"
import { useComandosPreferences } from "components/settings/preferences-provider"
import { receivingService } from "api/services/receiving.service"
import type { ReceivingInput, ReceivingSourceType } from "api/models/erp/receiving"

export const ReceivingWorkspace: React.FC = () => {
    const { tr } = useComandosPreferences()
    const [sourceType, setSourceType] = React.useState<ReceivingSourceType>("ACQUISITION")
    const [acquisitionId, setAcquisitionId] = React.useState("")
    const [documentNumber, setDocumentNumber] = React.useState("")
    const [invoiceNumber, setInvoiceNumber] = React.useState("")
    const [location, setLocation] = React.useState("")
    const [receivedBy, setReceivedBy] = React.useState("")
    const [itemModelId, setItemModelId] = React.useState("")
    const [quantity, setQuantity] = React.useState("1")
    const [lotNumber, setLotNumber] = React.useState("")
    const [serials, setSerials] = React.useState("")
    const [saving, setSaving] = React.useState(false)

    const save = async () => {
        setSaving(true)
        try {
            const payload: ReceivingInput = {
                sourceType,
                acquisitionId: acquisitionId ? Number(acquisitionId) : undefined,
                receivingLocation: location || undefined,
                deliveryDocumentNumber: documentNumber || undefined,
                invoiceNumber: invoiceNumber || undefined,
                receivedBy: receivedBy || undefined,
                items: [{
                    itemModelId: itemModelId ? Number(itemModelId) : undefined,
                    receivedQuantity: Number(quantity),
                    lotNumber: lotNumber || undefined,
                    serialNumbers: serials.split(/\r?\n|,/).map((v) => v.trim()).filter(Boolean)
                }]
            }
            await receivingService.create(payload)
        } finally {
            setSaving(false)
        }
    }

    return (
        <section className="registration-page">
            <div className="registration-header">
                <div>
                    <h1>{tr("Receiving")}</h1>
                    <p>{tr("Register the physical delivery and acceptance of equipment.")}</p>
                </div>
            </div>

            <div className="registration-form-grid">
                <div className="registration-field">
                    <label>{tr("Receiving source")}</label>
                    <select className="registration-input" value={sourceType}
                        onChange={(e) => setSourceType(e.target.value as ReceivingSourceType)}>
                        <option value="ACQUISITION">{tr("Acquisition")}</option>
                        <option value="TRANSFER">{tr("Transfer")}</option>
                        <option value="RETURN">{tr("Return")}</option>
                        <option value="LOAN">{tr("Loan")}</option>
                        <option value="DONATION">{tr("Donation")}</option>
                        <option value="MAINTENANCE_RETURN">{tr("Maintenance return")}</option>
                        <option value="OTHER">{tr("Other")}</option>
                    </select>
                </div>

                {sourceType === "ACQUISITION" && <div className="registration-field">
                    <label>{tr("Acquisition")}</label>
                    <input className="registration-input" inputMode="numeric" value={acquisitionId}
                        onChange={(e) => setAcquisitionId(e.target.value.replace(/\D/g, ""))} />
                </div>}

                <div className="registration-field"><label>{tr("Delivery document")}</label>
                    <input className="registration-input" value={documentNumber} onChange={(e) => setDocumentNumber(e.target.value)} /></div>
                <div className="registration-field"><label>{tr("Invoice")}</label>
                    <input className="registration-input" value={invoiceNumber} onChange={(e) => setInvoiceNumber(e.target.value)} /></div>
                <div className="registration-field"><label>{tr("Receiving location")}</label>
                    <input className="registration-input" value={location} onChange={(e) => setLocation(e.target.value)} /></div>
                <div className="registration-field"><label>{tr("Received by")}</label>
                    <input className="registration-input" value={receivedBy} onChange={(e) => setReceivedBy(e.target.value)} /></div>
                <div className="registration-field"><label>{tr("Item model ID")}</label>
                    <input className="registration-input" inputMode="numeric" value={itemModelId} onChange={(e) => setItemModelId(e.target.value.replace(/\D/g, ""))} /></div>
                <div className="registration-field"><label>{tr("Received quantity")}</label>
                    <input className="registration-input" inputMode="decimal" value={quantity} onChange={(e) => setQuantity(e.target.value)} /></div>
                <div className="registration-field"><label>{tr("Lot number")}</label>
                    <input className="registration-input" value={lotNumber} onChange={(e) => setLotNumber(e.target.value)} /></div>
                <div className="registration-field registration-field-full"><label>{tr("Serial numbers")}</label>
                    <textarea className="registration-input" rows={5} value={serials} onChange={(e) => setSerials(e.target.value)}
                        placeholder={tr("One serial number per line or separated by commas")} /></div>
            </div>

            <Button className="registration-yellow-button" onClick={save} disabled={saving}>
                {saving ? tr("Saving...") : tr("Save")}
            </Button>
        </section>
    )
}