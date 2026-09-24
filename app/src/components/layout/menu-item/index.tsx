"use client"

import Link from "next/link"
import { usePathname, useSearchParams } from "next/navigation"
import * as React from "react"
import { ChevronDown, ChevronRight } from "lucide-react"
import type { LucideIcon } from "lucide-react"

interface SubMenuItem {
    href?: string
    label: string
    subItems?: Array<SubMenuItem>
}

function hasActiveSubItem(
    items: Array<SubMenuItem> | undefined,
    isCurrentRoute: (href?: string) => boolean
): boolean {
    return items?.some((item) =>
        isCurrentRoute(item.href) || hasActiveSubItem(item.subItems, isCurrentRoute)
    ) ?? false
}

interface MenuItemProps {
    menuKey: string
    href?: string
    label: string
    icon: LucideIcon
    collapsed?: boolean
    selectedMenu: string | null
    onSelect: (menuKey: string | null) => void
    subItems?: Array<SubMenuItem>
}

export const MenuItem: React.FC<MenuItemProps> = (props) => {
    const pathname = usePathname()
    const searchParams = useSearchParams()
    const Icon = props.icon

    const isCurrentRoute = React.useCallback((href?: string): boolean => {
        if (!href) return false
        const [path, query = ""] = href.split("?")
        const pathMatches = path === "/" ? pathname === "/" : pathname.startsWith(path)
        if (!pathMatches) return false
        if (!query) return true
        const expected = new URLSearchParams(query)
        return [...expected.entries()].every(([key, value]) => searchParams.get(key) === value)
    }, [pathname, searchParams])

    const isRouteActive = props.href
        ? isCurrentRoute(props.href)
        : hasActiveSubItem(props.subItems, isCurrentRoute)
    const isActive = props.selectedMenu !== null
        ? props.selectedMenu === props.menuKey
        : isRouteActive
    const hasSubItems = Boolean(props.subItems?.length)
    const submenuOpen = props.selectedMenu === props.menuKey
        || (props.selectedMenu === null && isRouteActive)

    const [openGroups, setOpenGroups] = React.useState<Record<string, boolean>>({})

    const select = (key: string | null) => {
        const activeElement = document.activeElement
        if (activeElement instanceof HTMLElement) activeElement.blur()
        props.onSelect(key)
    }

    const renderItems = (items: Array<SubMenuItem>, depth = 0, parentKey = props.menuKey): React.ReactNode =>
        items.map((item, index) => {
            const itemKey = `${parentKey}-${depth}-${index}-${item.label}`
            const hasChildren = Boolean(item.subItems?.length)
            const childActive = hasChildren && hasActiveSubItem(item.subItems, isCurrentRoute)
            const itemOpen = openGroups[itemKey] ?? childActive
            const itemActive = item.href ? isCurrentRoute(item.href) : childActive

            if (hasChildren) {
                return (
                    <div className="comandos-sidebar-menu-item" key={itemKey}>
                        <button
                            type="button"
                            aria-expanded={itemOpen}
                            className={itemActive
                                ? "comandos-sidebar-menu-button comandos-sidebar-menu-button-active comandos-sidebar-submenu-group"
                                : "comandos-sidebar-menu-button comandos-sidebar-submenu-group"}
                            onClick={() => {
                                select(props.menuKey)
                                setOpenGroups(itemOpen ? {} : { [itemKey]: true })
                            }}
                        >
                            <span>{item.label}</span>
                            {itemOpen ? <ChevronDown className="ml-auto" size={16} /> : <ChevronRight className="ml-auto" size={16} />}
                        </button>
                        {itemOpen && (
                            <div className="comandos-sidebar-submenu comandos-sidebar-submenu-nested">
                                {renderItems(item.subItems ?? [], depth + 1, itemKey)}
                            </div>
                        )}
                    </div>
                )
            }

            if (!item.href) return null

            return (
                <div className="comandos-sidebar-menu-item" key={itemKey}>
                    <Link
                        href={item.href}
                        aria-current={itemActive ? "page" : undefined}
                        className={itemActive
                            ? "comandos-sidebar-menu-button comandos-sidebar-menu-button-active"
                            : "comandos-sidebar-menu-button"}
                        onClick={() => select(props.menuKey)}
                    >
                        <span>{item.label}</span>
                    </Link>
                </div>
            )
        })

    if (hasSubItems) {
        return (
            <div className="comandos-sidebar-menu-item">
                <button
                    type="button"
                    aria-expanded={submenuOpen}
                    className={isActive
                        ? "comandos-sidebar-menu-button comandos-sidebar-menu-button-active"
                        : "comandos-sidebar-menu-button"}
                    onClick={() => select(submenuOpen ? null : props.menuKey)}
                >
                    <Icon size={18} />
                    <span>{props.label}</span>
                    {!props.collapsed && (
                        submenuOpen
                            ? <ChevronDown className="ml-auto" size={16} />
                            : <ChevronRight className="ml-auto" size={16} />
                    )}
                </button>

                {!props.collapsed && submenuOpen && (
                    <div className="comandos-sidebar-submenu">
                        {renderItems(props.subItems ?? [])}
                    </div>
                )}

                {props.collapsed && (
                    <div className="comandos-sidebar-popup">
                        <div className="comandos-sidebar-popup-title">{props.label}</div>
                        {props.subItems?.map((item, index) => item.href ? (
                            <Link
                                key={`${props.menuKey}-popup-${index}`}
                                href={item.href}
                                className={isCurrentRoute(item.href)
                                    ? "comandos-sidebar-popup-link comandos-sidebar-popup-link-active"
                                    : "comandos-sidebar-popup-link"}
                                onClick={() => select(props.menuKey)}
                            >
                                {item.label}
                            </Link>
                        ) : null)}
                    </div>
                )}
            </div>
        )
    }

    return (
        <div className="comandos-sidebar-menu-item">
            <Link
                href={props.href ?? "/"}
                aria-current={isActive ? "page" : undefined}
                className={isActive
                    ? "comandos-sidebar-menu-button comandos-sidebar-menu-button-active"
                    : "comandos-sidebar-menu-button"}
                onClick={() => select(props.menuKey)}
            >
                <Icon size={18} />
                <span>{props.label}</span>
            </Link>
        </div>
    )
}
