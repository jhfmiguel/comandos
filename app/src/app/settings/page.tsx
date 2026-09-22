"use client"

import * as React from "react"
import { Moon, Sun } from "lucide-react"

import { Layout } from "components/layout"
import {
    type ComandosLocale,
    type ComandosPalette,
    useComandosPreferences
} from "components/settings/preferences-provider"

const fixedPalettes: ComandosPalette[] = [
    "green",
    "blue",
    "lilac",
    "red",
    "yellow",
    "orange",
    "pink",
    "black",
    "gray",
    "white"
]

export default function SettingsPage() {
    const {
        locale,
        theme,
        palette,
        customColor,
        setLocale,
        setTheme,
        setPalette,
        setCustomColor,
        t
    } = useComandosPreferences()

    const chooseCustomColor = (event: React.ChangeEvent<HTMLInputElement>) => {
        setPalette("custom")
        setCustomColor(event.target.value)
    }

    return (
        <Layout title={t("settings")}>
            <section className="comandos-settings-page">
                <div className="comandos-settings-heading">
                    <p className="comandos-settings-kicker">COMANDOS</p>
                    <h1>{t("settingsTitle")}</h1>
                    <p>{t("savedAutomatically")}</p>
                </div>

                <div className="comandos-settings-grid">
                    <article className="comandos-settings-card">
                        <div>
                            <h2>{t("language")}</h2>
                            <p>Português do Brasil / English</p>
                        </div>

                        <label>
                            <span>{t("language")}</span>
                            <select
                                value={locale}
                                onChange={(event) => setLocale(event.target.value as ComandosLocale)}
                            >
                                <option value="pt-BR">{t("portuguese")} (Brasil)</option>
                                <option value="en-US">{t("english")} (US)</option>
                            </select>
                        </label>
                    </article>

                    <article className="comandos-settings-card">
                        <div><h2>{t("appearance")}</h2></div>

                        <div className="comandos-theme-options comandos-theme-icon-options">
                            <button
                                type="button"
                                className={`comandos-theme-icon-button ${theme === "light" ? "is-selected" : ""}`}
                                onClick={() => setTheme("light")}
                                title={t("light")}
                                aria-label={t("light")}
                                aria-pressed={theme === "light"}
                            >
                                <Sun size={20} aria-hidden="true" />
                            </button>

                            <button
                                type="button"
                                className={`comandos-theme-icon-button ${theme === "dark" ? "is-selected" : ""}`}
                                onClick={() => setTheme("dark")}
                                title={t("dark")}
                                aria-label={t("dark")}
                                aria-pressed={theme === "dark"}
                            >
                                <Moon size={20} aria-hidden="true" />
                            </button>
                        </div>
                    </article>

                    <article className="comandos-settings-card comandos-settings-palette-card">
                        <div><h2>{t("palette")}</h2></div>

                        <div className="comandos-palette-options" aria-label={t("palette")}>
                            {fixedPalettes.map((option) => (
                                <button
                                    key={option}
                                    type="button"
                                    className={`comandos-palette-circle-button ${palette === option ? "is-selected" : ""}`}
                                    onClick={() => setPalette(option)}
                                    title={t(option)}
                                    aria-label={t(option)}
                                    aria-pressed={palette === option}
                                >
                                    <span
                                        className={`palette-preview palette-preview-${option}`}
                                        aria-hidden="true"
                                    />
                                </button>
                            ))}

                            <label
                                className={`comandos-palette-circle-button comandos-palette-custom-button ${palette === "custom" ? "is-selected" : ""}`}
                                title={t("custom")}
                                aria-label={t("custom")}
                            >
                                <span
                                    className="palette-preview palette-preview-custom"
                                    style={{ backgroundColor: customColor }}
                                    aria-hidden="true"
                                />
                                <input
                                    type="color"
                                    value={customColor}
                                    onChange={chooseCustomColor}
                                    onClick={() => setPalette("custom")}
                                    aria-label={t("custom")}
                                    className="sr-only"
                                />
                            </label>
                        </div>
                    </article>
                </div>
            </section>
        </Layout>
    )
}
