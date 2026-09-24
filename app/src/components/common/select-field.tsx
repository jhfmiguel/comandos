"use client";

import * as React from "react";
import { Combobox } from "@base-ui/react/combobox";
import { useComandosPreferences } from "components/settings/preferences-provider";
import styles from "./select-field.module.css";

export type ComandosSelectOption = {
    label: string;
    value: string;
    disabled?: boolean;
};

export function ComandosSelectField({
    id,
    label,
    value,
    options,
    onChange,
    required = false,
    disabled = false,
    placeholder = "",
    name,
    onDisabledAttempt,
    onBlur,
    invalid = false,
    describedBy,
    className,
    onSearchChange
}: {
    id: string;
    label: string;
    value: string;
    options: ComandosSelectOption[];
    onChange: (value: string) => void;
    required?: boolean;
    disabled?: boolean;
    placeholder?: string;
    name?: string;
    onDisabledAttempt?: () => void;
    onBlur?: () => void;
    invalid?: boolean;
    describedBy?: string;
    className?: string;
    onSearchChange?: (value: string) => void;
}) {
    const { tr } = useComandosPreferences();
    const [open, setOpen] = React.useState(false);
    const [hasUserTyped, setHasUserTyped] = React.useState(false);

    const localizedOptions = React.useMemo(
        () => options.map(option => ({
            ...option,
            label: tr(option.label)
        })),
        [options, tr]
    );

    const effectiveOptions = React.useMemo(
        () => required
            ? localizedOptions
            : [
                { value: "", label: tr(placeholder || "Nenhum") },
                ...localizedOptions.filter(option => option.value !== "")
            ],
        [localizedOptions, placeholder, required, tr]
    );

    const searchable = Boolean(onSearchChange);
    const visibleOptions = searchable
        ? (hasUserTyped ? effectiveOptions : [])
        : effectiveOptions;

    // Keep the complete option set registered with Base UI so a selected
    // internal value (for example PERSON or ONEROUS) is always rendered
    // using its human-readable label. Searchable reference fields still
    // keep their popup hidden until the user types.
    const items = React.useMemo(
        () => Combobox.createItems(effectiveOptions, {
            getValue: option => option.value,
            getLabel: option => option.label
        }),
        [effectiveOptions]
    );

    const selected = value || null;

    return (
        <div
            className={[styles.root, "comandos-composed-select", className ?? ""].filter(Boolean).join(" ")}
            data-active={open || value !== "" ? "true" : "false"}
            data-filled={value !== "" ? "true" : "false"}
            data-invalid={invalid ? "true" : "false"}
            onPointerDownCapture={() => {
                if (disabled) onDisabledAttempt?.();
            }}
        >
            <Combobox.Root
                items={items}
                value={selected}
                onValueChange={next => {
                    onChange(typeof next === "string" ? next : "");
                }}
                onOpenChange={nextOpen => {
                    setOpen(nextOpen);
                    if (!nextOpen) {
                        setHasUserTyped(false);
                        onSearchChange?.("");
                    }
                }}
                filter={onSearchChange ? null : undefined}
                required={required}
                name={name}
                disabled={disabled}
            >
                <Combobox.InputGroup className={styles.inputGroup}>
                    <Combobox.Input
                        id={id}
                        className={styles.input}
                        data-comandos-no-float="true"
                        placeholder={placeholder}
                        aria-invalid={invalid || undefined}
                        aria-describedby={describedBy}
                        autoComplete="off"
                        onChange={event => {
                            const next = event.currentTarget.value;
                            const typed = next.trim().length > 0;
                            setHasUserTyped(typed);
                            setOpen(typed);
                            onSearchChange?.(next);
                        }}
                        onBlur={onBlur}
                    />
                </Combobox.InputGroup>

                {(searchable ? hasUserTyped : open) && (
                <Combobox.Portal>
                    <Combobox.Positioner
                        className={styles.positioner}
                        sideOffset={4}
                    >
                        <Combobox.Popup className={styles.popup}>
                            <Combobox.Empty className={styles.empty}>
                                Nenhuma opção encontrada.
                            </Combobox.Empty>
                            <Combobox.List className={styles.list}>
                                {visibleOptions.map(option => (
                                    <Combobox.Item
                                        key={option.value || "__empty__"}
                                        value={option.value}
                                        disabled={option.disabled}
                                        className={styles.item}
                                    >
                                        {option.label}
                                    </Combobox.Item>
                                ))}
                            </Combobox.List>
                        </Combobox.Popup>
                    </Combobox.Positioner>
                </Combobox.Portal>
                )}
            </Combobox.Root>

            <label htmlFor={id} className={`${styles.label} comandos-composed-select-label`}>
                {tr(label)}{required ? " *" : ""}
            </label>
        </div>
    );
}
