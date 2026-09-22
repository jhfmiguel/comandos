"use client"

import * as React from "react"
import axios from "axios"
import { Button } from "components/ui/button"
import { useComandosPreferences } from "components/settings/preferences-provider"
import { createErpService } from "api/services/erp.service"
import { receivingService } from "api/services/receiving.service"
import { receivingIncorporationService, type ReceivingIncorporationView } from "api/services/receiving-incorporation.service"
import type { ReceivingView } from "api/models/erp/receiving"
import type { ErpRecord } from "api/models/erp"

const inventory = createErpService("inventory")
const conditions = ["NEW", "GOOD", "NEEDS_INSPECTION", "DAMAGED"]

function failure(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const data = error.response?.data
        if (typeof data?.message === "string") return data.message
        if (typeof data?.detail === "string") return data.detail
    }
    return "Unable to complete the request. Check the API connection and try again."
}

export const ReceivingIncorporationWorkspace: React.FC = () => {
    const { tr } = useComandosPreferences()
    const [receivingId, setReceivingId] = React.useState("")
    const [receiving, setReceiving] = React.useState<ReceivingView | null>(null)
    const [history, setHistory] = React.useState<ReceivingIncorporationView[]>([])
    const [locations, setLocations] = React.useState<ErpRecord[]>([])
    const [itemId, setItemId] = React.useState("")
    const [serialId, setSerialId] = React.useState("")
    const [locationId, setLocationId] = React.useState("")
    const [assetCode, setAssetCode] = React.useState("")
    const [lotNumber, setLotNumber] = React.useState("")
    const [quantity, setQuantity] = React.useState("")
    const [incorporationValue, setIncorporationValue] = React.useState("")
    const [initialCondition, setInitialCondition] = React.useState("GOOD")
    const [responsible, setResponsible] = React.useState("")
    const [notes, setNotes] = React.useState("")
    const [busy, setBusy] = React.useState(false)
    const [error, setError] = React.useState("")
    const [success, setSuccess] = React.useState("")

    const digits = (value: string) => value.replace(/\D/g, "")
    const selectedItem = receiving?.items.find(item => item.id === Number(itemId))
    const incorporatedForItem = history.filter(row => row.receivingItemId === Number(itemId))
        .reduce((sum, row) => sum + Number(row.quantity || 0), 0)
    const remaining = Math.max(0, Number(selectedItem?.acceptedQuantity || 0) - incorporatedForItem)
    const acceptedSerials = selectedItem?.serials.filter(serial => serial.accepted) ?? []
    const incorporatedSerialIds = new Set(history.map(row => row.receivingSerialId).filter(Boolean))

    React.useEffect(() => {
        inventory.list("locations", "", 0).then(page => setLocations(page.content)).catch(() => setLocations([]))
    }, [])

    const load = async () => {
        if (!receivingId) return
        setBusy(true); setError(""); setSuccess("")
        try {
            const id = Number(receivingId)
            const [loaded, rows] = await Promise.all([receivingService.get(id), receivingIncorporationService.list(id)])
            setReceiving(loaded); setHistory(rows)
            const first = loaded.items.find(item => Number(item.acceptedQuantity) > rows.filter(row => row.receivingItemId === item.id).reduce((s, row) => s + Number(row.quantity), 0))
            setItemId(first ? String(first.id) : ""); setSerialId("")
        } catch (caught) { setError(failure(caught)); setReceiving(null); setHistory([]) }
        finally { setBusy(false) }
    }

    const save = async () => {
        if (!receiving || !itemId || !locationId || remaining <= 0) return
        setBusy(true); setError(""); setSuccess("")
        try {
            const created = await receivingIncorporationService.create({
                receivingId: receiving.id,
                receivingItemId: Number(itemId),
                receivingSerialId: serialId ? Number(serialId) : undefined,
                stockLocationId: Number(locationId),
                assetCode: assetCode.trim() || undefined,
                lotNumber: lotNumber.trim() || undefined,
                quantity: quantity ? Number(quantity) : undefined,
                incorporationValue: incorporationValue ? Number(incorporationValue) : undefined,
                initialCondition,
                incorporatedBy: responsible.trim() || undefined,
                notes: notes.trim() || undefined
            })
            const rows = await receivingIncorporationService.list(receiving.id)
            setHistory(rows); setAssetCode(""); setQuantity(""); setSerialId("")
            setSuccess(created.inventoryResource === "assets" ? tr("Individual asset incorporated into inventory.") : tr("Stock lot incorporated into inventory."))
        } catch (caught) { setError(failure(caught)) }
        finally { setBusy(false) }
    }

    return (
        <section className="registration-page">
            <div className="registration-header"><div><h1>{tr("Inventory incorporation")}</h1><p>{tr("Incorporate definitively accepted equipment into inventory.")}</p></div></div>
            {error && <div className="registration-message registration-message-error">{tr(error)}</div>}
            {success && <div className="registration-message registration-message-success">{success}</div>}

            <div className="comandos-responsive-form-grid">
                <div className="registration-field"><label>{tr("Receiving")}</label><div style={{display:"flex",gap:"0.5rem"}}><input className="registration-input" inputMode="numeric" value={receivingId} onChange={e => setReceivingId(digits(e.target.value))}/><Button onClick={load} disabled={busy || !receivingId}>{tr("Load")}</Button></div></div>
                <div className="registration-field"><label>{tr("Receiving item")}</label><select className="registration-input" value={itemId} onChange={e => {setItemId(e.target.value);setSerialId("")}} disabled={!receiving}><option value="">{tr("Select")}</option>{receiving?.items.map(item => <option key={item.id} value={item.id}>{item.itemName ?? `#${item.itemModelId}`} — {tr("accepted")}: {item.acceptedQuantity}</option>)}</select></div>
                <div className="registration-field"><label>{tr("Accepted quantity remaining")}</label><input className="registration-input" value={remaining} readOnly /></div>
                <div className="registration-field"><label>{tr("Serial")}</label><select className="registration-input" value={serialId} onChange={e => setSerialId(e.target.value)} disabled={!acceptedSerials.length}><option value="">{acceptedSerials.length ? tr("Select") : tr("Not applicable")}</option>{acceptedSerials.map(serial => <option key={serial.id} value={serial.id} disabled={incorporatedSerialIds.has(serial.id)}>{serial.serialNumber}{incorporatedSerialIds.has(serial.id) ? ` — ${tr("incorporated")}` : ""}</option>)}</select></div>
                <div className="registration-field"><label>{tr("Initial stock location")}</label><select className="registration-input" value={locationId} onChange={e => setLocationId(e.target.value)}><option value="">{tr("Select")}</option>{locations.map(location => <option key={location.id} value={location.id}>{location.label}</option>)}</select></div>
                <div className="registration-field"><label>{tr("Asset code / patrimony")}</label><input className="registration-input" value={assetCode} onChange={e => setAssetCode(e.target.value)} placeholder={tr("Leave blank to generate automatically")}/></div>
                <div className="registration-field"><label>{tr("Lot number")}</label><input className="registration-input" value={lotNumber} onChange={e => setLotNumber(e.target.value)} placeholder={selectedItem?.lotNumber ?? ""}/></div>
                <div className="registration-field"><label>{tr("Quantity")}</label><input className="registration-input" type="number" min="0.0001" step="0.0001" value={quantity} onChange={e => setQuantity(e.target.value)} placeholder={String(remaining)}/></div>
                <div className="registration-field"><label>{tr("Incorporation value")}</label><input className="registration-input" type="number" min="0" step="0.0001" value={incorporationValue} onChange={e => setIncorporationValue(e.target.value)} placeholder={tr("Acquisition unit value when blank")}/></div>
                <div className="registration-field"><label>{tr("Initial condition")}</label><select className="registration-input" value={initialCondition} onChange={e => setInitialCondition(e.target.value)}>{conditions.map(value => <option key={value} value={value}>{tr(value)}</option>)}</select></div>
                <div className="registration-field comandos-grid-full"><label>{tr("Responsible person")}</label><input className="registration-input" value={responsible} onChange={e => setResponsible(e.target.value)}/></div>
                <div className="registration-field comandos-grid-full"><label>{tr("Notes")}</label><textarea className="registration-input" rows={4} value={notes} onChange={e => setNotes(e.target.value)}/></div>
            </div>

            <div className="comandos-responsive-actions"><Button className="registration-yellow-button" onClick={save} disabled={busy || !receiving || !itemId || !locationId || remaining <= 0}>{busy ? tr("Saving...") : tr("Incorporate")}</Button></div>

            <div style={{overflowX:"auto",marginTop:"1.5rem"}}><table style={{width:"100%",minWidth:"900px"}}><thead><tr><th>{tr("Item")}</th><th>{tr("Serial")}</th><th>{tr("Patrimony")}</th><th>{tr("Lot")}</th><th>{tr("Quantity")}</th><th>{tr("Value")}</th><th>{tr("Condition")}</th><th>{tr("Inventory record")}</th></tr></thead><tbody>{history.length ? history.map(row => <tr key={row.id}><td>#{row.receivingItemId}</td><td>{row.serialNumber ?? "—"}</td><td>{row.assetCode ?? "—"}</td><td>{row.lotNumber ?? "—"}</td><td>{row.quantity}</td><td>{row.incorporationValue}</td><td>{tr(row.initialCondition)}</td><td>{row.inventoryResource} #{row.inventoryRecordId}</td></tr>) : <tr><td colSpan={8}>{tr("No incorporations registered.")}</td></tr>}</tbody></table></div>
        </section>
    )
}
