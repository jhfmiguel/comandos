"use client";

import React, {
    useCallback,
    useEffect,
    useRef,
    useState
} from "react";

import { useRouter } from "next/navigation";

import {
    DataTable,
    FilterMatchMode
} from "components/ui/datatable";

import type {
    DataTableEditingEvent,
    DataTableFilterInstance,
    DataTableFilterMeta,
    DataTablePaginationInstance,
    DataTableRowEditEvent
} from "components/ui/datatable";

import { Paginator } from "components/ui/paginator";

import type {
    PaginatorPagesInstance,
    PaginatorRootChangeEvent
} from "components/ui/paginator";

import { InputText } from "components/ui/inputtext";
import { Dialog } from "components/ui/dialog";
import { Button as PrimeButton } from "components/ui/button";

import { AngleDoubleLeft } from "components/ui/icons";
import { AngleDoubleRight } from "components/ui/icons";
import { AngleLeft } from "components/ui/icons";
import { AngleRight } from "components/ui/icons";
import { Check } from "components/ui/icons";
import { EllipsisH } from "components/ui/icons";
import { Pencil } from "components/ui/icons";
import { Plus } from "components/ui/icons";
import { Times } from "components/ui/icons";
import { Trash } from "components/ui/icons";

import { Layout, Loader } from "components";

import { User } from "api/models/users";
import { useUserService } from "api/services/user.service";

interface DialogOpenChangeEvent {
    value?: boolean;
}

interface DataTableFilterEvent {
    filters: DataTableFilterMeta;
}

interface UserSearchFilters {
    name: string;
    cpf: string;
    birth: string;
    address: string;
    email: string;
    phone: string;
}

const INITIAL_FILTERS: DataTableFilterMeta = {
    name: {
        value: null,
        matchMode: FilterMatchMode.Contains
    },
    cpf: {
        value: null,
        matchMode: FilterMatchMode.Contains
    },
    birth: {
        value: null,
        matchMode: FilterMatchMode.Contains
    },
    address: {
        value: null,
        matchMode: FilterMatchMode.Contains
    },
    email: {
        value: null,
        matchMode: FilterMatchMode.Contains
    },
    phone: {
        value: null,
        matchMode: FilterMatchMode.Contains
    }
};

const EMPTY_SEARCH_FILTERS: UserSearchFilters = {
    name: "",
    cpf: "",
    birth: "",
    address: "",
    email: "",
    phone: ""
};

const onlyNumbers = (
    value: string | number | null | undefined
): string => {
    if (
        value === null ||
        value === undefined
    ) {
        return "";
    }

    return String(value).replace(/\D/g, "");
};

const formatCPF = (
    value: string | number | null | undefined
): string => {
    const numbers = onlyNumbers(value).slice(0, 11);

    if (!numbers) {
        return "";
    }

    return numbers
        .replace(/^(\d{3})(\d)/, "$1.$2")
        .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
        .replace(
            /^(\d{3})\.(\d{3})\.(\d{3})(\d)/,
            "$1.$2.$3-$4"
        );
};

const formatPhone = (
    value: string | number | null | undefined
): string => {
    const numbers = onlyNumbers(value).slice(0, 11);

    if (!numbers) {
        return "";
    }

    if (numbers.length <= 10) {
        return numbers
            .replace(/^(\d{2})(\d)/, "($1) $2")
            .replace(/(\d{4})(\d)/, "$1-$2");
    }

    return numbers
        .replace(/^(\d{2})(\d)/, "($1) $2")
        .replace(/(\d{5})(\d)/, "$1-$2");
};

const getFilterValue = (
    filters: DataTableFilterMeta,
    field: string
): string => {
    const filter = filters[field];

    if (
        filter === null ||
        filter === undefined ||
        typeof filter !== "object"
    ) {
        return "";
    }

    if (!("value" in filter)) {
        return "";
    }

    const value = filter.value;

    if (
        value === null ||
        value === undefined
    ) {
        return "";
    }

    return String(value);
};

const createSearchFilters = (
    filters: DataTableFilterMeta
): UserSearchFilters => {
    return {
        name: getFilterValue(
            filters,
            "name"
        ).trim(),

        cpf: onlyNumbers(
            getFilterValue(
                filters,
                "cpf"
            )
        ),

        birth: getFilterValue(
            filters,
            "birth"
        ).trim(),

        address: getFilterValue(
            filters,
            "address"
        ).trim(),

        email: getFilterValue(
            filters,
            "email"
        ).trim(),

        phone: onlyNumbers(
            getFilterValue(
                filters,
                "phone"
            )
        )
    };
};

const normalizeText = (
    value: unknown
): string => {
    if (
        value === null ||
        value === undefined
    ) {
        return "";
    }

    return String(value)
        .toLocaleLowerCase("pt-BR")
        .trim();
};

const filterUsers = (
    users: User[],
    filters: UserSearchFilters
): User[] => {
    return users.filter((user) => {
        const birthFilter =
            normalizeText(filters.birth);

        const addressFilter =
            normalizeText(filters.address);

        const emailFilter =
            normalizeText(filters.email);

        const phoneFilter =
            onlyNumbers(filters.phone);

        const matchesBirth =
            !birthFilter ||
            normalizeText(user.birth).includes(
                birthFilter
            );

        const matchesAddress =
            !addressFilter ||
            normalizeText(user.address).includes(
                addressFilter
            );

        const matchesEmail =
            !emailFilter ||
            normalizeText(user.email).includes(
                emailFilter
            );

        const matchesPhone =
            !phoneFilter ||
            onlyNumbers(user.phone).includes(
                phoneFilter
            );

        return (
            matchesBirth &&
            matchesAddress &&
            matchesEmail &&
            matchesPhone
        );
    });
};

const formatDate = (
    dateString?: string
): string => {
    if (!dateString) {
        return "-";
    }

    try {
        if (dateString.includes("-")) {
            const datePart =
                dateString.split("T")[0];

            const [
                year,
                month,
                day
            ] = datePart.split("-");

            if (
                year &&
                month &&
                day
            ) {
                return `${day}/${month}/${year}`;
            }
        }

        return new Date(
            dateString
        ).toLocaleDateString(
            "pt-BR"
        );
    } catch {
        return dateString;
    }
};

export const UsersList: React.FC = () => {
    const router = useRouter();

    const userService =
        useUserService();

    const [loading, setLoading] =
        useState<boolean>(true);

    const [users, setUsers] =
        useState<User[]>([]);

    const [
        totalRecords,
        setTotalRecords
    ] = useState<number>(0);

    const [rows, setRows] =
        useState<number>(10);

    const [
        currentPage,
        setCurrentPage
    ] = useState<number>(0);

    const [filters, setFilters] =
        useState<DataTableFilterMeta>(
            INITIAL_FILTERS
        );

    const [
        deleteUserId,
        setDeleteUserId
    ] = useState<
        string | number | null
    >(null);

    const [
        editingKeys,
        setEditingKeys
    ] = useState<
        Record<string, boolean>
    >({});

    const draftRef = useRef<
        Record<
            string,
            Partial<User>
        >
    >({});

    const filterTimeoutRef = useRef<
        ReturnType<typeof setTimeout> | null
    >(null);

    const requestRef = useRef<AbortController | null>(null);

    const loadUsers = useCallback(
        (
            searchFilters: UserSearchFilters,
            pageIndex: number,
            pageSize: number,
            signal: AbortSignal
        ): Promise<void> => {
            return userService.findUser(
                        searchFilters.name,
                        searchFilters.cpf,
                        pageIndex,
                        pageSize,
                        searchFilters.birth,
                        searchFilters.address,
                        searchFilters.email,
                        searchFilters.phone,
                        signal
                    ).then(data => {
                if (signal.aborted) return;

                const content =
                    data?.content ?? [];

                const sortedContent = [
                    ...content
                ].sort(
                    (a, b) =>
                        Number(a.id ?? 0) -
                        Number(b.id ?? 0)
                );

                setUsers(
                    filterUsers(
                        sortedContent,
                        searchFilters
                    )
                );

                setTotalRecords(
                    data?.totalElements ?? 0
                );
            }).catch(error => {
                if (signal.aborted) return;
                console.error(
                    "Failed to fetch users:",
                    error
                );
            }).finally(() => {
                if (!signal.aborted) setLoading(false);
            });
        },
        [userService]
    );

    const fetchUsers = useCallback((searchFilters: UserSearchFilters, pageIndex: number, pageSize: number) => {
        requestRef.current?.abort();
        const controller = new AbortController();
        requestRef.current = controller;
        setLoading(true);
        return loadUsers(searchFilters, pageIndex, pageSize, controller.signal);
    }, [loadUsers]);

    useEffect(() => {
        const controller = new AbortController();
        requestRef.current = controller;
        void loadUsers(
            EMPTY_SEARCH_FILTERS,
            0,
            10,
            controller.signal
        );

        return () => {
            requestRef.current?.abort();
            if (
                filterTimeoutRef.current
            ) {
                clearTimeout(
                    filterTimeoutRef.current
                );
            }
        };

    }, [loadUsers]);

    const handleFilterChange =
        useCallback(
            (
                nextFilters:
                    DataTableFilterMeta
            ): void => {
                setFilters(
                    nextFilters
                );

                setCurrentPage(0);

                if (
                    filterTimeoutRef.current
                ) {
                    clearTimeout(
                        filterTimeoutRef.current
                    );
                }

                filterTimeoutRef.current =
                    setTimeout(() => {
                        const searchFilters =
                            createSearchFilters(
                                nextFilters
                            );

                        fetchUsers(
                            searchFilters,
                            0,
                            rows
                        );
                    }, 400);
            },
            [
                fetchUsers,
                rows
            ]
        );

    const handleTableFilter = (
        event: DataTableFilterEvent
    ): void => {
        handleFilterChange(
            event.filters
        );
    };

    const handleSave = useCallback(
        async (
            event:
                DataTableRowEditEvent
        ): Promise<void> => {
            const userId =
                event.data.id as
                    | string
                    | number
                    | undefined;

            if (
                userId === undefined ||
                userId === null
            ) {
                return;
            }

            const rowId =
                userId.toString();

            const patch =
                draftRef.current[
                    rowId
                ];

            if (
                patch === undefined ||
                Object.keys(patch)
                    .length === 0
            ) {
                delete draftRef
                    .current[rowId];

                return;
            }

            const updatedUser: User = {
                ...(event.data as User),
                ...patch
            };

            setLoading(true);

            try {
                await userService.updateUser(
                    updatedUser
                );

                setEditingKeys(
                    (current) => {
                        const next = {
                            ...current
                        };

                        delete next[
                            rowId
                        ];

                        return next;
                    }
                );

                await fetchUsers(
                    createSearchFilters(
                        filters
                    ),
                    currentPage,
                    rows
                );
            } catch (error) {
                console.error(
                    "Failed to update user:",
                    error
                );
            } finally {
                delete draftRef
                    .current[rowId];

                setLoading(false);
            }
        },
        [
            userService,
            fetchUsers,
            filters,
            currentPage,
            rows
        ]
    );

    const handleCancel =
        useCallback(
            (
                event:
                    DataTableRowEditEvent
            ): void => {
                const userId =
                    event.data.id as
                        | string
                        | number
                        | undefined;

                if (
                    userId === undefined ||
                    userId === null
                ) {
                    return;
                }

                const rowId =
                    userId.toString();

                delete draftRef.current[
                    rowId
                ];

                setEditingKeys(
                    (current) => {
                        const next = {
                            ...current
                        };

                        delete next[
                            rowId
                        ];

                        return next;
                    }
                );
            },
            []
        );

    const handleDelete =
        useCallback(
            (
                id:
                    | string
                    | number
            ): void => {
                setDeleteUserId(
                    id
                );
            },
            []
        );

    const handleDeleteDialogOpenChange = (
        event: DialogOpenChangeEvent
    ): void => {
        if (
            event.value === false
        ) {
            setDeleteUserId(
                null
            );
        }
    };

    const confirmDelete = useCallback(
        async (): Promise<void> => {
            if (
                deleteUserId === null
            ) {
                return;
            }

            setLoading(true);

            try {
                await userService.deleteUser(
                    deleteUserId
                );

                setDeleteUserId(
                    null
                );

                const nextTotal =
                    Math.max(
                        totalRecords - 1,
                        0
                    );

                const maxPage =
                    Math.max(
                        Math.ceil(
                            nextTotal /
                                rows
                        ) - 1,
                        0
                    );

                const nextPage =
                    Math.min(
                        currentPage,
                        maxPage
                    );

                setCurrentPage(
                    nextPage
                );

                await fetchUsers(
                    createSearchFilters(
                        filters
                    ),
                    nextPage,
                    rows
                );
            } catch (error) {
                console.error(
                    "Failed to delete user:",
                    error
                );
            } finally {
                setLoading(false);
            }
        },
        [
            userService,
            deleteUserId,
            totalRecords,
            currentPage,
            fetchUsers,
            filters,
            rows
        ]
    );

    return (
        <Layout title="Users">
            <div
                style={{
                    position: "relative",
                    minHeight: "100%"
                }}
            >
                {loading && (
                    <Loader
                        show={loading}
                    />
                )}

                <div
                    style={{
                        display: "flex",
                        justifyContent: "flex-end",
                        marginBottom: "1rem"
                    }}
                >
                    <PrimeButton
                        type="button"
                        className="registration-yellow-button"
                        onClick={() => {
                            router.push(
                                "/registrations/users"
                            );
                        }}
                    >
                        <Plus size={18} />

                        <span
                            style={{
                                marginLeft: "0.5rem"
                            }}
                        >
                            New User
                        </span>
                    </PrimeButton>
                </div>

                <div className="formgrid grid">
                    <div
                        className="col-12"
                        style={{
                            width: "100%"
                        }}
                    >
                        <DataTable.Root
                            data={users}
                            dataKey="id"
                            lazy
                            paginator
                            rows={rows}
                            totalRecords={
                                totalRecords
                            }
                            first={
                                currentPage *
                                rows
                            }
                            filters={
                                filters
                            }
                            onFilter={
                                handleTableFilter
                            }
                            editMode="row"
                            editingKeys={
                                editingKeys
                            }
                            onEditingKeysChange={(
                                event:
                                    DataTableEditingEvent
                            ) => {
                                setEditingKeys(
                                    event.value
                                );
                            }}
                            onRowEditSave={
                                handleSave
                            }
                            onRowEditCancel={
                                handleCancel
                            }
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
                                        minWidth: "1250px",
                                        tableLayout: "auto"
                                    }}
                                >
                                    <DataTable.THead>
                                        <DataTable.THeadRow>
                                            <DataTable.THeadCell>
                                                ID
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                Name
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                CPF
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                Birth
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                Address
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                E-mail
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                Phone
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell
                                                style={{
                                                    width: "10rem",
                                                    textAlign: "center"
                                                }}
                                            >
                                                Actions
                                            </DataTable.THeadCell>
                                        </DataTable.THeadRow>

                                        <DataTable.THeadRow>
                                            <DataTable.THeadCell />

                                            <DataTable.THeadCell>
                                                <DataTable.Filter
                                                    field="name"
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
                                                            onChange={(
                                                                event:
                                                                    React.ChangeEvent<HTMLInputElement>
                                                            ) => {
                                                                onChange(
                                                                    event,
                                                                    event.target.value
                                                                );
                                                            }}
                                                            placeholder="Search name..."
                                                            size="small"
                                                            fluid
                                                        />
                                                    )}
                                                </DataTable.Filter>
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                <DataTable.Filter
                                                    field="cpf"
                                                    display="row"
                                                    dataType="text"
                                                >
                                                    {({
                                                        value,
                                                        onChange
                                                    }: DataTableFilterInstance) => (
                                                        <InputText
                                                            value={formatCPF(
                                                                (value as string) ??
                                                                    ""
                                                            )}
                                                            inputMode="numeric"
                                                            maxLength={14}
                                                            onChange={(
                                                                event:
                                                                    React.ChangeEvent<HTMLInputElement>
                                                            ) => {
                                                                const maskedValue =
                                                                    formatCPF(
                                                                        event.target.value
                                                                    );

                                                                onChange(
                                                                    event,
                                                                    maskedValue
                                                                );
                                                            }}
                                                            placeholder="000.000.000-00"
                                                            size="small"
                                                            fluid
                                                        />
                                                    )}
                                                </DataTable.Filter>
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                <DataTable.Filter
                                                    field="birth"
                                                    display="row"
                                                    dataType="text"
                                                >
                                                    {({
                                                        value,
                                                        onChange
                                                    }: DataTableFilterInstance) => (
                                                        <InputText
                                                            type="date"
                                                            value={
                                                                (value as string) ??
                                                                ""
                                                            }
                                                            onChange={(
                                                                event:
                                                                    React.ChangeEvent<HTMLInputElement>
                                                            ) => {
                                                                onChange(
                                                                    event,
                                                                    event.target.value
                                                                );
                                                            }}
                                                            size="small"
                                                            fluid
                                                        />
                                                    )}
                                                </DataTable.Filter>
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                <DataTable.Filter
                                                    field="address"
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
                                                            onChange={(
                                                                event:
                                                                    React.ChangeEvent<HTMLInputElement>
                                                            ) => {
                                                                onChange(
                                                                    event,
                                                                    event.target.value
                                                                );
                                                            }}
                                                            placeholder="Search address..."
                                                            size="small"
                                                            fluid
                                                        />
                                                    )}
                                                </DataTable.Filter>
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                <DataTable.Filter
                                                    field="email"
                                                    display="row"
                                                    dataType="text"
                                                >
                                                    {({
                                                        value,
                                                        onChange
                                                    }: DataTableFilterInstance) => (
                                                        <InputText
                                                            type="email"
                                                            value={
                                                                (value as string) ??
                                                                ""
                                                            }
                                                            onChange={(
                                                                event:
                                                                    React.ChangeEvent<HTMLInputElement>
                                                            ) => {
                                                                onChange(
                                                                    event,
                                                                    event.target.value
                                                                );
                                                            }}
                                                            placeholder="Search e-mail..."
                                                            size="small"
                                                            fluid
                                                        />
                                                    )}
                                                </DataTable.Filter>
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                <DataTable.Filter
                                                    field="phone"
                                                    display="row"
                                                    dataType="text"
                                                >
                                                    {({
                                                        value,
                                                        onChange
                                                    }: DataTableFilterInstance) => (
                                                        <InputText
                                                            value={formatPhone(
                                                                (value as string) ??
                                                                    ""
                                                            )}
                                                            inputMode="numeric"
                                                            maxLength={15}
                                                            onChange={(
                                                                event:
                                                                    React.ChangeEvent<HTMLInputElement>
                                                            ) => {
                                                                const maskedValue =
                                                                    formatPhone(
                                                                        event.target.value
                                                                    );

                                                                onChange(
                                                                    event,
                                                                    maskedValue
                                                                );
                                                            }}
                                                            placeholder="(00) 00000-0000"
                                                            size="small"
                                                            fluid
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
                                        }: {
                                            item: User;
                                            index: number;
                                        }) => {
                                            const userId =
                                                item.id;

                                            const rowId =
                                                userId?.toString() ??
                                                "";

                                            const isEditing =
                                                Boolean(
                                                    editingKeys[
                                                        rowId
                                                    ]
                                                );

                                            const rowData: Record<
                                                string,
                                                unknown
                                            > = {
                                                ...item
                                            };

                                            return (
                                                <DataTable.Row
                                                    key={
                                                        userId ??
                                                        index
                                                    }
                                                >
                                                    <DataTable.RowEditor
                                                        rowKey={
                                                            userId ??
                                                            index
                                                        }
                                                        rowData={
                                                            rowData
                                                        }
                                                    >
                                                        <DataTable.Cell>
                                                            {item.id}
                                                        </DataTable.Cell>

                                                        <DataTable.Cell>
                                                            <DataTable.CellEditor
                                                                field="name"
                                                                rowIndex={
                                                                    index
                                                                }
                                                                rowData={
                                                                    rowData
                                                                }
                                                            >
                                                                <DataTable.CellEditorDisplay>
                                                                    {item.name ??
                                                                        "-"}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        defaultValue={
                                                                            item.name ??
                                                                            ""
                                                                        }
                                                                        onChange={(
                                                                            event:
                                                                                React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                rowId
                                                                            ] = {
                                                                                ...draftRef.current[
                                                                                    rowId
                                                                                ],
                                                                                name:
                                                                                    event.target.value
                                                                            };
                                                                        }}
                                                                        fluid
                                                                    />
                                                                </DataTable.CellEditorContent>
                                                            </DataTable.CellEditor>
                                                        </DataTable.Cell>

                                                        <DataTable.Cell>
                                                            <DataTable.CellEditor
                                                                field="cpf"
                                                                rowIndex={
                                                                    index
                                                                }
                                                                rowData={
                                                                    rowData
                                                                }
                                                            >
                                                                <DataTable.CellEditorDisplay>
                                                                    {formatCPF(
                                                                        item.cpf
                                                                    ) ||
                                                                        "-"}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        defaultValue={formatCPF(
                                                                            item.cpf
                                                                        )}
                                                                        inputMode="numeric"
                                                                        maxLength={
                                                                            14
                                                                        }
                                                                        onChange={(
                                                                            event:
                                                                                React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            const cpf =
                                                                                onlyNumbers(
                                                                                    event.target.value
                                                                                ).slice(
                                                                                    0,
                                                                                    11
                                                                                );

                                                                            event.target.value =
                                                                                formatCPF(
                                                                                    cpf
                                                                                );

                                                                            draftRef.current[
                                                                                rowId
                                                                            ] = {
                                                                                ...draftRef.current[
                                                                                    rowId
                                                                                ],
                                                                                cpf:
                                                                                    cpf ||
                                                                                    undefined
                                                                            };
                                                                        }}
                                                                        fluid
                                                                    />
                                                                </DataTable.CellEditorContent>
                                                            </DataTable.CellEditor>
                                                        </DataTable.Cell>

                                                        <DataTable.Cell>
                                                            <DataTable.CellEditor
                                                                field="birth"
                                                                rowIndex={
                                                                    index
                                                                }
                                                                rowData={
                                                                    rowData
                                                                }
                                                            >
                                                                <DataTable.CellEditorDisplay>
                                                                    {formatDate(
                                                                        item.birth
                                                                    )}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        type="date"
                                                                        defaultValue={
                                                                            item.birth?.split(
                                                                                "T"
                                                                            )[0] ??
                                                                            ""
                                                                        }
                                                                        onChange={(
                                                                            event:
                                                                                React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                rowId
                                                                            ] = {
                                                                                ...draftRef.current[
                                                                                    rowId
                                                                                ],
                                                                                birth:
                                                                                    event.target.value ||
                                                                                    undefined
                                                                            };
                                                                        }}
                                                                        fluid
                                                                    />
                                                                </DataTable.CellEditorContent>
                                                            </DataTable.CellEditor>
                                                        </DataTable.Cell>

                                                        <DataTable.Cell>
                                                            <DataTable.CellEditor
                                                                field="address"
                                                                rowIndex={
                                                                    index
                                                                }
                                                                rowData={
                                                                    rowData
                                                                }
                                                            >
                                                                <DataTable.CellEditorDisplay>
                                                                    {item.address ??
                                                                        "-"}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        defaultValue={
                                                                            item.address ??
                                                                            ""
                                                                        }
                                                                        onChange={(
                                                                            event:
                                                                                React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                rowId
                                                                            ] = {
                                                                                ...draftRef.current[
                                                                                    rowId
                                                                                ],
                                                                                address:
                                                                                    event.target.value
                                                                            };
                                                                        }}
                                                                        fluid
                                                                    />
                                                                </DataTable.CellEditorContent>
                                                            </DataTable.CellEditor>
                                                        </DataTable.Cell>

                                                        <DataTable.Cell>
                                                            <DataTable.CellEditor
                                                                field="email"
                                                                rowIndex={
                                                                    index
                                                                }
                                                                rowData={
                                                                    rowData
                                                                }
                                                            >
                                                                <DataTable.CellEditorDisplay>
                                                                    {item.email ??
                                                                        "-"}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        type="email"
                                                                        defaultValue={
                                                                            item.email ??
                                                                            ""
                                                                        }
                                                                        onChange={(
                                                                            event:
                                                                                React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                rowId
                                                                            ] = {
                                                                                ...draftRef.current[
                                                                                    rowId
                                                                                ],
                                                                                email:
                                                                                    event.target.value
                                                                            };
                                                                        }}
                                                                        fluid
                                                                    />
                                                                </DataTable.CellEditorContent>
                                                            </DataTable.CellEditor>
                                                        </DataTable.Cell>

                                                        <DataTable.Cell>
                                                            <DataTable.CellEditor
                                                                field="phone"
                                                                rowIndex={
                                                                    index
                                                                }
                                                                rowData={
                                                                    rowData
                                                                }
                                                            >
                                                                <DataTable.CellEditorDisplay>
                                                                    {formatPhone(
                                                                        item.phone
                                                                    ) ||
                                                                        "-"}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        defaultValue={formatPhone(
                                                                            item.phone
                                                                        )}
                                                                        inputMode="numeric"
                                                                        maxLength={
                                                                            15
                                                                        }
                                                                        onChange={(
                                                                            event:
                                                                                React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            const phone =
                                                                                onlyNumbers(
                                                                                    event.target.value
                                                                                ).slice(
                                                                                    0,
                                                                                    11
                                                                                );

                                                                            event.target.value =
                                                                                formatPhone(
                                                                                    phone
                                                                                );

                                                                            draftRef.current[
                                                                                rowId
                                                                            ] = {
                                                                                ...draftRef.current[
                                                                                    rowId
                                                                                ],
                                                                                phone:
                                                                                    phone ||
                                                                                    undefined
                                                                            };
                                                                        }}
                                                                        fluid
                                                                    />
                                                                </DataTable.CellEditorContent>
                                                            </DataTable.CellEditor>
                                                        </DataTable.Cell>

                                                        <DataTable.Cell>
                                                            <div
                                                                style={{
                                                                    display:
                                                                        "flex",
                                                                    alignItems:
                                                                        "center",
                                                                    justifyContent:
                                                                        "center",
                                                                    gap:
                                                                        "0.5rem"
                                                                }}
                                                            >
                                                                {!isEditing && (
                                                                    <DataTable.RowEditorInit
                                                                        as={
                                                                            PrimeButton
                                                                        }
                                                                        variant="text"
                                                                        severity="secondary"
                                                                        title="Edit user"
                                                                        aria-label="Edit user"
                                                                    >
                                                                        <Pencil
                                                                            size={
                                                                                20
                                                                            }
                                                                        />
                                                                    </DataTable.RowEditorInit>
                                                                )}

                                                                {isEditing && (
                                                                    <>
                                                                        <DataTable.RowEditorSave
                                                                            as={
                                                                                PrimeButton
                                                                            }
                                                                            variant="text"
                                                                            severity="success"
                                                                            title="Save changes"
                                                                            aria-label="Save changes"
                                                                        >
                                                                            <Check
                                                                                size={
                                                                                    20
                                                                                }
                                                                            />
                                                                        </DataTable.RowEditorSave>

                                                                        <DataTable.RowEditorCancel
                                                                            as={
                                                                                PrimeButton
                                                                            }
                                                                            variant="text"
                                                                            severity="secondary"
                                                                            title="Cancel editing"
                                                                            aria-label="Cancel editing"
                                                                        >
                                                                            <Times
                                                                                size={
                                                                                    20
                                                                                }
                                                                            />
                                                                        </DataTable.RowEditorCancel>
                                                                    </>
                                                                )}

                                                                {!isEditing && (
                                                                    <PrimeButton
                                                                        type="button"
                                                                        variant="text"
                                                                        severity="danger"
                                                                        title="Delete user"
                                                                        aria-label="Delete user"
                                                                        onClick={(
                                                                            event:
                                                                                React.MouseEvent<HTMLButtonElement>
                                                                        ) => {
                                                                            event.preventDefault();
                                                                            event.stopPropagation();

                                                                            if (
                                                                                userId ===
                                                                                    undefined ||
                                                                                userId ===
                                                                                    null
                                                                            ) {
                                                                                return;
                                                                            }

                                                                            handleDelete(
                                                                                userId
                                                                            );
                                                                        }}
                                                                    >
                                                                        <Trash
                                                                            size={
                                                                                20
                                                                            }
                                                                        />
                                                                    </PrimeButton>
                                                                )}
                                                            </div>
                                                        </DataTable.Cell>
                                                    </DataTable.RowEditor>
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
                                            currentPage +
                                            1
                                        }
                                        total={
                                            totalRecords
                                        }
                                        itemsPerPage={
                                            rows
                                        }
                                        onPageChange={(
                                            event:
                                                PaginatorRootChangeEvent
                                        ) => {
                                            const nextPage =
                                                event.value -
                                                1;

                                            const nextRows =
                                                currentRows ??
                                                rows;

                                            setCurrentPage(
                                                nextPage
                                            );

                                            setRows(
                                                nextRows
                                            );

                                            fetchUsers(
                                                createSearchFilters(
                                                    filters
                                                ),
                                                nextPage,
                                                nextRows
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
                                                            page,
                                                            pageIndex
                                                        ) =>
                                                            page.type ===
                                                            "page" ? (
                                                                <Paginator.Page
                                                                    key={
                                                                        pageIndex
                                                                    }
                                                                    value={
                                                                        page.value
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
                                {totalRecords}
                            </div>
                        </DataTable.Root>
                    </div>
                </div>
            </div>

            <Dialog.Root
                open={
                    deleteUserId !== null
                }
                onOpenChange={
                    handleDeleteDialogOpenChange
                }
            >
                <Dialog.Portal>
                    <Dialog.Backdrop />

                    <Dialog.Positioner>
                        <Dialog.Popup
                            style={{
                                width: "26rem"
                            }}
                        >
                            <Dialog.Header>
                                <Dialog.Title>
                                    Delete User
                                </Dialog.Title>
                            </Dialog.Header>

                            <Dialog.Content>
                                <div
                                    style={{
                                        display: "flex",
                                        flexDirection:
                                            "column",
                                        gap: "1rem"
                                    }}
                                >
                                    <p
                                        style={{
                                            margin: 0
                                        }}
                                    >
                                        Are you sure you want
                                        to delete this user?
                                    </p>

                                    <div
                                        style={{
                                            display: "flex",
                                            justifyContent:
                                                "flex-end",
                                            gap: "0.5rem"
                                        }}
                                    >
                                        <PrimeButton
                                            type="button"
                                            severity="secondary"
                                            disabled={
                                                loading
                                            }
                                            onClick={() => {
                                                setDeleteUserId(
                                                    null
                                                );
                                            }}
                                        >
                                            Cancel
                                        </PrimeButton>

                                        <PrimeButton
                                            type="button"
                                            severity="danger"
                                            disabled={
                                                loading
                                            }
                                            onClick={
                                                confirmDelete
                                            }
                                        >
                                            {loading
                                                ? "Deleting..."
                                                : "Delete"}
                                        </PrimeButton>
                                    </div>
                                </div>
                            </Dialog.Content>
                        </Dialog.Popup>
                    </Dialog.Positioner>
                </Dialog.Portal>
            </Dialog.Root>
            
        
            <style jsx global>{`
                .registration-yellow-button {
                    background-color: #ff9900 !important;
                    border-color: #ff9900 !important;
                    color: #1f1f1f !important;
                    font-weight: 600;
                    transition:
                        background-color 0.2s ease,
                        border-color 0.2s ease,
                        box-shadow 0.2s ease;
                }

                .registration-yellow-button:hover:not(:disabled) {
                    background-color: #e68a00 !important;
                    border-color: #e68a00 !important;
                    color: #1f1f1f !important;
                }

                .registration-yellow-button:active:not(:disabled) {
                    background-color: #cc7a00 !important;
                    border-color: #cc7a00 !important;
                    color: #1f1f1f !important;
                }

                .registration-yellow-button:focus {
                    background-color: #ff9900 !important;
                    border-color: #ff9900 !important;
                    color: #1f1f1f !important;
                    box-shadow: 0 0 0 0.2rem rgba(255, 153, 0, 0.3) !important;
                }

                .registration-yellow-button:disabled {
                    background-color: #ff9900 !important;
                    border-color: #ff9900 !important;
                    color: #1f1f1f !important;
                    opacity: 0.55;
                    cursor: not-allowed;
                }
            `}</style>
</Layout>

        
    );
};
