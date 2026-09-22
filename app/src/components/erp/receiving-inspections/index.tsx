"use client"

import * as React from "react"
import { Button } from "components/ui/button"
import { useComandosPreferences } from "components/settings/preferences-provider"
import { receivingService } from "api/services/receiving.service"
import {
    receivingInspectionService,
    type ReceivingItemDecision
} from "api/services/receiving-inspection.service"
import type { ReceivingView } from "api/models/erp/receiving"

type Decision = {
    accepted: string
    rejected: string
    divergence: string
    rejectedSerialIds: number[]
}

export const ReceivingInspectionWorkspace: React.FC = () => {
    const { tr } = useComandosPreferences()
    const [receivingId, setReceivingId] = React.useState("")
    const [receiving, setReceiving] = React.useState<ReceivingView | null>(null)
    const [inspector, setInspector] = React.useState("")
    const [provisional, setProvisional] = React.useState(true)
    const [definitive, setDefinitive] = React.useState(false)
    const [nonConformity, setNonConformity] = React.useState("")
    const [notes, setNotes] = React.useState("")
    const [decisions, setDecisions] = React.useState<Record<number, Decision>>({})
    const [saving, setSaving] = React.useState(false)
    const [loading, setLoading] = React.useState(false)
    const [message, setMessage] = React.useState("")

    const load = async () => {
        if (!receivingId) return
        setLoading(true)
        setMessage("")
        try {
            const value = await receivingService.get(Number(receivingId))
            setReceiving(value)
            setDecisions(Object.fromEntries(value.items.map((item) => [
                item.id,
                {
                    accepted: String(
                        Number(item.acceptedQuantity) + Number(item.rejectedQuantity) > 0
                            ? item.acceptedQuantity
                            : item.receivedQuantity
                    ),
                    rejected: String(
                        Number(item.acceptedQuantity) + Number(item.rejectedQuantity) > 0
                            ? item.rejectedQuantity
                            : 0
                    ),
                    divergence: item.divergenceDescription || "",
                    rejectedSerialIds: item.serials
                        .filter((serial) => serial.accepted === false)
                        .map((serial) => serial.id)
                }
            ])))
        } catch {
            setReceiving(null)
            setMessage(tr("Receiving not found."))
        } finally {
            setLoading(false)
        }
    }

    const updateDecision = (itemId: number, patch: Partial<Decision>) => {
        setDecisions((current) => ({
            ...current,
            [itemId]: { ...current[itemId], ...patch }
        }))
    }

    const toggleSerial = (itemId: number, serialId: number, accepted: boolean) => {
        const current = decisions[itemId]
        if (!current) return

        const rejectedSerialIds = accepted
            ? current.rejectedSerialIds.filter((id) => id !== serialId)
            : Array.from(new Set([...current.rejectedSerialIds, serialId]))

        const item = receiving?.items.find((value) => value.id === itemId)
        if (!item) return

        updateDecision(itemId, {
            rejectedSerialIds,
            accepted: String(item.serials.length - rejectedSerialIds.length),
            rejected: String(rejectedSerialIds.length)
        })
    }

    const save = async () => {
        if (!receiving) return
        setSaving(true)
        setMessage("")
        try {
            const itemDecisions: ReceivingItemDecision[] = receiving.items.map((item) => {
                const decision = decisions[item.id]
                return {
                    receivingItemId: item.id,
                    acceptedQuantity: Number(decision.accepted),
                    rejectedQuantity: Number(decision.rejected),
                    divergenceDescription: decision.divergence || undefined,
                    serials: item.serials.map((serial) => ({
                        serialId: serial.id,
                        accepted: !decision.rejectedSerialIds.includes(serial.id),
                        rejectionReason: decision.rejectedSerialIds.includes(serial.id)
                            ? decision.divergence || nonConformity || tr("Rejected by inspection")
                            : undefined
                    }))
                }
            })

            const hasRejected = itemDecisions.some((item) => item.rejectedQuantity > 0)
            await receivingInspectionService.create(receiving.id, {
                inspector: inspector || undefined,
                approved: !hasRejected,
                provisionalReceipt: provisional,
                definitiveReceipt: definitive,
                nonConformity: nonConformity || undefined,
                decisionNotes: notes || undefined,
                items: itemDecisions
            })
            setMessage(tr("Inspection saved successfully."))
            await load()
        } catch (error) {
            setMessage(error instanceof Error ? error.message : tr("Unable to save inspection."))
        } finally {
            setSaving(false)
        }
    }

    const inspectionClosed = receiving
        ? ["DEFINITIVELY_ACCEPTED", "DEFINITIVELY_PARTIALLY_ACCEPTED", "REJECTED", "CANCELLED"].includes(receiving.status)
        : false

    const valid = receiving
        && !inspectionClosed
        && receiving.physicalChecked
        && receiving.documentsChecked
        && !receiving.items.some((item) => {
            const decision = decisions[item.id]
            if (!decision) return true
            return Number(decision.accepted) < 0
                || Number(decision.rejected) < 0
                || Number(decision.accepted) + Number(decision.rejected)
                    !== Number(item.receivedQuantity)
        })
        && !(provisional && definitive)

    return (
        <section className="registration-page">
            <div className="registration-header">
                <div>
                    <h1>{tr("Receiving inspection")}</h1>
                    <p>{tr("Inspect, accept or reject each delivered item before incorporation.")}</p>
                </div>
            </div>

            {message && <p>{message}</p>}
            {inspectionClosed && <p>{tr("Definitive receiving inspection is already closed.")}</p>}

            <div className="registration-form-grid">
                <div className="registration-field">
                    <label>{tr("Receiving")}</label>
                    <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                        <input className="registration-input" inputMode="numeric"
                            value={receivingId}
                            onChange={(event) =>
                                setReceivingId(event.target.value.replace(/\D/g, ""))} />
                        <Button onClick={load} disabled={!receivingId || loading}>
                            {loading ? tr("Loading...") : tr("Load")}
                        </Button>
                    </div>
                </div>

                <div className="registration-field">
                    <label>{tr("Inspector")}</label>
                    <input className="registration-input" value={inspector}
                        onChange={(event) => setInspector(event.target.value)} />
                </div>

                <label className="registration-field">
                    <span>{tr("Provisional receipt")}</span>
                    <input type="checkbox" checked={provisional}
                        onChange={(event) => {
                            setProvisional(event.target.checked)
                            if (event.target.checked) setDefinitive(false)
                        }} />
                </label>

                <label className="registration-field">
                    <span>{tr("Definitive receipt")}</span>
                    <input type="checkbox" checked={definitive}
                        onChange={(event) => {
                            setDefinitive(event.target.checked)
                            if (event.target.checked) setProvisional(false)
                        }} />
                </label>
            </div>

            {receiving && (
                <>
                    <p>
                        {tr("Physical check")}: {receiving.physicalChecked ? tr("Yes") : tr("No")}
                        {" · "}
                        {tr("Document check")}: {receiving.documentsChecked ? tr("Yes") : tr("No")}
                        {" · "}
                        {tr("Status")}: {tr(receiving.status)}
                    </p>

                    {(!receiving.physicalChecked || !receiving.documentsChecked) && (
                        <p>{tr("Physical and documentary checks are required before acceptance.")}</p>
                    )}

                    {receiving.items.map((item) => {
                        const decision = decisions[item.id]
                        if (!decision) return null

                        return (
                            <div className="registration-form-grid" key={item.id}>
                                <div className="registration-field">
                                    <label>{tr("Item")}</label>
                                    <input className="registration-input" readOnly
                                        value={item.itemName || `#${item.itemModelId || item.id}`} />
                                </div>

                                <div className="registration-field">
                                    <label>{tr("Received quantity")}</label>
                                    <input className="registration-input" readOnly
                                        value={item.receivedQuantity} />
                                </div>

                                <div className="registration-field">
                                    <label>{tr("Accepted quantity")}</label>
                                    <input className="registration-input" inputMode="decimal"
                                        value={decision.accepted}
                                        readOnly={item.serials.length > 0}
                                        onChange={(event) => updateDecision(item.id, {
                                            accepted: event.target.value
                                        })} />
                                </div>

                                <div className="registration-field">
                                    <label>{tr("Rejected quantity")}</label>
                                    <input className="registration-input" inputMode="decimal"
                                        value={decision.rejected}
                                        readOnly={item.serials.length > 0}
                                        onChange={(event) => updateDecision(item.id, {
                                            rejected: event.target.value
                                        })} />
                                </div>

                                <div className="registration-field registration-field-full">
                                    <label>{tr("Divergence / rejection reason")}</label>
                                    <textarea className="registration-input" rows={3}
                                        value={decision.divergence}
                                        onChange={(event) => updateDecision(item.id, {
                                            divergence: event.target.value
                                        })} />
                                </div>

                                {item.serials.length > 0 && (
                                    <div className="registration-field registration-field-full">
                                        <label>{tr("Serial inspection")}</label>
                                        <div style={{ overflowX: "auto" }}>
                                            <table style={{ width: "100%", minWidth: 520 }}>
                                                <thead>
                                                    <tr>
                                                        <th>{tr("Serial number")}</th>
                                                        <th>{tr("Accepted")}</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {item.serials.map((serial) => (
                                                        <tr key={serial.id}>
                                                            <td>{serial.serialNumber}</td>
                                                            <td>
                                                                <input
                                                                    type="checkbox"
                                                                    checked={!decision.rejectedSerialIds.includes(serial.id)}
                                                                    onChange={(event) =>
                                                                        toggleSerial(
                                                                            item.id,
                                                                            serial.id,
                                                                            event.target.checked
                                                                        )}
                                                                />
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                )}
                            </div>
                        )
                    })}

                    <div className="registration-form-grid">
                        <div className="registration-field registration-field-full">
                            <label>{tr("Non-conformity")}</label>
                            <textarea className="registration-input" rows={4}
                                value={nonConformity}
                                onChange={(event) => setNonConformity(event.target.value)} />
                        </div>
                        <div className="registration-field registration-field-full">
                            <label>{tr("Decision notes")}</label>
                            <textarea className="registration-input" rows={4}
                                value={notes}
                                onChange={(event) => setNotes(event.target.value)} />
                        </div>
                    </div>
                </>
            )}

            <Button className="registration-yellow-button" onClick={save}
                disabled={saving || !valid}>
                {saving ? tr("Saving...") : tr("Save")}
            </Button>
        </section>
    )
}
