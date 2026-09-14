"use client"

import { ReactNode } from "react"

import { Sidebar as SidebarIcon } from "@primeicons/react"
import { Button } from "@primereact/ui/button"
import { Sidebar } from "@primereact/ui/sidebar"

import { Menu } from "./menu"
import { TopToolbar } from "./top-toolbar"
import { Message } from "components"
import { Alert } from "components/common/message"

interface LayoutProps {
    title?: string
    children?: ReactNode
    message?: Array<Alert>
}

export const Layout: React.FC<LayoutProps> = ( props: LayoutProps ) => {

    return (

        <Sidebar.Layout className="min-h-screen">

            <Menu />

            <Sidebar.Main className="comandos-sidebar-main">

                <header className="comandos-main-header flex h-12 items-center gap-3 px-4">

                    <Sidebar.Trigger
                        as={Button}
                        severity="secondary"
                        variant="text"
                        size="small"
                        iconOnly
                        aria-label="Toggle sidebar"
                    >
                        <SidebarIcon />
                    </Sidebar.Trigger>

                    <TopToolbar />

                </header>

                <div className="flex-1 px-4 pb-4 pt-4">

                    <div className="comandos-main-card surface-card border-round shadow-2 overflow-hidden">

                        <div className="comandos-main-card-header p-3">

                            <p className="m-0 font-semibold">
                                { props.title }
                            </p>

                        </div>

                        <div className="p-4">

                            <div className="w-full">

                                { props.message &&
                                    props.message.map((msg, index) => (

                                        <Message
                                            key={ index }
                                            {...msg}
                                        />

                                    ))
                                }

                                { props.children }

                            </div>

                        </div>

                    </div>

                </div>

            </Sidebar.Main>

        </Sidebar.Layout>

    )

}
