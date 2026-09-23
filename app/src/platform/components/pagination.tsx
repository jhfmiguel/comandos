"use client"

import { ChevronsLeft, ChevronLeft, ChevronRight, ChevronsRight } from "lucide-react"

interface PaginationProps {
    page: number
    totalElements: number
    pageSize: number
    onPageChange: (page: number) => void
    pageSizeOptions?: number[]
    onPageSizeChange?: (pageSize: number) => void
    disabled?: boolean
    labels?: {
        page?: string
        of?: string
        totalRecords?: string
        first?: string
        previous?: string
        next?: string
        last?: string
    }
}

export function Pagination({
    page,
    totalElements,
    pageSize,
    onPageChange,
    pageSizeOptions = [10, 20, 50, 100],
    onPageSizeChange,
    disabled = false,
    labels
}: PaginationProps) {
    const totalPages = Math.max(Math.ceil(totalElements / Math.max(pageSize, 1)), 1)
    const currentPage = Math.min(Math.max(page, 0), totalPages - 1)

    const text = {
        page: labels?.page ?? "Page",
        of: labels?.of ?? "of",
        totalRecords: labels?.totalRecords ?? "Total records",
        first: labels?.first ?? "First page",
        previous: labels?.previous ?? "Previous page",
        next: labels?.next ?? "Next page",
        last: labels?.last ?? "Last page"
    }

    return (
        <div className="comandos-pagination">
            <div className="comandos-pagination-controls">
                <button
                    type="button"
                    className="comandos-icon-button"
                    aria-label={text.first}
                    disabled={disabled || currentPage === 0}
                    onClick={() => onPageChange(0)}
                >
                    <ChevronsLeft size={18} />
                </button>

                <button
                    type="button"
                    className="comandos-icon-button"
                    aria-label={text.previous}
                    disabled={disabled || currentPage === 0}
                    onClick={() => onPageChange(Math.max(0, currentPage - 1))}
                >
                    <ChevronLeft size={18} />
                </button>

                <span>
                    {text.page} {currentPage + 1} {text.of} {totalPages}
                </span>

                <button
                    type="button"
                    className="comandos-icon-button"
                    aria-label={text.next}
                    disabled={disabled || currentPage + 1 >= totalPages}
                    onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
                >
                    <ChevronRight size={18} />
                </button>

                <button
                    type="button"
                    className="comandos-icon-button"
                    aria-label={text.last}
                    disabled={disabled || currentPage + 1 >= totalPages}
                    onClick={() => onPageChange(totalPages - 1)}
                >
                    <ChevronsRight size={18} />
                </button>

                {onPageSizeChange && (
                    <select
                        className="comandos-input comandos-page-size"
                        aria-label="Page size"
                        value={pageSize}
                        disabled={disabled}
                        onChange={(event) => onPageSizeChange(Number(event.target.value))}
                    >
                        {pageSizeOptions.map((size) => (
                            <option key={size} value={size}>
                                {size}
                            </option>
                        ))}
                    </select>
                )}
            </div>

            <span>
                {text.totalRecords}: {totalElements}
            </span>
        </div>
    )
}
