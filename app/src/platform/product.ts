export interface ProductDefinition {
    id: string
    name: string
    shortName: string
    description: string
    defaultLocale: "pt-BR" | "en-US"
    defaultTheme: "dark" | "light"
    defaultAccent: string
    iconPath: string
    storageNamespace: string
}

const value = (
    source: string | undefined,
    fallback: string
): string => source?.trim() || fallback

export const productDefinition: ProductDefinition = {
    id: value(process.env.NEXT_PUBLIC_PRODUCT_ID, "comandos"),
    name: value(process.env.NEXT_PUBLIC_PRODUCT_NAME, "COMANDOS"),
    shortName: value(process.env.NEXT_PUBLIC_PRODUCT_SHORT_NAME, "COMANDOS"),
    description: value(
        process.env.NEXT_PUBLIC_PRODUCT_DESCRIPTION,
        "Plataforma de Gestão para Segurança Pública e Privada"
    ),
    defaultLocale:
        process.env.NEXT_PUBLIC_PRODUCT_LOCALE === "en-US" ? "en-US" : "pt-BR",
    defaultTheme:
        process.env.NEXT_PUBLIC_PRODUCT_THEME === "light" ? "light" : "dark",
    defaultAccent: value(
        process.env.NEXT_PUBLIC_PRODUCT_ACCENT,
        "#ff9900"
    ),
    iconPath: value(
        process.env.NEXT_PUBLIC_PRODUCT_ICON,
        "/comandos-logo-v4.png"
    ),
    storageNamespace: value(
        process.env.NEXT_PUBLIC_PRODUCT_STORAGE_NAMESPACE,
        "comandos"
    )
}

export function productStorageKey(key: string): string {
    return `${productDefinition.storageNamespace}-${key}`
}
