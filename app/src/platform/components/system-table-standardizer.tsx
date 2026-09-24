"use client";

import * as React from "react";

type TableState = {
    page: number;
    pageSize: number;
    pagination?: HTMLDivElement;
};

const states = new WeakMap<HTMLTableElement, TableState>();

const normalize = (value: string): string =>
    value
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim()
        .toLowerCase();

const ACTION_HEADERS = new Set([
    "acao",
    "acoes",
    "action",
    "actions",
    "detalhe",
    "detalhes",
    "detail",
    "details"
]);

const ID_HEADERS = new Set(["id", "#"]);

function tableRows(table: HTMLTableElement): HTMLTableRowElement[] {
    const body = table.tBodies.item(0);
    return body ? Array.from(body.rows) : [];
}

function cellValue(cell: HTMLTableCellElement | undefined): string {
    if (!cell) return "";
    const controls = Array.from(cell.querySelectorAll<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>(
        "input, select, textarea"
    ));
    const controlText = controls.map(control => control.value).join(" ");
    return normalize(`${cell.textContent ?? ""} ${controlText}`);
}

function actionColumns(table: HTMLTableElement): number[] {
    const header = table.tHead?.rows.item(0);
    if (!header) return [];
    return Array.from(header.cells)
        .map((cell, index) => ACTION_HEADERS.has(normalize(cell.textContent ?? "")) ? index : -1)
        .filter(index => index >= 0);
}

function markActionCells(table: HTMLTableElement) {
    const columns = actionColumns(table);
    if (!columns.length) return;

    const header = table.tHead?.rows.item(0);
    columns.forEach(index => header?.cells.item(index)?.classList.add("comandos-standard-actions-header"));

    tableRows(table).forEach(row => {
        columns.forEach(index => {
            const cell = row.cells.item(index);
            if (!cell) return;
            cell.classList.add("comandos-standard-actions-cell");

            const directContainer = Array.from(cell.children).find(
                child => child instanceof HTMLElement && child.tagName !== "BUTTON"
            ) as HTMLElement | undefined;

            if (directContainer) {
                directContainer.classList.add("comandos-row-actions");
            }
        });
    });
}

function findPaginationScope(table: HTMLTableElement): Element | null {
    return table.closest("section")
        ?? table.closest("form")
        ?? table.parentElement?.parentElement
        ?? table.parentElement;
}

function hasManagedPagination(table: HTMLTableElement): boolean {
    const scope = findPaginationScope(table);
    if (!scope) return false;
    return Array.from(scope.querySelectorAll<HTMLElement>(".comandos-pagination"))
        .some(element => element.dataset.comandosAutoPagination !== "true");
}

function createFilterRow(table: HTMLTableElement, apply: () => void) {
    const head = table.tHead;
    const header = head?.rows.item(0);
    if (!head || !header) return;
    if (head.querySelector(".comandos-filter-row")) return;

    const row = document.createElement("tr");
    row.className = "comandos-filter-row";
    row.dataset.comandosAutoFilter = "true";

    Array.from(header.cells).forEach((cell, index) => {
        const filterCell = document.createElement("th");
        const label = (cell.textContent ?? "").trim();
        const normalized = normalize(label);

        if (!label || ACTION_HEADERS.has(normalized) || ID_HEADERS.has(normalized)) {
            row.appendChild(filterCell);
            return;
        }

        const input = document.createElement("input");
        input.type = "search";
        input.className = "comandos-input";
        input.placeholder = `Pesquisar ${label.toLowerCase()}...`;
        input.setAttribute("aria-label", `Pesquisar ${label}`);
        input.dataset.comandosColumnFilter = String(index);
        input.addEventListener("input", apply);
        filterCell.appendChild(input);
        row.appendChild(filterCell);
    });

    head.appendChild(row);
}

function createPagination(table: HTMLTableElement, state: TableState, apply: () => void) {
    if (state.pagination?.isConnected || hasManagedPagination(table)) return;

    const container = table.closest(".comandos-native-table-container") ?? table.parentElement;
    if (!container?.parentElement) return;

    const pagination = document.createElement("div");
    pagination.className = "comandos-pagination comandos-auto-pagination";
    pagination.dataset.comandosAutoPagination = "true";

    const controls = document.createElement("nav");
    controls.className = "comandos-pagination-controls";
    controls.setAttribute("aria-label", "Paginação");

    const first = document.createElement("button");
    const previous = document.createElement("button");
    const current = document.createElement("button");
    const next = document.createElement("button");
    const last = document.createElement("button");

    [
        [first, "«", "Primeira página"],
        [previous, "‹", "Página anterior"],
        [next, "›", "Próxima página"],
        [last, "»", "Última página"]
    ].forEach(([button, text, label]) => {
        const value = button as HTMLButtonElement;
        value.type = "button";
        value.className = "comandos-pagination-nav";
        value.textContent = String(text);
        value.setAttribute("aria-label", String(label));
    });

    current.type = "button";
    current.className = "comandos-pagination-page is-active";
    current.setAttribute("aria-current", "page");
    current.disabled = true;

    first.addEventListener("click", () => {
        state.page = 0;
        apply();
    });
    previous.addEventListener("click", () => {
        state.page = Math.max(0, state.page - 1);
        apply();
    });
    next.addEventListener("click", () => {
        state.page += 1;
        apply();
    });
    last.addEventListener("click", () => {
        state.page = Number.MAX_SAFE_INTEGER;
        apply();
    });

    controls.append(first, previous, current, next, last);
    pagination.appendChild(controls);
    container.insertAdjacentElement("afterend", pagination);
    state.pagination = pagination;
}

function applyTable(table: HTMLTableElement) {
    const state = states.get(table);
    if (!state) return;

    markActionCells(table);

    const bodyRows = tableRows(table);
    const placeholderRows = bodyRows.filter(row =>
        row.cells.length === 1 && row.cells.item(0)?.hasAttribute("colspan")
    );
    const dataRows = bodyRows.filter(row => !placeholderRows.includes(row));

    const autoFilters = Array.from(
        table.querySelectorAll<HTMLInputElement>('tr[data-comandos-auto-filter="true"] input[data-comandos-column-filter]')
    );

    const filtered = dataRows.filter(row =>
        autoFilters.every(input => {
            const query = normalize(input.value);
            if (!query) return true;
            const index = Number(input.dataset.comandosColumnFilter);
            return cellValue(row.cells.item(index) ?? undefined).includes(query);
        })
    );

    if (!state.pagination?.isConnected) {
        dataRows.forEach(row => { row.hidden = !filtered.includes(row); });
        placeholderRows.forEach(row => { row.hidden = dataRows.length > 0; });
        return;
    }

    const totalPages = Math.max(Math.ceil(filtered.length / state.pageSize), 1);
    state.page = Math.min(Math.max(state.page, 0), totalPages - 1);
    const start = state.page * state.pageSize;
    const visible = new Set(filtered.slice(start, start + state.pageSize));

    dataRows.forEach(row => { row.hidden = !visible.has(row); });
    placeholderRows.forEach(row => { row.hidden = filtered.length > 0; });

    const buttons = state.pagination.querySelectorAll<HTMLButtonElement>("button");
    const [first, previous, current, next, last] = Array.from(buttons);
    if (current) current.textContent = String(state.page + 1);
    if (first) first.disabled = state.page === 0;
    if (previous) previous.disabled = state.page === 0;
    if (next) next.disabled = state.page + 1 >= totalPages;
    if (last) last.disabled = state.page + 1 >= totalPages;
}

function enhanceTable(table: HTMLTableElement) {
    if (table.dataset.comandosTableStandardize === "off") return;

    table.classList.add("comandos-native-table", "comandos-standard-table");

    const parent = table.parentElement;
    if (
        parent instanceof HTMLElement &&
        parent.children.length === 1 &&
        !parent.classList.contains("comandos-native-table-container")
    ) {
        parent.classList.add("comandos-native-table-container");
    }

    let state = states.get(table);
    if (!state) {
        state = { page: 0, pageSize: 10 };
        states.set(table, state);
    }

    const apply = () => {
        state!.page = 0;
        applyTable(table);
    };

    createFilterRow(table, apply);

    createPagination(table, state, () => applyTable(table));

    applyTable(table);
}

export function SystemTableStandardizer() {
    React.useEffect(() => {
        let scheduled = false;

        const scan = () => {
            scheduled = false;
            document
                .querySelectorAll<HTMLTableElement>(".comandos-main-content table")
                .forEach(enhanceTable);
        };

        const schedule = () => {
            if (scheduled) return;
            scheduled = true;
            requestAnimationFrame(scan);
        };

        scan();

        const observer = new MutationObserver(schedule);
        observer.observe(document.body, { childList: true, subtree: true });

        return () => observer.disconnect();
    }, []);

    return null;
}
