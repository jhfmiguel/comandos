"use client";

import * as React from "react";
import { httpClient } from "api/http";

type Evidence = { id: number; fileName: string; description: string | null; uploadedByLogin: string | null };

export function LifecycleEvidence({ resource, recordId, editable }: { resource: string; recordId: number; editable: boolean }) {
    const [items, setItems] = React.useState<Evidence[]>([]);
    const [file, setFile] = React.useState<File | null>(null);
    const [error, setError] = React.useState("");
    const [busy, setBusy] = React.useState(false);
    const input = React.useRef<HTMLInputElement>(null);
    React.useEffect(() => {
        const controller = new AbortController();
        httpClient.get<Evidence[]>("/api/erp/lifecycle/attachments", { params: { resource, recordId }, signal: controller.signal })
            .then(r => setItems(r.data)).catch(() => { if (!controller.signal.aborted) setError("Unable to load evidence."); });
        return () => controller.abort();
    }, [resource, recordId]);
    async function upload(e: React.FormEvent) {
        e.preventDefault();
        if (!file || busy || !editable) return;
        setBusy(true); setError("");
        try {
            if (file.size > 10 * 1024 * 1024) throw Error("Attachment exceeds 10 MB.");
            const base64 = await new Promise<string>((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => resolve(String(reader.result).split(",")[1]);
                reader.onerror = () => reject(Error("Unable to read file."));
                reader.readAsDataURL(file);
            });
            const response = await httpClient.post<Evidence>("/api/erp/lifecycle/attachments", { resource, recordId, fileName: file.name, contentType: file.type || "application/octet-stream", base64 });
            setItems(v => [...v, response.data]); setFile(null);
            if (input.current) input.current.value = "";
        } catch { setError("Unable to attach evidence. Check permissions and the 10 MB file limit."); }
        finally { setBusy(false); }
    }
    return <section><h3>Evidence</h3>{error && <p role="alert">{error}</p>}
        <ul>{items.map(item => <li key={item.id}><a href={`/api/erp/lifecycle/attachments/${item.id}/content`} download>{item.fileName}</a> · {item.uploadedByLogin}</li>)}</ul>
        {editable && <form onSubmit={upload}><label htmlFor={`evidence-${resource}-${recordId}`}>Evidence file (up to 10 MB)</label><input ref={input} id={`evidence-${resource}-${recordId}`} type="file" disabled={busy} onChange={e => setFile(e.target.files?.[0] || null)} /><button disabled={!file || busy}>Attach evidence</button></form>}
    </section>;
}
