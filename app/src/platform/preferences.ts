export {
    PreferencesProvider,
    useComandosPreferences,
    useComandosPreferences as usePlatformPreferences
} from "components/settings/preferences-provider"

export {
    ThemeProvider as SharedThemeProvider,
    resolvePlatformTheme
} from "@faria-miguel/platform/theme"

export {
    PreferencesProvider as SharedPreferencesProvider,
    usePlatformPreferences as useSharedPlatformPreferences
} from "@faria-miguel/platform/preferences"

export type {
    ComandosLocale,
    ComandosLocale as PlatformLocale,
    ComandosPalette,
    ComandosPalette as PlatformPalette,
    ComandosTheme,
    ComandosTheme as PlatformTheme
} from "components/settings/preferences-provider"
