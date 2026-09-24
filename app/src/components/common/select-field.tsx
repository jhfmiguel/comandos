"use client";

import * as React from "react";
import { Select } from "@base-ui/react/select";
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
    onDisabledAttempt
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
}) {
    const [open, setOpen] = React.useState(false);
    const filled = value !== "";
    const active = filled || open;

    if (disabled) {
        return (
            <div className={styles.root} data-active={active ? "true" : "false"}>
                <button
                    id={id}
                    type="button"
                    className={styles.trigger}
                    aria-disabled="true"
                    onPointerDown={event => {
                        event.preventDefault();
                        onDisabledAttempt?.();
                    }}
                    onClick={event => {
                        event.preventDefault();
                        onDisabledAttempt?.();
                    }}
                >
                    <span className={styles.value}>{placeholder}</span>
                    <span className={styles.indicator} aria-hidden="true">
                        <ChevronDown />
                    </span>
                </button>
                <label htmlFor={id} className={styles.label}>
                    {label}{required ? " *" : ""}
                </label>
            </div>
        );
    }

    const items = options.map(option => ({
        label: option.label,
        value: option.value
    }));

    return (
        <div
            className={styles.root}
            data-active={active ? "true" : "false"}
            data-filled={filled ? "true" : "false"}
        >
            <Select.Root
                value={value || null}
                onValueChange={next => onChange(typeof next === "string" ? next : "")}
                onOpenChange={setOpen}
                items={items}
                required={required}
                name={name}
            >
                <Select.Trigger id={id} className={styles.trigger}>
                    <Select.Value className={styles.value} placeholder={placeholder} />
                    <Select.Icon className={styles.indicator}>
                        <ChevronDown />
                    </Select.Icon>
                </Select.Trigger>

                <Select.Portal>
                    <Select.Positioner
                        className={styles.positioner}
                        sideOffset={4}
                        alignItemWithTrigger={false}
                    >
                        <Select.Popup className={styles.popup}>
                            <Select.List className={styles.list}>
                                {!required && (
                                    <Select.Item
                                        value={null}
                                        className={styles.item}
                                        label={placeholder || "Nenhum"}
                                    >
                                        <Select.ItemText>
                                            {placeholder || "Nenhum"}
                                        </Select.ItemText>
                                    </Select.Item>
                                )}
                                {options.map(option => (
                                    <Select.Item
                                        key={option.value}
                                        value={option.value}
                                        disabled={option.disabled}
                                        className={styles.item}
                                        label={option.label}
                                    >
                                        <Select.ItemText>{option.label}</Select.ItemText>
                                    </Select.Item>
                                ))}
                            </Select.List>
                        </Select.Popup>
                    </Select.Positioner>
                </Select.Portal>
            </Select.Root>

            <label htmlFor={id} className={styles.label}>
                {label}{required ? " *" : ""}
            </label>
        </div>
    );
}
