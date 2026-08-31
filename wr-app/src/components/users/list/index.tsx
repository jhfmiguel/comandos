"use client";

import React, {
    useCallback,
    useEffect,
    useRef,
    useState
} from "react";
import { useFormik } from "formik";

import { DataTable } from "@primereact/ui/datatable";
import type {
    DataTableEditingEvent,
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

import { EllipsisH } from "@primeicons/react/ellipsis-h";
import { AngleDoubleLeft } from "@primeicons/react/angle-double-left";
import { AngleDoubleRight } from "@primeicons/react/angle-double-right";
import { AngleLeft } from "@primeicons/react/angle-left";
import { AngleRight } from "@primeicons/react/angle-right";
import { Pencil } from "@primeicons/react/pencil";
import { Check } from "@primeicons/react/check";
import { Times } from "@primeicons/react/times";
import { Trash } from "@primeicons/react/trash";

import {
    Layout,
    Input,
    InputCPF,
    Button,
    Loader
} from "components";

import { User } from "api/models/users";
import { useUserService } from "api/services/user.service";

interface QueryUserForm {
    name: string;
    cpf: string;
}

interface DialogOpenChangeEvent {
    value?: boolean;
}

export const UsersList: React.FC = () => {
    const userService = useUserService();

    const [loading, setLoading] = useState<boolean>(false);
    const [users, setUsers] = useState<User[]>([]);
    const [totalRecords, setTotalRecords] = useState<number>(0);
    const [rows, setRows] = useState<number>(10);
    const [currentPage, setCurrentPage] = useState<number>(0);

    const [deleteUserId, setDeleteUserId] = useState<
        string | number | null
    >(null);

    const [editingKeys, setEditingKeys] = useState<
        Record<string, boolean>
    >({});

    const draftRef = useRef<
        Record<string, Partial<User>>
    >({});

    const fetchUsers = useCallback(
        async (
            name: string,
            cpf: string,
            pageIndex: number,
            pageSize: number
        ) => {
            setLoading(true);

            try {
                const data = await userService.findUser(
                    name,
                    cpf,
                    pageIndex,
                    pageSize
                );

                setUsers(data?.content || []);
                setTotalRecords(data?.totalElements || 0);
            } catch (error) {
                console.error("Failed to fetch users:", error);
            } finally {
                setLoading(false);
            }
        },
        [userService]
    );

    const userSubmit = async (
        filterValues: QueryUserForm
    ) => {
        setCurrentPage(0);

        await fetchUsers(
            filterValues.name,
            filterValues.cpf,
            0,
            rows
        );
    };

    useEffect(() => {
        fetchUsers("", "", 0, 10);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const {
        handleSubmit: formikSubmit,
        values: filter,
        handleChange: formikChange
    } = useFormik<QueryUserForm>({
        onSubmit: userSubmit,
        initialValues: {
            name: "",
            cpf: ""
        }
    });

    const handleSave = useCallback(
        async (event: DataTableRowEditEvent) => {
            const id = event.data.id as
                | string
                | number
                | undefined;

            if (id === undefined || id === null) {
                return;
            }

            const stringId = id.toString();
            const patch = draftRef.current[stringId];

            if (patch && Object.keys(patch).length > 0) {
                setLoading(true);

                try {
                    const updatedData = {
                        ...event.data,
                        ...patch
                    } as User;

                    await userService.updateUser(updatedData);

                    setUsers((previousUsers) => {
                        const updatedUsers = previousUsers.map(
                            (user) =>
                                user.id === id
                                    ? updatedData
                                    : user
                        );

                        return [...updatedUsers].sort(
                            (a, b) =>
                                Number(a.id ?? 0) -
                                Number(b.id ?? 0)
                        );
                    });
                } catch (error) {
                    console.error(
                        "Failed to update user:",
                        error
                    );
                } finally {
                    setLoading(false);
                }
            }

            delete draftRef.current[stringId];
        },
        [userService]
    );

    const handleCancel = useCallback(
        (event: DataTableRowEditEvent) => {
            const id = event.data.id as
                | string
                | number
                | undefined;

            if (id === undefined || id === null) {
                return;
            }

            delete draftRef.current[id.toString()];
        },
        []
    );

    const handleDelete = useCallback(
        (id: string | number) => {
            setDeleteUserId(id);
        },
        []
    );

    const handleDeleteDialogOpenChange = (
        event: DialogOpenChangeEvent
    ): void => {
        if (event.value === false) {
            setDeleteUserId(null);
        }
    };

    const confirmDelete = useCallback(async () => {
        if (deleteUserId === null) {
            return;
        }

        const targetId = deleteUserId;

        setLoading(true);

        try {
            await userService.deleteUser(
                targetId.toString()
            );

            setDeleteUserId(null);

            await fetchUsers(
                filter.name,
                filter.cpf,
                currentPage,
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
    }, [
        userService,
        deleteUserId,
        filter.name,
        filter.cpf,
        currentPage,
        rows,
        fetchUsers
    ]);

    return (
        <Layout title="Users">
            <div
                style={{
                    position: "relative",
                    minHeight: "100%"
                }}
            >
                {loading && (
                    <Loader show={loading} />
                )}

                <form onSubmit={formikSubmit}>
                    <div className="columns">
                        <Input
                            label="Name"
                            columnClasses="is-half"
                            id="name"
                            name="name"
                            onChange={formikChange}
                            value={filter.name}
                            autoComplete="off"
                        />

                        <InputCPF
                            label="CPF"
                            columnClasses="is-half"
                            id="cpf"
                            name="cpf"
                            onChange={formikChange}
                            value={filter.cpf}
                            autoComplete="off"
                        />
                    </div>

                    <div className="field is-grouped">
                        <div className="control is-link">
                            <Button
                                label="Find"
                                type="submit"
                                columnClasses=""
                            />
                        </div>
                    </div>
                </form>

                <div className="columns">
                    <div
                        className="column is-full"
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
                            totalRecords={totalRecords}
                            first={currentPage * rows}
                            style={{
                                width: "100%"
                            }}
                            loading={loading}
                            editMode="row"
                            editingKeys={editingKeys}
                            onEditingKeysChange={(
                                event: DataTableEditingEvent
                            ) => {
                                setEditingKeys(
                                    event.value
                                );
                            }}
                            onRowEditSave={handleSave}
                            onRowEditCancel={handleCancel}
                        >
                            <DataTable.Loading />

                            <div
                                className="p-3 font-bold"
                                style={{
                                    borderBottom:
                                        "1px solid #e5e7eb"
                                }}
                            >
                                Search Results
                            </div>

                            <DataTable.TableContainer
                                style={{
                                    width: "100%",
                                    overflowX: "auto"
                                }}
                            >
                                <DataTable.Table
                                    style={{
                                        width: "100%",
                                        minWidth: "100%",
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
                                    </DataTable.THead>

                                    <DataTable.TBody>
                                        {({
                                            item,
                                            index
                                        }: {
                                            item: User;
                                            index: number;
                                        }) => {
                                            const id =
                                                item.id?.toString() ?? "";
                                            const isEditing =
                                                Boolean(editingKeys[id]);

                                            /*
                                             * O PrimeReact espera
                                             * Record<string, unknown>
                                             * no rowData.
                                             */
                                            const rowData: Record<
                                                string,
                                                unknown
                                            > = {
                                                ...item
                                            };

                                            const formatDate = (
                                                dateString?: string
                                            ): string => {
                                                if (!dateString) {
                                                    return "-";
                                                }

                                                try {
                                                    if (
                                                        dateString.includes(
                                                            "-"
                                                        )
                                                    ) {
                                                        const datePart =
                                                            dateString.split(
                                                                "T"
                                                            )[0];

                                                        const [
                                                            year,
                                                            month,
                                                            day
                                                        ] =
                                                            datePart.split(
                                                                "-"
                                                            );

                                                        return `${day}/${month}/${year}`;
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

                                            const formatCPF = (
                                                rawCpf?:
                                                    | string
                                                    | number
                                            ): string => {
                                                if (
                                                    rawCpf ===
                                                        undefined ||
                                                    rawCpf === null ||
                                                    rawCpf === ""
                                                ) {
                                                    return "-";
                                                }

                                                const originalValue =
                                                    rawCpf.toString();

                                                const clean =
                                                    originalValue.replace(
                                                        /\D/g,
                                                        ""
                                                    );

                                                if (
                                                    clean.length === 11
                                                ) {
                                                    return clean.replace(
                                                        /(\d{3})(\d{3})(\d{3})(\d{2})/,
                                                        "$1.$2.$3-$4"
                                                    );
                                                }

                                                return originalValue;
                                            };

                                            return (
                                                <DataTable.Row
                                                    key={
                                                        item.id ??
                                                        ""
                                                    }
                                                >
                                                    <DataTable.RowEditor
                                                        rowKey={
                                                            item.id ??
                                                            ""
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
                                                                field="name"
                                                                rowIndex={
                                                                    index
                                                                }
                                                                rowData={
                                                                    rowData
                                                                }
                                                            >
                                                                <DataTable.CellEditorDisplay>
                                                                    <span className="font-medium">
                                                                        {
                                                                            item.name
                                                                        }
                                                                    </span>
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        defaultValue={
                                                                            item.name
                                                                        }
                                                                        style={{
                                                                            width:
                                                                                "100%"
                                                                        }}
                                                                        onChange={(
                                                                            event: React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                id
                                                                            ] =
                                                                                {
                                                                                    ...draftRef
                                                                                        .current[
                                                                                        id
                                                                                    ],
                                                                                    name:
                                                                                        event
                                                                                            .target
                                                                                            .value
                                                                                };
                                                                        }}
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
                                                                    )}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        defaultValue={
                                                                            item.cpf?.toString()
                                                                        }
                                                                        style={{
                                                                            width:
                                                                                "100%"
                                                                        }}
                                                                        onChange={(
                                                                            event: React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                id
                                                                            ] =
                                                                                {
                                                                                    ...draftRef
                                                                                        .current[
                                                                                        id
                                                                                    ],
                                                                                    cpf:
                                                                                        event.target.value.replace(
                                                                                            /\D/g,
                                                                                            ""
                                                                                        ) ||
                                                                                        undefined
                                                                                };
                                                                        }}
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
                                                                            item.birth
                                                                        }
                                                                        style={{
                                                                            width:
                                                                                "100%"
                                                                        }}
                                                                        onChange={(
                                                                            event: React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                id
                                                                            ] =
                                                                                {
                                                                                    ...draftRef
                                                                                        .current[
                                                                                        id
                                                                                    ],
                                                                                    birth:
                                                                                        event
                                                                                            .target
                                                                                            .value ||
                                                                                        undefined
                                                                                };
                                                                        }}
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
                                                                    {item.address ||
                                                                        "-"}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        defaultValue={
                                                                            item.address
                                                                        }
                                                                        style={{
                                                                            width:
                                                                                "100%"
                                                                        }}
                                                                        onChange={(
                                                                            event: React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                id
                                                                            ] =
                                                                                {
                                                                                    ...draftRef
                                                                                        .current[
                                                                                        id
                                                                                    ],
                                                                                    address:
                                                                                        event
                                                                                            .target
                                                                                            .value
                                                                                };
                                                                        }}
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
                                                                    {item.email ||
                                                                        "-"}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        defaultValue={
                                                                            item.email
                                                                        }
                                                                        style={{
                                                                            width:
                                                                                "100%"
                                                                        }}
                                                                        onChange={(
                                                                            event: React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                id
                                                                            ] =
                                                                                {
                                                                                    ...draftRef
                                                                                        .current[
                                                                                        id
                                                                                    ],
                                                                                    email:
                                                                                        event
                                                                                            .target
                                                                                            .value
                                                                                };
                                                                        }}
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
                                                                    {item.phone ||
                                                                        "-"}
                                                                </DataTable.CellEditorDisplay>

                                                                <DataTable.CellEditorContent>
                                                                    <InputText
                                                                        defaultValue={
                                                                            item.phone?.toString()
                                                                        }
                                                                        style={{
                                                                            width:
                                                                                "100%"
                                                                        }}
                                                                        onChange={(
                                                                            event: React.ChangeEvent<HTMLInputElement>
                                                                        ) => {
                                                                            draftRef.current[
                                                                                id
                                                                            ] =
                                                                                {
                                                                                    ...draftRef
                                                                                        .current[
                                                                                        id
                                                                                    ],
                                                                                    phone:
                                                                                        event.target.value.replace(
                                                                                            /\D/g,
                                                                                            ""
                                                                                        ) ||
                                                                                        undefined
                                                                                };
                                                                        }}
                                                                    />
                                                                </DataTable.CellEditorContent>
                                                            </DataTable.CellEditor>
                                                        </DataTable.Cell>

                                                        <DataTable.Cell>
                                                            <div className="flex gap-2 justify-center items-center">
                                                                <DataTable.RowEditorInit
                                                                    as={PrimeButton}
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

                                                                <DataTable.RowEditorSave
                                                                    as={PrimeButton}
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
                                                                    as={PrimeButton}
                                                                    variant="text"
                                                                    severity="danger"
                                                                    title="Cancel editing"
                                                                    aria-label="Cancel editing"
                                                                >
                                                                        <Times
                                                                            size={
                                                                                20
                                                                            }
                                                                        />
                                                                </DataTable.RowEditorCancel>

                                                                {!isEditing && (
                                                                    <PrimeButton
                                                                        type="button"
                                                                        variant="text"
                                                                        severity="danger"
                                                                        title="Delete user"
                                                                        aria-label="Delete user"
                                                                        onClick={(
                                                                            event: React.MouseEvent<HTMLButtonElement>
                                                                        ) => {
                                                                            event.stopPropagation();
                                                                            event.preventDefault();

                                                                            if (
                                                                                item.id ===
                                                                                    undefined ||
                                                                                item.id ===
                                                                                    null
                                                                            ) {
                                                                                return;
                                                                            }

                                                                            handleDelete(
                                                                                item.id
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

                                    <DataTable.EmptyTBody>
                                        <DataTable.Row>
                                            <DataTable.Cell
                                                colSpan={8}
                                            >
                                                <div
                                                    style={{
                                                        textAlign:
                                                            "center",
                                                        padding:
                                                            "2rem",
                                                        color:
                                                            "#9ca3af"
                                                    }}
                                                >
                                                    No records
                                                    found. Try
                                                    adjusting your
                                                    search filters.
                                                </div>
                                            </DataTable.Cell>
                                        </DataTable.Row>
                                    </DataTable.EmptyTBody>
                                </DataTable.Table>
                            </DataTable.TableContainer>

                            <DataTable.Pagination>
                                {({
                                    rows: currentRows
                                }: DataTablePaginationInstance) => {
                                    return (
                                        <Paginator.Root
                                            className="py-3 px-3.5 border-t border-surface-200 dark:border-surface-700"
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

                                                setRows(
                                                    nextRows
                                                );

                                                setCurrentPage(
                                                    nextPage
                                                );

                                                fetchUsers(
                                                    filter.name,
                                                    filter.cpf,
                                                    nextPage,
                                                    nextRows
                                                );
                                            }}
                                            style={{
                                                display:
                                                    "flex",
                                                alignItems:
                                                    "center",
                                                justifyContent:
                                                    "center",
                                                gap:
                                                    "0.5rem",
                                                padding:
                                                    "0.5rem"
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
                                    );
                                }}
                            </DataTable.Pagination>

                            <div
                                className="p-2 text-right"
                                style={{
                                    borderTop:
                                        "1px solid #e5e7eb",
                                    fontSize: "0.875rem"
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
                open={deleteUserId !== null}
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
                                            color:
                                                "#6b7280",
                                            fontSize:
                                                "0.875rem",
                                            margin: 0
                                        }}
                                    >
                                        This action cannot
                                        be undone. All of
                                        this user's data
                                        will be permanently
                                        removed.
                                    </p>

                                    <div
                                        style={{
                                            display: "flex",
                                            justifyContent:
                                                "flex-end",
                                            gap: "0.5rem",
                                            marginTop:
                                                "0.5rem"
                                        }}
                                    >
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setDeleteUserId(
                                                    null
                                                );
                                            }}
                                            disabled={
                                                loading
                                            }
                                            style={{
                                                padding:
                                                    "0.5rem 1rem",
                                                background:
                                                    "#f3f4f6",
                                                color:
                                                    "#111827",
                                                border:
                                                    "none",
                                                borderRadius:
                                                    "4px",
                                                cursor:
                                                    loading
                                                        ? "not-allowed"
                                                        : "pointer",
                                                fontSize:
                                                    "0.875rem",
                                                opacity:
                                                    loading
                                                        ? 0.7
                                                        : 1
                                            }}
                                        >
                                            Cancel
                                        </button>

                                        <button
                                            type="button"
                                            onClick={
                                                confirmDelete
                                            }
                                            disabled={
                                                loading
                                            }
                                            style={{
                                                padding:
                                                    "0.5rem 1rem",
                                                background:
                                                    "#dc2626",
                                                color:
                                                    "#ffffff",
                                                border:
                                                    "none",
                                                borderRadius:
                                                    "4px",
                                                cursor:
                                                    loading
                                                        ? "not-allowed"
                                                        : "pointer",
                                                fontSize:
                                                    "0.875rem",
                                                opacity:
                                                    loading
                                                        ? 0.7
                                                        : 1
                                            }}
                                        >
                                            {loading
                                                ? "Deleting..."
                                                : "Delete"}
                                        </button>
                                    </div>
                                </div>
                            </Dialog.Content>
                        </Dialog.Popup>
                    </Dialog.Positioner>
                </Dialog.Portal>
            </Dialog.Root>
        </Layout>
    );
};
