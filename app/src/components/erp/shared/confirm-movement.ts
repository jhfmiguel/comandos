/** Review the visible references and entered values before an irreversible movement. */
export function confirmMovement(form: HTMLFormElement, outcome: string, items: string) {
    const values = Array.from(form.querySelectorAll<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>("fieldset input, fieldset select, fieldset textarea"))
        .filter(field => field.type !== "search" && field.type !== "checkbox" && field.value)
        .map(field => {
            const label = field.labels?.[0]?.textContent || field.closest("div")?.querySelector("label")?.textContent
                || field.closest("div")?.parentElement?.querySelector("label")?.textContent || "Value";
            const value = field instanceof HTMLSelectElement ? field.selectedOptions[0]?.textContent : field.value;
            return `${label}: ${value}`;
        });
    return window.confirm(["Confirm irreversible movement", ...values, items, outcome, "Proceed?"].join("\n"));
}
