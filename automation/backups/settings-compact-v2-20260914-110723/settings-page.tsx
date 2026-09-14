"use client"

import * as React from "react"
import { InputColor, parseColor, type InputColorRootChangeEvent } from "@primereact/ui/inputcolor"
import { InputText } from "@primereact/ui/inputtext"
import { Layout } from "components/layout"
import {
    type ComandosLocale,
    type ComandosPalette,
    type ComandosTheme,
    useComandosPreferences
} from "components/settings/preferences-provider"

const palettes: ComandosPalette[] = [
    "green",
    "blue",
    "lilac",
    "red",
    "yellow",
    "orange",
    "custom",
    "pink",
    "black",
    "gray"
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

        const customColorValue = React.useMemo(
        () => parseColor(customColor),
        [customColor]
    )
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
                            <p>Português do Brasil / English</p>
                        </div>

                        <label>
                            <span>{t("language")}</span>
                            <select
                                value={locale}
                                onChange={(event) =>
                                    setLocale(
                                        event.target.value as ComandosLocale
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
                            <p>{t("light")} / {t("dark")}</p>
                        </div>

                        <div className="comandos-theme-options">
                            {(["light", "dark"] as ComandosTheme[]).map(
                                (option) => (
                                    <button
                                        key={option}
                                        type="button"
                                        className={
                                            theme === option
                                                ? "is-selected"
                                                : ""
                                        }
                                        onClick={() => setTheme(option)}
                                        aria-pressed={theme === option}
                                    >
                                        <span
                                            className={
                                                `theme-preview theme-preview-${option}`
                                            }
                                        />
                                        {option === "light"
                                            ? t("light")
                                            : t("dark")
                                        }
                                    </button>
                                )
                            )}
                        </div>
                    </article>

                    <article className="comandos-settings-card comandos-settings-palette-card">
                        <div>
                            <h2>{t("palette")}</h2>
                            <p>
                                {palettes
                                    .filter((option) => option !== "custom")
                                    .map((option) => t(option))
                                    .join(" · ")}
                            </p>
                        </div>

                        <div className="comandos-palette-options">
                            {palettes.map((option) => (
                                <button
                                    key={option}
                                    type="button"
                                    className={palette === option ? "is-selected" : ""}
                                    onClick={() => setPalette(option)}
                                    aria-pressed={palette === option}
                                >
                                    <span className={`palette-preview palette-preview-${option}`} />
                                    {t(option)}
                                </button>
                            ))}
                        </div>

                        {palette === "custom" && (
                            <div className="comandos-custom-color-picker">
                                <InputColor.Root
                                    value={customColorValue}
                                    onValueChange={(event: InputColorRootChangeEvent) => {
                                        setCustomColor(event.value.toString("hex"))
                                    }}
                                >
                                    <InputColor.Area>
                                        <InputColor.AreaBackground />
                                        <InputColor.AreaHandle />
                                    </InputColor.Area>

                                    <InputColor.Slider>
                                        <InputColor.TransparencyGrid />
                                        <InputColor.SliderTrack />
                                        <InputColor.SliderHandle />
                                    </InputColor.Slider>

                                    <div className="comandos-custom-color-row">
                                        <InputColor.Input as={InputText} fluid channel="hex" />
                                        <InputColor.Swatch>
                                            <InputColor.TransparencyGrid />
                                            <InputColor.SwatchBackground />
                                        </InputColor.Swatch>
                                    </div>
                                </InputColor.Root>
                            </div>
                        )}
                    </article>
                </div>
            </section>
        </Layout>
    )
}
