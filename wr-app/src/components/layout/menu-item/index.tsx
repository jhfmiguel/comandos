"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import * as React from "react"

import { ChevronDown } from "@primeicons/react/chevron-down"
import { ChevronRight } from "@primeicons/react/chevron-right"
import type { IconProps } from "@primeicons/react/core"
import { Sidebar } from "@primereact/ui/sidebar"

interface SubMenuItem {
    href: string
    label: string
}

interface MenuItemProps {
    menuKey: string
    href?: string
    label: string
    icon: React.FC<IconProps>
    collapsed?: boolean
    selectedMenu: string | null
    onSelect: ( menuKey: string | null ) => void
    subItems?: Array<SubMenuItem>
}

export const MenuItem: React.FC<MenuItemProps> = ( props: MenuItemProps ) => {

    const pathname = usePathname()

    const isCurrentRoute = ( href: string ): boolean => {

        return href === "/"
            ? pathname === href
            : pathname.startsWith(href)

    }

    const isRouteActive = props.href
        ? isCurrentRoute(props.href)
        : props.subItems?.some(
            (item) => isCurrentRoute(item.href)
        ) ?? false

    const isActive = props.selectedMenu !== null
        ? props.selectedMenu === props.menuKey
        : isRouteActive

    const Icon = props.icon

    const hasSubItems = Boolean(props.subItems?.length)

    const [submenuOpen, setSubmenuOpen] = React.useState(isRouteActive)

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

                    <span>
                        {props.label}
                    </span>

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

                        {props.subItems?.map((item) => {

                            const isSubItemActive =
                                props.selectedMenu === null &&
                                isCurrentRoute(item.href)

                            return (

                                <Sidebar.MenuSubItem key={item.href}>

                                    <Sidebar.MenuSubButton
                                        as={Link}
                                        href={item.href}
                                        isActive={isSubItemActive}
                                        aria-current={isSubItemActive ? "page" : undefined}
                                        className={
                                            isSubItemActive
                                                ? "comandos-sidebar-menu-button comandos-sidebar-menu-button-active"
                                                : "comandos-sidebar-menu-button"
                                        }
                                        onClick={(event) => {

                                            event.stopPropagation()

                                            removeCurrentFocus()

                                            props.onSelect(props.menuKey)

                                        }}
                                    >

                                        <span>
                                            {item.label}
                                        </span>

                                    </Sidebar.MenuSubButton>

                                </Sidebar.MenuSubItem>

                            )

                        })}

                    </Sidebar.MenuSub>

                )}

                {props.collapsed && (

                    <div className="comandos-sidebar-popup">

                        <div className="comandos-sidebar-popup-title">
                            {props.label}
                        </div>

                        {props.subItems?.map((item) => {

                            const isSubItemActive = props.selectedMenu === null && isCurrentRoute(item.href)

                            return (

                                <Link
                                    key={item.href}
                                    href={item.href}
                                    aria-current={isSubItemActive ? "page" : undefined}
                                    className={
                                        isSubItemActive
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

                        })}

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

                <span>
                    {props.label}
                </span>

            </Sidebar.MenuButton>

        </Sidebar.MenuItem>

    )

}
