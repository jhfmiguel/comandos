export interface ProductDefinition {
    id: string
    name: string
    shortName: string
    description: string
    defaultLocale: "pt-BR" | "en-US"
    defaultTheme: "dark" | "light"
    defaultAccent: string
    logoPath: string
    faviconPath: string
    storageNamespace: string
    loginPath: string
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
    logoPath: value(
        process.env.NEXT_PUBLIC_BRAND_LOGO,
        value(
            process.env.NEXT_PUBLIC_PRODUCT_LOGO,
            "/comandos-logo-v4.png"
        )
    ),
    faviconPath: value(
        process.env.NEXT_PUBLIC_BRAND_FAVICON,
        value(
            process.env.NEXT_PUBLIC_PRODUCT_FAVICON,
            "/comandos-logo-v4.png"
        )
    ),
    storageNamespace: value(
        process.env.NEXT_PUBLIC_PRODUCT_STORAGE_NAMESPACE,
        "comandos"
    ),
    loginPath: value(
        process.env.NEXT_PUBLIC_PRODUCT_LOGIN_PATH,
        "/login"
    )
}

export function productStorageKey(key: string): string {
    return `${productDefinition.storageNamespace}-${key}`
}
