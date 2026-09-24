import type { ComandosLocale } from "utils/locale"
import { formatEditableDecimal, parseLocaleDecimal } from "utils/locale"

export const convertToBigDecimal = (
    value: string | number | undefined | null,
    locale: ComandosLocale = "pt-BR"
): number => parseLocaleDecimal(value, locale)

export const formatReal = (
    value: string | number | undefined | null,
    locale: ComandosLocale = "pt-BR"
): string => {
    if (value === null || value === undefined || value === "") return ""
    return formatEditableDecimal(String(value), locale)
}

export const isMonetaryField = (fieldName: string): boolean => {
    const name = fieldName.trim().toLowerCase()
    if (!name || ["value", "requiredvalue"].includes(name)) return false

    return /(price|cost|amount|freight|tax|discount|subtotal|total|refund|value)/.test(name)
}

export const parseBRLValue = (
    value: string | number | undefined | null
): number => {
    if (value === null || value === undefined || value === "") return 0
    if (typeof value === "number") return Number.isFinite(value) ? value : 0

    const trimmed = value.trim()
    if (/^-?\d+(?:\.\d+)?$/.test(trimmed)) {
        const numeric = Number(trimmed)
        return Number.isFinite(numeric) ? numeric : 0
    }

    return parseLocaleDecimal(trimmed, "pt-BR")
}

export const formatBRLValue = (
    value: string | number | undefined | null
): string => {
    if (value === null || value === undefined || value === "") return ""

    return new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(parseBRLValue(value))
}

export const maskBRLInput = (value: string): string => {
    const negative = value.trim().startsWith("-")
    const digits = value.replace(/\D/g, "").slice(0, 17)
    if (!digits) return ""

    const amount = Number(digits) / 100 * (negative ? -1 : 1)
    return formatBRLValue(amount)
}
