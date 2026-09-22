"use client";

import * as React from "react";
import { Button } from "components/common/button";

type Box = { boxes: string; units: string };
const positive = /^[1-9][0-9]{0,14}$/;
const nonnegative = /^(0|[1-9][0-9]{0,14})$/;

/** Packaging is an input aid. Inventory and unit prices always use individual units. */
export function QuantityInput({ code, value, max, disabled, individual, wholeUnits = false, onChange }: {
    code: string; value: string; max?: string; disabled?: boolean; individual?: boolean; wholeUnits?: boolean;
    onChange: (quantity: string) => void;
}) {
    const [mode, setMode] = React.useState("UNITS");
    const [rows, setRows] = React.useState<Box[]>([{ boxes: "1", units: "50" }]);
    const [loose, setLoose] = React.useState("0");
    function converted(boxes: Box[], units: string) {
        if (!nonnegative.test(units) || boxes.some(row => !positive.test(row.boxes) || !positive.test(row.units))) return "";
        const total = boxes.reduce((sum, row) => sum + BigInt(row.boxes) * BigInt(row.units), BigInt(units)).toString();
        return total.length <= 15 && total !== "0" ? total : "";
    }
    return <fieldset disabled={disabled}>
        {!individual && <label>Quantity format for {code}
            <select aria-label={`Quantity format for ${code}`} value={mode} onChange={event => {
                setMode(event.target.value); onChange(event.target.value === "BOXES" ? converted(rows, loose) : value);
            }}><option value="UNITS">Units without boxes</option><option value="BOXES">Boxes and loose units</option></select></label>}
        {mode === "UNITS" ? <input type="number" aria-label={`Quantity for ${code}`} required min={wholeUnits ? "1" : "0.0001"}
            step={wholeUnits ? "1" : "0.0001"} max={max} value={value} disabled={individual}
            onChange={event => onChange(event.target.value)} /> : <>
            {rows.map((row, index) => <div key={index}>
                <label>Boxes <input aria-label={`Boxes ${index + 1} for ${code}`} type="text" inputMode="numeric" required maxLength={15} value={row.boxes}
                    onChange={event => { const next = rows.map((r, i) => i === index ? { ...r, boxes: event.target.value } : r); setRows(next); onChange(converted(next, loose)); }} /></label>
                <label>Units per box <input aria-label={`Units per box ${index + 1} for ${code}`} type="text" inputMode="numeric" required maxLength={15} value={row.units}
                    onChange={event => { const next = rows.map((r, i) => i === index ? { ...r, units: event.target.value } : r); setRows(next); onChange(converted(next, loose)); }} /></label>
                <Button type="button" aria-label={`Remove box row ${index + 1} for ${code}`} onClick={() => {
                    const next = rows.filter((_, i) => i !== index); setRows(next); onChange(converted(next, loose));
                }}>Remove box row</Button>
            </div>)}
            <Button type="button" disabled={rows.length >= 100} onClick={() => { const next = [...rows, { boxes: "1", units: "50" }]; setRows(next); onChange(converted(next, loose)); }}>Add box size</Button>
            <label>Loose units <input aria-label={`Loose units for ${code}`} type="text" inputMode="numeric" required maxLength={15} value={loose}
                onChange={event => { setLoose(event.target.value); onChange(converted(rows, event.target.value)); }} /></label>
            <p>Total units: {value || "Enter valid whole quantities"}</p>
        </>}
    </fieldset>;
}
