import { FormatUtils } from "@4us-dev/utils"
import type { ComandosLocale } from "utils/locale"

const formatUtils = new FormatUtils()

export const formatDate = (value: string, locale: ComandosLocale = "pt-BR"): string => {
    void locale
    if (!value) return ""
    const digits = formatUtils.formatOnlyIntegers(value).slice(0, 8)
    if (digits.length <= 2) return digits
    if (digits.length <= 4) return `${digits.slice(0, 2)}/${digits.slice(2)}`
    return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`
}

export const convertToIsoDate = (
    dateStr: string | undefined | null,
    locale: ComandosLocale = "pt-BR"
): string => {
    if (!dateStr) return ""
    const parts = dateStr.split("/")
    if (parts.length !== 3) return dateStr
    if (locale === "en-US") {
        const [month, day, year] = parts
        return `${year}-${month}-${day}`
    }
    const [day, month, year] = parts
    return `${year}-${month}-${day}`
}
