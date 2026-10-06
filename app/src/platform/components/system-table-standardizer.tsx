"use client"

import { TableStandardizer } from "@faria-miguel/ui/table-standardizer"

export function SystemTableStandardizer() {
    return (
        <TableStandardizer
            tableSelector=".comandos-main-content table"
            pageSize={10}
            disableAttribute="data-comandos-table-standardize"
            forceFilterAttribute="data-comandos-table-filter"
            noFilterAttribute="data-comandos-table-filter"
            managedPaginationSelector=".comandos-pagination"
            classes={{
                table: "comandos-native-table",
                container: "comandos-native-table-container",
                actionHeader: "comandos-standard-actions-header",
                actionCell: "comandos-standard-actions-cell",
                actionRow: "comandos-row-actions",
                filterRow: "comandos-filter-row",
                filterInput: "comandos-input",
                pagination: "comandos-pagination comandos-auto-pagination",
                paginationControls: "comandos-pagination-controls",
                paginationNav: "comandos-pagination-nav",
                paginationPage: "comandos-pagination-page"
            }}
            labels={{
                search: (column) => `Pesquisar ${column.toLowerCase()}...`,
                first: "Primeira página",
                previous: "Página anterior",
                next: "Próxima página",
                last: "Última página",
                pagination: "Paginação",
                page: (page, totalPages) => `Página ${page} de ${totalPages}`
            }}
        />
    )
}
