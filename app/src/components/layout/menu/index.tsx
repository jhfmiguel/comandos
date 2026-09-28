"use client"

import Image from "next/image"
import { productDefinition } from "platform/product"
import Link from "next/link"
import { usePathname } from "next/navigation"
import * as React from "react"
import {
    ArrowRightLeft,
    Bell,
    ChevronDown,
    ChevronUp,
    Cog,
    History,
    Home,
    LogIn,
    LogOut,
    PackageOpen,
    Shield
} from "lucide-react"

import { MenuItem } from "../menu-item"
import { useSession } from "components/auth/session-provider"
import { useComandosPreferences } from "components/settings/preferences-provider"

interface MenuProps {
    open: boolean
    onOpenChange: (open: boolean) => void
}

export const Menu: React.FC<MenuProps> = ({ open, onOpenChange }) => {
    const { session, signOut, can } = useSession()
    const { tr } = useComandosPreferences()
    const pathname = usePathname()
    const [selection, setSelection] = React.useState<{ pathname: string; menu: string | null }>({ pathname, menu: null })
    const selectedMenu = selection.pathname === pathname ? selection.menu : null
    const [mobile, setMobile] = React.useState(false)
    const [userMenuOpen, setUserMenuOpen] = React.useState(false)
    const [signOutError, setSignOutError] = React.useState("")
    const [signingOut, setSigningOut] = React.useState(false)

    React.useEffect(() => {
        const media = window.matchMedia("(max-width: 768px)")
        const sync = () => {
            setMobile(media.matches)
            if (media.matches) onOpenChange(false)
        }
        sync()
        media.addEventListener("change", sync)
        return () => media.removeEventListener("change", sync)
    }, [onOpenChange])

    const handleSelect = (menuKey: string | null) => {
        setSelection({ pathname, menu: menuKey })
        setUserMenuOpen(false)
    }

    return (
        <div
            id="comandos-sidebar"
            className="comandos-sidebar"
            data-state={open ? "expanded" : "collapsed"}
        >
            <aside
                className={`comandos-sidebar-aside ${mobile && open ? "comandos-sidebar-mobile-overlay-open" : ""}`}
            >
                <div className="comandos-sidebar-panel">
                    <header>
                        <nav>
                            <Link
                                href="/"
                                className="comandos-sidebar-brand"
                                onClick={() => handleSelect("dashboard")}
                            >
                                <div className="comandos-sidebar-logo">
                                    <Image
                                        src={productDefinition.logoPath}
                                        alt="Comandos"
                                        width={1240}
                                        height={1240}
                                        priority
                                        className="comandos-sidebar-logo-image"
                                    />
                                </div>
                            </Link>
                        </nav>
                    </header>

                    <div className="comandos-sidebar-content">
                        <div>
                            <div className="comandos-sidebar-label">{tr("Navigation")}</div>
                            <nav>
                                <MenuItem
                                    menuKey="dashboard"
                                    href="/"
                                    label={tr("Dashboard")}
                                    icon={Home}
                                    collapsed={!open}
                                    selectedMenu={selectedMenu}
                                    onSelect={handleSelect}
                                />

                                <MenuItem
                                    menuKey="institutional-core"
                                    label={tr("Institutional core")}
                                    icon={Cog}
                                    collapsed={!open}
                                    selectedMenu={selectedMenu}
                                    onSelect={handleSelect}
                                    subItems={[
                                        { href: "/erp/core?section=institutional&resource=organization-natures", label: "Naturezas das organizações" },
                                        { href: "/erp/core?section=institutional&resource=economic-activities", label: "Atividades econômicas" },
                                        { href: "/erp/core?section=institutional&resource=unit-types", label: "Tipos de unidades organizacionais" },
                                        { href: "/erp/core?section=institutional&resource=organizations", label: "Organizações" },
                                        { href: "/erp/core?section=institutional&resource=units", label: "Unidades organizacionais" },
                                        { href: "/erp/core?section=people&resource=person-types", label: "Tipos de pessoas" },
                                        { href: "/erp/core?section=people&resource=contact-types", label: "Tipos de contato" },
                                        { href: "/erp/core?section=people&resource=roles", label: "Tipos de vínculos" },
                                        { href: "/erp/core?section=people&resource=people", label: "Pessoas" },
                                        { href: "/erp/core?section=access&resource=profile-levels", label: "Níveis de perfil de acesso" },
                                        { href: "/erp/core?section=access&resource=permission-resources", label: "Recursos de permissão" },
                                        { href: "/erp/core?section=access&resource=permission-actions", label: "Ações de permissão" },
                                        { href: "/erp/core?section=access", label: "Acesso" }
                                    ]}
                                />

                                <MenuItem
                                    menuKey="inventory"
                                    label={tr("Inventory")}
                                    icon={PackageOpen}
                                    collapsed={!open}
                                    selectedMenu={selectedMenu}
                                    onSelect={handleSelect}
                                    subItems={[
                                        { href: "/queries/weapons", label: "Consulta de armamento" },
                                        { href: "/erp/inventory?section=reference-data", label: "Reference data" },
                                        { href: "/erp/inventory?section=technical-parameters&resource=calibers", label: "Parâmetros técnicos" },
                                        { href: "/erp/inventory?section=catalog", label: "Catalog" },
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
                                    collapsed={!open}
                                    selectedMenu={selectedMenu}
                                    onSelect={handleSelect}
                                />

                                <MenuItem
                                    menuKey="transactions"
                                    href="/erp/transactions"
                                    label={tr("Transactions")}
                                    icon={ArrowRightLeft}
                                    collapsed={!open}
                                    selectedMenu={selectedMenu}
                                    onSelect={handleSelect}
                                />

                                {can("audit", "READ") && (
                                    <MenuItem
                                        menuKey="audit"
                                        href="/erp/audit"
                                        label={tr("Audit history")}
                                        icon={History}
                                        collapsed={!open}
                                        selectedMenu={selectedMenu}
                                        onSelect={handleSelect}
                                    />
                                )}
                            </nav>
                        </div>
                    </div>

                    <footer className="comandos-sidebar-footer">
                        <div className="comandos-sidebar-menu-item" style={{ position: "relative" }}>
                            <button
                                type="button"
                                className="comandos-sidebar-user p-1!"
                                aria-label={tr("Open user menu")}
                                aria-expanded={userMenuOpen}
                                onClick={() => setUserMenuOpen((value) => !value)}
                            >
                                <span className="comandos-sidebar-avatar">
                                    {(session?.user?.name.slice(0, 2) || "G").toUpperCase()}
                                </span>
                                <span>{session?.user?.name || tr("Guest")}</span>
                                {userMenuOpen
                                    ? <ChevronUp className="comandos-sidebar-user-chevron ml-auto" size={16} />
                                    : <ChevronDown className="comandos-sidebar-user-chevron ml-auto" size={16} />}
                            </button>

                            {userMenuOpen && (
                                <div className="comandos-sidebar-user-popup">
                                    <div className="px-3 py-2 text-sm">
                                        {session?.user?.login || tr("Setup mode")}
                                    </div>
                                    <div role="separator" className="border-t" />
                                    <Link href="/settings" className="comandos-sidebar-popup-link" onClick={() => setUserMenuOpen(false)}>
                                        <Cog size={16} /> Settings
                                    </Link>
                                    <button type="button" className="comandos-sidebar-popup-link">
                                        <Bell size={16} /> Notifications
                                    </button>
                                    <div role="separator" className="border-t" />
                                    {session?.user ? (
                                        <button
                                            type="button"
                                            className="comandos-sidebar-popup-link"
                                            disabled={signingOut}
                                            onClick={async () => {
                                                setSigningOut(true)
                                                setSignOutError("")
                                                try {
                                                    await signOut()
                                                    setUserMenuOpen(false)
                                                } catch {
                                                    setSignOutError("Unable to sign out. Please try again.")
                                                } finally {
                                                    setSigningOut(false)
                                                }
                                            }}
                                        >
                                            <LogOut size={16} />
                                            {signingOut ? tr("Signing out…") : tr("Sign out")}
                                        </button>
                                    ) : (
                                        <Link href="/login" className="comandos-sidebar-popup-link">
                                            <LogIn size={16} /> {tr("Sign in")}
                                        </Link>
                                    )}
                                </div>
                            )}
                        </div>
                        {signOutError && <p role="alert" className="px-3 text-sm">{signOutError}</p>}
                    </footer>
                </div>
            </aside>
        </div>
    )
}
