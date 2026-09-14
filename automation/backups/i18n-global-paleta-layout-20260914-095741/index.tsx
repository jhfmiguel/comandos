"use client"

import Image from "next/image"
import Link from "next/link"
import { usePathname } from "next/navigation"
import * as React from "react"

import {
    Bell,
    ChevronDown,
    ChevronUp,
    Cog,
    Home,
    ListCheck,
    Shield,
    SignOut,
    Users
} from "@primeicons/react"

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

let persistedSidebarOpen = true

export const Menu: React.FC = () => {

    const { session, signOut, can } = useSession()
    const { tr } = useComandosPreferences()
    const [signOutError, setSignOutError] = React.useState("")
    const [signingOut, setSigningOut] = React.useState(false)

    const pathname = usePathname()

    const [sidebarOpen, setSidebarOpen] = React.useState(persistedSidebarOpen)

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
                                        icon={ListCheck}
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
                                                label: "Institutional",
                                                subItems: [
                                                    {
                                                        href: "/erp/core?resource=organizations",
                                                        label: "Organizations"
                                                    },
                                                    {
                                                        href: "/erp/core?resource=organizational-units",
                                                        label: "Organizational units"
                                                    }
                                                ]
                                            },
                                            {
                                                label: "People",
                                                subItems: [
                                                    {
                                                        href: "/erp/core?resource=people",
                                                        label: "People"
                                                    },
                                                    {
                                                        href: "/erp/core?resource=person-roles",
                                                        label: "Person roles"
                                                    },
                                                    {
                                                        href: "/erp/core?resource=person-role-assignments",
                                                        label: "Person role assignments"
                                                    },
                                                    {
                                                        href: "/erp/core?resource=role-details",
                                                        label: "Role details"
                                                    },
                                                    {
                                                        href: "/erp/core?resource=credentials",
                                                        label: "Credentials"
                                                    },
                                                    {
                                                        href: "/erp/core?resource=qualifications",
                                                        label: "Qualifications"
                                                    }
                                                ]
                                            },
                                            {
                                                label: "Access",
                                                subItems: [
                                                    {
                                                        href: "/erp/core?resource=system-users",
                                                        label: "System users"
                                                    },
                                                    {
                                                        href: "/erp/core?resource=access-profiles",
                                                        label: "Access profiles"
                                                    },
                                                    {
                                                        href: "/erp/core?resource=permissions",
                                                        label: "Permissions"
                                                    },
                                                    {
                                                        href: "/erp/core?resource=user-profiles",
                                                        label: "User profiles"
                                                    },
                                                    {
                                                        href: "/erp/core?resource=profile-permissions",
                                                        label: "Profile permissions"
                                                    }
                                                ]
                                            }
                                        ]}
                                    />

                                    <MenuItem
                                        menuKey="inventory"
                                        label={tr("Assets and inventory")}
                                        icon={Shield}
                                        collapsed={!sidebarOpen}
                                        selectedMenu={selectedMenu}
                                        onSelect={setSelectedMenu}
                                        subItems={[
                                            {
                                                label: "Reference data",
                                                subItems: [
                                                    {
                                                        href: "/erp/inventory?resource=custody-return-condition-types",
                                                        label: "Custody return condition types"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=sale-return-reason-types",
                                                        label: "Sale return reason types"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=inventory-count-status-types",
                                                        label: "Inventory count status types"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=inventory-count-result-types",
                                                        label: "Inventory count result types"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=reservation-status-types",
                                                        label: "Reservation status types"
                                                    }
                                                ]
                                            },
                                            {
                                                label: "Catalog",
                                                subItems: [
                                                    {
                                                        href: "/erp/inventory?resource=item-categories",
                                                        label: "Item categories"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=armament-types",
                                                        label: "Armament types"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=armament-classifications",
                                                        label: "Armament classifications"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=brands",
                                                        label: "Brands"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=item-models",
                                                        label: "Item models"
                                                    }
                                                ]
                                            },
                                            {
                                                label: "Specifications",
                                                subItems: [
                                                    {
                                                        href: "/erp/inventory?resource=technical-characteristics",
                                                        label: "Technical characteristics"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=category-characteristics",
                                                        label: "Category characteristics"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=model-characteristics",
                                                        label: "Model characteristics"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=asset-characteristics",
                                                        label: "Asset characteristics"
                                                    }
                                                ]
                                            },
                                            {
                                                label: "Controlled equipment",
                                                subItems: [
                                                    {
                                                        href: "/erp/inventory?resource=firearm-specifications",
                                                        label: "Firearm specifications"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=ammunition-specifications",
                                                        label: "Ammunition specifications"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=grenade-specifications",
                                                        label: "Grenade specifications"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=spray-specifications",
                                                        label: "Spray specifications"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=ballistic-protection-specifications",
                                                        label: "Ballistic protection specifications"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=electrical-device-specifications",
                                                        label: "Electrical device specifications"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=optical-specifications",
                                                        label: "Optical specifications"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=regulatory-controls",
                                                        label: "Regulatory controls"
                                                    }
                                                ]
                                            },
                                            {
                                                label: "Compliance",
                                                subItems: [
                                                    {
                                                        href: "/erp/inventory?resource=expiration-controls",
                                                        label: "Expiration controls"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=certifications",
                                                        label: "Certifications"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=recalls",
                                                        label: "Recalls"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=recall-items",
                                                        label: "Recall items"
                                                    }
                                                ]
                                            },
                                            {
                                                label: "Inventory",
                                                subItems: [
                                                    {
                                                        href: "/erp/inventory?resource=stock-locations",
                                                        label: "Stock locations"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=equipment-sets",
                                                        label: "Equipment sets"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=equipment-set-components",
                                                        label: "Equipment set components"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=individual-assets",
                                                        label: "Individual assets"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=stock-lots",
                                                        label: "Stock lots"
                                                    }
                                                ]
                                            },
                                            {
                                                label: "Stock history",
                                                subItems: [
                                                    {
                                                        href: "/erp/inventory?resource=stock-balances",
                                                        label: "Stock balances"
                                                    },
                                                    {
                                                        href: "/erp/inventory?resource=stock-movements",
                                                        label: "Stock movements"
                                                    }
                                                ]
                                            }
                                        ]}
                                    />
                                    <MenuItem
                                        menuKey="equipment-movement"
                                        label={tr("Equipment Movement")}
                                        icon={Shield}
                                        collapsed={!sidebarOpen}
                                        selectedMenu={selectedMenu}
                                        onSelect={setSelectedMenu}
                                        subItems={[
                                            {
                                                href: "/erp/sales",
                                                label: "Sale"
                                            },
                                            {
                                                href: "/erp/custody",
                                                label: "Custody"
                                            },
                                            {
                                                href: "/erp/ammunition-consumption",
                                                label: "Ammunition consumption"
                                            },
                                            {
                                                href: "/erp/donations",
                                                label: "Donation"
                                            },
                                            {
                                                href: "/erp/transfers",
                                                label: "Transfer"
                                            },
                                            {
                                                href: "/erp/disposals",
                                                label: "Disposal"
                                            },
                                            {
                                                href: "/erp/reservations",
                                                label: "Reservation"
                                            },
                                            {
                                                href: "/erp/maintenance",
                                                label: "Maintenance and inspection"
                                            }
                                        ]}
                                    />
                                    <MenuItem menuKey="inventory-counts" href="/erp/inventory-counts" label={tr("Physical inventory")} icon={Shield}
                                        collapsed={!sidebarOpen} selectedMenu={selectedMenu} onSelect={setSelectedMenu} />

                                    {can("audit", "READ") && <MenuItem
                                        menuKey="audit"
                                        href="/erp/audit"
                                        label={tr("Audit history")}
                                        icon={Shield}
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
                                        aria-label={tr(tr("Open user menu"))}
                                    >

                                        <Avatar.Root
                                            className="comandos-sidebar-avatar size-7! shrink-0! text-xs!"
                                            shape="circle"
                                        >
                                            <Avatar.Fallback>{session?.user?.name.slice(0, 2).toUpperCase() || "G"}</Avatar.Fallback>
                                        </Avatar.Root>

                                        <span>
                                            {session?.user?.name || tr("Guest")}
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
                                                        {session?.user?.login || tr("Setup mode")}
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
                                                        <SignOut />{signingOut ? tr("Signing out…") : tr("Sign out")}
                                                    </PopupMenu.Item> : <PopupMenu.Item as={Link} href="/login"><SignOut />{tr("Sign in")}</PopupMenu.Item>}

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


