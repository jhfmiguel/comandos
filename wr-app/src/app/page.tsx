"use client";

import React from "react";
import {
    ArrowDownRight,
    ArrowUpRight,
    Boxes,
    ChevronDown,
    MoreHorizontal,
    PackageCheck,
    RefreshCw,
    ShieldCheck,
    UsersRound
} from "lucide-react";

import { Layout } from "components";
import styles from "./dashboard.module.css";

type Period = "monthly" | "quarterly" | "annually";

const monthlyMovement = [168, 186, 203, 190, 226, 242, 238, 271, 286, 279, 311, 342];

const statistics = {
    movements: [340, 510, 445, 620, 570, 710, 680, 790, 755, 860, 825, 940],
    custodies: [260, 360, 325, 430, 405, 505, 480, 570, 545, 630, 610, 690]
};

const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

const recentMovements = [
    { asset: "Beretta APX", code: "APX-008421", movement: "Custody", destination: "Operational Unit 03", status: "Completed" },
    { asset: "Ballistic Vest III-A", code: "BV-002914", movement: "Transfer", destination: "Regional Unit 01", status: "Completed" },
    { asset: "9mm Ammunition", code: "LOT-00982", movement: "Consumption", destination: "Training Center", status: "Processing" },
    { asset: "OC Spray", code: "OC-001833", movement: "Reservation", destination: "Special Operations", status: "Pending" },
    { asset: "Optical Sight", code: "OPT-000417", movement: "Maintenance", destination: "Armory", status: "Processing" }
];

function linePoints(values: number[], width = 620, height = 220): string {
    const max = Math.max(...values);
    const min = Math.min(...values);
    const range = Math.max(max - min, 1);

    return values.map((value, index) => {
        const x = 16 + (index * (width - 32)) / Math.max(values.length - 1, 1);
        const y = height - 22 - ((value - min) / range) * (height - 48);
        return `${x},${y}`;
    }).join(" ");
}

function MetricCard({
    icon,
    title,
    value,
    change,
    positive = true
}: {
    icon: React.ReactNode;
    title: string;
    value: string;
    change: string;
    positive?: boolean;
}) {
    return (
        <article className={styles.metricCard}>
            <div className={styles.metricIcon}>{icon}</div>

            <div className={styles.metricBody}>
                <span className={styles.metricLabel}>{title}</span>
                <strong>{value}</strong>
            </div>

            <span className={positive ? styles.changeUp : styles.changeDown}>
                {positive ? <ArrowUpRight size={13} /> : <ArrowDownRight size={13} />}
                {change}
            </span>
        </article>
    );
}

export default function DashboardPage() {
    const [period, setPeriod] = React.useState<Period>("monthly");
    const [updatedAt, setUpdatedAt] = React.useState("Just now");

    return (
        <Layout title="Dashboard">
            <main className={styles.dashboard}>
                <header className={styles.pageHeader}>


                    <div className={styles.headerActions}>
                        <label className={styles.selectControl}>
                            <select
                                aria-label="Dashboard period"
                                value={period}
                                onChange={(event) => setPeriod(event.target.value as Period)}
                            >
                                <option value="monthly">Monthly</option>
                                <option value="quarterly">Quarterly</option>
                                <option value="annually">Annually</option>
                            </select>
                            <ChevronDown size={15} />
                        </label>

                        <button
                            type="button"
                            className={styles.refreshButton}
                            onClick={() =>
                                setUpdatedAt(
                                    new Intl.DateTimeFormat("en-US", {
                                        hour: "2-digit",
                                        minute: "2-digit"
                                    }).format(new Date())
                                )
                            }
                        >
                            <RefreshCw size={15} />
                            Refresh
                        </button>
                    </div>
                </header>

                <section className={styles.topGrid}>
                    <div className={styles.metricGrid}>
                        <MetricCard
                            icon={<Boxes size={22} />}
                            title="Total assets"
                            value="12,842"
                            change="11.01%"
                        />
                        <MetricCard
                            icon={<UsersRound size={22} />}
                            title="Active custodies"
                            value="2,914"
                            change="9.05%"
                        />
                        <MetricCard
                            icon={<PackageCheck size={22} />}
                            title="Available inventory"
                            value="8,476"
                            change="5.26%"
                        />
                        <MetricCard
                            icon={<ShieldCheck size={22} />}
                            title="Pending controls"
                            value="127"
                            change="3.48%"
                            positive={false}
                        />
                    </div>

                    <article className={`${styles.card} ${styles.monthlySalesCard}`}>
                        <div className={styles.cardHeader}>
                            <div>
                                <h2>Monthly movements</h2>
                                <p>Equipment movement volume</p>
                            </div>
                            <button type="button" className={styles.iconButton} aria-label="Monthly movements options">
                                <MoreHorizontal size={19} />
                            </button>
                        </div>

                        <div className={styles.salesValueRow}>
                            <div>
                                <strong>3,842</strong>
                                <span className={styles.changeUp}>
                                    <ArrowUpRight size={13} />
                                    8.4%
                                </span>
                            </div>
                            <small>this year</small>
                        </div>

                        <div className={styles.miniChart}>
                            <div className={styles.gridLines}><i /><i /><i /><i /></div>
                            <svg viewBox="0 0 620 220" preserveAspectRatio="none" aria-label="Monthly movements chart">
                                <defs>
                                    <linearGradient id="monthlyArea" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="0%" stopColor="#ff9900" stopOpacity=".22" />
                                        <stop offset="100%" stopColor="#ff9900" stopOpacity="0" />
                                    </linearGradient>
                                </defs>
                                <polygon
                                    className={styles.area}
                                    points={`16,208 ${linePoints(monthlyMovement)} 604,208`}
                                />
                                <polyline
                                    className={styles.mainLine}
                                    points={linePoints(monthlyMovement)}
                                />
                            </svg>
                            <div className={styles.monthLabels}>
                                {months.map((month) => <span key={month}>{month}</span>)}
                            </div>
                        </div>
                    </article>

                    <article className={`${styles.card} ${styles.targetCard}`}>
                        <div className={styles.cardHeader}>
                            <div>
                                <h2>Monthly target</h2>
                                <p>Operational readiness</p>
                            </div>
                            <button type="button" className={styles.iconButton} aria-label="Monthly target options">
                                <MoreHorizontal size={19} />
                            </button>
                        </div>

                        <div className={styles.radialWrap}>
                            <div className={styles.radial}>
                                <div className={styles.radialCenter}>
                                    <strong>75.55%</strong>
                                    <span>Readiness</span>
                                </div>
                            </div>
                        </div>

                        <div className={styles.targetText}>
                            <strong>+10.5% from last month</strong>
                            <p>Inventory availability is progressing toward the operational target.</p>
                        </div>

                        <div className={styles.targetStats}>
                            <div>
                                <span>Target</span>
                                <strong>10,000</strong>
                                <small className={styles.changeDown}><ArrowDownRight size={12} /> 3.8%</small>
                            </div>
                            <div>
                                <span>Available</span>
                                <strong>8,476</strong>
                                <small className={styles.changeUp}><ArrowUpRight size={12} /> 8.1%</small>
                            </div>
                            <div>
                                <span>Today</span>
                                <strong>+86</strong>
                                <small className={styles.changeUp}><ArrowUpRight size={12} /> 4.4%</small>
                            </div>
                        </div>
                    </article>
                </section>

                <section className={styles.middleGrid}>
                    <article className={`${styles.card} ${styles.statisticsCard}`}>
                        <div className={styles.cardHeader}>
                            <div>
                                <h2>Statistics</h2>
                                <p>Movements and custodies</p>
                            </div>
                            <div className={styles.legend}>
                                <span><i className={styles.legendPrimary} /> Movements</span>
                                <span><i className={styles.legendSecondary} /> Custodies</span>
                            </div>
                        </div>

                        <div className={styles.statisticsChart}>
                            <div className={styles.gridLines}><i /><i /><i /><i /><i /></div>
                            <svg viewBox="0 0 620 230" preserveAspectRatio="none" aria-label="Statistics chart">
                                <polyline
                                    className={styles.secondaryLine}
                                    points={linePoints(statistics.custodies, 620, 230)}
                                />
                                <polyline
                                    className={styles.mainLine}
                                    points={linePoints(statistics.movements, 620, 230)}
                                />
                            </svg>
                            <div className={styles.monthLabels}>
                                {months.map((month) => <span key={month}>{month}</span>)}
                            </div>
                        </div>
                    </article>

                    <article className={`${styles.card} ${styles.inventoryCard}`}>
                        <div className={styles.cardHeader}>
                            <div>
                                <h2>Inventory overview</h2>
                                <p>Asset availability by status</p>
                            </div>
                            <button type="button" className={styles.iconButton} aria-label="Inventory overview options">
                                <MoreHorizontal size={19} />
                            </button>
                        </div>

                        <div className={styles.barChart}>
                            {[
                                { label: "Available", value: 8476, percent: 86 },
                                { label: "Custody", value: 2914, percent: 54 },
                                { label: "Reserved", value: 683, percent: 34 },
                                { label: "Maintenance", value: 421, percent: 23 },
                                { label: "Restricted", value: 348, percent: 18 }
                            ].map((item) => (
                                <div className={styles.barRow} key={item.label}>
                                    <div>
                                        <span>{item.label}</span>
                                        <strong>{item.value.toLocaleString("en-US")}</strong>
                                    </div>
                                    <div className={styles.barTrack}>
                                        <span style={{ width: `${item.percent}%` }} />
                                    </div>
                                </div>
                            ))}
                        </div>
                    </article>
                </section>

                <section className={styles.card}>
                    <div className={styles.tableHeader}>
                        <div>
                            <h2>Recent movements</h2>
                            <p>Latest inventory operations</p>
                        </div>
                        <span>Updated {updatedAt}</span>
                    </div>

                    <div className={styles.tableWrap}>
                        <table>
                            <thead>
                                <tr>
                                    <th>Asset</th>
                                    <th>Code</th>
                                    <th>Movement</th>
                                    <th>Destination / Unit</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                {recentMovements.map((item) => (
                                    <tr key={item.code}>
                                        <td><strong>{item.asset}</strong></td>
                                        <td>{item.code}</td>
                                        <td>{item.movement}</td>
                                        <td>{item.destination}</td>
                                        <td>
                                            <span className={`${styles.status} ${styles[`status${item.status}`]}`}>
                                                {item.status}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </section>

                <footer className={styles.footer}>
                    Live data integration will replace the placeholder values shown in this dashboard.
                </footer>
            </main>
        </Layout>
    );
}
