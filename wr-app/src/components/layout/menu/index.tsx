"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import * as React from "react"

import {
    Bell,
    ChevronDown,
    ChevronUp,
    Cog,
    Home,
    Shield,
    SignOut,
    Users
} from "@primeicons/react"

import { Avatar } from "@primereact/ui/avatar"
import { Menu as PopupMenu } from "@primereact/ui/menu"
import type { MenuRootOpenChangeEvent } from "@primereact/ui/menu"
import { Sidebar } from "@primereact/ui/sidebar"

import { MenuItem } from "../menu-item"

interface SidebarOpenChangeEvent {
    originalEvent?: React.SyntheticEvent
    value?: boolean
}

let persistedSidebarOpen = true

const SkullIcon = (): React.JSX.Element => {

    return (

        <svg
            className="comandos-sidebar-skull"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 64 64"
            width="24"
            height="24"
            aria-hidden="true"
            style={{
                display: "block",
                minWidth: "24px",
                minHeight: "24px",
                overflow: "visible"
            }}
        >

            {/* Skull silhouette */}
            <path
                d="
                    M32 3
                    C16 3 7 14 7 29
                    C7 39 12 46 19 50
                    L19 57
                    L25 57
                    L27 52
                    L30 58
                    L34 58
                    L37 52
                    L39 57
                    L45 57
                    L45 50
                    C52 46 57 39 57 29
                    C57 14 48 3 32 3
                    Z
                "
                style={{
                    fill: "#ff9900",
                    stroke: "#ff9900",
                    strokeWidth: 1.5,
                    strokeLinejoin: "round"
                }}
            />

            {/* Left angry eye */}
            <path
                className="comandos-sidebar-skull-detail"
                d="
                    M12 23
                    L29 18
                    L26 34
                    L17 32
                    L14 28
                    Z
                "
                style={{
                    fill: "#363636",
                    stroke: "none"
                }}
            />

            {/* Right angry eye */}
            <path
                className="comandos-sidebar-skull-detail"
                d="
                    M52 23
                    L35 18
                    L38 34
                    L47 32
                    L50 28
                    Z
                "
                style={{
                    fill: "#363636",
                    stroke: "none"
                }}
            />

            {/* Nose */}
            <path
                className="comandos-sidebar-skull-detail"
                d="
                    M32 34
                    L26 44
                    L32 41
                    L38 44
                    Z
                "
                style={{
                    fill: "#363636",
                    stroke: "none"
                }}
            />

        </svg>

    )

}

export const Menu: React.FC = () => {

    const pathname = usePathname()

    const [sidebarOpen, setSidebarOpen] = React.useState(persistedSidebarOpen)

    const [selectedMenu, setSelectedMenu] = React.useState<string | null>(null)

    const [userMenuOpen, setUserMenuOpen] = React.useState(false)

    React.useEffect(() => {

        setSelectedMenu(null)

    }, [pathname])

    return (

        <Sidebar.Root
            id="comandos-sidebar"
            collapsible="icon"
            open={sidebarOpen}
            onOpenChange={(event: SidebarOpenChangeEvent) => {

                const target = event.originalEvent?.target

                if ( target instanceof Element && target.closest("a") ) {
                    return
                }

                const open = event.value ?? false

                persistedSidebarOpen = open

                setSidebarOpen(open)

            }}
            className="comandos-sidebar"
        >

            <Sidebar.Spacer />

            <Sidebar.Aside className="comandos-sidebar-aside">

                <Sidebar.Panel className="comandos-sidebar-panel">

                    <Sidebar.Header>

                        <Sidebar.Menu>

                            <Sidebar.MenuItem>

                                <Sidebar.MenuButton
                                    as={Link}
                                    href="/"
                                    className="comandos-sidebar-brand"
                                    onClick={(event) => {

                                        event.stopPropagation()

                                        setSelectedMenu("dashboard")

                                    }}
                                >

                                    <div className="comandos-sidebar-logo">

                                        <SkullIcon />

                                    </div>

                                    <span className="font-bold">
                                        Comandos
                                    </span>

                                </Sidebar.MenuButton>

                            </Sidebar.MenuItem>

                        </Sidebar.Menu>

                    </Sidebar.Header>

                    <Sidebar.Content className="comandos-sidebar-content">

                        <Sidebar.Group>

                            <Sidebar.GroupLabel className="comandos-sidebar-label">
                                Navigation
                            </Sidebar.GroupLabel>

                            <Sidebar.GroupContent>

                                <Sidebar.Menu>

                                    <MenuItem
                                        menuKey="dashboard"
                                        href="/"
                                        label="Dashboard"
                                        icon={Home}
                                        collapsed={!sidebarOpen}
                                        selectedMenu={selectedMenu}
                                        onSelect={setSelectedMenu}
                                    />

                                    <MenuItem
                                        menuKey="users"
                                        href="/queries/users"
                                        label="Users"
                                        icon={Users}
                                        collapsed={!sidebarOpen}
                                        selectedMenu={selectedMenu}
                                        onSelect={setSelectedMenu}
                                    />

                                    <MenuItem
                                        menuKey="weapons"
                                        label="Weapons"
                                        icon={Shield}
                                        collapsed={!sidebarOpen}
                                        selectedMenu={selectedMenu}
                                        onSelect={setSelectedMenu}
                                        subItems={[
                                            {
                                                href: "/queries/weapons",
                                                label: "Registration"
                                            },
                                            {
                                                href: "/sales/new-sale",
                                                label: "Sales"
                                            }
                                        ]}
                                    />

                                </Sidebar.Menu>

                            </Sidebar.GroupContent>

                        </Sidebar.Group>

                    </Sidebar.Content>

                    <Sidebar.Footer className="comandos-sidebar-footer">

                        <Sidebar.Menu>

                            <Sidebar.MenuItem>

                                <PopupMenu.Root
                                    open={userMenuOpen}
                                    onOpenChange={(event: MenuRootOpenChangeEvent) => {

                                        setUserMenuOpen(event.value)

                                    }}
                                    className="w-full"
                                >

                                    <PopupMenu.Trigger
                                        as={Sidebar.MenuButton}
                                        className="comandos-sidebar-user p-1!"
                                        aria-label="Open user menu"
                                    >

                                        <Avatar.Root
                                            className="comandos-sidebar-avatar size-7! shrink-0! text-xs!"
                                            shape="circle"
                                        >
                                            <Avatar.Fallback>JD</Avatar.Fallback>
                                        </Avatar.Root>

                                        <span>
                                            John Doe
                                        </span>

                                        {userMenuOpen
                                            ? <ChevronUp className="comandos-sidebar-user-chevron ml-auto" />
                                            : <ChevronDown className="comandos-sidebar-user-chevron ml-auto" />
                                        }

                                    </PopupMenu.Trigger>

                                    <PopupMenu.Portal>

                                        <PopupMenu.Positioner
                                            side="top"
                                            align="start"
                                            sideOffset={4}
                                            className="comandos-sidebar-user-positioner"
                                        >

                                            <PopupMenu.Popup className="comandos-sidebar-user-popup">

                                                <PopupMenu.List>

                                                    <PopupMenu.Label>
                                                        john@acme.com
                                                    </PopupMenu.Label>

                                                    <PopupMenu.Separator />

                                                    <PopupMenu.Item>
                                                        <Cog />
                                                        Settings
                                                    </PopupMenu.Item>

                                                    <PopupMenu.Item>
                                                        <Bell />
                                                        Notifications
                                                    </PopupMenu.Item>

                                                    <PopupMenu.Separator />

                                                    <PopupMenu.Item
                                                        as={Link}
                                                        href="/"
                                                        onClick={() => {

                                                            setSelectedMenu(null)

                                                        }}
                                                    >
                                                        <SignOut />
                                                        Sign out
                                                    </PopupMenu.Item>

                                                </PopupMenu.List>

                                            </PopupMenu.Popup>

                                        </PopupMenu.Positioner>

                                    </PopupMenu.Portal>

                                </PopupMenu.Root>

                            </Sidebar.MenuItem>

                        </Sidebar.Menu>

                    </Sidebar.Footer>

                    <Sidebar.Rail />

                </Sidebar.Panel>

            </Sidebar.Aside>

        </Sidebar.Root>

    )

}
