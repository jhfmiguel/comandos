"use client"

import * as React from "react"
import { SearchableSelect } from "@faria-miguel/ui/searchable-select"
import { useComandosPreferences } from "components/settings/preferences-provider"
import styles from "./select-field.module.css"

export type ComandosSelectOption = {
    label: string
    value: string
    disabled?: boolean
}

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
    id: string
    label: string
    value: string
    options: ComandosSelectOption[]
    onChange: (value: string) => void
    required?: boolean
    disabled?: boolean
    placeholder?: string
    name?: string
    onDisabledAttempt?: () => void
    onBlur?: () => void
    invalid?: boolean
    describedBy?: string
    className?: string
    onSearchChange?: (value: string) => void
}) {
    const { tr } = useComandosPreferences()

    const localizedOptions = React.useMemo(
        () => options.map(option => ({
            ...option,
            label: tr(option.label)
        })),
        [options, tr]
    )

    return (
        <SearchableSelect
            id={id}
            label={tr(label)}
            value={value}
            options={localizedOptions}
            onChange={onChange}
            required={required}
            disabled={disabled}
            placeholder={placeholder ? tr(placeholder) : ""}
            name={name}
            onDisabledAttempt={onDisabledAttempt}
            onBlur={onBlur}
            invalid={invalid}
            describedBy={describedBy}
            className={["comandos-composed-select", className ?? ""].filter(Boolean).join(" ")}
            onSearchChange={onSearchChange}
            emptyText={tr("Nenhuma opção encontrada.")}
            classes={{
                root: styles.root,
                inputGroup: styles.inputGroup,
                input: styles.input,
                positioner: styles.positioner,
                popup: styles.popup,
                empty: styles.empty,
                list: styles.list,
                item: styles.item
            }}
        />
    )
}
