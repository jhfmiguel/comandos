"use client";

import * as React from "react";
import { createPortal } from "react-dom";
import { PostalCodeField } from "components/erp/core/postal-code-field";

import {
    formatLocaleDate,
    formatLocaleDateTime,
    formatLocaleNumber,
    parseLocaleDecimal
} from "utils/locale";
import { convertToIsoDate, formatDate } from "utils/date";

import axios from "axios";

import {
    CircleHelp,
    Plus,
    Pencil
} from "lucide-react";
import { Trash } from "@primeicons/react";

import { Layout } from "components/layout";
import { StockIntakeEditor } from "components/erp/inventory/stock-intake-editor";
import { Message } from "components/common/message";
import { ComandosSelectField } from "components/common/select-field";
import { Pagination } from "platform/components/pagination";
import { useSession } from "components/auth/session-provider";
import { useComandosPreferences } from "components/settings/preferences-provider";
import type { ErpField, ErpPage, ErpRecord, ErpResource, ErpValue } from "api/models/erp";
import { createErpService, type ErpService, type ErpModule } from "api/services/erp.service";
import styles from "./workspace.module.css";
import {
    PersonContactsEditor,
    type AddressDraft as PersonAddressDraft,
    type PhoneDraft as PersonPhoneDraft,
    type EmailDraft as PersonEmailDraft
} from "./person-contacts-editor";


type ErpTableFilters = Record<string, string>;

function createErpTableFilters(fields: ErpField[]): ErpTableFilters {
    return Object.fromEntries(fields.map(field => [field.name, ""]));
}

function FieldHint({ text }: { text: string }) {
    const [open, setOpen] = React.useState(false);
    const rootRef = React.useRef<HTMLSpanElement | null>(null);

    React.useEffect(() => {
        if (!open) return;

        const close = (event: PointerEvent) => {
            if (!rootRef.current?.contains(event.target as Node)) {
                setOpen(false);
            }
        };

        document.addEventListener("pointerdown", close);
        return () => document.removeEventListener("pointerdown", close);
    }, [open]);

    return (
        <span
            ref={rootRef}
            className={[styles.fieldHint, open ? styles.fieldHintOpen : ""].filter(Boolean).join(" ")}
        >
            <button
                type="button"
                className={`${styles.fieldHintButton} comandos-field-hint-button`}
                aria-label="Ajuda sobre o campo"
                aria-expanded={open}
                onClick={() => setOpen(current => !current)}
            >
                <CircleHelp size={16} aria-hidden="true" />
            </button>
            <span role="tooltip" className={styles.fieldHintBubble}>
                {text}
            </span>
        </span>
    );
}

function errorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const detail = error.response?.data?.detail;
        return typeof detail === "string" ? detail : "Unable to complete the request. Check the API connection and try again.";
    }
    return "Unable to complete the request. Please try again.";
}

function maskCpf(value: string): string {
    const digits = value.replace(/\D/g, "").slice(0, 11);
    return digits
        .replace(/^(\d{3})(\d)/, "$1.$2")
        .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
        .replace(/\.(\d{3})(\d)/, ".$1-$2");
}

function maskCnpj(value: string): string {
    const digits = value.replace(/\D/g, "").slice(0, 14);
    return digits
        .replace(/^(\d{2})(\d)/, "$1.$2")
        .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
        .replace(/\.(\d{3})(\d)/, ".$1/$2")
        .replace(/(\d{4})(\d)/, "$1-$2");
}

function maskPersonTaxId(value: string, personType: string): string {
    if (personType === "INDIVIDUAL") return maskCpf(value);
    if (personType === "LEGAL_ENTITY") return maskCnpj(value);
    return value.replace(/\D/g, "").slice(0, 14);
}


export function RecordWorkspace({
    module,
    title,
    initialResource,
    description,
    showNavigation = true,
    tabs
}: {
    module: ErpModule;
    title: string;
    initialResource: string;
    description: string;
    showNavigation?: boolean;
    tabs?: Array<{
        resource: string;
        label: string;
        content?: React.ReactNode;
        definition?: ErpResource;
    }>;
}) {

    const service = React.useMemo(() => createErpService(module), [module]);
    const { session } = useSession();
    const { tr } = useComandosPreferences();
    const accessRevision = JSON.stringify(session?.access);

    const [catalog, setCatalog] = React.useState<ErpResource[]>([]);
    const [catalogLoaded, setCatalogLoaded] = React.useState(false);
    const [selected, setSelected] = React.useState(initialResource);
    const [error, setError] = React.useState("");
    const [retry, setRetry] = React.useState(0);

    React.useEffect(() => {

        const controller = new AbortController();

        service.catalog(controller.signal).then(data => {
            if (!controller.signal.aborted) {
                setCatalog(data);
                setCatalogLoaded(true);
            }
        }).catch(error => {
            if (!controller.signal.aborted) {
                setError(errorMessage(error));
            }
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

    const workspaceTabs = tabs ?? [];
    const initialTabResource = workspaceTabs.some(tab => tab.resource === initialResource)
        ? initialResource
        : workspaceTabs[0]?.resource ?? "";
    const [activeTab, setActiveTab] = React.useState(initialTabResource);
    const resolvedActiveTab = workspaceTabs.some(tab => tab.resource === activeTab)
        ? activeTab
        : initialTabResource;
    const hasWorkspaceContent =
        catalog.length > 0 || workspaceTabs.length > 0;
    const tabsListRef = React.useRef<HTMLDivElement | null>(null);
    const [tabScrollState, setTabScrollState] = React.useState({
        overflow: false,
        canPrevious: false,
        canNext: false
    });

    const syncTabScroll = React.useCallback((element?: HTMLDivElement | null) => {
        const target = element ?? tabsListRef.current;
        if (!target) return;
        const maxScroll = Math.max(target.scrollWidth - target.clientWidth, 0);
        setTabScrollState({
            overflow: maxScroll > 2,
            canPrevious: target.scrollLeft > 2,
            canNext: target.scrollLeft < maxScroll - 2
        });
    }, []);

    const setTabsListElement = React.useCallback((element: HTMLDivElement | null) => {
        tabsListRef.current = element;
        if (element) {
            requestAnimationFrame(() => syncTabScroll(element));
        }
    }, [syncTabScroll]);

    const scrollTabs = React.useCallback((direction: -1 | 1) => {
        const element = tabsListRef.current;
        if (!element) return;
        element.scrollBy({
            left: direction * Math.max(element.clientWidth * 0.72, 220),
            behavior: "smooth"
        });
        window.setTimeout(() => syncTabScroll(element), 220);
    }, [syncTabScroll]);

    return (

        <Layout title={tr(title)}>

            <div className={styles.workspace}>

                <p className={styles.intro}>{tr(description)}</p>

                {error && <Message type="error" text={error} />}
                {error && (
                    <button
                        type="button"
                        className="comandos-secondary-button"
                        onClick={() => {
                            setError("");
                            setRetry(value => value + 1);
                        }}
                    >
                        {tr("Retry")}
                    </button>
                )}

                {!catalogLoaded && !error && (
                    <p role="status">{tr("Loading records…")}</p>
                )}

                {catalogLoaded && !catalog.length && !workspaceTabs.some(tab => tab.definition) && (
                    <Message
                        type="info"
                        text={tr("No resources are available for your access profile.")}
                    />
                )}

                {hasWorkspaceContent && (

                    <div
                        className={
                            showNavigation
                                ? styles.shell
                                : `${styles.shell} ${styles.shellSingle}`
                        }
                    >

                        {showNavigation && (

                            <nav
                                aria-label={`${title} resources`}
                                className={styles.navigation}
                            >

                                {[...new Set(catalog.map(item => item.group))].map(group => (

                                    <div key={group}>

                                        <p className={styles.group}>
                                            {tr(group)}
                                        </p>

                                        {catalog
                                            .filter(item => item.group === group)
                                            .map(item => (

                                                <button
                                                    key={item.key}
                                                    type="button"
                                                    aria-current={
                                                        resource?.key === item.key
                                                            ? "page"
                                                            : undefined
                                                    }
                                                    onClick={() => setSelected(item.key)}
                                                >
                                                    {tr(item.label)}
                                                </button>

                                            ))
                                        }

                                    </div>

                                ))}

                            </nav>

                        )}

                        {!!workspaceTabs.length ? (
                            <div className="comandos-tabs">
                                <div className="comandos-tabs-scroll-shell">
                                    <button
                                        type="button"
                                        className={`comandos-tabs-scroll-button comandos-tabs-scroll-button-prev ${tabScrollState.overflow ? "is-visible" : ""}`}
                                        aria-label={tr("Previous tabs")}
                                        disabled={!tabScrollState.canPrevious}
                                        onClick={() => scrollTabs(-1)}
                                    >
                                        <span aria-hidden="true">&lt;</span>
                                    </button>
                                    <div
                                        ref={setTabsListElement}
                                        className="comandos-tabs-list"
                                        role="tablist"
                                        aria-label={tr(title)}
                                        onScroll={event => syncTabScroll(event.currentTarget)}
                                        onWheel={event => {
                                            const element = event.currentTarget;
                                            if (element.scrollWidth <= element.clientWidth) return;
                                            const delta = Math.abs(event.deltaX) > Math.abs(event.deltaY)
                                                ? event.deltaX
                                                : event.deltaY;
                                            if (!delta) return;
                                            event.preventDefault();
                                            element.scrollLeft += delta;
                                            syncTabScroll(element);
                                        }}
                                    >
                                    {workspaceTabs.map((tab) => {
                                        const tabResource = catalog.find(
                                            (item) =>
                                                item.key === tab.resource ||
                                                normalizeResourceName(item.label) === tab.resource
                                        ) ?? tab.definition;
                                        return (
                                            <button
                                                key={tab.resource}
                                                type="button"
                                                role="tab"
                                                aria-selected={resolvedActiveTab === tab.resource}
                                                className={`comandos-tab ${resolvedActiveTab === tab.resource ? "is-active" : ""}`}
                                                onClick={event => {
                                                    setActiveTab(tab.resource);
                                                    event.currentTarget.scrollIntoView({
                                                        behavior: "smooth",
                                                        block: "nearest",
                                                        inline: "nearest"
                                                    });
                                                    if (!tab.content && !tabResource) {
                                                        setError("");
                                                        setRetry(value => value + 1);
                                                    }
                                                }}
                                            >
                                                {tr(tab.label)}
                                            </button>
                                        );
                                    })}
                                    </div>
                                    <button
                                        type="button"
                                        className={`comandos-tabs-scroll-button comandos-tabs-scroll-button-next ${tabScrollState.overflow ? "is-visible" : ""}`}
                                        aria-label={tr("Next tabs")}
                                        disabled={!tabScrollState.canNext}
                                        onClick={() => scrollTabs(1)}
                                    >
                                        <span aria-hidden="true">&gt;</span>
                                    </button>
                                </div>
                                {workspaceTabs.map((tab) => {
                                    if (resolvedActiveTab !== tab.resource) return null;
                                    const catalogTabResource = catalog.find(
                                        (item) =>
                                            item.key === tab.resource ||
                                            normalizeResourceName(item.label) === tab.resource
                                    );
                                    const missingExpectedResource =
                                        catalogLoaded && !catalogTabResource && Boolean(tab.definition);
                                    return (
                                        <div key={tab.resource} role="tabpanel" className="comandos-tab-panel">
                                            {tab.content ?? (
                                                !catalogLoaded ? (
                                                    <p role="status">{tr("Loading records…")}</p>
                                                ) : catalogTabResource ? (
                                                    <ResourcePanel
                                                        key={catalogTabResource.key}
                                                        resource={catalogTabResource}
                                                        service={service}
                                                    />
                                                ) : (
                                                    <div className={styles.missingTabResource}>
                                                        <Message
                                                            type={missingExpectedResource ? "warning" : "info"}
                                                            text={tr(
                                                                missingExpectedResource
                                                                    ? "This registration exists in the current project, but the running API does not expose it yet. Update and restart the API, then refresh the registrations."
                                                                    : "This registration is not available in the API catalog yet. Refresh the registrations after restarting the API."
                                                            )}
                                                        />
                                                        <button
                                                            type="button"
                                                            className="comandos-secondary-button"
                                                            onClick={() => {
                                                                setError("");
                                                                setRetry(value => value + 1);
                                                            }}
                                                        >
                                                            {tr("Refresh registrations")}
                                                        </button>
                                                    </div>
                                                )
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        ) : (
                            resource && (
                                <ResourcePanel
                                    key={resource.key}
                                    resource={resource}
                                    service={service}
                                />
                            )
                        )}

                    </div>

                )}

            </div>

        </Layout>

    );

}

function getErpServerFilters(filters: ErpTableFilters): Record<string, string> {
    return Object.fromEntries(
        Object.entries(filters)
            .map(([field, value]) => [field, value.trim()])
            .filter(([, value]) => value !== "")
    );
}

function ResourcePanel({ resource, service }: { resource: ErpResource; service: ErpService }) {
    const { tr, locale } = useComandosPreferences();
    const allowed = (action: string) => !resource.readOnly && (!resource.actions || resource.actions.includes(action));
    
    const [search] = React.useState("");
    const [serverFilters, setServerFilters] = React.useState<Record<string, string>>({});
    const [page, setPage] = React.useState(0);
    const [filters, setFilters] = React.useState<ErpTableFilters>(
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
    
    const handleTableFilter = (field: string, value: string): void => {
        const nextFilters = { ...filters, [field]: value };
        setFilters(nextFilters);
        setPage(0);

        if (filterTimeoutRef.current) {
            clearTimeout(filterTimeoutRef.current);
        }

        filterTimeoutRef.current = setTimeout(() => {
            setLoading(true);
            setServerFilters(getErpServerFilters(nextFilters));
        }, 400);
    };
    const columns = resource.fields
                                .filter(field => field.type !== "password")
                                .filter((field, index) =>
                                    resource.readOnly
                                    || index < 4
                                    || ["status", "availableQuantity"].includes(field.name)
                                    || (resource.key === "contact-types"
                                        && ["addressEnabled", "phoneEnabled", "emailEnabled", "active"].includes(field.name))
                                    || (["recalls", "recall-items"].includes(resource.key) && field.name === "description")
                                );
    
    const display = (record: ErpRecord, field: ErpField): string => {
        const value = record[field.name];

        if (value == null || value === "") return "—";
        if (resource.key === "people" && field.name === "taxId") {
            return maskPersonTaxId(String(value), String(record.personTypeCode ?? ""));
        }
        if (field.type === "reference") return record.referenceLabels[field.name] || `#${value}`;
        if (field.type === "boolean") return value ? tr("Yes") : tr("No");

        if (field.type === "integer") {
            const numericValue = Number(value);
            return Number.isFinite(numericValue)
                ? formatLocaleNumber(numericValue, locale, { maximumFractionDigits: 0 })
                : String(value);
        }

        if (field.type === "decimal") {
            const numericValue = Number(value);
            return Number.isFinite(numericValue)
                ? formatLocaleNumber(numericValue, locale)
                : String(value);
        }

        if (field.type === "date") {
            return formatLocaleDate(String(value), locale);
        }

        if (["datetime", "date-time", "local-date-time"].includes(field.type)) {
            return formatLocaleDateTime(String(value), locale);
        }

        return tr(String(value).replaceAll("_", " "));
    };

    return (

        <section className={styles.panel} aria-labelledby="resource-title">
            
            <div className={styles.toolbar}>
                
                <h1 id="resource-title">{tr(resource.label)}</h1>
                {allowed("CREATE") && 
                <button type="button" className="registration-yellow-button" onClick={() => resource.key === "assets" ? setIntake(true) : setEditor({})}>
                    <Plus size={16} /><span>{tr("New record")}</span>
                </button>
                }
                {resource.key === "assets" && allowed("CREATE") && <button type="button" className="comandos-secondary-button"
                    onClick={() => setEditor({})}>{tr("Register single asset")}</button>}
                {resource.key === "lots" && allowed("CREATE") && <button type="button" className="registration-yellow-button"
                    onClick={() => setIntake(true)}>{tr("Receive ammunition boxes")}</button>}
            </div>
            
            {notice && 
            <Message type={notice.type} text={notice.text} onClose={() => setNotice(null)} />
            }
<div className={styles.tableContainer} aria-busy={loading}>
                <div className="comandos-native-table-container">
                    <table className="comandos-native-table comandos-erp-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                {columns.map((field) => (
                                    <th key={field.name}>{tr(field.label)}</th>
                                ))}
                                {!resource.readOnly && <th className={styles.actionCell}>Actions</th>}
                            </tr>
                            <tr className="comandos-filter-row">
                                <th />
                                {columns.map((field) => (
                                    <th key={field.name}>
                                        <input
                                            className="comandos-input comandos-input-small"
                                            value={filters[field.name] ?? ""}
                                            onChange={(event) => handleTableFilter(field.name, event.target.value)}
                                            placeholder={`${tr("Search")} ${tr(field.label).toLowerCase()}...`}
                                        />
                                    </th>
                                ))}
                                {!resource.readOnly && <th />}
                            </tr>
                        </thead>
                        <tbody>
                            {(result?.content ?? []).map((record, index) => (
                                <tr key={record.id ?? index}>
                                    <td>{record.id}</td>
                                    {columns.map((field) => (
                                        <td key={field.name}>{display(record, field)}</td>
                                    ))}
                                    {!resource.readOnly && (
                                        <td className={styles.actionCell}>
                                            <div className={`${styles.actions} ${styles.tableActions}`}>
                                                <button
                                                    type="button"
                                                    className="comandos-icon-button comandos-icon-button-edit"
                                                    aria-label={`${tr("Edit")} ${record.label}`}
                                                    disabled={!allowed("UPDATE")}
                                                    onClick={() => setEditor({ record })}
                                                >
                                                    <Pencil size={18} />
                                                </button>
                                                <button
                                                    type="button"
                                                    className="comandos-icon-button comandos-icon-button-danger"
                                                    aria-label={`${tr("Delete")} ${record.label}`}
                                                    disabled={!allowed("DELETE")}
                                                    onClick={() => setDeleting(record)}
                                                >
                                                    <Trash size={18} />
                                                </button>
                                            </div>
                                        </td>
                                    )}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                <Pagination
                    page={page}
                    totalElements={result?.totalElements ?? 0}
                    pageSize={result?.size ?? 10}
                    disabled={loading}
                    onPageChange={(nextPage) => {
                        setLoading(true);
                        setPage(nextPage);
                    }}
                    labels={{
                        totalRecords: tr("Total records"),
                        first: tr("First page"),
                        previous: tr("Previous page"),
                        next: tr("Next page"),
                        last: tr("Last page")
                    }}
                />

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
            
            {deleting !== null && (
                <div className="comandos-dialog-layer">
                    <button
                        type="button"
                        className="comandos-dialog-backdrop"
                        aria-label="Close dialog"
                        disabled={busy}
                        onClick={() => { if (!busy) setDeleting(null); }}
                    />
                    <div role="dialog" aria-modal="true" className={`comandos-native-dialog ${styles.dialog}`}>
                        <div className="comandos-native-dialog-header">
                            <h2>Delete record</h2>
                        </div>
                        <div className="comandos-native-dialog-content">
                            <p>Delete {deleting?.label}? Records referenced by another record cannot be deleted.</p>
                            <div className={styles.actions}>
                                <button type="button" className="comandos-action-button comandos-button-outlined comandos-button-secondary" disabled={busy} onClick={() => setDeleting(null)}>Cancel</button>
                                <button
                                    type="button"
                                    className="comandos-action-button comandos-button-outlined comandos-button-danger"
                                    disabled={busy}
                                    onClick={async () => {
                                        if (!deleting || busy) return;
                                        setBusy(true);
                                        try {
                                            await service.remove(resource.key, deleting);
                                            setDeleting(null);
                                            setNotice({ type: "success", text: "Record deleted successfully." });
                                            if (result?.content.length === 1 && page > 0) setPage(value => value - 1);
                                            refresh();
                                        } catch (error) {
                                            setDeleting(null);
                                            setNotice({ type: "error", text: errorMessage(error) });
                                        } finally {
                                            setBusy(false);
                                        }
                                    }}
                                >
                                    {busy ? "Deleting…" : "Delete"}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

        </section>
    );
}

const equipmentModelFamilies: Record<string, string> = {
    "firearm-specifications": "FIREARM",
    "ammunition-specifications": "AMMUNITION",
    "grenade-specifications": "GRENADE",
    "spray-specifications": "SPRAY",
    "ballistic-protection-specifications": "BALLISTIC_PROTECTION",
    "electrical-device-specifications": "ELECTRICAL_DEVICE",
    "optical-specifications": "OPTICAL",
};

function RecordEditor({ resource, service, record, onCancel, onSaved }: {

    resource: ErpResource; 
    service: ErpService; 
    record?: ErpRecord; 
    onCancel: () => void; 
    onSaved: () => void;

}) {

    const { tr, locale } = useComandosPreferences();

        const maskDecimalInput = (value: string): string => {
        const digits = value.replace(/\D/g, "").slice(0, 17);
        if (!digits) return "";

        return new Intl.NumberFormat(locale, {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(Number(digits) / 100);
    };

const [values, setValues] = React.useState<Record<string, ErpValue>>(() => {
        const initial = Object.fromEntries(resource.fields.map(field =>
            [field.name, field.type === "password" ? ""
                : record?.[field.name]
                ?? (field.type === "boolean" ? field.name === "active" : "")]
        )) as Record<string, ErpValue>;

        if (resource.key === "person-addresses" && !record) {
            initial.contactTypeId = "";
            initial.country = "Brasil";
            initial.foreignAddress = false;
        }
        if (resource.key === "person-phones" && !record) {
            initial.contactTypeId = "";
            initial.countryCode = "+55";
        }
        if (resource.key === "person-emails" && !record) {
            initial.contactTypeId = "";
        }

        return initial;
    });

    const editorResourceLabel =
        resource.key === "roles"
            ? "Person roles"
            : resource.key === "person-roles"
                ? "Person role assignments"
                : resource.label;

    const isNewPerson = resource.key === "people" && !record;
    const [personTypeCode, setPersonTypeCode] = React.useState(
        String(record?.personTypeCode ?? "")
    );
    const [personAddresses, setPersonAddresses] = React.useState<PersonAddressDraft[]>([]);
    const [personPhones, setPersonPhones] = React.useState<PersonPhoneDraft[]>([]);
    const [personEmails, setPersonEmails] = React.useState<PersonEmailDraft[]>([]);
    const [busy, setBusy] = React.useState(false);
    const saving = React.useRef(false);
    const manuallyEditedAddressFields = React.useRef(new Set<string>());
    const [error, setError] = React.useState("");
    
    const change = (field: ErpField, value: ErpValue) => {
        if (resource.key === "person-addresses") {
            if (field.name === "postalCode") manuallyEditedAddressFields.current.clear();
            else manuallyEditedAddressFields.current.add(field.name);
        }
        setValues(current => ({
            ...current,
            [field.name]: value,
            ...(resource.key === "person-addresses" && field.name === "foreignAddress"
                ? { country: value ? "" : "Brasil" }
                : {}),
            ...(resource.key === "people" && field.name === "personTypeRefId"
                ? { taxId: "" }
                : {}),
            ...(resource.key === "models" && field.name === "categoryId" ? { armamentTypeId: "", armamentClassificationId: "" } : {}),
            ...(resource.key === "models" && field.name === "armamentTypeId" ? { armamentClassificationId: "" } : {}),
            ...(field.name === "organizationId" ? {
                ...(resource.fields.some(f => f.name === "unitId") ? { unitId: "" } : {}),
                ...(resource.fields.some(f => f.name === "parentUnitId") ? { parentUnitId: "" } : {})
            } : {})
        }));
    };

    return createPortal(
        <div className="comandos-dialog-layer">
            <button
                type="button"
                className="comandos-dialog-backdrop"
                aria-label="Close dialog"
                disabled={busy}
                onClick={() => { if (!busy) onCancel(); }}
            />
            <div
                role="dialog"
                aria-modal="true"
                className={`comandos-native-dialog ${styles.dialog} ${isNewPerson ? styles.personDialog : ""}`}
            >
            
                <div className="comandos-native-dialog-header">
                    <h2>{tr(record ? "Edit" : "New")} · {tr(editorResourceLabel)}</h2>
                </div>
                <div className="comandos-native-dialog-content">
                            
                            <form data-comandos-erp-form="true" 
                                className={styles.form} 
                                onSubmit={async event => {
                                    event.preventDefault();
                                    if (saving.current) return;
                                    saving.current = true; setBusy(true); setError("");
                                    try {
                                        const payload = Object.fromEntries(
                                            resource.fields
                                                .filter(field => !field.readOnly)
                                                .map(field => {
                                                    const value = values[field.name];

                                                    if (field.type === "decimal") {
                                                        return [field.name, value === "" ? null : parseLocaleDecimal(String(value), locale)];
                                                    }

                                                    if (field.type === "integer") {
                                                        return [field.name, value === "" ? null : Number(value)];
                                                    }

                                                    if (field.type === "date") {
                                                        return [field.name, value === "" ? null : convertToIsoDate(String(value), locale)];
                                                    }

                                                    return [field.name, value];
                                                })
                                        );
                                        if (isNewPerson) {
                                            await service.savePersonWithContacts({
                                                person: payload,
                                                addresses: personAddresses as unknown as Array<Record<string, ErpValue>>,
                                                phones: personPhones as unknown as Array<Record<string, ErpValue>>,
                                                emails: personEmails as unknown as Array<Record<string, ErpValue>>
                                            });
                                        } else {
                                            await service.save(
                                                resource.key,
                                                { ...payload, ...(record ? { version: record.version } : {}) },
                                                record?.id
                                            );
                                        }
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
                                <fieldset
                                    disabled={busy}
                                    className={styles.fields}
                                    data-comandos-resource={resource.key}
                                >
                                    {resource.fields.map(field => {
                                        const personTaxId = resource.key === "people" && field.name === "taxId";
                                        const organizationTaxId = resource.key === "organizations" && field.name === "taxId";
                                        const personType = personTypeCode;
                                        const fieldLabel = personTaxId
                                            ? personType === "INDIVIDUAL"
                                                ? "CPF"
                                                : personType === "LEGAL_ENTITY"
                                                    ? "CNPJ"
                                                    : "CPF / CNPJ"
                                            : tr(field.label);
                                        const required = (
                                            field.required
                                            || (resource.key === "person-addresses"
                                                && field.name === "postalCode"
                                                && !Boolean(values.foreignAddress))
                                            || (resource.key === "person-addresses"
                                                && field.name === "country"
                                                && Boolean(values.foreignAddress))
                                        ) && !(record && field.type === "password");
                                        return <fieldset
                                            key={field.name}
                                            data-comandos-field="true"
                                            data-comandos-field-name={field.name}
                                            className={[
                                                styles.field,
                                                resource.key === "organizations" && field.name === "taxId" ? styles.organizationTaxField : "",
                                                resource.key === "organizations" && field.name === "publicOrganization" ? styles.organizationPublicField : "",
                                                resource.key === "organizations" && field.name === "active" ? styles.organizationActiveField : "",
                                                resource.key === "profiles" && field.name === "levelTypeId" ? styles.fieldWithHint : "",
                                                resource.key === "permissions" && ["resourceTypeId", "actionTypeId"].includes(field.name) ? styles.fieldWithHint : "",
                                                resource.key === "user-profiles" && field.name === "unitId" ? styles.fieldWithHint : ""
                                            ].filter(Boolean).join(" ")}
                                            disabled={field.readOnly || Boolean(record && (
                                                field.createOnly
                                                || (["person-addresses", "person-phones", "person-emails"].includes(resource.key)
                                                    && field.name === "personId")
                                            ))}>
                                            {field.type !== "reference" && field.type !== "choice" && (
                                                <label htmlFor={`core-${field.name}`}>{fieldLabel}{required ? " *" : ""}</label>
                                            )}
                                            {resource.key === "models" && field.name === "manufacturerCode" && (
                                                <small>{tr("Manufacturer catalog/part code for the model; distinct from serial number and internal SKU.")}</small>
                                            )}
                                            {resource.key === "profiles" && field.name === "levelTypeId" && (
                                                <FieldHint text={tr("Use SYSTEM (all organizations), ORGANIZATION or UNIT. Other levels do not grant access.")} />
                                            )}
                                            {resource.key === "permissions" && field.name === "resourceTypeId" && (
                                                <FieldHint text={tr("Use an exact resource code, such as core/people, inventory/assets, sales or security/access. An asterisk grants all resources.")} />
                                            )}
                                            {resource.key === "permissions" && field.name === "actionTypeId" && (
                                                <FieldHint text={tr("Use READ, CREATE, UPDATE, DELETE, MANAGE (access administration), or *.")} />
                                            )}
                                            {resource.key === "user-profiles" && field.name === "unitId" && (
                                                <FieldHint text={tr("Required for UNIT profiles; leave empty for SYSTEM and ORGANIZATION profiles.")} />
                                            )}
                                            {resource.key === "person-addresses"
                                            && field.name === "postalCode"
                                            && !Boolean(values.foreignAddress) ? (
                                                <PostalCodeField
                                                    id="core-postalCode"
                                                    value={String(values.postalCode ?? "")}
                                                    required={required}
                                                    onChange={value => change(field, value)}
                                                    onResolved={address => setValues(current => ({ ...current,
                                                        ...Object.fromEntries(Object.entries(address).filter(([key]) => !manuallyEditedAddressFields.current.has(key))),
                                                        country: "Brasil",
                                                        complement: manuallyEditedAddressFields.current.has("complement")
                                                            ? current.complement : current.complement || address.complement || "" }))} />
                                            ) : field.type === "reference" ? (
                                                <ReferenceSelectField
                                                    key={
                                                        field.name === "unitId"
                                                            ? `${field.name}:${String(values.organizationId ?? "")}`
                                                            : field.name
                                                    }
                                                    service={service}
                                                    field={field}
                                                    label={fieldLabel}
                                                    required={required}
                                                    value={values[field.name]}
                                                    selectedLabel={record?.referenceLabels[field.name]}
                                                    organizationId={values.organizationId}
                                                    excludedId={["parentUnitId", "parentCategoryId"].includes(field.name) ? record?.id : undefined}
                                                    modelFamily={
                                                        field.name === "modelId"
                                                            ? resource.key === "lots"
                                                                ? "AMMUNITION"
                                                                : equipmentModelFamilies[resource.key]
                                                            : undefined
                                                    }
                                                    optionFilter={option => {
                                                        if (option.active === false) return false;
                                                        if (resource.key === "models" && field.name === "armamentTypeId") {
                                                            return String(option.categoryId) === String(values.categoryId);
                                                        }
                                                        if (resource.key === "models" && field.name === "armamentClassificationId") {
                                                            return String(option.typeId) === String(values.armamentTypeId);
                                                        }
                                                        if (field.name === "contactTypeId") {
                                                            if (resource.key === "person-addresses") return option.addressEnabled === true;
                                                            if (resource.key === "person-phones") return option.phoneEnabled === true;
                                                            if (resource.key === "person-emails") return option.emailEnabled === true;
                                                        }
                                                        return true;
                                                    }}
                                                    onChange={value => change(field, value)}
                                                    onSelectOption={option => {
                                                        if (resource.key === "people" && field.name === "personTypeRefId") {
                                                            setPersonTypeCode(String(option?.code ?? ""));
                                                        }
                                                    }}
                                                />
                                            ) : field.type === "boolean" ? (
                                                <input id={`core-${field.name}`} type="checkbox" checked={Boolean(values[field.name])} onChange={event => change(field, event.target.checked)} />
                                            ) : field.type === "choice" ? (
                                                <ComandosSelectField
                                                    id={`core-${field.name}`}
                                                    label={fieldLabel}
                                                    required={required}
                                                    value={String(values[field.name] ?? "")}
                                                    options={field.choices.map(value => ({
                                                        value,
                                                        label: value.replaceAll("_", " ")
                                                    }))}
                                                    onChange={value => change(field, value)}
                                                />
                                            ) : organizationTaxId ? (
                                                <input
                                                    id={`core-${field.name}`}
                                                    type="text"
                                                    inputMode="numeric"
                                                    autoComplete="off"
                                                    placeholder="00.000.000/0000-00"
                                                    maxLength={18}
                                                    required={required}
                                                    value={maskCnpj(String(values[field.name] ?? ""))}
                                                    onChange={event => change(field, maskCnpj(event.target.value))}
                                                />
                                            ) : personTaxId ? (
                                                <input
                                                    id={`core-${field.name}`}
                                                    type="text"
                                                    inputMode="numeric"
                                                    autoComplete="off"
                                                    placeholder={
                                                        personType === "INDIVIDUAL"
                                                            ? "000.000.000-00"
                                                            : personType === "LEGAL_ENTITY"
                                                                ? "00.000.000/0000-00"
                                                                : "Selecione o tipo de pessoa"
                                                    }
                                                    maxLength={personType === "INDIVIDUAL" ? 14 : personType === "LEGAL_ENTITY" ? 18 : 255}
                                                    required={required}
                                                    disabled={!personType}
                                                    value={maskPersonTaxId(String(values[field.name] ?? ""), personType)}
                                                    onChange={event => change(field, maskPersonTaxId(event.target.value, personType))}
                                                />
                                            ) : field.type === "decimal" ? (
                                                <input
                                                    id={`core-${field.name}`}
                                                    type="text"
                                                    inputMode="numeric"
                                                    required={required}
                                                    value={String(values[field.name] ?? "")}
                                                    onChange={event => change(field, maskDecimalInput(event.target.value))}
                                                />
                                            ) : field.type === "date" ? (
                                                <input
                                                    id={`core-${field.name}`}
                                                    type="text"
                                                    inputMode="numeric"
                                                    placeholder={locale === "pt-BR" ? "DD/MM/AAAA" : "MM/DD/YYYY"}
                                                    maxLength={10}
                                                    required={required}
                                                    value={String(values[field.name] ?? "")}
                                                    onChange={event => change(field, formatDate(event.target.value, locale))}
                                                />
                                            ) : (
                                                <input
                                                    id={`core-${field.name}`}
                                                    type={field.type === "integer" ? "number" : field.type}
                                                    step={field.type === "integer" ? "1" : undefined}
                                                    min={field.type === "integer" ? "0" : undefined}
                                                    required={required}
                                                    value={String(values[field.name] ?? "")}
                                                    maxLength={field.type === "password" ? 72 : 255}
                                                    minLength={field.type === "password" ? 12 : undefined}
                                                    autoComplete={field.type === "password" ? "new-password" : undefined}
                                                    onChange={event => change(field, event.target.value)}
                                                />
                                            )}
                                            {field.type === "password" && <small>At least 12 characters. {record ? "Leave blank to keep the current password." : ""}</small>}
                                            {resource.key === "person-addresses" && field.name === "primaryAddress" && <small>Somente um endereço principal por pessoa. Desmarque o atual antes de escolher outro.</small>}
                                            {field.readOnly && <small>Calculated by inventory operations.</small>}
                                            {record && field.createOnly && <small>Fixed after registration.</small>}
                                        </fieldset>;
                                    })}
                                </fieldset>

                                {isNewPerson && (
                                    <PersonContactsEditor
                                        addresses={personAddresses}
                                        setAddresses={setPersonAddresses}
                                        phones={personPhones}
                                        setPhones={setPersonPhones}
                                        emails={personEmails}
                                        setEmails={setPersonEmails}
                                    />
                                )}

                                <div className={styles.actions}>
                                    <button type="button" className="registration-yellow-button" disabled={busy} onClick={onCancel}>Cancel</button>
                                    <button type="submit" className="registration-yellow-button" disabled={busy}>{busy ? "Saving…" : "Save"}</button>
                                </div>
                            </form>
                </div>
            </div>
        </div>,
        document.body
    );
}

export function ReferenceSelectField({
    service,
    field,
    label,
    required,
    value,
    selectedLabel,
    organizationId,
    excludedId,
    modelFamily,
    optionFilter,
    onChange,
    onSelectOption
}: {
    service: ErpService;
    field: ErpField;
    label?: string;
    required?: boolean;
    value: ErpValue;
    selectedLabel?: string;
    organizationId: ErpValue;
    excludedId?: number;
    modelFamily?: string;
    optionFilter?: (record: ErpRecord) => boolean;
    onChange: (value: ErpValue) => void;
    onSelectOption?: (option: ErpRecord | null) => void;
}) {
    const isUnit = ["units", "core/units"].includes(field.reference ?? "");
    const organizationMissing = isUnit && !organizationId;
    const [resultState, setResultState] = React.useState<{
        key: string;
        options: ErpRecord[];
        error: string;
    }>({
        key: "",
        options: [],
        error: ""
    });
    const [unitAttempted, setUnitAttempted] = React.useState(false);
    const searchable = ["natureId", "economicActivityId"].includes(field.name);
    const [referenceSearch, setReferenceSearch] = React.useState("");

    const shouldSearch = !searchable || referenceSearch.trim().length > 0;
    const requestKey = JSON.stringify([
        field.reference ?? "",
        organizationId ?? "",
        modelFamily ?? "",
        searchable ? referenceSearch : ""
    ]);

    React.useEffect(() => {
        if (!field.reference || organizationMissing || !shouldSearch) return;

        const controller = new AbortController();

        service.list(
            field.reference,
            searchable ? referenceSearch : "",
            0,
            controller.signal,
            isUnit ? organizationId : undefined,
            modelFamily ? { modelFamily } : {},
            100
        ).then(data => {
            if (controller.signal.aborted) return;

            const records = data.content
                .filter(option =>
                    option.active !== false
                    && (!modelFamily || option.modelFamily === modelFamily)
                    && (
                        !isUnit
                        || !organizationId
                        || String(option.organizationId) === String(organizationId)
                    )
                )
                .sort((left, right) =>
                    String(left.label ?? "").localeCompare(String(right.label ?? ""), "pt-BR")
                );

            setResultState({
                key: requestKey,
                options: records,
                error: ""
            });
        }).catch(error => {
            if (!controller.signal.aborted) {
                setResultState({
                    key: requestKey,
                    options: [],
                    error: errorMessage(error)
                });
            }
        });

        return () => controller.abort();
    }, [
        field.reference,
        isUnit,
        organizationId,
        organizationMissing,
        service,
        modelFamily,
        searchable,
        referenceSearch,
        shouldSearch,
        requestKey
    ]);

    const current = String(value ?? "");
    const requestCurrent = resultState.key === requestKey;
    const options = (organizationMissing || !shouldSearch || !requestCurrent
        ? []
        : resultState.options
    ).filter(option =>
        option.id !== excludedId
        && (!optionFilter || optionFilter(option))
    );
    const loading = Boolean(field.reference)
        && shouldSearch
        && !organizationMissing
        && !requestCurrent;
    const error = requestCurrent ? resultState.error : "";
    const currentKnown = options.some(option => String(option.id ?? "") === current);
    const selectOptions = [
        ...(!currentKnown && current && selectedLabel
            ? [{ id: Number(current), label: selectedLabel } as ErpRecord]
            : []),
        ...options
    ];

    return (
        <div className={`${styles.referenceSelect} comandos-reference-select`}>
            <ComandosSelectField
                id={`core-${field.name}`}
                label={label ?? field.label}
                required={required ?? field.required}
                value={current}
                options={selectOptions.map(option => ({
                    value: String(option.id ?? ""),
                    label:
                        field.name === "natureId"
                            ? String(option.name ?? option.label ?? "")
                            : field.name === "economicActivityId"
                                ? String(option.description ?? option.label ?? "")
                                : String(option.label ?? "")
                }))}
                disabled={organizationMissing}
                onSearchChange={searchable ? setReferenceSearch : undefined}
                onDisabledAttempt={() => setUnitAttempted(true)}
                onChange={nextValue => {
                    const nextId = nextValue ? Number(nextValue) : null;
                    onChange(nextId);
                    onSelectOption?.(
                        nextId == null
                            ? null
                            : selectOptions.find(option => Number(option.id) === nextId) ?? null
                    );
                }}
            />
            {loading && (
                <small className={styles.referenceNotice}>Carregando opções...</small>
            )}

            {unitAttempted && organizationMissing && (
                <small className={styles.referenceNotice}>
                    Selecione uma organização primeiro.
                </small>
            )}

            {error && (
                <small className={styles.referenceError}>
                    {error}
                </small>
            )}
        </div>
    );
}

export function ReferenceField({ service, field, value, selectedLabel, organizationId, excludedId, optionFilter, modelFamily, onChange, onSelectOption }: {
    service: ErpService; field: ErpField; value: ErpValue; selectedLabel?: string; organizationId: ErpValue;
    excludedId?: number; optionFilter?: (record: ErpRecord) => boolean; modelFamily?: string; onChange: (value: ErpValue) => void;
    onSelectOption?: (option: ErpRecord | null) => void;
}) {
    return (
        <ReferenceSelectField
            service={service}
            field={field}
            label={field.label}
            required={field.required}
            value={value}
            selectedLabel={selectedLabel}
            organizationId={organizationId}
            excludedId={excludedId}
            optionFilter={optionFilter}
            modelFamily={modelFamily}
            onChange={onChange}
            onSelectOption={onSelectOption}
        />
    );
}

