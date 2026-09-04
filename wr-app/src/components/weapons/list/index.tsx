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
} from "@primereact/ui/datatable";

import type {
    DataTableEditingEvent,
    DataTableFilterInstance,
    DataTableFilterMeta,
    DataTablePaginationInstance,
    DataTableRowEditEvent
} from "@primereact/ui/datatable";

import { Paginator } from "@primereact/ui/paginator";

import type {
    PaginatorPagesInstance,
    PaginatorRootChangeEvent
} from "@primereact/ui/paginator";

import { InputText } from "@primereact/ui/inputtext";
import { Dialog } from "@primereact/ui/dialog";
import { Button as PrimeButton } from "@primereact/ui/button";

import { AngleDoubleLeft } from "@primeicons/react/angle-double-left";
import { AngleDoubleRight } from "@primeicons/react/angle-double-right";
import { AngleLeft } from "@primeicons/react/angle-left";
import { AngleRight } from "@primeicons/react/angle-right";
import { Check } from "@primeicons/react/check";
import { EllipsisH } from "@primeicons/react/ellipsis-h";
import { Pencil } from "@primeicons/react/pencil";
import { Plus } from "@primeicons/react/plus";
import { Times } from "@primeicons/react/times";
import { Trash } from "@primeicons/react/trash";

import { Layout, Loader } from "components";

import { Weapon } from "api/models/weapons";

import {
    WeaponSearchFilters,
    useWeaponService
} from "api/services/weapon.service";

interface DialogOpenChangeEvent {
    value?: boolean;
}

interface DataTableFilterEvent {
    filters: DataTableFilterMeta;
}

const INITIAL_FILTERS: DataTableFilterMeta = {
    sku: {
        value: null,
        matchMode: FilterMatchMode.Contains
    },
    name: {
        value: null,
        matchMode: FilterMatchMode.Contains
    },
    price: {
        value: null,
        matchMode: FilterMatchMode.Contains
    },
    description: {
        value: null,
        matchMode: FilterMatchMode.Contains
    }
};

const EMPTY_SEARCH_FILTERS: WeaponSearchFilters = {
    sku: "",
    name: "",
    price: "",
    description: ""
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


const MAX_PRICE_DIGITS = 15;

const formatCurrencyInput = (
    value: string | number | null | undefined
): string => {
    if (
        value === null ||
        value === undefined ||
        value === ""
    ) {
        return "";
    }

    const digits = String(value)
        .replace(/\D/g, "")
        .slice(0, MAX_PRICE_DIGITS);

    if (!digits || /^0+$/.test(digits)) {
        return "";
    }

    const amount = Number(digits) / 100;

    return new Intl.NumberFormat(
        "pt-BR",
        {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }
    ).format(amount);
};

const parseCurrencyToDecimal = (
    value: string | number | null | undefined
): string => {
    if (
        value === null ||
        value === undefined ||
        value === ""
    ) {
        return "";
    }

    const digits = String(value).replace(/\D/g, "");

    if (!digits) {
        return "";
    }

    return (Number(digits) / 100).toFixed(2);
};

const createSearchFilters = (
    filters: DataTableFilterMeta
): WeaponSearchFilters => {
    return {
        sku: getFilterValue(
            filters,
            "sku"
        ).trim(),

        name: getFilterValue(
            filters,
            "name"
        ).trim(),

        price: parseCurrencyToDecimal(
            getFilterValue(
                filters,
                "price"
            )
        ),

        description: getFilterValue(
            filters,
            "description"
        ).trim()
    };
};

const formatPrice = (
    weapon: Weapon
): string => {
    if (
        weapon.price !== undefined &&
        weapon.price !== null
    ) {
        return new Intl.NumberFormat(
            "pt-BR",
            {
                style: "currency",
                currency: "BRL"
            }
        ).format(Number(weapon.price));
    }

    if (weapon.priceFormatted) {
        return weapon.priceFormatted;
    }

    return "R$ 0,00";
};

export const WeaponsList: React.FC = () => {
    const router = useRouter();

    const weaponService = useWeaponService();

    const [loading, setLoading] =
        useState<boolean>(false);

    const [weapons, setWeapons] =
        useState<Weapon[]>([]);

    const [totalRecords, setTotalRecords] =
        useState<number>(0);

    const [rows, setRows] =
        useState<number>(10);

    const [currentPage, setCurrentPage] =
        useState<number>(0);

    const [filters, setFilters] =
        useState<DataTableFilterMeta>(
            INITIAL_FILTERS
        );

    const [
        deleteWeaponId,
        setDeleteWeaponId
    ] = useState<string | number | null>(
        null
    );

    const [editingKeys, setEditingKeys] =
        useState<Record<string, boolean>>(
            {}
        );

    const draftRef = useRef<
        Record<string, Partial<Weapon>>
    >({});

    const filterTimeoutRef = useRef<
        ReturnType<typeof setTimeout> | null
    >(null);

    const fetchWeapons = useCallback(
        async (
            searchFilters: WeaponSearchFilters,
            pageIndex: number,
            pageSize: number
        ): Promise<void> => {
            setLoading(true);

            try {
                const data =
                    await weaponService.findWeapon(
                        searchFilters,
                        pageIndex,
                        pageSize
                    );

                const content =
                    data?.content ?? [];

                setWeapons(
                    [...content].sort(
                        (a, b) =>
                            Number(a.id ?? 0) -
                            Number(b.id ?? 0)
                    )
                );

                setTotalRecords(
                    data?.totalElements ?? 0
                );
            } catch (error) {
                console.error(
                    "Failed to fetch weapons:",
                    error
                );
            } finally {
                setLoading(false);
            }
        },
        [weaponService]
    );

    useEffect(() => {
        fetchWeapons(
            EMPTY_SEARCH_FILTERS,
            0,
            10
        );

        return () => {
            if (
                filterTimeoutRef.current
            ) {
                clearTimeout(
                    filterTimeoutRef.current
                );
            }
        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleFilterChange =
        useCallback(
            (
                nextFilters: DataTableFilterMeta
            ): void => {
                setFilters(nextFilters);
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
                        fetchWeapons(
                            createSearchFilters(
                                nextFilters
                            ),
                            0,
                            rows
                        );
                    }, 400);
            },
            [
                fetchWeapons,
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
            event: DataTableRowEditEvent
        ): Promise<void> => {
            const weaponId =
                event.data.id as
                    | string
                    | number
                    | undefined;

            if (
                weaponId === undefined ||
                weaponId === null
            ) {
                return;
            }

            const rowId =
                weaponId.toString();

            const patch =
                draftRef.current[rowId];

            if (
                patch === undefined ||
                Object.keys(patch).length === 0
            ) {
                delete draftRef.current[rowId];

                return;
            }

            const updatedWeapon: Weapon = {
                ...(event.data as Weapon),
                ...patch,
                id: weaponId.toString()
            };

            setLoading(true);

            try {
                await weaponService.updateWeapon(
                    updatedWeapon
                );

                setEditingKeys((current) => {
                    const next = {
                        ...current
                    };

                    delete next[rowId];

                    return next;
                });

                await fetchWeapons(
                    createSearchFilters(filters),
                    currentPage,
                    rows
                );
            } catch (error) {
                console.error(
                    "Failed to update weapon:",
                    error
                );
            } finally {
                delete draftRef.current[rowId];

                setLoading(false);
            }
        },
        [
            weaponService,
            fetchWeapons,
            filters,
            currentPage,
            rows
        ]
    );

    const handleCancel = useCallback(
        (
            event: DataTableRowEditEvent
        ): void => {
            const weaponId =
                event.data.id as
                    | string
                    | number
                    | undefined;

            if (
                weaponId === undefined ||
                weaponId === null
            ) {
                return;
            }

            const rowId =
                weaponId.toString();

            delete draftRef.current[
                rowId
            ];

            setEditingKeys(
                (current) => {
                    const next = {
                        ...current
                    };

                    delete next[rowId];

                    return next;
                }
            );
        },
        []
    );

    const handleDelete = useCallback(
        (
            id: string | number
        ): void => {
            setDeleteWeaponId(id);
        },
        []
    );

    const handleDeleteDialogOpenChange =
        (
            event: DialogOpenChangeEvent
        ): void => {
            if (
                event.value === false
            ) {
                setDeleteWeaponId(
                    null
                );
            }
        };

    const confirmDelete = useCallback(
        async (): Promise<void> => {
            if (
                deleteWeaponId === null
            ) {
                return;
            }

            setLoading(true);

            try {
                await weaponService.deleteWeapon(
                    deleteWeaponId
                );

                setDeleteWeaponId(
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

                await fetchWeapons(
                    createSearchFilters(
                        filters
                    ),
                    nextPage,
                    rows
                );
            } catch (error) {
                console.error(
                    "Failed to delete weapon:",
                    error
                );
            } finally {
                setLoading(false);
            }
        },
        [
            weaponService,
            deleteWeaponId,
            totalRecords,
            currentPage,
            fetchWeapons,
            filters,
            rows
        ]
    );

    return (
        <Layout title="Weapons">
            <div
                style={{
                    position:
                        "relative",
                    minHeight:
                        "100%"
                }}
            >
                {loading && (
                    <Loader
                        show={loading}
                    />
                )}

                <div
                    style={{
                        display:
                            "flex",
                        justifyContent:
                            "flex-end",
                        marginBottom:
                            "1rem"
                    }}
                >
                    <PrimeButton
                        type="button"
                        className="registration-yellow-button"
                        onClick={() => {
                            router.push(
                                "/registrations/weapons"
                            );
                        }}
                    >
                        <Plus
                            size={18}
                        />

                        <span
                            style={{
                                marginLeft:
                                    "0.5rem"
                            }}
                        >
                            New Weapon
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
                            data={weapons}
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
                                event: DataTableEditingEvent
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
                                    width:
                                        "100%",
                                    overflowX:
                                        "auto"
                                }}
                            >
                                <DataTable.Table
                                    style={{
                                        width:
                                            "100%",
                                        minWidth:
                                            "900px",
                                        tableLayout:
                                            "auto"
                                    }}
                                >
                                    <DataTable.THead>
                                        <DataTable.THeadRow>
                                            <DataTable.THeadCell>
                                                ID
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                SKU
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                Name
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                Price
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                Description
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell
                                                style={{
                                                    width:
                                                        "10rem",
                                                    textAlign:
                                                        "center"
                                                }}
                                            >
                                                Actions
                                            </DataTable.THeadCell>
                                        </DataTable.THeadRow>

                                        <DataTable.THeadRow>
                                            <DataTable.THeadCell />

                                            <DataTable.THeadCell>
                                                <DataTable.Filter
                                                    field="sku"
                                                    display="row"
                                                    dataType="text"
                                                >
                                                    {({
                                                        value,
                                                        onChange
                                                    }: DataTableFilterInstance) => (
                                                        <InputText
                                                            value={(value as string) ?? ""}
                                                            onChange={(
                                                                event: React.ChangeEvent<HTMLInputElement>
                                                            ) => {
                                                                onChange(
                                                                    event,
                                                                    event.target.value
                                                                );
                                                            }}
                                                            placeholder="Search SKU..."
                                                            size="small"
                                                            fluid
                                                        />
                                                    )}
                                                </DataTable.Filter>
                                            </DataTable.THeadCell>

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
                                                            value={(value as string) ?? ""}
                                                            onChange={(
                                                                event: React.ChangeEvent<HTMLInputElement>
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
                                                    field="price"
                                                    display="row"
                                                    dataType="text"
                                                >
                                                    {({
                                                        value,
                                                        onChange
                                                    }: DataTableFilterInstance) => (
                                                        <InputText
                                                            value={formatCurrencyInput(
                                                                (value as string) ?? ""
                                                            )}
                                                            inputMode="numeric"
                                                            onChange={(
                                                                event: React.ChangeEvent<HTMLInputElement>
                                                            ) => {
                                                                const digits = event.target.value
                                                                    .replace(/\D/g, "")
                                                                    .slice(0, MAX_PRICE_DIGITS);

                                                                const maskedValue =
                                                                    formatCurrencyInput(digits);

                                                                onChange(
                                                                    event,
                                                                    maskedValue
                                                                );
                                                            }}
                                                            placeholder="Search price..."
                                                            size="small"
                                                            fluid
                                                        />
                                                    )}
                                                </DataTable.Filter>
                                            </DataTable.THeadCell>

                                            <DataTable.THeadCell>
                                                <DataTable.Filter
                                                    field="description"
                                                    display="row"
                                                    dataType="text"
                                                >
                                                    {({
                                                        value,
                                                        onChange
                                                    }: DataTableFilterInstance) => (
                                                        <InputText
                                                            value={(value as string) ?? ""}
                                                            onChange={(
                                                                event: React.ChangeEvent<HTMLInputElement>
                                                            ) => {
                                                                onChange(
                                                                    event,
                                                                    event.target.value
                                                                );
                                                            }}
                                                            placeholder="Search description..."
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
                                            item: Weapon;
                                            index: number;
                                        }) => {
                                            const weaponId =
                                                item.id;

                                            const rowId =
                                                weaponId?.toString() ??
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
                                                        weaponId ??
                                                        index
                                                    }
                                                >
                                                    <DataTable.RowEditor
                                                        rowKey={
                                                            weaponId ??
                                                            index
                                                        }
                                                        rowData={
                                                            rowData
                                                        }
                                                    >
                                                        <DataTable.Cell>
                                                            {
                                                                item.id
                                                            }
                                                        </DataTable.Cell>

                                                        <DataTable.Cell>
                                                            <DataTable.CellEditor
                                                                field="sku"
                                                                rowIndex={
                                                                    index
                                                                }
                                                                rowData={
                                                                    rowData
                                                                }
                                                            >
                                                                <DataTable.CellEditorDisplay>
                                                                    {item.sku ??
                                                                        "-"}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        defaultValue={
                                                                            item.sku ??
                                                                            ""
                                                                        }
                                                                        onChange={(
                                                                            event: React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                rowId
                                                                            ] = {
                                                                                ...draftRef.current[
                                                                                    rowId
                                                                                ],
                                                                                sku:
                                                                                    event
                                                                                        .target
                                                                                        .value
                                                                            };
                                                                        }}
                                                                        fluid
                                                                    />
                                                                </DataTable.CellEditorContent>
                                                            </DataTable.CellEditor>
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
                                                                            event: React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                rowId
                                                                            ] = {
                                                                                ...draftRef.current[
                                                                                    rowId
                                                                                ],
                                                                                name:
                                                                                    event
                                                                                        .target
                                                                                        .value
                                                                            };
                                                                        }}
                                                                        fluid
                                                                    />
                                                                </DataTable.CellEditorContent>
                                                            </DataTable.CellEditor>
                                                        </DataTable.Cell>

                                                        <DataTable.Cell>
                                                            <DataTable.CellEditor
                                                                field="price"
                                                                rowIndex={
                                                                    index
                                                                }
                                                                rowData={
                                                                    rowData
                                                                }
                                                            >
                                                                <DataTable.CellEditorDisplay>
                                                                    {formatPrice(
                                                                        item
                                                                    )}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        defaultValue={formatCurrencyInput(
                                                                            item.price !== undefined &&
                                                                                item.price !== null
                                                                                ? Math.round(
                                                                                      Number(item.price) *
                                                                                          100
                                                                                  ).toString()
                                                                                : ""
                                                                        )}
                                                                        inputMode="numeric"
                                                                        onChange={(
                                                                            event: React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            const maskedValue =
                                                                                formatCurrencyInput(
                                                                                    event.target.value
                                                                                );

                                                                            event.target.value =
                                                                                maskedValue;

                                                                            const parsedValue =
                                                                                parseCurrencyToDecimal(
                                                                                    maskedValue
                                                                                );

                                                                            draftRef.current[
                                                                                rowId
                                                                            ] = {
                                                                                ...draftRef.current[
                                                                                    rowId
                                                                                ],
                                                                                price: parsedValue
                                                                                    ? Number(
                                                                                          parsedValue
                                                                                      )
                                                                                    : 0
                                                                            };
                                                                        }}
                                                                        fluid
                                                                    />
                                                                </DataTable.CellEditorContent>
                                                            </DataTable.CellEditor>
                                                        </DataTable.Cell>

                                                        <DataTable.Cell>
                                                            <DataTable.CellEditor
                                                                field="description"
                                                                rowIndex={
                                                                    index
                                                                }
                                                                rowData={
                                                                    rowData
                                                                }
                                                            >
                                                                <DataTable.CellEditorDisplay>
                                                                    {item.description ??
                                                                        "-"}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        defaultValue={
                                                                            item.description ??
                                                                            ""
                                                                        }
                                                                        onChange={(
                                                                            event: React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                rowId
                                                                            ] = {
                                                                                ...draftRef.current[
                                                                                    rowId
                                                                                ],
                                                                                description:
                                                                                    event
                                                                                        .target
                                                                                        .value
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
                                                                        title="Edit weapon"
                                                                        aria-label="Edit weapon"
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
                                                                        title="Delete weapon"
                                                                        aria-label="Delete weapon"
                                                                        onClick={(
                                                                            event: React.MouseEvent<HTMLButtonElement>
                                                                        ) => {
                                                                            event.preventDefault();
                                                                            event.stopPropagation();

                                                                            if (
                                                                                weaponId ===
                                                                                    undefined ||
                                                                                weaponId ===
                                                                                    null
                                                                            ) {
                                                                                return;
                                                                            }

                                                                            handleDelete(
                                                                                weaponId
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
                                    rows: currentRows
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
                                            event: PaginatorRootChangeEvent
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

                                            fetchWeapons(
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
                                    padding:
                                        "0.5rem",
                                    textAlign:
                                        "right",
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
                    deleteWeaponId !==
                    null
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
                                width:
                                    "26rem"
                            }}
                        >
                            <Dialog.Header>
                                <Dialog.Title>
                                    Delete Weapon
                                </Dialog.Title>
                            </Dialog.Header>

                            <Dialog.Content>
                                <div
                                    style={{
                                        display:
                                            "flex",
                                        flexDirection:
                                            "column",
                                        gap:
                                            "1rem"
                                    }}
                                >
                                    <p
                                        style={{
                                            margin: 0
                                        }}
                                    >
                                        Are you sure you want
                                        to delete this weapon?
                                    </p>

                                    <div
                                        style={{
                                            display:
                                                "flex",
                                            justifyContent:
                                                "flex-end",
                                            gap:
                                                "0.5rem"
                                        }}
                                    >
                                        <PrimeButton
                                            type="button"
                                            severity="secondary"
                                            disabled={
                                                loading
                                            }
                                            onClick={() => {
                                                setDeleteWeaponId(
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
