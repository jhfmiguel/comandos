"use client";

import * as React from "react";

const CONTROL_SELECTOR = [
    'input:not([type="checkbox"]):not([type="radio"]):not([type="hidden"]):not([type="file"]):not([type="range"]):not([type="color"]):not([type="button"]):not([type="submit"]):not([type="reset"])',
    "select",
    "textarea"
].join(",");

type FloatControl = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

const normalizeLabel = (value: string): string =>
    value
        .replace(/\s+/g, " ")
        .replace(/\s*:\s*$/, "")
        .trim();

const tableHeaderLabel = (control: FloatControl): string => {
    const cell = control.closest("td, th") as HTMLTableCellElement | null;
    const table = cell?.closest("table") as HTMLTableElement | null;
    const headerRow = table?.tHead?.rows?.[0];

    if (!cell || !headerRow) return "";

    const header = headerRow.cells[cell.cellIndex];
    return normalizeLabel(header?.textContent ?? "");
};

const sourceLabelFor = (control: FloatControl): HTMLLabelElement | null => {
    const labels = control.labels;
    if (labels?.length) return labels[0];

    const field = control.closest(
        ".field, .registration-field, [data-comandos-field], .comandos-field"
    );

    return field?.querySelector("label") ?? null;
};

const resolveLabel = (control: FloatControl): {
    text: string;
    source: HTMLLabelElement | null;
} => {
    const source = sourceLabelFor(control);
    const sourceText = normalizeLabel(source?.textContent ?? "");
    if (sourceText) return { text: sourceText, source };

    const tableText = tableHeaderLabel(control);
    if (tableText) return { text: tableText, source: null };

    const ariaText = normalizeLabel(control.getAttribute("aria-label") ?? "");
    if (ariaText) return { text: ariaText, source: null };

    const placeholderText = normalizeLabel(control.getAttribute("placeholder") ?? "");
    if (placeholderText) return { text: placeholderText, source: null };

    const fallback = normalizeLabel(
        control.getAttribute("name")
        ?? control.id
        ?? ""
    )
        .replace(/[-_]+/g, " ")
        .replace(/^core\s+/i, "");

    return { text: fallback, source: null };
};

const chooseHost = (control: FloatControl): HTMLElement | null => {
    const explicit = control.closest("[data-comandos-field]") as HTMLElement | null;
    if (explicit && explicit.querySelectorAll(CONTROL_SELECTOR).length <= 1) {
        return explicit;
    }

    const linkedLabel = control.labels?.[0] ?? null;
    const labelParent = linkedLabel?.parentElement ?? null;
    if (
        labelParent
        && labelParent.contains(control)
        && labelParent.querySelectorAll(CONTROL_SELECTOR).length <= 1
    ) {
        return labelParent;
    }

    const preferred = control.closest(
        ".field, .registration-field, .comandos-field, td, th"
    ) as HTMLElement | null;

    if (preferred) {
        const count = preferred.querySelectorAll(CONTROL_SELECTOR).length;
        if (count <= 1) return preferred;
    }

    return control.parentElement;
};

const hasValue = (control: FloatControl): boolean => {
    if (control instanceof HTMLSelectElement) return true;

    if (
        control instanceof HTMLInputElement
        && ["date", "datetime-local", "time", "month", "week"].includes(control.type)
    ) {
        return true;
    }

    return control.value.trim() !== "";
};

const positionLabel = (control: FloatControl, host: HTMLElement): void => {
    const hostRect = host.getBoundingClientRect();
    const controlRect = control.getBoundingClientRect();

    if (!hostRect.width || !controlRect.width) return;

    const left = controlRect.left - hostRect.left + 12;
    const top = controlRect.top - hostRect.top;
    const width = Math.max(controlRect.width - 24, 32);

    host.style.setProperty("--comandos-float-left", `${left}px`);
    host.style.setProperty("--comandos-float-idle-top", `${top + controlRect.height / 2}px`);
    const activeTop = control instanceof HTMLSelectElement ? top : top + 2;
    host.style.setProperty("--comandos-float-active-top", `${activeTop}px`);
    host.style.setProperty("--comandos-float-width", `${width}px`);
};

const updateState = (control: FloatControl): void => {
    const host = chooseHost(control);
    if (!host) return;

    const focused = document.activeElement === control;
    host.dataset.comandosFloatActive = focused || hasValue(control) ? "true" : "false";
    host.dataset.comandosFloatFocused = focused ? "true" : "false";
    positionLabel(control, host);
};

const enhanceControl = (control: FloatControl): void => {
    const isReferenceCombobox =
        control instanceof HTMLInputElement
        && control.type === "search"
        && (
            control.dataset.comandosFloatForce === "true"
            || control.getAttribute("role") === "combobox"
        );

    if (
        control.dataset.comandosNoFloat === "true"
        || control.closest(".comandos-filter-row")
        || control.closest("[role='search']")
        || (
            control instanceof HTMLInputElement
            && control.type === "search"
            && !isReferenceCombobox
        )
    ) return;

    const { text, source } = resolveLabel(control);
    if (!text) return;

    const host = chooseHost(control);
    if (!host) return;

    host.dataset.comandosFloatHost = "true";
    host.dataset.comandosFloatLabel = text;
    control.dataset.comandosFloatControl = "true";

    if (source && !source.contains(control)) {
        source.dataset.comandosFloatSource = "true";
    }

    updateState(control);
};

const scan = (root: ParentNode = document): void => {
    root.querySelectorAll<FloatControl>(CONTROL_SELECTOR).forEach(enhanceControl);
};

export function FloatLabelEnhancer() {
    React.useEffect(() => {
        const refresh = () => requestAnimationFrame(() => scan());

        scan();

        const observer = new MutationObserver((mutations) => {
            let shouldRefresh = false;

            for (const mutation of mutations) {
                if (mutation.type === "childList" && mutation.addedNodes.length) {
                    shouldRefresh = true;
                    break;
                }

                if (
                    mutation.type === "attributes"
                    && mutation.target instanceof HTMLElement
                    && mutation.attributeName === "hidden"
                ) {
                    shouldRefresh = true;
                    break;
                }
            }

            if (shouldRefresh) refresh();
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ["hidden"]
        });

        const syncFromEvent = (event: Event) => {
            const target = event.target;
            if (
                target instanceof HTMLInputElement
                || target instanceof HTMLSelectElement
                || target instanceof HTMLTextAreaElement
            ) {
                enhanceControl(target);
                updateState(target);
            }
        };

        document.addEventListener("focusin", syncFromEvent);
        document.addEventListener("focusout", syncFromEvent);
        document.addEventListener("input", syncFromEvent);
        document.addEventListener("change", syncFromEvent);
        document.addEventListener("click", refresh);
        document.addEventListener("comandos:float-label-refresh", refresh);
        window.addEventListener("resize", refresh);

        return () => {
            observer.disconnect();
            document.removeEventListener("focusin", syncFromEvent);
            document.removeEventListener("focusout", syncFromEvent);
            document.removeEventListener("input", syncFromEvent);
            document.removeEventListener("change", syncFromEvent);
            document.removeEventListener("click", refresh);
            document.removeEventListener("comandos:float-label-refresh", refresh);
            window.removeEventListener("resize", refresh);
        };
    }, []);

    return null;
}
