"use client"

import * as React from "react"

import { Layout } from "components/layout"
import { useComandosPreferences } from "components/settings/preferences-provider"
import { PurchasePanel } from "components/erp/purchases"
import { InventorySalesPanel } from "components/erp/sales"
import { CustodyPanel } from "components/erp/custody"
import { AmmunitionConsumptionPanel } from "components/erp/ammunition-consumption"
import { DonationPanel } from "components/erp/donations"
import { TransferPanel } from "components/erp/transfers"
import { DisposalPanel } from "components/erp/disposals"
import { ReservationPanel } from "components/erp/reservations"
import { MaintenancePanel } from "components/erp/maintenance"
import { LifecyclePanel } from "components/erp/lifecycle"

type TransactionTab =
    | "purchase"
    | "sale"
    | "custody"
    | "ammunition-consumption"
    | "donation"
    | "transfer"
    | "disposal"
    | "reservation"
    | "maintenance"
    | "lifecycle"

const transactionTabs: Array<{
    value: TransactionTab
    label: string
}> = [
    { value: "purchase", label: "Purchase" },
    { value: "sale", label: "Sale" },
    { value: "custody", label: "Custody" },
    { value: "ammunition-consumption", label: "Ammunition consumption" },
    { value: "donation", label: "Donation" },
    { value: "transfer", label: "Transfer" },
    { value: "disposal", label: "Disposal" },
    { value: "reservation", label: "Reservation" },
    { value: "maintenance", label: "Maintenance and inspection" },
    { value: "lifecycle", label: "Lifecycle control" }
]

function TransactionPanel({ value }: { value: TransactionTab }) {
    switch (value) {
        case "purchase":
            return <PurchasePanel />
        case "sale":
            return <InventorySalesPanel />
        case "custody":
            return <CustodyPanel />
        case "ammunition-consumption":
            return <AmmunitionConsumptionPanel />
        case "donation":
            return <DonationPanel />
        case "transfer":
            return <TransferPanel />
        case "disposal":
            return <DisposalPanel />
        case "reservation":
            return <ReservationPanel />
        case "maintenance":
            return <MaintenancePanel />
        case "lifecycle":
            return <LifecyclePanel />
    }
}

export function TransactionsWorkspace() {
    const { tr } = useComandosPreferences()
    const [activeTab, setActiveTab] = React.useState<TransactionTab>("purchase")

    return (
        <Layout title="Transactions">
            <div className="comandos-tabs">
                <div
                    className="comandos-tabs-list"
                    role="tablist"
                    aria-label={tr("Transactions")}
                >
                    {transactionTabs.map((tab) => (
                        <button
                            key={tab.value}
                            type="button"
                            role="tab"
                            aria-selected={activeTab === tab.value}
                            className={`comandos-tab ${activeTab === tab.value ? "is-active" : ""}`}
                            onClick={() => setActiveTab(tab.value)}
                            onKeyDown={(event) => {
                                const currentIndex = transactionTabs.findIndex(
                                    (item) => item.value === activeTab
                                )

                                if (event.key === "ArrowLeft") {
                                    event.preventDefault()
                                    const nextIndex =
                                        (currentIndex - 1 + transactionTabs.length) %
                                        transactionTabs.length
                                    setActiveTab(transactionTabs[nextIndex].value)
                                }

                                if (event.key === "ArrowRight") {
                                    event.preventDefault()
                                    const nextIndex =
                                        (currentIndex + 1) % transactionTabs.length
                                    setActiveTab(transactionTabs[nextIndex].value)
                                }
                            }}
                        >
                            {tr(tab.label)}
                        </button>
                    ))}
                </div>

                <div
                    className="comandos-tab-panel"
                    role="tabpanel"
                    aria-label={tr(
                        transactionTabs.find((tab) => tab.value === activeTab)?.label ??
                        "Transactions"
                    )}
                >
                    <TransactionPanel value={activeTab} />
                </div>
            </div>
        </Layout>
    )
}