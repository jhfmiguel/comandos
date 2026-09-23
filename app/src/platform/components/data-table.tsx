import * as React from "react"

export interface DataTableColumn<T> {
    key: string
    header: React.ReactNode
    render: (row: T, index: number) => React.ReactNode
    className?: string
    headerClassName?: string
}

interface DataTableProps<T> {
    rows: T[]
    columns: DataTableColumn<T>[]
    rowKey: (row: T, index: number) => React.Key
    emptyText?: React.ReactNode
    caption?: React.ReactNode
    minWidth?: string
    className?: string
}

export function DataTable<T>({
    rows,
    columns,
    rowKey,
    emptyText = "No records found.",
    caption,
    minWidth = "48rem",
    className = ""
}: DataTableProps<T>) {
    return (
        <div className="comandos-native-table-container">
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
                                className={column.headerClassName}
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
