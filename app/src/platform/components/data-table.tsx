import * as React from "react"

export interface DataTableColumn<T> {
    key: string
    header: React.ReactNode
    render: (row: T, index: number) => React.ReactNode
    className?: string
    headerClassName?: string
    align?: "left" | "center" | "right"
}

interface DataTableProps<T> {
    rows: T[]
    columns: DataTableColumn<T>[]
    rowKey: (row: T, index: number) => React.Key
    emptyText?: React.ReactNode
    caption?: React.ReactNode
    ariaLabel?: string
    minWidth?: string
    className?: string
}

export function DataTable<T>({
    rows,
    columns,
    rowKey,
    emptyText = "No records found.",
    caption,
    ariaLabel,
    minWidth = "48rem",
    className = ""
}: DataTableProps<T>) {
    return (
        <div
            className="comandos-native-table-container"
            role="region"
            aria-label={ariaLabel}
            tabIndex={0}
        >
            <table
                className={`comandos-native-table ${className}`.trim()}
                style={{ minWidth }}
            >
                {caption && <caption>{caption}</caption>}

                <thead>
                    <tr>
                        {columns.map((column) => (
                            <th
                                key={column.key}
                                scope="col"
                                className={column.headerClassName}
                                style={{ textAlign: column.align }}
                            >
                                {column.header}
                            </th>
                        ))}
                    </tr>
                </thead>

                <tbody>
                    {rows.map((row, index) => (
                        <tr key={rowKey(row, index)}>
                            {columns.map((column) => (
                                <td
                                    key={column.key}
                                    className={column.className}
                                    style={{ textAlign: column.align }}
                                >
                                    {column.render(row, index)}
                                </td>
                            ))}
                        </tr>
                    ))}

                    {!rows.length && (
                        <tr>
                            <td
                                colSpan={Math.max(columns.length, 1)}
                                className="text-center"
                                role="status"
                            >
                                {emptyText}
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
        </div>
    )
}
