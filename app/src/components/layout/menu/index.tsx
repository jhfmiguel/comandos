"use client"

import Image from "next/image"
import Link from "next/link"
import { usePathname } from "next/navigation"
import * as React from "react"

import {
    ArrowRightArrowLeft,
    Bell,
    ChevronDown,
    ChevronUp,
    Cog,
    Home,
    Shield,
    SignOut,
    SlidersV
} from "@primeicons/react"
import { History as HistoryIcon } from "@primeicons/react"
import { Avatar } from "@primereact/ui/avatar"
import { Menu as PopupMenu } from "@primereact/ui/menu"
import type { MenuRootOpenChangeEvent } from "@primereact/ui/menu"
import { Sidebar } from "@primereact/ui/sidebar"

import { MenuItem } from "../menu-item"
import { useSession } from "components/auth/session-provider"
import { useComandosPreferences } from "components/settings/preferences-provider"

interface SidebarOpenChangeEvent {
    originalEvent?: React.SyntheticEvent
    value?: boolean
}

const InventoryBookIcon: React.FC<import("@primeicons/react/core").IconProps> = ({
    className
}) => (
    <i
        className={`pi pi-book ${className ?? ""}`.trim()}
        aria-hidden="true"
    />
)
let persistedSidebarOpen = true

export const Menu: React.FC = () => {

    const { session, signOut, can } = useSession()
    const { tr } = useComandosPreferences()
    const [signOutError, setSignOutError] = React.useState("")
    const [signingOut, setSigningOut] = React.useState(false)

    const pathname = usePathname()

    const [sidebarOpen, setSidebarOpen] = React.useState(persistedSidebarOpen)
    const [mobile, setMobile] = React.useState(false)

    React.useEffect(() => {
        const media = window.matchMedia("(max-width: 768px)")

        const syncViewport = () => {
            const isMobile = media.matches
            setMobile(isMobile)

            if (isMobile) {
                persistedSidebarOpen = false
                setSidebarOpen(false)
            }
        }

        syncViewport()
        media.addEventListener("change", syncViewport)

        return () => media.removeEventListener("change", syncViewport)
    }, [])

    const [selection, setSelection] = React.useState<{ pathname: string; menu: string | null }>({ pathname, menu: null })
    const selectedMenu = selection.pathname === pathname ? selection.menu : null
    const setSelectedMenu = (menu: string | null) => setSelection({ pathname, menu })

    const [userMenuOpen, setUserMenuOpen] = React.useState(false)

    if (selection.pathname !== pathname) {
        setSelection({ pathname, menu: null })
    }

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

            <Sidebar.Spacer className="comandos-sidebar-spacer" />

            <Sidebar.Aside
                className={`comandos-sidebar-aside ${mobile && sidebarOpen ? "comandos-sidebar-mobile-overlay-open" : ""}`}
                onClickCapture={(event: React.MouseEvent<HTMLElement>) => {
                    if (!mobile || !sidebarOpen) {
                        return
                    }

                    const target = event.target
                    if (!(target instanceof Element)) {
                        return
                    }

                    const link = target.closest("a")
                    if (link) {
                        persistedSidebarOpen = false
                        setSidebarOpen(false)
                    }
                }}
            >

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
                                        <Image
                                            src="/comandos-logo-v4.png"
                                            alt="Comandos"
                                            width={1240}
                                            height={1240}
                                            priority
                                            className="comandos-sidebar-logo-image"
                                        />
                                    </div>

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
                                        label={tr("Dashboard")}
                                        icon={Home}
                                        collapsed={!sidebarOpen}
                                        selectedMenu={selectedMenu}
                                        onSelect={setSelectedMenu}
                                    />

                                    <MenuItem
                                        menuKey="command-center"
                                        href="/bot"
                                        label={tr("Command Center")}
                                        icon={SlidersV}
                                        collapsed={!sidebarOpen}
                                        selectedMenu={selectedMenu}
                                        onSelect={setSelectedMenu}
                                    />

                                    <MenuItem
                                        menuKey="institutional-core"
                                        label={tr("Institutional core")}
                                        icon={Cog}
                                        collapsed={!sidebarOpen}
                                        selectedMenu={selectedMenu}
                                        onSelect={setSelectedMenu}
                                        subItems={[
                                            {
                                                href: "/erp/core?section=institutional",
                                                label: "Institutional"
                                            },
                                            {
                                                href: "/erp/core?section=people",
                                                label: "People"
                                            },
                                            {
                                                href: "/erp/core?section=access",
                                                label: "Access"
                                            }
                                        ]}
                                    />

<MenuItem
                                        menuKey="inventory"
                                        label={tr("Inventory")}
                                        icon={InventoryBookIcon}
                                        collapsed={!sidebarOpen}
                                        selectedMenu={selectedMenu}
                                        onSelect={setSelectedMenu}
                                        subItems={[
                                            { href: "/queries/weapons", label: "Consulta de armamento" },
                                            { href: "/erp/inventory?section=reference-data", label: "Reference data" },
                                            { href: "/erp/inventory?section=catalog", label: "Catalog" },
                                            { href: "/erp/inventory?section=specifications", label: "Specifications" },
                                            { href: "/erp/inventory?section=equipment", label: "Equipment" },
                                            { href: "/erp/inventory?section=compliance", label: "Compliance" },
                                            { href: "/erp/inventory?section=inventory", label: "Inventory" },
                                            { href: "/erp/inventory?section=stock", label: "Stock" }
                                        ]}
                                    />

<MenuItem
                                        menuKey="armament-governance"
                                        href="/erp/governance"
                                        label={tr("Governance and reports")}
                                        icon={Shield}
                                        collapsed={!sidebarOpen}
                                        selectedMenu={selectedMenu}
                                        onSelect={setSelectedMenu}
                                    />

<MenuItem
                                        menuKey="transactions"
                                        href="/erp/transactions"
                                        label={tr("Transactions")}
                                        icon={ArrowRightArrowLeft}
                                        collapsed={!sidebarOpen}
                                        selectedMenu={selectedMenu}
                                        onSelect={setSelectedMenu}
                                    />

                                    {can("audit", "READ") && <MenuItem
                                        menuKey="audit"
                                        href="/erp/audit"
                                        label={tr("Audit history")}
                                        icon={HistoryIcon}
                                        collapsed={!sidebarOpen}
                                        selectedMenu={selectedMenu}
                                        onSelect={setSelectedMenu}
                                    />}

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
                                        aria-label={tr(tr(tr(tr("Open user menu"))))}
                                    >

                                        <Avatar.Root
                                            className="comandos-sidebar-avatar size-7! shrink-0! text-xs!"
                                            shape="circle"
                                        >
                                            <Avatar.Fallback>{session?.user?.name.slice(0, 2).toUpperCase() || "G"}</Avatar.Fallback>
                                        </Avatar.Root>

                                        <span>
                                            {session?.user?.name || tr(tr(tr("Guest")))}
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
                                                        {session?.user?.login || tr(tr(tr("Setup mode")))}
                                                    </PopupMenu.Label>

                                                    <PopupMenu.Separator />

                                                    <PopupMenu.Item as={Link} href="/settings">
                                                        <Cog />
                                                        Settings
                                                    </PopupMenu.Item>

                                                    <PopupMenu.Item>
                                                        <Bell />
                                                        Notifications
                                                    </PopupMenu.Item>

                                                    <PopupMenu.Separator />

                                                    {session?.user ? <PopupMenu.Item disabled={signingOut}
                                                        onClick={async () => {
                                                            setSigningOut(true)
                                                            setSignOutError("")
                                                            try { await signOut() }
                                                            catch { setSignOutError("Unable to sign out. Please try again.") }
                                                            finally { setSigningOut(false) }
                                                        }}>
                                                        <SignOut />{signingOut ? tr(tr(tr("Signing out…"))) : tr(tr(tr("Sign out")))}
                                                    </PopupMenu.Item> : <PopupMenu.Item as={Link} href="/login"><SignOut />{tr(tr(tr("Sign in")))}</PopupMenu.Item>}

                                                </PopupMenu.List>

                                            </PopupMenu.Popup>

                                        </PopupMenu.Positioner>

                                    </PopupMenu.Portal>

                                </PopupMenu.Root>

                            </Sidebar.MenuItem>

                        </Sidebar.Menu>

                    </Sidebar.Footer>

                    <Sidebar.Rail />
                    {signOutError && <p role="alert" className="px-3 text-sm">{signOutError}</p>}

                </Sidebar.Panel>

            </Sidebar.Aside>

        </Sidebar.Root>

    )

}

