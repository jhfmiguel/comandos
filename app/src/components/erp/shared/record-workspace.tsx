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
    Plus,
    Pencil
} from "lucide-react";
import { Trash } from "@primeicons/react";

import { Layout } from "components/layout";
import { StockIntakeEditor } from "components/erp/inventory/stock-intake-editor";
import { Message } from "components/common/message";
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
        catalog.length > 0 || workspaceTabs.some(tab => Boolean(tab.definition));
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
                                    const tabResource = catalog.find(
                                        (item) =>
                                            item.key === tab.resource ||
                                            normalizeResourceName(item.label) === tab.resource
                                    ) ?? tab.definition;
                                    return (
                                        <div key={tab.resource} role="tabpanel" className="comandos-tab-panel">
                                            {tab.content ?? (
                                                tabResource ? (
                                                    <ResourcePanel
                                                        key={tabResource.key}
                                                        resource={tabResource}
                                                        service={service}
                                                    />
                                                ) : (
                                                    <div className={styles.missingTabResource}>
                                                        <Message
                                                            type="info"
                                                            text={tr("This registration is not available in the API catalog yet. Refresh the registrations after restarting the API.")}
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
                                .filter((field, index) => resource.readOnly || index < 4 || ["status", "availableQuantity"]
                                .includes(field.name) || (["recalls", "recall-items"].includes(resource.key) && field.name === "description"));
    
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
            initial.type = "RESIDENTIAL";
            initial.country = "Brasil";
            initial.foreignAddress = false;
        }
        if (resource.key === "person-phones" && !record) {
            initial.type = "MOBILE";
            initial.countryCode = "+55";
        }
        if (resource.key === "person-emails" && !record) {
            initial.type = "PERSONAL";
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
                                <fieldset disabled={busy} className={styles.fields}>
                                    {resource.fields.map(field => {
                                        const personTaxId = resource.key === "people" && field.name === "taxId";
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
                                        return <fieldset key={field.name} className={styles.field} disabled={field.readOnly || Boolean(record && (
                                                field.createOnly
                                                || (["person-addresses", "person-phones", "person-emails"].includes(resource.key)
                                                    && field.name === "personId")
                                            ))}>
                                            <label htmlFor={`core-${field.name}`}>{fieldLabel}{required ? " *" : ""}</label>
                                            {resource.key === "models" && field.name === "manufacturerCode" && (
                                                <small>{tr("Manufacturer catalog/part code for the model; distinct from serial number and internal SKU.")}</small>
                                            )}
                                            {resource.key === "profiles" && field.name === "level" && <small>Use SYSTEM (all organizations), ORGANIZATION or UNIT. Other levels do not grant access.</small>}
                                            {resource.key === "permissions" && field.name === "resource" && <small>Use an exact resource code, such as core/people, inventory/assets, sales or security/access. An asterisk grants all resources.</small>}
                                            {resource.key === "permissions" && field.name === "action" && <small>Use READ, CREATE, UPDATE, DELETE, MANAGE (access administration), or *.</small>}
                                            {resource.key === "user-profiles" && field.name === "unitId" && <small>Required for UNIT profiles; leave empty for SYSTEM and ORGANIZATION profiles.</small>}
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
                                                <ReferenceField service={service} field={field} value={values[field.name]} selectedLabel={record?.referenceLabels[field.name]}
                                                    modelFamily={field.name === "modelId" ? equipmentModelFamilies[resource.key] : undefined}
                                                    organizationId={values.organizationId} excludedId={["parentUnitId", "parentCategoryId"].includes(field.name) ? record?.id : undefined}
                                                    optionFilter={option => {
                                                        if (option.active === false) return false;
                                                        if (resource.key === "models" && field.name === "armamentTypeId") return String(option.categoryId) === String(values.categoryId);
                                                        if (resource.key === "models" && field.name === "armamentClassificationId") return String(option.typeId) === String(values.armamentTypeId);
                                                        return true;
                                                    }}
                                                    onChange={value => change(field, value)}
                                                    onSelectOption={option => {
                                                        if (resource.key === "people" && field.name === "personTypeRefId") {
                                                            setPersonTypeCode(String(option?.code ?? ""));
                                                        }
                                                    }} />
                                            ) : field.type === "boolean" ? (
                                                <input id={`core-${field.name}`} type="checkbox" checked={Boolean(values[field.name])} onChange={event => change(field, event.target.checked)} />
                                            ) : field.type === "choice" ? (
                                                <select id={`core-${field.name}`} required={required} value={String(values[field.name] ?? "")} onChange={event => change(field, event.target.value)}>
                                                    <option value=""></option>
                                                    {field.choices.map(value => <option key={value} value={value}>{value.replaceAll("_", " ")}</option>)}
                                                </select>
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

export function ReferenceField({ service, field, value, selectedLabel, organizationId, excludedId, optionFilter, modelFamily, onChange, onSelectOption }: {
    service: ErpService; field: ErpField; value: ErpValue; selectedLabel?: string; organizationId: ErpValue;
    excludedId?: number; optionFilter?: (record: ErpRecord) => boolean; modelFamily?: string; onChange: (value: ErpValue) => void;
    onSelectOption?: (option: ErpRecord | null) => void;
}) {
    const current = String(value ?? "");
    const disabled = ["units", "core/units"].includes(field.reference ?? "") && !organizationId;
    const [search, setSearch] = React.useState(selectedLabel ?? "");
    const [result, setResult] = React.useState<ErpPage | null>(null);
    const [error, setError] = React.useState("");
    const [completedQuery, setCompletedQuery] = React.useState("");
    const [retry, setRetry] = React.useState(0);
    const [open, setOpen] = React.useState(false);
    const [activeIndex, setActiveIndex] = React.useState(-1);

    const displayedSearch = !current && !open ? "" : search;
    const query = open && displayedSearch.trim() ? displayedSearch.trim() : "";
    const queryKey = JSON.stringify([field.reference, query, organizationId, modelFamily, retry]);
    const loading = Boolean(query) && !disabled && completedQuery !== queryKey;

    React.useEffect(() => {
        if (!query || disabled) return;

        const controller = new AbortController();
        const timer = setTimeout(() => {
            service.list(
                field.reference!,
                query,
                0,
                controller.signal,
                ["units", "core/units"].includes(field.reference ?? "") ? organizationId : undefined,
                modelFamily ? { modelFamily } : {}
            ).then(data => {
                if (!controller.signal.aborted) {
                    setResult(data);
                    setError("");
                }
            }).catch(error => {
                if (!controller.signal.aborted) {
                    setResult(null);
                    setError(errorMessage(error));
                }
            }).finally(() => {
                if (!controller.signal.aborted) setCompletedQuery(queryKey);
            });
        }, 250);

        return () => {
            clearTimeout(timer);
            controller.abort();
        };
    }, [field.reference, query, organizationId, service, modelFamily, queryKey, disabled]);

    const visibleResult = completedQuery === queryKey ? result : null;
    const visibleError = completedQuery === queryKey ? error : "";

    const options = (loading ? [] : visibleResult?.content ?? []).filter(item =>
        (!modelFamily || item.modelFamily === modelFamily) &&
        item.id !== excludedId &&
        (!optionFilter || optionFilter(item)) &&
        !(["units", "core/units"].includes(field.reference ?? "") &&
            organizationId &&
            String(item.organizationId) !== String(organizationId))
    );

    const selectOption = (option: ErpRecord): void => {
        onChange(option.id ?? null);
        onSelectOption?.(option);
        setSearch(option.label);
        setOpen(false);
        setActiveIndex(-1);
    };

    const clearSelectionForSearch = (nextSearch: string): void => {
        setSearch(nextSearch);
        setResult(null);
        setError("");
        setCompletedQuery("");
        setActiveIndex(-1);
        setOpen(Boolean(nextSearch.trim()));
        if (current) {
            onChange(null);
            onSelectOption?.(null);
        }
    };

    return (
        <div className={styles.reference}>
            <div className={styles.referenceCombobox}>
                <input
                    id={`core-${field.name}`}
                    type="search"
                    role="combobox"
                    data-comandos-float-force="true"
                    autoComplete="off"
                    aria-label={`Search ${field.label.toLowerCase()}`}
                    aria-autocomplete="list"
                    aria-expanded={open && Boolean(query)}
                    aria-controls={`core-${field.name}-options`}
                    aria-activedescendant={
                        open && activeIndex >= 0 && options[activeIndex]
                            ? `core-${field.name}-option-${options[activeIndex].id}`
                            : undefined
                    }
                    placeholder="Digite para localizar um registro"
                    value={displayedSearch}
                    required={field.required}
                    disabled={disabled}
                    onFocus={() => {
                        if (displayedSearch.trim() && !current) setOpen(true);
                    }}
                    onChange={event => clearSelectionForSearch(event.target.value)}
                    onKeyDown={event => {
                        if (event.key === "ArrowDown" && options.length) {
                            event.preventDefault();
                            setOpen(true);
                            setActiveIndex(index => Math.min(index + 1, options.length - 1));
                        } else if (event.key === "ArrowUp" && options.length) {
                            event.preventDefault();
                            setActiveIndex(index => Math.max(index - 1, 0));
                        } else if (event.key === "Enter" && open && activeIndex >= 0 && options[activeIndex]) {
                            event.preventDefault();
                            selectOption(options[activeIndex]);
                        } else if (event.key === "Escape") {
                            setOpen(false);
                            setActiveIndex(-1);
                        }
                    }}
                    onBlur={() => {
                        window.setTimeout(() => setOpen(false), 100);
                    }}
                />

                {open && Boolean(query) && (
                    <div
                        id={`core-${field.name}-options`}
                        role="listbox"
                        className={styles.referenceOptions}
                    >
                        {loading && (
                            <div className={styles.referenceStatus} role="status">
                                Localizando registros...
                            </div>
                        )}

                        {!loading && !visibleError && options.map((option, index) => (
                            <button
                                key={option.id}
                                id={`core-${field.name}-option-${option.id}`}
                                type="button"
                                role="option"
                                aria-selected={index === activeIndex}
                                className={index === activeIndex ? styles.referenceOptionActive : styles.referenceOption}
                                onMouseDown={event => event.preventDefault()}
                                onMouseEnter={() => setActiveIndex(index)}
                                onClick={() => selectOption(option)}
                            >
                                {option.label}
                            </button>
                        ))}

                        {!loading && !visibleError && options.length === 0 && (
                            <div className={styles.referenceStatus} role="status">
                                Nenhum registro encontrado.
                            </div>
                        )}

                        {!loading && visibleError && (
                            <div className={styles.referenceStatus} role="alert">
                                {visibleError}
                                {" "}
                                <button type="button" onMouseDown={event => event.preventDefault()} onClick={() => setRetry(value => value + 1)}>
                                    Tentar novamente
                                </button>
                            </div>
                        )}

                        {!loading && visibleResult && visibleResult.totalElements > visibleResult.size && (
                            <div className={styles.referenceHint}>
                                Exibindo os primeiros {visibleResult.size} resultados. Continue digitando para refinar.
                            </div>
                        )}
                    </div>
                )}
            </div>

            {disabled && <small>Selecione uma organização primeiro.</small>}
        </div>
    );
}
