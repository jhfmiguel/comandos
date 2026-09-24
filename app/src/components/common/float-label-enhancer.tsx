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
    const fieldLabel = field?.querySelector("label");
    if (fieldLabel) return fieldLabel;

    // Legacy ERP forms often use CSS-module field classes and labels without htmlFor.
    // Walk only through small single-control wrappers so a group label is never
    // accidentally attached to the wrong select/input.
    let current: HTMLElement | null = control.parentElement;
    for (let depth = 0; current && depth < 4; depth += 1) {
        const controls = current.querySelectorAll<FloatControl>(CONTROL_SELECTOR);
        if (controls.length === 1 && controls[0] === control) {
            const candidate = current.querySelector("label");
            if (candidate && !candidate.contains(control)) return candidate;
        }

        if (current.tagName === "FORM" || current.tagName === "FIELDSET") break;
        current = current.parentElement;
    }

    return null;
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

    const source = sourceLabelFor(control);
    if (source) {
        let current: HTMLElement | null = source.parentElement;
        for (let depth = 0; current && depth < 4; depth += 1) {
            if (
                current.contains(control)
                && current.querySelectorAll(CONTROL_SELECTOR).length === 1
            ) {
                return current;
            }
            current = current.parentElement;
        }
    }

    const preferred = control.closest(
        ".field, .registration-field, .comandos-field, td, th"
    ) as HTMLElement | null;

    if (preferred && preferred.querySelectorAll(CONTROL_SELECTOR).length <= 1) {
        return preferred;
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

    const sourceBelongsToHost =
        source
        && host.contains(source)
        && !source.contains(control);

    if (sourceBelongsToHost) {
        host.dataset.comandosFloatUsesSource = "true";
        source.dataset.comandosFloatLabelElement = "true";
        delete source.dataset.comandosFloatSource;
    } else if (source && !source.contains(control)) {
        delete host.dataset.comandosFloatUsesSource;
        source.dataset.comandosFloatSource = "true";
    }

    updateState(control);
};

const scan = (root: ParentNode = document): void => {
    root.querySelectorAll<FloatControl>(CONTROL_SELECTOR).forEach(enhanceControl);
};

export function FloatLabelEnhancer() {
    React.useLayoutEffect(() => {
        const refresh = () => scan();

        scan();

        const observer = new MutationObserver((mutations) => {
            let shouldRefresh = false;

            for (const mutation of mutations) {
                if (mutation.type === "childList" && mutation.addedNodes.length) {
                    for (const node of mutation.addedNodes) {
                        if (node instanceof HTMLElement) {
                            if (node.matches(CONTROL_SELECTOR)) {
                                enhanceControl(node as FloatControl);
                            }
                            scan(node);
                        }
                    }
                    shouldRefresh = false;
                    continue;
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
