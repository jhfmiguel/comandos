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

type PageToken = number | "ellipsis-left" | "ellipsis-right"

function pageTokens(currentPage: number, totalPages: number): PageToken[] {
    if (totalPages <= 7) {
        return Array.from({ length: totalPages }, (_, index) => index)
    }

    if (currentPage <= 3) {
        return [0, 1, 2, 3, 4, "ellipsis-right", totalPages - 1]
    }

    if (currentPage >= totalPages - 4) {
        return [
            0,
            "ellipsis-left",
            totalPages - 5,
            totalPages - 4,
            totalPages - 3,
            totalPages - 2,
            totalPages - 1
        ]
    }

    return [
        0,
        "ellipsis-left",
        currentPage - 1,
        currentPage,
        currentPage + 1,
        "ellipsis-right",
        totalPages - 1
    ]
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
    const tokens = pageTokens(currentPage, totalPages)

    const text = {
        totalRecords: labels?.totalRecords ?? "Total records",
        first: labels?.first ?? "First page",
        previous: labels?.previous ?? "Previous page",
        next: labels?.next ?? "Next page",
        last: labels?.last ?? "Last page"
    }

    return (
        <div className="comandos-pagination">
            <nav className="comandos-pagination-controls" aria-label="Pagination">
                <button
                    type="button"
                    className="comandos-pagination-nav"
                    aria-label={text.first}
                    disabled={disabled || currentPage === 0}
                    onClick={() => onPageChange(0)}
                >
                    <ChevronsLeft size={17} />
                </button>

                <button
                    type="button"
                    className="comandos-pagination-nav"
                    aria-label={text.previous}
                    disabled={disabled || currentPage === 0}
                    onClick={() => onPageChange(Math.max(0, currentPage - 1))}
                >
                    <ChevronLeft size={17} />
                </button>

                {tokens.map((token) =>
                    typeof token === "number" ? (
                        <button
                            key={token}
                            type="button"
                            className={
                                token === currentPage
                                    ? "comandos-pagination-page is-active"
                                    : "comandos-pagination-page"
                            }
                            aria-current={token === currentPage ? "page" : undefined}
                            disabled={disabled}
                            onClick={() => onPageChange(token)}
                        >
                            {token + 1}
                        </button>
                    ) : (
                        <span
                            key={token}
                            className="comandos-pagination-ellipsis"
                            aria-hidden="true"
                        >
                            …
                        </span>
                    )
                )}

                <button
                    type="button"
                    className="comandos-pagination-nav"
                    aria-label={text.next}
                    disabled={disabled || currentPage + 1 >= totalPages}
                    onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
                >
                    <ChevronRight size={17} />
                </button>

                <button
                    type="button"
                    className="comandos-pagination-nav"
                    aria-label={text.last}
                    disabled={disabled || currentPage + 1 >= totalPages}
                    onClick={() => onPageChange(totalPages - 1)}
                >
                    <ChevronsRight size={17} />
                </button>
            </nav>

            {onPageSizeChange && (
                <div className="comandos-pagination-meta">
                    <select
                        className="comandos-input comandos-page-size"
                        aria-label="Page size"
                        value={pageSize}
                        disabled={disabled}
                        onChange={(event) => onPageSizeChange(Number(event.target.value))}
                    >
                        {pageSizeOptions.map((size) => (
                            <option key={size} value={size}>
                                {size} rows
                            </option>
                        ))}
                    </select>
                    <span>{totalElements} {text.totalRecords.toLowerCase()}</span>
                </div>
            )}
        </div>
    )
}
