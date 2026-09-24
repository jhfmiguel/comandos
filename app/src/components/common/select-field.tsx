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
    onDisabledAttempt,
    onBlur,
    invalid = false,
    describedBy,
    className,
    searchable = false,
    searchValue,
    onSearchChange,
    searchPlaceholder = "Pesquisar..."
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
    searchable?: boolean;
    searchValue?: string;
    onSearchChange?: (value: string) => void;
    searchPlaceholder?: string;
}) {
    const [open, setOpen] = React.useState(false);
    const [internalSearch, setInternalSearch] = React.useState("");
    const search = searchValue ?? internalSearch;
    const filled = value !== "";
    const active = filled || open;

    const updateSearch = (next: string) => {
        if (onSearchChange) onSearchChange(next);
        else setInternalSearch(next);
    };

    const visibleOptions = searchable && !onSearchChange && search.trim()
        ? options.filter(option =>
            option.label.toLocaleLowerCase("pt-BR").includes(search.trim().toLocaleLowerCase("pt-BR"))
        )
        : options;

    if (disabled) {
        return (
            <div className={[styles.root, "comandos-composed-select", className ?? ""].filter(Boolean).join(" ")} data-active={active ? "true" : "false"} data-invalid={invalid ? "true" : "false"}>
                <button
                    id={id}
                    type="button"
                    className={`${styles.trigger} comandos-composed-select-trigger`}
                    aria-disabled="true"
                    aria-describedby={describedBy}
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
                <label htmlFor={id} className={`${styles.label} comandos-composed-select-label`}>
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
            className={[styles.root, "comandos-composed-select", className ?? ""].filter(Boolean).join(" ")}
            data-active={active ? "true" : "false"}
            data-filled={filled ? "true" : "false"}
            data-invalid={invalid ? "true" : "false"}
        >
            <Select.Root
                value={value || null}
                onValueChange={next => onChange(typeof next === "string" ? next : "")}
                onOpenChange={nextOpen => {
                    setOpen(nextOpen);
                    if (!nextOpen && searchable) updateSearch("");
                }}
                items={items}
                required={required}
                name={name}
            >
                <Select.Trigger
                    id={id}
                    className={`${styles.trigger} comandos-composed-select-trigger`}
                    aria-invalid={invalid || undefined}
                    aria-describedby={describedBy}
                    onBlur={onBlur}
                >
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
                            {searchable && (
                                <div className={styles.searchBox}>
                                    <input
                                        type="search"
                                        className={styles.searchInput}
                                        data-comandos-no-float="true"
                                        value={search}
                                        placeholder={searchPlaceholder}
                                        autoComplete="off"
                                        onChange={event => updateSearch(event.target.value)}
                                        onKeyDown={event => event.stopPropagation()}
                                    />
                                </div>
                            )}
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
                                {visibleOptions.map(option => (
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

            <label htmlFor={id} className={`${styles.label} comandos-composed-select-label`}>
                {label}{required ? " *" : ""}
            </label>
        </div>
    );
}
