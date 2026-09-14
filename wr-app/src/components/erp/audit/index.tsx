"use client";

import * as React from "react";

import axios from "axios";

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

import { InputText } from "@primereact/ui/inputtext";
import { Button } from "@primereact/ui/button";

import { AngleDoubleLeft } from "@primeicons/react/angle-double-left";
import { AngleDoubleRight } from "@primeicons/react/angle-double-right";
import { AngleLeft } from "@primeicons/react/angle-left";
import { AngleRight } from "@primeicons/react/angle-right";
import { EllipsisH } from "@primeicons/react/ellipsis-h";

import { Layout } from "components/layout";
import { Message } from "components/common/message";
import { useSession } from "components/auth/session-provider";

import {
    auditService,
    type AuditDetail,
    type AuditFilters,
    type AuditPage,
    type AuditSummary
} from "api/services/audit.service";

import styles from "../shared/workspace.module.css";

interface DataTableFilterEvent {
    filters: DataTableFilterMeta;
}

const EMPTY_FILTERS: AuditFilters = {
    resource: "",
    recordId: "",
    action: "",
    actor: "",
    from: "",
    until: ""
};

const INITIAL_TABLE_FILTERS: DataTableFilterMeta = {
    actor: {
        value: null,
        matchMode: FilterMatchMode.Contains
    },
    action: {
        value: null,
        matchMode: FilterMatchMode.Contains
    },
    resource: {
        value: null,
        matchMode: FilterMatchMode.Contains
    },
    recordId: {
        value: null,
        matchMode: FilterMatchMode.Contains
    }
};

const failure = (error: unknown): string =>
    axios.isAxiosError(error) &&
    typeof error.response?.data?.detail === "string"
        ? error.response.data.detail
        : "Unable to load audit records. Check the connection and try again.";

const getFilterValue = (
    filters: DataTableFilterMeta,
    field: string
): string => {
    const filter = filters[field];

    if (
        filter === null ||
        filter === undefined ||
        typeof filter !== "object" ||
        !("value" in filter) ||
        filter.value === null ||
        filter.value === undefined
    ) {
        return "";
    }

    return String(filter.value).trim();
};

export function AuditWorkspace(): React.JSX.Element {
    const { can } = useSession();

    return (
        <Layout title="Audit history">
            {can("audit", "READ")
                ? <AuditHistory />
                : (
                    <Message
                        type="info"
                        text="Your profile cannot view audit history."
                    />
                )
            }
        </Layout>
    );
}

function AuditHistory(): React.JSX.Element {
    const [filters, setFilters] =
        React.useState<AuditFilters>(EMPTY_FILTERS);

    const [tableFilters, setTableFilters] =
        React.useState<DataTableFilterMeta>(INITIAL_TABLE_FILTERS);

    const [page, setPage] =
        React.useState<number>(0);

    const rows = 10;

    const [revision, setRevision] =
        React.useState<number>(0);

    const [result, setResult] =
        React.useState<AuditPage | null>(null);

    const [loading, setLoading] =
        React.useState<boolean>(true);

    const [selected, setSelected] =
        React.useState<number | null>(null);

    const [detail, setDetail] =
        React.useState<AuditDetail | null>(null);

    const [error, setError] =
        React.useState<string>("");

    const [detailError, setDetailError] =
        React.useState<string>("");

    React.useEffect(() => {
        const controller = new AbortController();

        const timer = setTimeout(() => {
            setLoading(true);

            auditService
                .list(
                    filters,
                    page,
                    controller.signal,
                    rows
                )
                .then((data) => {
                    if (controller.signal.aborted) {
                        return;
                    }

                    setResult(data);
                    setError("");
                })
                .catch((requestError: unknown) => {
                    if (!controller.signal.aborted) {
                        setError(
                            failure(requestError)
                        );
                    }
                })
                .finally(() => {
                    if (!controller.signal.aborted) {
                        setLoading(false);
                    }
                });
        }, 250);

        return () => {
            clearTimeout(timer);
            controller.abort();
        };
    }, [
        filters,
        page,
        revision
    ]);

    React.useEffect(() => {
        if (selected === null) {
            return;
        }

        const controller =
            new AbortController();

        auditService
            .get(
                selected,
                controller.signal
            )
            .then((data) => {
                if (!controller.signal.aborted) {
                    setDetail(data);
                }
            })
            .catch((requestError: unknown) => {
                if (!controller.signal.aborted) {
                    setDetailError(
                        failure(requestError)
                    );
                }
            });

        return () => controller.abort();
    }, [
        selected,
        revision
    ]);

    const applyTableFilters = (
        nextTableFilters: DataTableFilterMeta
    ): void => {
        setTableFilters(nextTableFilters);

        setFilters((current) => ({
            ...current,
            actor: getFilterValue(
                nextTableFilters,
                "actor"
            ),
            action: getFilterValue(
                nextTableFilters,
                "action"
            ).toUpperCase(),
            resource: getFilterValue(
                nextTableFilters,
                "resource"
            ),
            recordId: getFilterValue(
                nextTableFilters,
                "recordId"
            )
        }));

        setPage(0);
        setResult(null);
        setError("");
        setSelected(null);
        setDetail(null);
    };

    const clearFilters = (): void => {
        setFilters(EMPTY_FILTERS);
        setTableFilters(INITIAL_TABLE_FILTERS);
        setPage(0);
        setResult(null);
        setError("");
        setSelected(null);
        setDetail(null);
    };

    const refresh = (): void => {
        setResult(null);
        setError("");
        setDetailError("");
        setLoading(true);
        setRevision(
            (value) => value + 1
        );
    };

    return (
        <div className={styles.workspace}>
            <p className={styles.intro}>
                Review recorded changes to ERP records and finalized sales.
                Audit entries cannot be edited or deleted here.
            </p>

            <div className={styles.toolbar}>
                <div className={styles.fields}>
                    <div className={styles.field}>
                        <label htmlFor="audit-from">
                            From (local time)
                        </label>

                        <InputText
                            id="audit-from"
                            type="datetime-local"
                            value={filters.from}
                            onChange={(
                                event:
                                    React.ChangeEvent<HTMLInputElement>
                            ) => {
                                setFilters(
                                    (current) => ({
                                        ...current,
                                        from:
                                            event.target.value
                                    })
                                );
                                setPage(0);
                            }}
                        />
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="audit-until">
                            Until (local time)
                        </label>

                        <InputText
                            id="audit-until"
                            type="datetime-local"
                            value={filters.until}
                            onChange={(
                                event:
                                    React.ChangeEvent<HTMLInputElement>
                            ) => {
                                setFilters(
                                    (current) => ({
                                        ...current,
                                        until:
                                            event.target.value
                                    })
                                );
                                setPage(0);
                            }}
                        />
                    </div>
                </div>

                <div
                    style={{
                        display: "flex",
                        alignItems: "flex-end",
                        gap: "0.5rem"
                    }}
                >
                    <Button
                        type="button"
                        severity="secondary"
                        onClick={clearFilters}
                    >
                        Clear filters
                    </Button>

                    <Button
                        type="button"
                        severity="secondary"
                        onClick={refresh}
                        disabled={loading}
                    >
                        Refresh
                    </Button>
                </div>
            </div>

            {error && (
                <Message
                    type="error"
                    text={error}
                />
            )}

            <div
                className="formgrid grid"
                style={{
                    width: "100%"
                }}
            >
                <div
                    className="col-12"
                    style={{
                        width: "100%"
                    }}
                >
                    <DataTable.Root
                        data={
                            result?.content ??
                            []
                        }
                        dataKey="id"
                        lazy
                        paginator
                        rows={rows}
                        totalRecords={
                            result?.totalElements ??
                            0
                        }
                        first={
                            page * rows
                        }
                        filters={
                            tableFilters
                        }
                        onFilter={(
                            event:
                                DataTableFilterEvent
                        ) => {
                            applyTableFilters(
                                event.filters
                            );
                        }}
                        style={{
                            width: "100%"
                        }}
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
                                    minWidth: "1050px",
                                    tableLayout: "auto"
                                }}
                            >
                                <DataTable.THead>
                                    <DataTable.THeadRow>
                                        <DataTable.THeadCell>
                                            Event
                                        </DataTable.THeadCell>

                                        <DataTable.THeadCell>
                                            Time
                                        </DataTable.THeadCell>

                                        <DataTable.THeadCell>
                                            Account
                                        </DataTable.THeadCell>

                                        <DataTable.THeadCell>
                                            Operation
                                        </DataTable.THeadCell>

                                        <DataTable.THeadCell>
                                            Resource
                                        </DataTable.THeadCell>

                                        <DataTable.THeadCell>
                                            Record
                                        </DataTable.THeadCell>

                                        <DataTable.THeadCell
                                            style={{
                                                width:
                                                    "7rem",
                                                textAlign:
                                                    "center"
                                            }}
                                        >
                                            Details
                                        </DataTable.THeadCell>
                                    </DataTable.THeadRow>

                                    <DataTable.THeadRow>
                                        <DataTable.THeadCell />
                                        <DataTable.THeadCell />

                                        <DataTable.THeadCell>
                                            <DataTable.Filter
                                                field="actor"
                                                display="row"
                                                dataType="text"
                                            >
                                                {({
                                                    value,
                                                    onChange
                                                }: DataTableFilterInstance) => (
                                                    <InputText
                                                        value={
                                                            (value as string) ??
                                                            ""
                                                        }
                                                        placeholder="Search account..."
                                                        size="small"
                                                        fluid
                                                        onChange={(
                                                            event:
                                                                React.ChangeEvent<HTMLInputElement>
                                                        ) => {
                                                            onChange(
                                                                event,
                                                                event.target.value
                                                            );
                                                        }}
                                                    />
                                                )}
                                            </DataTable.Filter>
                                        </DataTable.THeadCell>

                                        <DataTable.THeadCell>
                                            <DataTable.Filter
                                                field="action"
                                                display="row"
                                                dataType="text"
                                            >
                                                {({
                                                    value,
                                                    onChange
                                                }: DataTableFilterInstance) => (
                                                    <InputText
                                                        value={
                                                            (value as string) ??
                                                            ""
                                                        }
                                                        placeholder="Search operation..."
                                                        size="small"
                                                        fluid
                                                        onChange={(
                                                            event:
                                                                React.ChangeEvent<HTMLInputElement>
                                                        ) => {
                                                            onChange(
                                                                event,
                                                                event.target.value
                                                            );
                                                        }}
                                                    />
                                                )}
                                            </DataTable.Filter>
                                        </DataTable.THeadCell>

                                        <DataTable.THeadCell>
                                            <DataTable.Filter
                                                field="resource"
                                                display="row"
                                                dataType="text"
                                            >
                                                {({
                                                    value,
                                                    onChange
                                                }: DataTableFilterInstance) => (
                                                    <InputText
                                                        value={
                                                            (value as string) ??
                                                            ""
                                                        }
                                                        placeholder="Search resource..."
                                                        size="small"
                                                        fluid
                                                        onChange={(
                                                            event:
                                                                React.ChangeEvent<HTMLInputElement>
                                                        ) => {
                                                            onChange(
                                                                event,
                                                                event.target.value
                                                            );
                                                        }}
                                                    />
                                                )}
                                            </DataTable.Filter>
                                        </DataTable.THeadCell>

                                        <DataTable.THeadCell>
                                            <DataTable.Filter
                                                field="recordId"
                                                display="row"
                                                dataType="text"
                                            >
                                                {({
                                                    value,
                                                    onChange
                                                }: DataTableFilterInstance) => (
                                                    <InputText
                                                        value={
                                                            (value as string) ??
                                                            ""
                                                        }
                                                        inputMode="numeric"
                                                        placeholder="Search ID..."
                                                        size="small"
                                                        fluid
                                                        onChange={(
                                                            event:
                                                                React.ChangeEvent<HTMLInputElement>
                                                        ) => {
                                                            const nextValue =
                                                                event.target.value
                                                                    .replace(
                                                                        /\D/g,
                                                                        ""
                                                                    );

                                                            onChange(
                                                                event,
                                                                nextValue
                                                            );
                                                        }}
                                                    />
                                                )}
                                            </DataTable.Filter>
                                        </DataTable.THeadCell>

                                        <DataTable.THeadCell />
                                    </DataTable.THeadRow>
                                </DataTable.THead>

                                <DataTable.TBody>
                                    {({
                                        item,
                                        index
                                    }) => {
                                        const auditEvent =
                                            item as unknown as AuditSummary;

                                        return (
                                            <DataTable.Row
                                                key={
                                                    auditEvent.id ??
                                                    index
                                                }
                                            >
                                                <DataTable.Cell>
                                                    {auditEvent.id}
                                                </DataTable.Cell>

                                                <DataTable.Cell>
                                                    {new Date(
                                                        auditEvent.occurredAt
                                                    ).toLocaleString()}
                                                </DataTable.Cell>

                                                <DataTable.Cell>
                                                    {auditEvent.actorLogin ||
                                                        "Unauthenticated"}
                                                </DataTable.Cell>

                                                <DataTable.Cell>
                                                    {auditEvent.action}
                                                </DataTable.Cell>

                                                <DataTable.Cell>
                                                    {auditEvent.resource}
                                                </DataTable.Cell>

                                                <DataTable.Cell>
                                                    {auditEvent.recordId}
                                                </DataTable.Cell>

                                                <DataTable.Cell>
                                                    <div
                                                        style={{
                                                            display:
                                                                "flex",
                                                            justifyContent:
                                                                "center"
                                                        }}
                                                    >
                                                        <Button
                                                            type="button"
                                                            variant="text"
                                                            aria-label={
                                                                `View audit event ${auditEvent.id}`
                                                            }
                                                            onClick={() => {
                                                                if (
                                                                    selected !==
                                                                    auditEvent.id
                                                                ) {
                                                                    setDetail(
                                                                        null
                                                                    );
                                                                    setDetailError(
                                                                        ""
                                                                    );
                                                                    setSelected(
                                                                        auditEvent.id
                                                                    );
                                                                }
                                                            }}
                                                        >
                                                            View
                                                        </Button>
                                                    </div>
                                                </DataTable.Cell>
                                            </DataTable.Row>
                                        );
                                    }}
                                </DataTable.TBody>
                            </DataTable.Table>
                        </DataTable.TableContainer>

                        <DataTable.Pagination>
                            {({
                                rows:
                                    currentRows
                            }: DataTablePaginationInstance) => (
                                <Paginator.Root
                                    className="comandos-datatable-paginator"
                                    page={
                                        page + 1
                                    }
                                    total={
                                        result?.totalElements ??
                                        0
                                    }
                                    itemsPerPage={
                                        currentRows ??
                                        rows
                                    }
                                    onPageChange={(
                                        event:
                                            PaginatorRootChangeEvent
                                    ) => {
                                        setLoading(true);
                                        setResult(null);
                                        setPage(
                                            event.value -
                                                1
                                        );
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
                                                        paginatorPage.type ===
                                                        "page" ? (
                                                            <Paginator.Page
                                                                key={
                                                                    pageIndex
                                                                }
                                                                value={
                                                                    paginatorPage.value
                                                                }
                                                            />
                                                        ) : (
                                                            <Paginator.Ellipsis
                                                                key={
                                                                    pageIndex
                                                                }
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
                                borderTop:
                                    "1px solid #e5e7eb",
                                fontSize:
                                    "0.875rem"
                            }}
                        >
                            Total records:{" "}
                            {result?.totalElements ??
                                0}
                        </div>
                    </DataTable.Root>

                    {loading && (
                        <p role="status">
                            Loading audit historyâ€¦
                        </p>
                    )}

                    {!loading &&
                        !error &&
                        result?.content.length === 0 && (
                            <p>
                                No audit records match these filters.
                            </p>
                        )}
                </div>
            </div>

            {selected !== null && (
                <section
                    className="mt-6"
                    aria-label="Audit event details"
                >
                    <div className={styles.toolbar}>
                        <h2>
                            Audit event #{selected}
                        </h2>

                        <Button
                            type="button"
                            severity="secondary"
                            onClick={() => {
                                setSelected(null);
                                setDetail(null);
                            }}
                        >
                            Close details
                        </Button>
                    </div>

                    {detailError && (
                        <Message
                            type="error"
                            text={detailError}
                        />
                    )}

                    {!detail &&
                        !detailError && (
                            <p role="status">
                                Loading event detailsâ€¦
                            </p>
                        )}

                    {detail && (
                        <div className={styles.fields}>
                            {(
                                [
                                    "before",
                                    "after"
                                ] as const
                            ).map((key) => (
                                <div
                                    key={key}
                                    className={
                                        styles.field
                                    }
                                >
                                    <h3>
                                        {key === "before"
                                            ? "Before"
                                            : "After"}
                                    </h3>

                                    <pre
                                        className="max-h-96 overflow-auto p-3 border-round surface-ground"
                                        style={{
                                            whiteSpace:
                                                "pre-wrap",
                                            overflowWrap:
                                                "anywhere"
                                        }}
                                    >
                                        {detail[key] ===
                                        null
                                            ? "No snapshot"
                                            : JSON.stringify(
                                                detail[
                                                    key
                                                ],
                                                null,
                                                2
                                            )}
                                    </pre>
                                </div>
                            ))}
                        </div>
                    )}
                </section>
            )}
        </div>
    );
}
