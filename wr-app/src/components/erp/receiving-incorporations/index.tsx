"use client"

import * as React from "react"
import { Button } from "@primereact/ui/button"
import { useComandosPreferences } from "components/settings/preferences-provider"
import { receivingIncorporationService } from "api/services/receiving-incorporation.service"

export const ReceivingIncorporationWorkspace: React.FC = () => {
    const { tr } = useComandosPreferences()
    const [receivingId, setReceivingId] = React.useState("")
    const [itemId, setItemId] = React.useState("")
    const [serialId, setSerialId] = React.useState("")
    const [locationId, setLocationId] = React.useState("")
    const [assetCode, setAssetCode] = React.useState("")
    const [lotNumber, setLotNumber] = React.useState("")
    const [responsible, setResponsible] = React.useState("")
    const [notes, setNotes] = React.useState("")
    const [saving, setSaving] = React.useState(false)

    const digits = (value: string) => value.replace(/\D/g, "")

    const save = async () => {
        if (!receivingId || !itemId || !locationId) return
        setSaving(true)
        try {
            await receivingIncorporationService.create({
                receivingId: Number(receivingId),
                receivingItemId: Number(itemId),
                receivingSerialId: serialId ? Number(serialId) : undefined,
                stockLocationId: Number(locationId),
                assetCode: assetCode || undefined,
                lotNumber: lotNumber || undefined,
                incorporatedBy: responsible || undefined,
                notes: notes || undefined
            })
        } finally {
            setSaving(false)
        }
    }

    return (
        <section className="registration-page">
            <div className="registration-header">
                <div>
                    <h1>{tr("Inventory incorporation")}</h1>
                    <p>{tr("Incorporate definitively accepted equipment into inventory.")}</p>
                </div>
            </div>

            <div className="comandos-responsive-form-grid">
                <div className="registration-field">
                    <label>{tr("Receiving")}</label>
                    <input className="registration-input" inputMode="numeric"
                        value={receivingId} onChange={(e) => setReceivingId(digits(e.target.value))} />
                </div>
                <div className="registration-field">
                    <label>{tr("Receiving item")}</label>
                    <input className="registration-input" inputMode="numeric"
                        value={itemId} onChange={(e) => setItemId(digits(e.target.value))} />
                </div>
                <div className="registration-field">
                    <label>{tr("Serial")}</label>
                    <input className="registration-input" inputMode="numeric"
                        value={serialId} onChange={(e) => setSerialId(digits(e.target.value))} />
                </div>
                <div className="registration-field">
                    <label>{tr("Stock location")}</label>
                    <input className="registration-input" inputMode="numeric"
                        value={locationId} onChange={(e) => setLocationId(digits(e.target.value))} />
                </div>
                <div className="registration-field">
                    <label>{tr("Asset code")}</label>
                    <input className="registration-input" value={assetCode}
                        onChange={(e) => setAssetCode(e.target.value)} />
                </div>
                <div className="registration-field">
                    <label>{tr("Lot number")}</label>
                    <input className="registration-input" value={lotNumber}
                        onChange={(e) => setLotNumber(e.target.value)} />
                </div>
                <div className="registration-field comandos-grid-full">
                    <label>{tr("Responsible person")}</label>
                    <input className="registration-input" value={responsible}
                        onChange={(e) => setResponsible(e.target.value)} />
                </div>
                <div className="registration-field comandos-grid-full">
                    <label>{tr("Notes")}</label>
                    <textarea className="registration-input" rows={4} value={notes}
                        onChange={(e) => setNotes(e.target.value)} />
                </div>
            </div>

            <div className="comandos-responsive-actions">
                <Button className="registration-yellow-button"
                    onClick={save} disabled={saving || !receivingId || !itemId || !locationId}>
                    {saving ? tr("Saving...") : tr("Incorporate")}
                </Button>
            </div>
        </section>
    )
}