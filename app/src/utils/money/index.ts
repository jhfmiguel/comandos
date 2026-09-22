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
