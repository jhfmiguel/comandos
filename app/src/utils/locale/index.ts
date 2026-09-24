export type ComandosLocale = "pt-BR" | "en-US"

export const formatLocaleNumber = (value: number, locale: ComandosLocale, options?: Intl.NumberFormatOptions): string =>
    new Intl.NumberFormat(locale, options).format(value)

export const formatLocaleCurrency = (value: number, currency: string, locale: ComandosLocale): string =>
    new Intl.NumberFormat(locale, { style: "currency", currency }).format(value)

export const formatLocaleDate = (
    value: Date | string | number,
    locale: ComandosLocale,
    options?: Intl.DateTimeFormatOptions
): string => new Intl.DateTimeFormat(locale, options).format(new Date(value))

export const formatLocaleDateTime = (value: Date | string | number, locale: ComandosLocale): string =>
    formatLocaleDate(value, locale, {
        year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit"
    })

export const formatEditableDecimal = (value: string, locale: ComandosLocale): string => {
    const digits = value.replace(/\D/g, "")
    if (!digits) return ""
    return new Intl.NumberFormat(locale, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(Number(digits) / 100)
}

export const parseLocaleDecimal = (
    value: string | number | null | undefined,
    locale: ComandosLocale
): number => {
    if (value === null || value === undefined || value === "") return 0
    if (typeof value === "number") return value
    const cleaned = value.replace(/[^0-9,.-]/g, "")
    const normalized = locale === "pt-BR"
        ? cleaned.replace(/\./g, "").replace(",", ".")
        : cleaned.replace(/,/g, "")
    return Number(normalized) || 0
}
