"use client"

import { ReactNode, useState } from "react"
import { PanelLeft } from "lucide-react"

import { Menu } from "./menu"
import { TopToolbar } from "./top-toolbar"
import { Message } from "components"
import { Alert } from "components/common/message"
import { SystemTableStandardizer } from "platform/components/system-table-standardizer"

interface LayoutProps {
    title?: string
    children?: ReactNode
    message?: Array<Alert>
}

export const Layout: React.FC<LayoutProps> = (props: LayoutProps) => {
    const [sidebarOpen, setSidebarOpen] = useState(true)

    return (
        <div className="comandos-app-layout min-h-screen">
            <SystemTableStandardizer />
            <Menu open={sidebarOpen} onOpenChange={setSidebarOpen} />

            <main className="comandos-sidebar-main pt-0">
                <header className="comandos-main-header flex h-12 items-center gap-3 px-4">
                    <button
                        type="button"
                        aria-label="Toggle sidebar"
                        className="comandos-sidebar-trigger"
                        onClick={() => setSidebarOpen((open) => !open)}
                    >
                        <PanelLeft size={18} />
                    </button>

                    <TopToolbar />
                </header>

                <div className="comandos-main-content flex-1 px-4 pb-4 pt-4">
                    <div className="comandos-main-card surface-card border-round shadow-2 overflow-hidden">
                        <div className="comandos-main-card-header p-3">
                            <p className="m-0 font-semibold">{props.title}</p>
                        </div>

                        <div className="p-4">
                            <div className="w-full">
                                {props.message?.map((msg, index) => (
                                    <Message key={index} {...msg} />
                                ))}
                                {props.children}
                            </div>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    )
}
