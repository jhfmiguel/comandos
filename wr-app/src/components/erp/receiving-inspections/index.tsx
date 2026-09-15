"use client"

import * as React from "react"
import { Button } from "@primereact/ui/button"
import { useComandosPreferences } from "components/settings/preferences-provider"
import { receivingInspectionService } from "api/services/receiving-inspection.service"

export const ReceivingInspectionWorkspace: React.FC = () => {
    const { tr } = useComandosPreferences()
    const [receivingId, setReceivingId] = React.useState("")
    const [inspector, setInspector] = React.useState("")
    const [approved, setApproved] = React.useState(true)
    const [provisional, setProvisional] = React.useState(true)
    const [definitive, setDefinitive] = React.useState(false)
    const [nonConformity, setNonConformity] = React.useState("")
    const [notes, setNotes] = React.useState("")
    const [saving, setSaving] = React.useState(false)

    const save = async () => {
        if (!receivingId) return
        setSaving(true)
        try {
            await receivingInspectionService.create(Number(receivingId), {
                inspector: inspector || undefined,
                approved,
                provisionalReceipt: provisional,
                definitiveReceipt: definitive,
                nonConformity: nonConformity || undefined,
                decisionNotes: notes || undefined
            })
        } finally {
            setSaving(false)
        }
    }

    return (
        <section className="registration-page">
            <div className="registration-header">
                <div>
                    <h1>{tr("Receiving inspection")}</h1>
                    <p>{tr("Inspect delivered equipment before definitive incorporation into inventory.")}</p>
                </div>
            </div>

            <div className="registration-form-grid">
                <div className="registration-field">
                    <label>{tr("Receiving")}</label>
                    <input className="registration-input" inputMode="numeric" value={receivingId}
                        onChange={(e) => setReceivingId(e.target.value.replace(/\D/g, ""))} />
                </div>
                <div className="registration-field">
                    <label>{tr("Inspector")}</label>
                    <input className="registration-input" value={inspector}
                        onChange={(e) => setInspector(e.target.value)} />
                </div>
                <label className="registration-field">
                    <span>{tr("Approved")}</span>
                    <input type="checkbox" checked={approved} onChange={(e) => setApproved(e.target.checked)} />
                </label>
                <label className="registration-field">
                    <span>{tr("Provisional receipt")}</span>
                    <input type="checkbox" checked={provisional}
                        onChange={(e) => setProvisional(e.target.checked)} />
                </label>
                <label className="registration-field">
                    <span>{tr("Definitive receipt")}</span>
                    <input type="checkbox" checked={definitive}
                        onChange={(e) => setDefinitive(e.target.checked)} />
                </label>
                <div className="registration-field registration-field-full">
                    <label>{tr("Non-conformity")}</label>
                    <textarea className="registration-input" rows={4} value={nonConformity}
                        onChange={(e) => setNonConformity(e.target.value)} />
                </div>
                <div className="registration-field registration-field-full">
                    <label>{tr("Decision notes")}</label>
                    <textarea className="registration-input" rows={4} value={notes}
                        onChange={(e) => setNotes(e.target.value)} />
                </div>
            </div>

            <Button className="registration-yellow-button" onClick={save}
                disabled={saving || !receivingId}>
                {saving ? tr("Saving...") : tr("Save")}
            </Button>
        </section>
    )
}