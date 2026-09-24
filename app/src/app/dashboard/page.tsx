"use client";

import * as React from "react";
import {
    Activity,
    Boxes,
    PackageCheck,
    RefreshCw,
    ShieldCheck,
    Wrench,
    Archive,
    TrendingUp
} from "lucide-react";

import { Layout } from "components";
import { httpClient } from "api/http";

import styles from "./dashboard.module.css";

type Metric = { label: string; value: number };
type Trend = { month: string; movements: number; custodies: number };
type LocationMetric = { label: string; assets: number; stockUnits: number };
type DashboardData = {
    generatedAt: string;
    kpis: {
        totalAssets: number;
        availableAssets: number;
        activeCustodies: number;
        openMaintenance: number;
        totalLots: number;
        stockAvailable: number;
        stockReserved: number;
        stockBlocked: number;
    };
    trend: Trend[];
    assetCategories: Metric[];
    assetStatuses: Metric[];
    categoryValues: Metric[];
    stockComposition: Metric[];
    locations: LocationMetric[];
    operations: Metric[];
    lotExpiry: Metric[];
};

const emptyData: DashboardData = {
    generatedAt: "",
    kpis: {
        totalAssets: 0,
        availableAssets: 0,
        activeCustodies: 0,
        openMaintenance: 0,
        totalLots: 0,
        stockAvailable: 0,
        stockReserved: 0,
        stockBlocked: 0
    },
    trend: [],
    assetCategories: [],
    assetStatuses: [],
    categoryValues: [],
    stockComposition: [],
    locations: [],
    operations: [],
    lotExpiry: []
};

function number(value: number) {
    return new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 }).format(value || 0);
}

function money(value: number) {
    return new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL",
        maximumFractionDigits: 0
    }).format(value || 0);
}

function chartPoints(values: number[]): string {
    if (!values.length) return "";
    const max = Math.max(...values);
    const min = Math.min(...values);
    const range = Math.max(max - min, 1);

    return values.map((value, index) => {
        const x = values.length === 1 ? 300 : 24 + (index * 552) / (values.length - 1);
        const y = 210 - ((value - min) / range) * 160;
        return `${x},${y}`;
    }).join(" ");
}

function maxMetric(values: Metric[]) {
    return Math.max(1, ...values.map(item => Number(item.value) || 0));
}

function statusLabel(value: string) {
    const labels: Record<string, string> = {
        AVAILABLE: "Disponível",
        BLOCKED: "Bloqueado",
        CUSTODIED: "Cautelado",
        RESERVED: "Reservado",
        IN_MAINTENANCE: "Em manutenção",
        TRANSFERRED: "Transferido",
        DONATED: "Doado",
        SOLD: "Vendido",
        DISPOSED: "Baixado"
    };
    return labels[value] ?? value.replaceAll("_", " ");
}

function donutBackground(values: Metric[]) {
    const palette = [
        "var(--accent)",
        "#64748b",
        "#94a3b8",
        "#475569",
        "#cbd5e1",
        "#22c55e",
        "#ef4444"
    ];
    const total = values.reduce((sum, item) => sum + Number(item.value || 0), 0);
    if (!total) return "conic-gradient(color-mix(in srgb, currentColor 10%, transparent) 0 100%)";

    let cursor = 0;
    const parts = values.map((item, index) => {
        const start = cursor;
        cursor += (Number(item.value || 0) / total) * 100;
        return `${palette[index % palette.length]} ${start}% ${cursor}%`;
    });
    return `conic-gradient(${parts.join(",")})`;
}

export default function DashboardPage() {
    const [data, setData] = React.useState<DashboardData>(emptyData);
    const [loading, setLoading] = React.useState(true);
    const [error, setError] = React.useState("");
    const [locationMetric, setLocationMetric] = React.useState<"assets" | "stockUnits">("assets");

    const load = React.useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const response = await httpClient.get<DashboardData>("/api/dashboard");
            setData(response.data);
        } catch {
            setError("Não foi possível carregar os indicadores do dashboard.");
        } finally {
            setLoading(false);
        }
    }, []);

    React.useEffect(() => {
        void load();
    }, [load]);

    const movementPoints = chartPoints(data.trend.map(item => Number(item.movements)));
    const custodyPoints = chartPoints(data.trend.map(item => Number(item.custodies)));
    const categoryMax = maxMetric(data.assetCategories);
    const operationMax = maxMetric(data.operations);
    const categoryValueMax = maxMetric(data.categoryValues);
    const locationMax = Math.max(
        1,
        ...data.locations.map(item => Number(item[locationMetric]) || 0)
    );

    const stockTotal = data.stockComposition.reduce((sum, item) => sum + Number(item.value || 0), 0);
    const statusTotal = data.assetStatuses.reduce((sum, item) => sum + Number(item.value || 0), 0);

    return (
        <Layout title="Dashboard">
            <main className={styles.dashboard}>
                <section className={styles.hero}>
                    <div>
                        <span className={styles.eyebrow}>CENTRAL OPERACIONAL</span>
                        <h1>Dashboard operacional</h1>
                        <p>Indicadores consolidados de bens, estoque e operações do COMANDOS.</p>
                    </div>

                    <div className={styles.heroActions}>
                        <div className={styles.live}>
                            <span />
                            Dados do ERP
                        </div>
                        <button
                            type="button"
                            className={styles.refreshButton}
                            disabled={loading}
                            onClick={() => void load()}
                        >
                            <RefreshCw size={16} className={loading ? styles.spinning : undefined} />
                            Atualizar
                        </button>
                    </div>
                </section>

                {error && <div className={styles.error}>{error}</div>}

                <section className={styles.kpis} aria-busy={loading}>
                    <article className={styles.kpi}>
                        <div className={styles.kpiIcon}><Boxes size={21} /></div>
                        <div><span>Bens individuais</span><strong>{number(data.kpis.totalAssets)}</strong><small>cadastrados</small></div>
                    </article>
                    <article className={styles.kpi}>
                        <div className={styles.kpiIcon}><PackageCheck size={21} /></div>
                        <div><span>Bens disponíveis</span><strong>{number(data.kpis.availableAssets)}</strong><small>prontos para movimentação</small></div>
                    </article>
                    <article className={styles.kpi}>
                        <div className={styles.kpiIcon}><Archive size={21} /></div>
                        <div><span>Saldo disponível</span><strong>{number(data.kpis.stockAvailable)}</strong><small>{number(data.kpis.totalLots)} lotes cadastrados</small></div>
                    </article>
                    <article className={styles.kpi}>
                        <div className={styles.kpiIcon}><ShieldCheck size={21} /></div>
                        <div><span>Cautelas ativas</span><strong>{number(data.kpis.activeCustodies)}</strong><small>em responsabilidade</small></div>
                    </article>
                    <article className={styles.kpi}>
                        <div className={styles.kpiIcon}><Wrench size={21} /></div>
                        <div><span>Em manutenção</span><strong>{number(data.kpis.openMaintenance)}</strong><small>ordens abertas</small></div>
                    </article>
                    <article className={styles.kpi}>
                        <div className={styles.kpiIcon}><Activity size={21} /></div>
                        <div><span>Estoque comprometido</span><strong>{number(data.kpis.stockReserved + data.kpis.stockBlocked)}</strong><small>reservado + bloqueado</small></div>
                    </article>
                </section>

                <section className={styles.grid}>
                    <article className={`${styles.card} ${styles.wide}`}>
                        <header className={styles.cardHeader}>
                            <div>
                                <span className={styles.cardEyebrow}>EVOLUÇÃO</span>
                                <h2>Movimentações e cautelas</h2>
                            </div>
                            <span className={styles.period}>Últimos 12 meses</span>
                        </header>

                        <div className={styles.legend}>
                            <span><i className={styles.dotPrimary} /> Movimentações</span>
                            <span><i className={styles.dotMuted} /> Cautelas</span>
                        </div>

                        <div className={styles.lineChart}>
                            <div className={styles.gridLines}><i /><i /><i /><i /><i /></div>
                            <svg viewBox="0 0 600 240" preserveAspectRatio="none" aria-label="Evolução mensal de movimentações e cautelas">
                                <polyline className={styles.lineMuted} points={custodyPoints} />
                                <polyline className={styles.linePrimary} points={movementPoints} />
                            </svg>
                            <div className={styles.months}>
                                {data.trend.map(item => <span key={item.month}>{item.month}</span>)}
                            </div>
                        </div>
                    </article>

                    <article className={styles.card}>
                        <header className={styles.cardHeader}>
                            <div>
                                <span className={styles.cardEyebrow}>PATRIMÔNIO</span>
                                <h2>Bens por categoria</h2>
                            </div>
                        </header>
                        <div className={styles.bars}>
                            {data.assetCategories.map(item => (
                                <div className={styles.barRow} key={item.label}>
                                    <div><span>{item.label}</span><strong>{number(item.value)}</strong></div>
                                    <div className={styles.track}><span style={{ width: `${(item.value / categoryMax) * 100}%` }} /></div>
                                </div>
                            ))}
                        </div>
                    </article>

                    <article className={styles.card}>
                        <header className={styles.cardHeader}>
                            <div>
                                <span className={styles.cardEyebrow}>SITUAÇÃO</span>
                                <h2>Distribuição dos bens</h2>
                            </div>
                        </header>
                        <div className={styles.donutArea}>
                            <div className={styles.donut} style={{ background: donutBackground(data.assetStatuses) }}>
                                <div><strong>{number(statusTotal)}</strong><span>bens</span></div>
                            </div>
                            <div className={styles.donutLegend}>
                                {data.assetStatuses.map(item => (
                                    <div key={item.label}><span>{statusLabel(item.label)}</span><strong>{number(item.value)}</strong></div>
                                ))}
                            </div>
                        </div>
                    </article>

                    <article className={`${styles.card} ${styles.wide}`}>
                        <header className={styles.cardHeader}>
                            <div>
                                <span className={styles.cardEyebrow}>ESTOQUE</span>
                                <h2>Composição dos saldos</h2>
                            </div>
                            <strong>{number(stockTotal)} unidades</strong>
                        </header>
                        <div className={styles.stackedBar}>
                            {data.stockComposition.map((item, index) => (
                                <span
                                    key={item.label}
                                    className={styles[`stack${index + 1}`] ?? styles.stack1}
                                    style={{ width: `${stockTotal ? (item.value / stockTotal) * 100 : 0}%` }}
                                    title={`${item.label}: ${number(item.value)}`}
                                />
                            ))}
                        </div>
                        <div className={styles.stockLegend}>
                            {data.stockComposition.map(item => (
                                <div key={item.label}><span>{item.label}</span><strong>{number(item.value)}</strong></div>
                            ))}
                        </div>
                    </article>

                    <article className={styles.card}>
                        <header className={styles.cardHeader}>
                            <div>
                                <span className={styles.cardEyebrow}>LOCALIZAÇÃO</span>
                                <h2>Distribuição por local</h2>
                            </div>
                            <div className={styles.segmented}>
                                <button type="button" className={locationMetric === "assets" ? styles.selected : undefined} onClick={() => setLocationMetric("assets")}>Bens</button>
                                <button type="button" className={locationMetric === "stockUnits" ? styles.selected : undefined} onClick={() => setLocationMetric("stockUnits")}>Estoque</button>
                            </div>
                        </header>
                        <div className={styles.locationChart}>
                            {data.locations.map(item => {
                                const value = Number(item[locationMetric]) || 0;
                                return (
                                    <div className={styles.locationRow} key={item.label}>
                                        <span>{item.label}</span>
                                        <div><i style={{ width: `${(value / locationMax) * 100}%` }} /></div>
                                        <strong>{number(value)}</strong>
                                    </div>
                                );
                            })}
                        </div>
                    </article>

                    <article className={styles.card}>
                        <header className={styles.cardHeader}>
                            <div>
                                <span className={styles.cardEyebrow}>OPERAÇÕES</span>
                                <h2>Volume por fluxo</h2>
                            </div>
                        </header>
                        <div className={styles.operationChart}>
                            {data.operations.map(item => (
                                <div key={item.label}>
                                    <span>{item.label}</span>
                                    <div className={styles.operationTrack}><i style={{ width: `${(item.value / operationMax) * 100}%` }} /></div>
                                    <strong>{number(item.value)}</strong>
                                </div>
                            ))}
                        </div>
                    </article>

                    <article className={styles.card}>
                        <header className={styles.cardHeader}>
                            <div>
                                <span className={styles.cardEyebrow}>VALOR PATRIMONIAL</span>
                                <h2>Valor por categoria</h2>
                            </div>
                        </header>
                        <div className={styles.valueChart}>
                            {data.categoryValues.map(item => (
                                <div key={item.label}>
                                    <div><span>{item.label}</span><strong>{money(item.value)}</strong></div>
                                    <div className={styles.valueTrack}><i style={{ width: `${(item.value / categoryValueMax) * 100}%` }} /></div>
                                </div>
                            ))}
                        </div>
                    </article>

                    <article className={styles.card}>
                        <header className={styles.cardHeader}>
                            <div>
                                <span className={styles.cardEyebrow}>VALIDADE</span>
                                <h2>Lotes por vencimento</h2>
                            </div>
                        </header>
                        <div className={styles.expiryChart}>
                            {data.lotExpiry.map(item => (
                                <div key={item.label}>
                                    <span>{item.label}</span>
                                    <strong>{number(item.value)}</strong>
                                </div>
                            ))}
                        </div>
                    </article>
                </section>

                <p className={styles.note}>
                    {data.generatedAt ? `Atualizado em ${new Date(data.generatedAt).toLocaleString("pt-BR")}` : "Carregando dados do ERP..."}
                </p>
            </main>
        </Layout>
    );
}
