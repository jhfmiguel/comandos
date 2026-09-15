"use client"

import * as React from "react"

import { Tabs } from "@primereact/ui/tabs"
import type { TabsRootChangeEvent } from "@primereact/ui/tabs"

import { Layout } from "components/layout"
import { useComandosPreferences } from "components/settings/preferences-provider"
import { InventorySalesPanel } from "components/erp/sales"
import { CustodyPanel } from "components/erp/custody"
import { AmmunitionConsumptionPanel } from "components/erp/ammunition-consumption"
import { DonationPanel } from "components/erp/donations"
import { TransferPanel } from "components/erp/transfers"
import { DisposalPanel } from "components/erp/disposals"
import { ReservationPanel } from "components/erp/reservations"
import { MaintenancePanel } from "components/erp/maintenance"

type TransactionTab =
    | "sale"
    | "custody"
    | "ammunition-consumption"
    | "donation"
    | "transfer"
    | "disposal"
    | "reservation"
    | "maintenance"

const transactionTabs: Array<{
    value: TransactionTab
    label: string
}> = [
    { value: "sale", label: "Sale" },
    { value: "custody", label: "Custody" },
    { value: "ammunition-consumption", label: "Ammunition consumption" },
    { value: "donation", label: "Donation" },
    { value: "transfer", label: "Transfer" },
    { value: "disposal", label: "Disposal" },
    { value: "reservation", label: "Reservation" },
    { value: "maintenance", label: "Maintenance and inspection" }
]

function TransactionPanel({ value }: { value: TransactionTab }) {
    switch (value) {
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
    }
}

export function TransactionsWorkspace() {
    const { tr } = useComandosPreferences()
    const [activeTab, setActiveTab] = React.useState<TransactionTab>("sale")

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
                    {transactionTabs.map((tab) => (
                        <Tabs.Tab key={tab.value} value={tab.value}>
                            {tr(tab.label)}
                        </Tabs.Tab>
                    ))}
                    <Tabs.Indicator />
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