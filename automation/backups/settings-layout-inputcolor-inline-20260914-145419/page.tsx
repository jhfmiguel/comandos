"use client"

import * as React from "react"
import { createPortal } from "react-dom"
import { Moon, Sun } from "lucide-react"
import {
    InputColor,
    parseColor,
    type InputColorRootChangeEvent
} from "@primereact/ui/inputcolor"
import { InputText } from "@primereact/ui/inputtext"

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

    const [customPaletteOpen, setCustomPaletteOpen] =
        React.useState(false)

    const customPaletteRef =
        React.useRef<HTMLDivElement | null>(null)

    const customColorValue = React.useMemo(
        () => parseColor(customColor),
        [customColor]
    )

    React.useEffect(() => {
        if (!customPaletteOpen) {
            return
        }

        const handlePointerDown = (event: PointerEvent) => {
            const target = event.target as Node

            if (
                customPaletteRef.current &&
                !customPaletteRef.current.contains(target)
            ) {
                setCustomPaletteOpen(false)
            }
        }

        const handleEscape = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                setCustomPaletteOpen(false)
            }
        }

        document.addEventListener(
            "pointerdown",
            handlePointerDown
        )
        document.addEventListener(
            "keydown",
            handleEscape
        )

        return () => {
            document.removeEventListener(
                "pointerdown",
                handlePointerDown
            )
            document.removeEventListener(
                "keydown",
                handleEscape
            )
        }
    }, [customPaletteOpen])

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
                    <article className="comandos-settings-card comandos-settings-language-card">
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

                    <article className="comandos-settings-card comandos-settings-palette-card comandos-settings-appearance-card">
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
                                    onClick={() => {
                                        setPalette(option)
                                        setCustomPaletteOpen(false)
                                    }}
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

                            <div
                                ref={customPaletteRef}
                                className={
                                    "comandos-custom-palette-anchor"
                                }
                            >
                                <button
                                    type="button"
                                    className={
                                        "comandos-palette-circle-button " +
                                        "comandos-palette-custom-button " +
                                        (
                                            palette === "custom"
                                                ? "is-selected"
                                                : ""
                                        )
                                    }
                                    onClick={() => {
                                        setPalette("custom")
                                        setCustomPaletteOpen(
                                            (current) => !current
                                        )
                                    }}
                                    title={t("custom")}
                                    aria-label={t("custom")}
                                    aria-pressed={
                                        palette === "custom"
                                    }
                                    aria-expanded={
                                        customPaletteOpen
                                    }
                                    aria-haspopup="dialog"
                                >
                                    <span
                                        className={
                                            "palette-preview " +
                                            "palette-preview-custom"
                                        }
                                        aria-hidden="true"
                                    />
                                </button>

                                {customPaletteOpen &&
                                    typeof document !== "undefined" &&
                                    createPortal(
                                        <div
                                        className={
                                            "comandos-custom-color-popup"
                                        }
                                        role="dialog"
                                        aria-label={t("custom")}
                                    >
                                        <InputColor.Root
                                            value={customColorValue}
                                            onValueChange={(
                                                event:
                                                    InputColorRootChangeEvent
                                            ) => {
                                                setCustomColor(
                                                    event.value
                                                        .toString("hex")
                                                )
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

                                            <div
                                                className={
                                                    "comandos-custom-color-row"
                                                }
                                            >
                                                <InputColor.Input
                                                    as={InputText}
                                                    fluid
                                                    channel="hex"
                                                />

                                                <InputColor.Swatch>
                                                    <InputColor.TransparencyGrid />
                                                    <InputColor.SwatchBackground />
                                                </InputColor.Swatch>
                                            </div>
                                        </InputColor.Root>
                                    </div>,
                                        document.body
                                    )
                                }
                            </div>
                        </div>
                    </article>
                </div>
            </section>
        </Layout>
    )
}
