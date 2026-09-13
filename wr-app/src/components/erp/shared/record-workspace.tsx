"use client";

import * as React from "react";

import axios from "axios";

import { Plus, Pencil, Trash } from "@primeicons/react";
import { Button } from "@primereact/ui/button";
import { Dialog } from "@primereact/ui/dialog";
import { InputText } from "@primereact/ui/inputtext";

import { Layout } from "components/layout";
import { Message } from "components/common/message";
import { useSession } from "components/auth/session-provider";
import type { ErpField, ErpPage, ErpRecord, ErpResource, ErpValue } from "api/models/erp";
import { createErpService, type ErpService, type ErpModule } from "api/services/erp.service";
import styles from "./workspace.module.css";


function errorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const detail = error.response?.data?.detail;
        return typeof detail === "string" ? detail : "Unable to complete the request. Check the API connection and try again.";
    }
    return "Unable to complete the request. Please try again.";
}


export function RecordWorkspace({ module, title, initialResource, description }: {
    
    module: ErpModule; 
    title: string; 
    initialResource: string; 
    description: string;

}) {

    const service = React.useMemo(() => createErpService(module), [module]);
    const { session } = useSession();
    const accessRevision = JSON.stringify(session?.access);
    
    const [catalog, setCatalog] = React.useState<ErpResource[]>([]);
    const [catalogLoaded, setCatalogLoaded] = React.useState(false);
    const [selected, setSelected] = React.useState(initialResource);
    const [error, setError] = React.useState("");
    const [retry, setRetry] = React.useState(0);

    React.useEffect(() => {

        const controller = new AbortController();

        service.catalog(controller.signal).then(data => {
            if (!controller.signal.aborted) { setCatalog(data); setCatalogLoaded(true); }
        }).catch(error => {
            if (!controller.signal.aborted) setError(errorMessage(error));
        });

        return () => controller.abort();

    }, [retry, service, accessRevision]);

    const resource = catalog.find(item => item.key === selected) ?? catalog[0];
    
    return (

        <Layout title={title}>

            <div className={styles.workspace}>
                
                <p className={styles.intro}>{description}</p>
                
                {error && <Message type="error" text={error} />}
                {error && <Button type="button" onClick={() => { setError(""); setRetry(value => value + 1); }}>Retry</Button>}
                {!catalogLoaded && !error && <p role="status">Loading records…</p>}
                {catalogLoaded && !catalog.length && <Message type="info" text="No resources are available for your access profile." />}
                {!!catalog.length && (
                    
                    <div className={styles.shell}>
                        
                        <nav aria-label={`${title} resources`} className={styles.navigation}>
                            {[...new Set(catalog.map(item => item.group))].map(group => (
                                <div key={group}>
                                    
                                    <p className={styles.group}>{group}</p>
                                    {catalog.filter(item => item.group === group).map(item => (
                                        <button key={item.key} type="button" aria-current={resource?.key === item.key ? "page" : undefined}
                                            onClick={() => setSelected(item.key)}>{item.label}</button>
                                    ))}

                                </div>
                            ))}
                        </nav>
                        {resource && <ResourcePanel key={resource.key} resource={resource} service={service} />}

                    </div>
                )}
            </div>
        </Layout>
    );
}

function ResourcePanel({ resource, service }: { resource: ErpResource; service: ErpService }) {
    const allowed = (action: string) => !resource.readOnly && (!resource.actions || resource.actions.includes(action));
    
    const [search, setSearch] = React.useState("");
    const [page, setPage] = React.useState(0);
    const [result, setResult] = React.useState<ErpPage | null>(null);
    const [loading, setLoading] = React.useState(true);
    const [revision, setRevision] = React.useState(0);
    const [notice, setNotice] = React.useState<{ type: string; text: string } | null>(null);
    const [editor, setEditor] = React.useState<{ record?: ErpRecord } | null>(null);
    const [deleting, setDeleting] = React.useState<ErpRecord | null>(null);
    const [busy, setBusy] = React.useState(false);

    React.useEffect(() => {

        const controller = new AbortController();
        
        const timer = setTimeout(() => {
            service.list(resource.key, search, page, controller.signal).then(data => {
                if (!controller.signal.aborted) setResult(data);
            }).catch(error => {
                if (!controller.signal.aborted) { setResult(null); setNotice({ type: "error", text: errorMessage(error) }); }
            }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
        }, 250);
        return () => { clearTimeout(timer); controller.abort(); };
    }, [resource.key, search, page, revision, service]);

    const refresh = () => { 
        
        setLoading(true); 
        setRevision(value => value + 1); 

    };
    
    const columns = resource.fields
                                .filter(field => field.type !== "password")
                                .filter((field, index) => resource.readOnly || index < 4 || ["status", "availableQuantity"]
                                .includes(field.name));
    
    const display = (record: ErpRecord, field: ErpField): string => {
        
        const value = record[field.name];
        
        if (value == null || value === "") return "—";
        if (field.type === "reference") return record.referenceLabels[field.name] || `#${value}`;
        if (field.type === "boolean") return value ? "Yes" : "No";

        return String(value).replaceAll("_", " ");
    
    };

    return (

        <section className={styles.panel} aria-labelledby="resource-title">
            
            <div className={styles.toolbar}>
                
                <h1 id="resource-title">{resource.label}</h1>
                {allowed("CREATE") && 
                <Button type="button" className="registration-yellow-button" onClick={() => setEditor({})}>
                    <Plus size={16} /><span>New record</span>
                </Button>
                }
            </div>
            
            {notice && 
            <Message type={notice.type} text={notice.text} onClose={() => setNotice(null)} />
            }
            
            <div className={styles.toolbar}>
                
                <label className={styles.search}>Search
                    <InputText 
                        value={search} 
                        placeholder={`Search ${resource.label.toLowerCase()}`}
                        onChange={
                            (event: React.ChangeEvent<HTMLInputElement>) => { 
                                setLoading(true); setSearch(event.target.value); setPage(0); 
                                }
                        } 
                    />
                </label>

                <Button 
                    type="button" 
                    severity="secondary" 
                    onClick={refresh} 
                    disabled={loading}>
                Refresh
                </Button>

            </div>

            <div className={styles.tableContainer} aria-busy={loading}>
                
                <table>
                    
                    <thead>
                        <tr>
                            <th>ID</th>
                            {columns.map(field => 
                            <th key={field.name}>{field.label}</th>)}
                            {!resource.readOnly && 
                            <th>Actions</th>}
                        </tr>
                    </thead>

                    <tbody>
                        {!loading && result?.content.map(record => (
                        <tr key={record.id}>
                            <td>{record.id}</td>
                            {columns.map(field => 
                            <td key={field.name}>{display(record, field)}</td>)}
                            {!resource.readOnly && 
                            <td>
                                <div className={styles.actions}>
                                    <Button 
                                        type="button" 
                                        variant="text" 
                                        severity="secondary" 
                                        aria-label={`Edit ${record.label}`} 
                                        disabled={!allowed("UPDATE")}
                                        onClick={() => setEditor({ record })}>
                                            <Pencil size={18} />
                                    </Button>
                                    <Button 
                                        type="button" 
                                        variant="text" 
                                        severity="danger" 
                                        aria-label={`Delete ${record.label}`} 
                                        disabled={!allowed("DELETE")}
                                        onClick={() => setDeleting(record)}>
                                            <Trash size={18} />
                                    </Button>
                                </div>
                            </td>}
                        </tr>
                        ))}

                    </tbody>

                </table>
                {loading && <p role="status">Loading records…</p>}
                {!loading && result?.content.length === 0 && <p>No records found.</p>}
            </div>
            
            <div className={styles.pagination}>
                
                <span>{result?.totalElements ?? 0} records · Page {page + 1}</span>

                <Button 
                    type="button" 
                    severity="secondary" 
                    disabled={loading || page === 0} 
                    onClick={() => { setLoading(true); setPage(value => value - 1); }}>
                        Previous
                    </Button>
                <Button 
                    type="button" 
                    severity="secondary" 
                    disabled={loading || !result || (page + 1) * result.size >= result.totalElements} 
                    onClick={() => { setLoading(true); setPage(value => value + 1); }}>
                        Next
                </Button>
            
            </div>

            {editor && 
            <RecordEditor 
                resource={resource} 
                service={service} 
                record={editor.record} 
                onCancel={() => setEditor(null)} 
                onSaved={() => {
                    setEditor(null); 
                    setNotice({ type: "success", text: "Record saved successfully." }); 
                    refresh();
                }} 
            />
            }
            
            <Dialog.Root open={deleting !== null} onOpenChange={(event: { value?: boolean }) => { if (!event.value && !busy) setDeleting(null); }}>
                
                <Dialog.Portal>
                    
                    <Dialog.Backdrop />
                    
                    <Dialog.Positioner>
                        
                        <Dialog.Popup className={styles.dialog}>
                            
                            <Dialog.Header>
                                <Dialog.Title>Delete record</Dialog.Title>
                            </Dialog.Header>
                            
                            <Dialog.Content>

                                <p>Delete {deleting?.label}? Records referenced by another record cannot be deleted.</p>
                                <div className={styles.actions}>
                                    <Button type="button" severity="secondary" disabled={busy} onClick={() => setDeleting(null)}>Cancel</Button>
                                    <Button 
                                        type="button" 
                                        severity="danger" 
                                        disabled={busy} 
                                        onClick={async () => {
                                            if (!deleting || busy) return;
                                            setBusy(true);
                                            try {
                                                await service.remove(resource.key, deleting);
                                                setDeleting(null); setNotice({ type: "success", text: "Record deleted successfully." });
                                                if (result?.content.length === 1 && page > 0) setPage(value => value - 1);
                                                refresh();
                                            } catch (error) { setDeleting(null); setNotice({ type: "error", text: errorMessage(error) }); }
                                            finally { setBusy(false); }
                                    }}>
                                        {busy ? "Deleting…" : "Delete"}
                                    </Button>
                                </div>

                            </Dialog.Content>

                        </Dialog.Popup>

                    </Dialog.Positioner>

                </Dialog.Portal>

            </Dialog.Root>

        </section>
    );
}

function RecordEditor({ resource, service, record, onCancel, onSaved }: {

    resource: ErpResource; 
    service: ErpService; 
    record?: ErpRecord; 
    onCancel: () => void; 
    onSaved: () => void;

}) {

    const [values, setValues] = React.useState<Record<string, ErpValue>>
        (() => Object.fromEntries(resource.fields.map(field => 
        [field.name, field.type === "password" ? "" 
            : record?.[field.name] 
            ?? (field.type === "boolean" ? field.name === "active" : "")])) as Record<string, ErpValue>);
    
    const [busy, setBusy] = React.useState(false);
    const saving = React.useRef(false);
    const [error, setError] = React.useState("");
    
    const change = (field: ErpField, value: ErpValue) => 
        setValues(current => ({ ...current, [field.name]: value,
        ...(field.name === "organizationId" ? { ...(resource.fields.some(f => f.name === "unitId") ? { unitId: "" } : {}), 
        ...(resource.fields.some(f => f.name === "parentUnitId") ? { parentUnitId: "" } : {}) } : {})
    }));

    return (
        
        <Dialog.Root open onOpenChange={(event: { value?: boolean }) => { if (!event.value && !busy) onCancel(); }}>
            
            <Dialog.Portal>
                
                <Dialog.Backdrop />
                
                <Dialog.Positioner>
                
                    <Dialog.Popup className={styles.dialog}>
                        
                        <Dialog.Header>
                            <Dialog.Title>{record ? "Edit" : "New"} · {resource.label}</Dialog.Title>
                        </Dialog.Header>
                    
                        <Dialog.Content>
                            
                            <form 
                                className={styles.form} 
                                onSubmit={async event => {
                                    event.preventDefault();
                                    if (saving.current) return;
                                    saving.current = true; setBusy(true); setError("");
                                    try {
                                        const payload = Object.fromEntries(resource.fields.filter(field => !field.readOnly).map(field => [field.name, values[field.name]]));
                                        await service.save(resource.key, { ...payload, ...(record ? { version: record.version } : {}) }, record?.id);
                                        onSaved();
                                    } catch (error) { setError(errorMessage(error)); }
                                    finally { saving.current = false; setBusy(false); }
                            }}>
                                {error && <Message type="error" text={error} onClose={() => setError("")} />}
                                <fieldset disabled={busy} className={styles.fields}>
                                    {resource.fields.map(field => {
                                        const required = field.required && !(record && field.type === "password");
                                        return <fieldset key={field.name} className={styles.field} disabled={field.readOnly || Boolean(record && field.createOnly)}>
                                            <label htmlFor={`core-${field.name}`}>{field.label}{required ? " *" : ""}</label>
                                            {resource.key === "profiles" && field.name === "level" && <small>Use SYSTEM (all organizations), ORGANIZATION or UNIT. Other levels do not grant access.</small>}
                                            {resource.key === "permissions" && field.name === "resource" && <small>Use an exact resource code, such as core/people, inventory/assets, sales or security/access. An asterisk grants all resources.</small>}
                                            {resource.key === "permissions" && field.name === "action" && <small>Use READ, CREATE, UPDATE, DELETE, MANAGE (access administration), or *.</small>}
                                            {resource.key === "user-profiles" && field.name === "unitId" && <small>Required for UNIT profiles; leave empty for SYSTEM and ORGANIZATION profiles.</small>}
                                            {field.type === "reference" ? (
                                                <ReferenceField service={service} field={field} value={values[field.name]} selectedLabel={record?.referenceLabels[field.name]}
                                                    organizationId={values.organizationId} excludedId={["parentUnitId", "parentCategoryId"].includes(field.name) ? record?.id : undefined}
                                                    onChange={value => change(field, value)} />
                                            ) : field.type === "boolean" ? (
                                                <input id={`core-${field.name}`} type="checkbox" checked={Boolean(values[field.name])} onChange={event => change(field, event.target.checked)} />
                                            ) : field.type === "choice" ? (
                                                <select id={`core-${field.name}`} required={required} value={String(values[field.name] ?? "")} onChange={event => change(field, event.target.value)}>
                                                    <option value="">Select an option</option>
                                                    {field.choices.map(value => <option key={value} value={value}>{value.replaceAll("_", " ")}</option>)}
                                                </select>
                                            ) : (
                                                <input id={`core-${field.name}`} type={["decimal", "integer"].includes(field.type) ? "number" : field.type}
                                                    step={field.type === "decimal" ? "0.0001" : field.type === "integer" ? "1" : undefined}
                                                    min={["decimal", "integer"].includes(field.type) ? "0" : undefined} required={required} value={String(values[field.name] ?? "")}
                                                    maxLength={field.type === "password" ? 72 : 255} minLength={field.type === "password" ? 12 : undefined}
                                                    autoComplete={field.type === "password" ? "new-password" : undefined}
                                                    onChange={event => change(field, event.target.value)} />
                                            )}
                                            {field.type === "password" && <small>At least 12 characters. {record ? "Leave blank to keep the current password." : ""}</small>}
                                            {field.readOnly && <small>Calculated by inventory operations.</small>}
                                            {record && field.createOnly && <small>Fixed after registration.</small>}
                                        </fieldset>;
                                    })}
                                </fieldset>
                                <div className={styles.actions}>
                                    <Button type="button" severity="secondary" disabled={busy} onClick={onCancel}>Cancel</Button>
                                    <Button type="submit" className="registration-yellow-button" disabled={busy}>{busy ? "Saving…" : "Save"}</Button>
                                </div>
                            </form>
                        </Dialog.Content>
                    </Dialog.Popup>
                </Dialog.Positioner>
            </Dialog.Portal>
        </Dialog.Root>
    );
}

export function ReferenceField({ service, field, value, selectedLabel, organizationId, excludedId, optionFilter, onChange }: {
    service: ErpService; field: ErpField; value: ErpValue; selectedLabel?: string; organizationId: ErpValue;
    excludedId?: number; optionFilter?: (record: ErpRecord) => boolean; onChange: (value: ErpValue) => void;
}) {
    const [search, setSearch] = React.useState("");
    const [result, setResult] = React.useState<ErpPage | null>(null);
    const [error, setError] = React.useState("");
    React.useEffect(() => {
        const controller = new AbortController();
        const timer = setTimeout(() => {
            service.list(field.reference!, search, 0, controller.signal, ["units", "core/units"].includes(field.reference ?? "") ? organizationId : undefined).then(data => {
                if (!controller.signal.aborted) { setResult(data); setError(""); }
            }).catch(error => { if (!controller.signal.aborted) setError(errorMessage(error)); });
        }, 250);
        return () => { clearTimeout(timer); controller.abort(); };
    }, [field.reference, search, organizationId, service]);
    const options = (result?.content ?? []).filter(item => item.id !== excludedId && (!optionFilter || optionFilter(item)) &&
        !(["units", "core/units"].includes(field.reference ?? "") && organizationId && String(item.organizationId) !== String(organizationId)));
    const current = String(value ?? "");
    return (
        <div className={styles.reference}>
            <input type="search" aria-label={`Search ${field.label.toLowerCase()}`} placeholder="Type to find a record"
                value={search} onChange={event => setSearch(event.target.value)} />
            <select id={`core-${field.name}`} value={current} required={field.required} disabled={["units", "core/units"].includes(field.reference ?? "") && !organizationId}
                onChange={event => onChange(event.target.value ? Number(event.target.value) : null)}>
                <option value="">Select a record</option>
                {current && !options.some(option => String(option.id) === current) && <option value={current}>{selectedLabel || `#${current}`}</option>}
                {options.map(option => <option key={option.id} value={option.id}>{option.label}</option>)}
            </select>
            {["units", "core/units"].includes(field.reference ?? "") && !organizationId && <small>Select an organization first.</small>}
            {result && result.totalElements > result.size && <small>Showing the first 20 matches. Refine your search for more records.</small>}
            {error && <small role="alert">{error}</small>}
        </div>
    );
}
