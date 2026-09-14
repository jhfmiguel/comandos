"use client";

import * as React from "react";

import axios from "axios";

import { Plus, Pencil, Trash } from "@primeicons/react";
import { Button } from "@primereact/ui/button";
import { Dialog } from "@primereact/ui/dialog";
import { InputText } from "@primereact/ui/inputtext";
import {
    DataTable,
    FilterMatchMode
} from "@primereact/ui/datatable";

import type {
    DataTableFilterInstance,
    DataTableFilterMeta,
    DataTablePaginationInstance
} from "@primereact/ui/datatable";

import { Paginator } from "@primereact/ui/paginator";

import type {
    PaginatorPagesInstance,
    PaginatorRootChangeEvent
} from "@primereact/ui/paginator";

import { AngleDoubleLeft } from "@primeicons/react/angle-double-left";
import { AngleDoubleRight } from "@primeicons/react/angle-double-right";
import { AngleLeft } from "@primeicons/react/angle-left";
import { AngleRight } from "@primeicons/react/angle-right";
import { EllipsisH } from "@primeicons/react/ellipsis-h";

import { Layout } from "components/layout";
import { StockIntakeEditor } from "components/erp/inventory/stock-intake-editor";
import { Message } from "components/common/message";
import { useSession } from "components/auth/session-provider";
import type { ErpField, ErpPage, ErpRecord, ErpResource, ErpValue } from "api/models/erp";
import { createErpService, type ErpService, type ErpModule } from "api/services/erp.service";
import styles from "./workspace.module.css";


function createErpTableFilters(
    fields: ErpField[]
): DataTableFilterMeta {

    const filters: DataTableFilterMeta = {};

    fields.forEach((field) => {
        filters[field.name] = {
            value: null,
            matchMode: FilterMatchMode.Contains
        };
    });

    return filters;

}

function getErpFilterSearch(
    filters: DataTableFilterMeta
): string {

    return Object.values(filters)
        .map((filter) => {

            if (
                !filter ||
                typeof filter !== "object" ||
                !("value" in filter)
            ) {
                return "";
            }

            const value = filter.value;

            return value == null
                ? ""
                : String(value).trim();

        })
        .filter(Boolean)
        .join(" ");

}

function errorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const detail = error.response?.data?.detail;
        return typeof detail === "string" ? detail : "Unable to complete the request. Check the API connection and try again.";
    }
    return "Unable to complete the request. Please try again.";
}


export function RecordWorkspace({ module, title, initialResource, description, showNavigation = true }: {
    module: ErpModule;
    title: string;
    initialResource: string;
    description: string;
    showNavigation?: boolean;
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

    const normalizeResourceName = (value: string): string =>
        value
            .trim()
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .replace(/[^a-z0-9]+/g, "-")
            .replace(/^-+|-+$/g, "");

    const requestedCatalogResource = catalog.find((item) =>
        item.key === initialResource ||
        normalizeResourceName(item.label) === initialResource
    );

    const selectedResourceKey = showNavigation
        ? selected
        : requestedCatalogResource?.key ?? initialResource;

    const resource = catalog.find(
        (item) => item.key === selectedResourceKey
    ) ?? catalog[0];
    
    return (

        <Layout title={title}>

            <div className={styles.workspace}>
                
                <p className={styles.intro}>{description}</p>
                
                {error && <Message type="error" text={error} />}
                {error && <Button type="button" onClick={() => { setError(""); setRetry(value => value + 1); }}>Retry</Button>}
                {!catalogLoaded && !error && <p role="status">Loading records…</p>}
                {catalogLoaded && !catalog.length && <Message type="info" text="No resources are available for your access profile." />}
                {!!catalog.length && (
                    
                    <div className={showNavigation ? styles.shell : `${styles.shell} ${styles.shellSingle}`}>
                        
                        {showNavigation && (

                        
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

                        
                        )}
                        {resource && <ResourcePanel key={resource.key} resource={resource} service={service} />}

                    </div>
                )}
            </div>
        </Layout>
    );
}

function getErpServerFilters(
    filters: DataTableFilterMeta
): Record<string, string> {

    return Object.fromEntries(
        Object.entries(filters)
            .map(([field, filter]) => {

                if (
                    !filter ||
                    typeof filter !== "object" ||
                    !("value" in filter)
                ) {
                    return [field, ""];
                }

                const value = filter.value;

                return [
                    field,
                    value == null
                        ? ""
                        : String(value).trim()
                ];

            })
            .filter(([, value]) => value !== "")
    );

}

function ResourcePanel({ resource, service }: { resource: ErpResource; service: ErpService }) {
    const allowed = (action: string) => !resource.readOnly && (!resource.actions || resource.actions.includes(action));
    
    const [search, setSearch] = React.useState("");
    const [serverFilters, setServerFilters] = React.useState<Record<string, string>>({});
    const [page, setPage] = React.useState(0);
    const [filters, setFilters] = React.useState<DataTableFilterMeta>(
        () => createErpTableFilters(resource.fields)
    );
    const filterTimeoutRef = React.useRef<ReturnType<typeof setTimeout> | null>(null);
    const [result, setResult] = React.useState<ErpPage | null>(null);
    const [loading, setLoading] = React.useState(true);
    const [revision, setRevision] = React.useState(0);
    const [notice, setNotice] = React.useState<{ type: string; text: string } | null>(null);
    const [editor, setEditor] = React.useState<{ record?: ErpRecord } | null>(null);
    const [intake, setIntake] = React.useState(false);
    const [deleting, setDeleting] = React.useState<ErpRecord | null>(null);
    const [busy, setBusy] = React.useState(false);

    React.useEffect(() => {

        const controller = new AbortController();
        
        const timer = setTimeout(() => {
            service.list(resource.key, search, page, controller.signal, undefined, serverFilters).then(data => {
                if (!controller.signal.aborted) setResult(data);
            }).catch(error => {
                if (!controller.signal.aborted) { setResult(null); setNotice({ type: "error", text: errorMessage(error) }); }
            }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
        }, 250);
        return () => { clearTimeout(timer); controller.abort(); };
    }, [resource.key, search, serverFilters, page, revision, service]);

    const refresh = () => { 
        
        setLoading(true); 
        setRevision(value => value + 1); 

    };
    
    const handleTableFilter = (event: {
        filters: DataTableFilterMeta;
    }): void => {

        setFilters(event.filters);
        setPage(0);

        if (filterTimeoutRef.current) {
            clearTimeout(filterTimeoutRef.current);
        }

        filterTimeoutRef.current = setTimeout(() => {
            setLoading(true);
            setServerFilters(getErpServerFilters(event.filters));
        }, 400);

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
                <Button type="button" className="registration-yellow-button" onClick={() => resource.key === "assets" ? setIntake(true) : setEditor({})}>
                    <Plus size={16} /><span>New record</span>
                </Button>
                }
                {resource.key === "assets" && allowed("CREATE") && <Button type="button" severity="secondary"
                    onClick={() => setEditor({})}>Register single asset</Button>}
                {resource.key === "lots" && allowed("CREATE") && <Button type="button" className="registration-yellow-button"
                    onClick={() => setIntake(true)}>Receive ammunition boxes</Button>}
            </div>
            
            {notice && 
            <Message type={notice.type} text={notice.text} onClose={() => setNotice(null)} />
            }
<div className={styles.tableContainer} aria-busy={loading}>
                <DataTable.Root
                    data={result?.content ?? []}
                    dataKey="id"
                    lazy
                    paginator
                    rows={result?.size ?? 10}
                    totalRecords={result?.totalElements ?? 0}
                    first={page * (result?.size ?? 10)}
                    filters={filters}
                    onFilter={handleTableFilter}
                    style={{ width: "100%" }}
                >
                    <DataTable.TableContainer
                        style={{
                            width: "100%",
                            overflowX: "auto"
                        }}
                    >
                        <DataTable.Table
                            style={{
                                width: "100%",
                                minWidth: "900px",
                                tableLayout: "auto"
                            }}
                        >
                            <DataTable.THead>
                                <DataTable.THeadRow>
                                    <DataTable.THeadCell>
                                        ID
                                    </DataTable.THeadCell>

                                    {columns.map((field) => (
                                        <DataTable.THeadCell key={field.name}>
                                            {field.label}
                                        </DataTable.THeadCell>
                                    ))}

                                    {!resource.readOnly && (
                                        <DataTable.THeadCell
                                            style={{
                                                width: "8rem",
                                                textAlign: "center"
                                            }}
                                        >
                                            Actions
                                        </DataTable.THeadCell>
                                    )}
                                </DataTable.THeadRow>

                                <DataTable.THeadRow>
                                    <DataTable.THeadCell />

                                    {columns.map((field) => (
                                        <DataTable.THeadCell key={field.name}>
                                            <DataTable.Filter
                                                field={field.name}
                                                display="row"
                                                dataType="text"
                                            >
                                                {({
                                                    value,
                                                    onChange
                                                }: DataTableFilterInstance) => (
                                                    <InputText
                                                        value={
                                                            value == null
                                                                ? ""
                                                                : String(value)
                                                        }
                                                        onChange={(
                                                            event: React.ChangeEvent<HTMLInputElement>
                                                        ) => {
                                                            onChange(
                                                                event,
                                                                event.target.value
                                                            );
                                                        }}
                                                        placeholder={`Search ${field.label.toLowerCase()}...`}
                                                        size="small"
                                                        fluid
                                                    />
                                                )}
                                            </DataTable.Filter>
                                        </DataTable.THeadCell>
                                    ))}

                                    {!resource.readOnly && (
                                        <DataTable.THeadCell />
                                    )}
                                </DataTable.THeadRow>
                            </DataTable.THead>

                            <DataTable.TBody>
                                {({
                                    item,
                                    index
                                }) => {
                                    const record =
                                        item as ErpRecord;

                                    return (
                                    <DataTable.Row
                                        key={
                                            record.id ??
                                            index
                                        }
                                    >
                                        <DataTable.Cell>
                                            {record.id}
                                        </DataTable.Cell>

                                        {columns.map((field) => (
                                            <DataTable.Cell key={field.name}>
                                                {display(record, field)}
                                            </DataTable.Cell>
                                        ))}

                                        {!resource.readOnly && (
                                            <DataTable.Cell>
                                                <div className={styles.actions}>
                                                    <Button
                                                        type="button"
                                                        variant="text"
                                                        severity="secondary"
                                                        aria-label={`Edit ${record.label}`}
                                                        disabled={!allowed("UPDATE")}
                                                        onClick={() => setEditor({ record })}
                                                    >
                                                        <Pencil size={18} />
                                                    </Button>

                                                    <Button
                                                        type="button"
                                                        variant="text"
                                                        severity="danger"
                                                        aria-label={`Delete ${record.label}`}
                                                        disabled={!allowed("DELETE")}
                                                        onClick={() => setDeleting(record)}
                                                    >
                                                        <Trash size={18} />
                                                    </Button>
                                                </div>
                                            </DataTable.Cell>
                                        )}
                                    </DataTable.Row>
                                    );
                                }}
                            </DataTable.TBody>
                        </DataTable.Table>
                    </DataTable.TableContainer>

                    <DataTable.Pagination>
                        {({
                            rows: currentRows
                        }: DataTablePaginationInstance) => (
                            <Paginator.Root
                                className="comandos-datatable-paginator"
                                page={page + 1}
                                total={result?.totalElements ?? 0}
                                itemsPerPage={
                                    currentRows ??
                                    result?.size ??
                                    10
                                }
                                onPageChange={(
                                    event: PaginatorRootChangeEvent
                                ) => {
                                    const nextPage =
                                        event.value - 1;

                                    setLoading(true);
                                    setPage(nextPage);
                                }}
                            >
                                <Paginator.Content>
                                    <Paginator.First>
                                        <AngleDoubleLeft />
                                    </Paginator.First>

                                    <Paginator.Prev>
                                        <AngleLeft />
                                    </Paginator.Prev>

                                    <Paginator.Pages>
                                        {({
                                            paginator
                                        }: PaginatorPagesInstance) =>
                                            paginator?.pages.map(
                                                (
                                                    paginatorPage,
                                                    pageIndex
                                                ) =>
                                                    paginatorPage.type === "page" ? (
                                                        <Paginator.Page
                                                            key={pageIndex}
                                                            value={paginatorPage.value}
                                                        />
                                                    ) : (
                                                        <Paginator.Ellipsis
                                                            key={pageIndex}
                                                        >
                                                            <EllipsisH />
                                                        </Paginator.Ellipsis>
                                                    )
                                            )
                                        }
                                    </Paginator.Pages>

                                    <Paginator.Next>
                                        <AngleRight />
                                    </Paginator.Next>

                                    <Paginator.Last>
                                        <AngleDoubleRight />
                                    </Paginator.Last>
                                </Paginator.Content>
                            </Paginator.Root>
                        )}
                    </DataTable.Pagination>

                    <div
                        style={{
                            padding: "0.5rem",
                            textAlign: "right",
                            borderTop: "1px solid #e5e7eb",
                            fontSize: "0.875rem"
                        }}
                    >
                        Total records: {result?.totalElements ?? 0}
                    </div>
                </DataTable.Root>

                {loading && <p role="status">Loading recordsâ€¦</p>}

                {!loading && result?.content.length === 0 && (
                    <p>No records found.</p>
                )}
            </div>
{intake && <StockIntakeEditor resource={resource} service={service} onCancel={() => setIntake(false)}
                onSaved={quantity => { setIntake(false); setNotice({ type: "success", text: resource.key === "assets"
                    ? `${quantity} individual assets registered successfully.` : `${quantity} rounds received successfully.` }); refresh(); }} />}
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
        ...(resource.key === "models" && field.name === "categoryId" ? { armamentTypeId: "", armamentClassificationId: "" } : {}),
        ...(resource.key === "models" && field.name === "armamentTypeId" ? { armamentClassificationId: "" } : {}),
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
                            
                            <form data-comandos-erp-form="true" 
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
                                {resource.key === "assets" && <p>
                                    Select an existing model and register one individual item. Asset code must be unique;
                                    serial number is required for serialized models and must be unique within the model.
                                    Leading and trailing spaces are removed; letter case is preserved.
                                </p>}
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
                                                    optionFilter={option => {
                                                        if (["armament-types", "armament-classifications"].includes(field.reference ?? "") && option.active === false) return false;
                                                        if (resource.key === "models" && field.name === "armamentTypeId") return String(option.categoryId) === String(values.categoryId);
                                                        if (resource.key === "models" && field.name === "armamentClassificationId") return String(option.typeId) === String(values.armamentTypeId);
                                                        return true;
                                                    }}
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
