"use client";

import * as React from "react";
import { Combobox } from "@base-ui/react/combobox";
import { ChevronDown } from "@primeicons/react/chevron-down";
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
    const [open, setOpen] = React.useState(false);

    const effectiveOptions = React.useMemo(
        () => required
            ? options
            : [
                { value: "", label: placeholder || "Nenhum" },
                ...options.filter(option => option.value !== "")
            ],
        [options, placeholder, required]
    );

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
                onInputValueChange={(next, details) => {
                    if (details.reason !== "item-press") {
                        onSearchChange?.(String(next ?? ""));
                    }
                }}
                onOpenChange={setOpen}
                filter={onSearchChange ? null : undefined}
                required={required}
                name={name}
                disabled={disabled}
            >
                <Combobox.InputGroup className={styles.inputGroup}>
                    <Combobox.Input
                        id={id}
                        className={styles.input}
                        placeholder={placeholder}
                        aria-invalid={invalid || undefined}
                        aria-describedby={describedBy}
                        autoComplete="off"
                        onBlur={onBlur}
                    />
                    <Combobox.Trigger
                        className={styles.trigger}
                        aria-label={`Abrir opções de ${label}`}
                    >
                        <Combobox.Icon className={styles.indicator}>
                            <ChevronDown />
                        </Combobox.Icon>
                    </Combobox.Trigger>
                </Combobox.InputGroup>

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
                                {effectiveOptions.map(option => (
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
            </Combobox.Root>

            <label htmlFor={id} className={`${styles.label} comandos-composed-select-label`}>
                {label}{required ? " *" : ""}
            </label>
        </div>
    );
}
