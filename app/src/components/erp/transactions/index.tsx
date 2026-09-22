"use client"

import * as React from "react"

import { ChevronLeft, ChevronRight } from "components/ui/icons"

import { Tabs } from "components/ui/tabs"
import type { TabsRootChangeEvent } from "components/ui/tabs"

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
            <Tabs.Root
                value={activeTab}
                onValueChange={(event: TabsRootChangeEvent) =>
                    setActiveTab(event.value as TransactionTab)
                }
                selectOnFocus
            >
                <Tabs.List>
                    <Tabs.Prev aria-label={tr("Previous")}>
                        <ChevronLeft />
                    </Tabs.Prev>

                    <Tabs.Content>
                        {transactionTabs.map((tab) => (
                            <Tabs.Tab key={tab.value} value={tab.value}>
                                {tr(tab.label)}
                            </Tabs.Tab>
                        ))}

                        <Tabs.Indicator />
                    </Tabs.Content>

                    <Tabs.Next aria-label={tr("Next")}>
                        <ChevronRight />
                    </Tabs.Next>
                </Tabs.List>

                <Tabs.Panels>
                    <Tabs.Panel value={activeTab}>
                        <TransactionPanel value={activeTab} />
                    </Tabs.Panel>
                </Tabs.Panels>
            </Tabs.Root>
        </Layout>
    )
}