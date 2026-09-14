"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import * as React from "react"

import { ChevronDown } from "@primeicons/react/chevron-down"
import { ChevronRight } from "@primeicons/react/chevron-right"
import type { IconProps } from "@primeicons/react/core"
import { Sidebar } from "@primereact/ui/sidebar"

interface SubMenuItem {
    href?: string
    label: string
    subItems?: Array<SubMenuItem>
}

interface MenuItemProps {
    menuKey: string
    href?: string
    label: string
    icon: React.FC<IconProps>
    collapsed?: boolean
    selectedMenu: string | null
    onSelect: (menuKey: string | null) => void
    subItems?: Array<SubMenuItem>
}

function hasActiveSubItem(
    items: Array<SubMenuItem> | undefined,
    isCurrentRoute: (href?: string) => boolean
): boolean {

    return items?.some((item) =>
        isCurrentRoute(item.href) ||
        hasActiveSubItem(item.subItems, isCurrentRoute)
    ) ?? false

}

export const MenuItem: React.FC<MenuItemProps> = (props: MenuItemProps) => {

    const pathname = usePathname()

    const isCurrentRoute = (href?: string): boolean => {

        if (!href) {
            return false
        }

        const [path, query = ""] = href.split("?")

        const pathMatches = path === "/"
            ? pathname === path
            : pathname.startsWith(path)

        if (!pathMatches) {
            return false
        }

        if (!query || typeof window === "undefined") {
            return pathMatches
        }

        const expected = new URLSearchParams(query)
        const current = new URLSearchParams(window.location.search)

        return [...expected.entries()].every(
            ([key, value]) => current.get(key) === value
        )

    }

    const isRouteActive = props.href
        ? isCurrentRoute(props.href)
        : hasActiveSubItem(props.subItems, isCurrentRoute)

    const isActive = props.selectedMenu !== null
        ? props.selectedMenu === props.menuKey
        : isRouteActive

    const Icon = props.icon
    const hasSubItems = Boolean(props.subItems?.length)

    const [submenuOpen, setSubmenuOpen] = React.useState(isRouteActive)
    const [openGroups, setOpenGroups] = React.useState<Record<string, boolean>>({})

    const removeCurrentFocus = (): void => {

        const activeElement = document.activeElement

        if (activeElement instanceof HTMLElement) {
            activeElement.blur()
        }

    }

    const handleMenuSelection = (): void => {

        removeCurrentFocus()
        props.onSelect(props.menuKey)

    }

    const handleSubmenuToggle = (): void => {

        handleMenuSelection()
        setSubmenuOpen((currentValue) => !currentValue)

    }

    const renderExpandedItems = (
        items: Array<SubMenuItem>,
        depth = 0,
        parentKey = props.menuKey
    ): React.ReactNode => {

        return items.map((item, index) => {

            const itemKey = `${parentKey}-${depth}-${index}-${item.label}`
            const hasChildren = Boolean(item.subItems?.length)
            const childActive = hasChildren
                ? hasActiveSubItem(item.subItems, isCurrentRoute)
                : false

            const itemOpen = openGroups[itemKey] ?? childActive
            const itemActive = item.href
                ? isCurrentRoute(item.href)
                : childActive

            if (hasChildren) {

                return (

                    <Sidebar.MenuSubItem key={itemKey}>

                        <button
                            type="button"
                            aria-expanded={itemOpen}
                            className={
                                itemActive
                                    ? "comandos-sidebar-menu-button comandos-sidebar-menu-button-active comandos-sidebar-submenu-group"
                                    : "comandos-sidebar-menu-button comandos-sidebar-submenu-group"
                            }
                            onClick={(event) => {

                                event.stopPropagation()
                                removeCurrentFocus()
                                props.onSelect(props.menuKey)

                                setOpenGroups((current) => ({
                                    ...current,
                                    [itemKey]: !itemOpen
                                }))

                            }}
                        >

                            <span>{item.label}</span>

                            {itemOpen
                                ? <ChevronDown className="ml-auto" />
                                : <ChevronRight className="ml-auto" />
                            }

                        </button>

                        {itemOpen && (

                            <Sidebar.MenuSub
                                className="comandos-sidebar-submenu comandos-sidebar-submenu-nested border-l-0!"
                                style={{ borderLeft: "none" }}
                            >
                                {renderExpandedItems(
                                    item.subItems ?? [],
                                    depth + 1,
                                    itemKey
                                )}
                            </Sidebar.MenuSub>

                        )}

                    </Sidebar.MenuSubItem>

                )

            }

            if (!item.href) {
                return null
            }

            return (

                <Sidebar.MenuSubItem key={itemKey}>

                    <Sidebar.MenuSubButton
                        as={Link}
                        href={item.href}
                        isActive={itemActive}
                        aria-current={itemActive ? "page" : undefined}
                        className={
                            itemActive
                                ? "comandos-sidebar-menu-button comandos-sidebar-menu-button-active"
                                : "comandos-sidebar-menu-button"
                        }
                        onClick={(event) => {

                            event.stopPropagation()
                            removeCurrentFocus()
                            props.onSelect(props.menuKey)

                        }}
                    >

                        <span>{item.label}</span>

                    </Sidebar.MenuSubButton>

                </Sidebar.MenuSubItem>

            )

        })

    }

    const renderCollapsedItems = (
        items: Array<SubMenuItem>,
        depth = 0,
        parentKey = props.menuKey
    ): React.ReactNode => {

        return items.map((item, index) => {

            const itemKey = `${parentKey}-popup-${depth}-${index}-${item.label}`

            if (item.subItems?.length) {

                return (

                    <div
                        key={itemKey}
                        className="comandos-sidebar-popup-group"
                    >

                        <div className="comandos-sidebar-popup-group-title">
                            {item.label}
                        </div>

                        {renderCollapsedItems(
                            item.subItems,
                            depth + 1,
                            itemKey
                        )}

                    </div>

                )

            }

            if (!item.href) {
                return null
            }

            const itemActive = isCurrentRoute(item.href)

            return (

                <Link
                    key={itemKey}
                    href={item.href}
                    aria-current={itemActive ? "page" : undefined}
                    className={
                        itemActive
                            ? "comandos-sidebar-popup-link comandos-sidebar-popup-link-active"
                            : "comandos-sidebar-popup-link"
                    }
                    onClick={(event) => {

                        event.stopPropagation()
                        removeCurrentFocus()
                        props.onSelect(props.menuKey)

                    }}
                >
                    {item.label}
                </Link>

            )

        })

    }

    if (hasSubItems) {

        return (

            <Sidebar.MenuItem className="comandos-sidebar-menu-item">

                <Sidebar.MenuButton
                    isActive={isActive}
                    aria-expanded={submenuOpen}
                    className={
                        isActive
                            ? "comandos-sidebar-menu-button comandos-sidebar-menu-button-active"
                            : "comandos-sidebar-menu-button"
                    }
                    onClick={handleSubmenuToggle}
                >

                    <Icon />

                    <span>{props.label}</span>

                    {!props.collapsed && (
                        submenuOpen
                            ? <ChevronDown className="ml-auto" />
                            : <ChevronRight className="ml-auto" />
                    )}

                </Sidebar.MenuButton>

                {!props.collapsed && submenuOpen && (

                    <Sidebar.MenuSub
                        className="comandos-sidebar-submenu border-l-0!"
                        style={{ borderLeft: "none" }}
                    >
                        {renderExpandedItems(props.subItems ?? [])}
                    </Sidebar.MenuSub>

                )}

                {props.collapsed && (

                    <div className="comandos-sidebar-popup">

                        <div className="comandos-sidebar-popup-title">
                            {props.label}
                        </div>

                        {renderCollapsedItems(props.subItems ?? [])}

                    </div>

                )}

            </Sidebar.MenuItem>

        )

    }

    return (

        <Sidebar.MenuItem>

            <Sidebar.MenuButton
                as={Link}
                href={props.href ?? "/"}
                isActive={isActive}
                aria-current={isActive ? "page" : undefined}
                className={
                    isActive
                        ? "comandos-sidebar-menu-button comandos-sidebar-menu-button-active"
                        : "comandos-sidebar-menu-button"
                }
                onClick={(event) => {

                    event.stopPropagation()
                    removeCurrentFocus()
                    props.onSelect(props.menuKey)

                }}
            >

                <Icon />
                <span>{props.label}</span>

            </Sidebar.MenuButton>

        </Sidebar.MenuItem>

    )

}