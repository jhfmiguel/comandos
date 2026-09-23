export interface PlatformAccount {
    id: number
    login: string
    name: string
}

export type PlatformAccessScope =
    | "SYSTEM"
    | "ORGANIZATION"
    | "UNIT"

export interface PlatformGrant {
    resource: string
    action: string
    scope: PlatformAccessScope
    organizationId: number
    unitId: number | null
}

export interface PlatformAccess {
    enforced: boolean
    grants: PlatformGrant[]
}

export interface PlatformSession {
    requireLogin: boolean
    user: PlatformAccount | null
    access?: PlatformAccess
}

export interface AuthorizationContext {
    resource: string
    action: string
    organizationId?: number
    unitId?: number
}

export function isGranted(
    session: PlatformSession | null,
    context: AuthorizationContext
): boolean {
    if (!session?.access?.enforced) return true

    const {
        resource,
        action,
        organizationId,
        unitId
    } = context

    return session.access.grants.some((grant) => {
        const resourceMatches =
            grant.resource === resource || grant.resource === "*"

        const actionMatches =
            grant.action === action || grant.action === "*"

        if (!resourceMatches || !actionMatches) return false

        if (grant.scope === "SYSTEM") return true

        if (grant.scope === "ORGANIZATION") {
            return organizationId === undefined
                || grant.organizationId === organizationId
        }

        return (
            grant.scope === "UNIT"
            && organizationId !== undefined
            && unitId !== undefined
            && grant.organizationId === organizationId
            && grant.unitId === unitId
        )
    })
}
