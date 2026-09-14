"use client"

import { Layout } from "components/layout"
import {
    type ComandosLocale,
    type ComandosTheme,
    useComandosPreferences
} from "components/settings/preferences-provider"

export default function SettingsPage() {
    const {
        locale,
        theme,
        setLocale,
        setTheme,
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
                                PortuguÃªs do Brasil / English
                            </p>
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
                            <p>
                                Amarelo, preto e cinza.
                            </p>
                        </div>

                        <div className="comandos-theme-options">
                            {(["dark", "light"] as ComandosTheme[]).map(
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
                                    >
                                        <span className={`theme-preview theme-preview-${option}`} />
                                        {option === "dark"
                                            ? t("dark")
                                            : t("light")
                                        }
                                    </button>
                                )
                            )}
                        </div>
                    </article>
                </div>
            </section>
        </Layout>
    )
}