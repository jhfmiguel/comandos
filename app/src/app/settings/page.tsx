"use client"

import * as React from "react"
import { EyeDropper } from "components/ui/icons"
import { Button } from "components/ui/button"
import { InputText } from "components/ui/inputtext"
import { Popover } from "components/ui/popover"
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

    return (
        <Layout title={t("settings")}>
            <section className="comandos-settings-page">
                <div className="comandos-settings-heading">
                    <p className="comandos-settings-kicker">
                        COMANDOS
                    </p>

                    <h1>{t("settingsTitle")}</h1>
                    <p>{t("savedAutomatically")}</p>
                </div>

                <div className="comandos-settings-grid">
                    <article className="comandos-settings-card">
                        <div>
                            <h2>{t("language")}</h2>
                            <p>
                                Português do Brasil / English
                            </p>
                        </div>

                        <label>
                            <span>{t("language")}</span>

                            <select
                                value={locale}
                                onChange={(event) =>
                                    setLocale(
                                        event.target
                                            .value as ComandosLocale
                                    )
                                }
                            >
                                <option value="pt-BR">
                                    {t("portuguese")} (Brasil)
                                </option>

                                <option value="en-US">
                                    {t("english")} (US)
                                </option>
                            </select>
                        </label>
                    </article>

                    <article className="comandos-settings-card">
                        <div>
                            <h2>{t("appearance")}</h2>
                        </div>

                        <div
                            className={
                                "comandos-theme-options " +
                                "comandos-theme-icon-options"
                            }
                        >
                            <button
                                type="button"
                                className={
                                    "comandos-theme-icon-button " +
                                    (
                                        theme === "light"
                                            ? "is-selected"
                                            : ""
                                    )
                                }
                                onClick={() =>
                                    setTheme("light")
                                }
                                title={t("light")}
                                aria-label={t("light")}
                                aria-pressed={theme === "light"}
                            >
                                <Sun
                                    size={20}
                                    aria-hidden="true"
                                />
                            </button>

                            <button
                                type="button"
                                className={
                                    "comandos-theme-icon-button " +
                                    (
                                        theme === "dark"
                                            ? "is-selected"
                                            : ""
                                    )
                                }
                                onClick={() =>
                                    setTheme("dark")
                                }
                                title={t("dark")}
                                aria-label={t("dark")}
                                aria-pressed={theme === "dark"}
                            >
                                <Moon
                                    size={20}
                                    aria-hidden="true"
                                />
                            </button>
                        </div>
                    </article>

                    <article
                        className={
                            "comandos-settings-card " +
                            "comandos-settings-palette-card"
                        }
                    >
                        <div>
                            <h2>{t("palette")}</h2>
                        </div>

                        <div
                            className="comandos-palette-options"
                            aria-label={t("palette")}
                        >
                            {fixedPalettes.map((option) => (
                                <button
                                    key={option}
                                    type="button"
                                    className={
                                        "comandos-palette-circle-button " +
                                        (
                                            palette === option
                                                ? "is-selected"
                                                : ""
                                        )
                                    }
                                    onClick={() =>
                                        setPalette(option)
                                    }
                                    title={t(option)}
                                    aria-label={t(option)}
                                    aria-pressed={
                                        palette === option
                                    }
                                >
                                    <span
                                        className={
                                            "palette-preview " +
                                            `palette-preview-${option}`
                                        }
                                        aria-hidden="true"
                                    />
                                </button>
                            ))}

                            <div className="comandos-custom-palette-anchor">
                                <Popover.Root>
                                    <Popover.Trigger
                                        type="button"
                                        className={
                                            "comandos-palette-circle-button " +
                                            "comandos-palette-custom-button " +
                                            (palette === "custom" ? "is-selected" : "")
                                        }
                                        onClick={() => setPalette("custom")}
                                        title={t("custom")}
                                        aria-label={t("custom")}
                                        aria-pressed={palette === "custom"}
                                    >
                                        <span
                                            className="palette-preview palette-preview-custom"
                                            style={{ backgroundColor: customColor }}
                                            aria-hidden="true"
                                        />
                                    </Popover.Trigger>

                                    <Popover.Portal>
                                        <Popover.Positioner sideOffset={12} side="left" align="start">
                                            <Popover.Popup className="comandos-color-popover w-72 p-3 space-y-3">
                                                <Popover.Arrow />

                                                <label className="block font-semibold">
                                                    {t("custom")}
                                                </label>

                                                <input
                                                    type="color"
                                                    value={customColor}
                                                    onChange={(event) => {
                                                        setPalette("custom")
                                                        setCustomColor(event.target.value)
                                                    }}
                                                    className="comandos-native-color-picker"
                                                    aria-label={t("custom")}
                                                />

                                                <div className="flex items-center gap-2">
                                                    <InputText
                                                        value={customColor}
                                                        onChange={(event) => {
                                                            const value = event.target.value.trim()
                                                            if (/^#[0-9a-fA-F]{6}$/.test(value)) {
                                                                setPalette("custom")
                                                                setCustomColor(value)
                                                            }
                                                        }}
                                                        className="flex-1"
                                                        aria-label="HEX"
                                                    />
                                                    <Button
                                                        type="button"
                                                        iconOnly
                                                        severity="secondary"
                                                        variant="outlined"
                                                        title={t("custom")}
                                                        onClick={() => document.querySelector<HTMLInputElement>(".comandos-native-color-picker")?.click()}
                                                    >
                                                        <EyeDropper />
                                                    </Button>
                                                </div>
                                            </Popover.Popup>
                                        </Popover.Positioner>
                                    </Popover.Portal>
                                </Popover.Root>
                            </div>
                        </div>
                    </article>
                </div>
            </section>
        </Layout>
    )
}
