"use client";

import {
    Activity,
    Boxes,
    PackageCheck,
    ShieldCheck,
    TrendingUp,
    UsersRound
} from "lucide-react";

import { Layout } from "components";

import styles from "./dashboard.module.css";

const months = [
    "Sep", "Oct", "Nov", "Dec", "Jan", "Feb",
    "Mar", "Apr", "May", "Jun", "Jul", "Aug"
];

const movements = [42, 58, 51, 76, 64, 89, 82, 103, 97, 118, 110, 132];
const custodies = [31, 36, 43, 39, 52, 55, 63, 59, 70, 74, 81, 86];

const categories = [
    ["Firearms", 82],
    ["Ammunition", 68],
    ["Ballistic protection", 54],
    ["Less lethal", 43],
    ["Optical equipment", 35]
] as const;

const operations = [
    ["Custody", 38],
    ["Transfer", 24],
    ["Reservation", 17],
    ["Consumption", 12],
    ["Donation / Disposal", 9]
] as const;

function chartPoints(values: number[]): string {
    const max = Math.max(...values);
    const min = Math.min(...values);
    const range = Math.max(max - min, 1);

    return values.map((value, index) => {
        const x = 24 + (index * 552) / (values.length - 1);
        const y = 210 - ((value - min) / range) * 160;
        return `${x},${y}`;
    }).join(" ");
}

export default function DashboardPage() {
    const movementPoints = chartPoints(movements);
    const custodyPoints = chartPoints(custodies);

    return (
        <Layout title="Dashboard">
            <main className={styles.dashboard}>
                <section className={styles.hero}>
                    <div>
                        <span className={styles.eyebrow}>COMMAND CENTER</span>
                        <h1>Operational Dashboard</h1>
                        <p>
                            Consolidated view of assets, inventory and equipment movement.
                        </p>
                    </div>

                    <div className={styles.live}>
                        <span />
                        Operational
                    </div>
                </section>

                <section className={styles.kpis}>
                    <article className={styles.kpi}>
                        <div className={styles.kpiIcon}><Boxes size={21} /></div>
                        <div>
                            <span>Total assets</span>
                            <strong>12,842</strong>
                            <small><TrendingUp size={13} /> 4.8% this month</small>
                        </div>
                    </article>

                    <article className={styles.kpi}>
                        <div className={styles.kpiIcon}><PackageCheck size={21} /></div>
                        <div>
                            <span>Available stock</span>
                            <strong>8,476</strong>
                            <small><TrendingUp size={13} /> 2.3% this month</small>
                        </div>
                    </article>

                    <article className={styles.kpi}>
                        <div className={styles.kpiIcon}><UsersRound size={21} /></div>
                        <div>
                            <span>Active custodies</span>
                            <strong>2,914</strong>
                            <small><Activity size={13} /> 86 updated today</small>
                        </div>
                    </article>

                    <article className={styles.kpi}>
                        <div className={styles.kpiIcon}><ShieldCheck size={21} /></div>
                        <div>
                            <span>Compliance</span>
                            <strong>97.6%</strong>
                            <small><TrendingUp size={13} /> 1.2% this month</small>
                        </div>
                    </article>
                </section>

                <section className={styles.grid}>
                    <article className={`${styles.card} ${styles.wide}`}>
                        <header className={styles.cardHeader}>
                            <div>
                                <span className={styles.cardEyebrow}>EQUIPMENT MOVEMENT</span>
                                <h2>Movement overview</h2>
                            </div>
                            <span className={styles.period}>Last 12 months</span>
                        </header>

                        <div className={styles.legend}>
                            <span><i className={styles.dotPrimary} /> Movements</span>
                            <span><i className={styles.dotMuted} /> Custodies</span>
                        </div>

                        <div className={styles.lineChart}>
                            <div className={styles.gridLines}>
                                <i /><i /><i /><i /><i />
                            </div>

                            <svg viewBox="0 0 600 240" preserveAspectRatio="none">
                                <polyline className={styles.lineMuted} points={custodyPoints} />
                                <polyline className={styles.linePrimary} points={movementPoints} />
                            </svg>

                            <div className={styles.months}>
                                {months.map((month) => (
                                    <span key={month}>{month}</span>
                                ))}
                            </div>
                        </div>
                    </article>

                    <article className={styles.card}>
                        <header className={styles.cardHeader}>
                            <div>
                                <span className={styles.cardEyebrow}>INVENTORY</span>
                                <h2>Assets by category</h2>
                            </div>
                        </header>

                        <div className={styles.bars}>
                            {categories.map(([label, value]) => (
                                <div className={styles.barRow} key={label}>
                                    <div>
                                        <span>{label}</span>
                                        <strong>{value}%</strong>
                                    </div>
                                    <div className={styles.track}>
                                        <span style={{ width: `${value}%` }} />
                                    </div>
                                </div>
                            ))}
                        </div>
                    </article>

                    <article className={styles.card}>
                        <header className={styles.cardHeader}>
                            <div>
                                <span className={styles.cardEyebrow}>DISTRIBUTION</span>
                                <h2>Movement types</h2>
                            </div>
                        </header>

                        <div className={styles.donutArea}>
                            <div className={styles.donut}>
                                <div>
                                    <strong>4,382</strong>
                                    <span>movements</span>
                                </div>
                            </div>

                            <div className={styles.donutLegend}>
                                {operations.map(([label, value]) => (
                                    <div key={label}>
                                        <span>{label}</span>
                                        <strong>{value}%</strong>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </article>

                    <article className={`${styles.card} ${styles.wide}`}>
                        <header className={styles.cardHeader}>
                            <div>
                                <span className={styles.cardEyebrow}>OPERATIONAL STATUS</span>
                                <h2>Inventory readiness</h2>
                            </div>
                        </header>

                        <div className={styles.readiness}>
                            <div><strong>8,476</strong><span>Available</span></div>
                            <div><strong>2,914</strong><span>In custody</span></div>
                            <div><strong>683</strong><span>Reserved</span></div>
                            <div><strong>421</strong><span>Maintenance</span></div>
                            <div><strong>348</strong><span>Restricted</span></div>
                        </div>
                    </article>
                </section>

                <p className={styles.note}>
                    Visual dashboard structure. Values will be connected to the ERP API.
                </p>
            </main>
        </Layout>
    );
}
