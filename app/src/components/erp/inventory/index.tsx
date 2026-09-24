"use client"

import { InventoryCountPanel } from "components/erp/inventory-counts"
import { RecordWorkspace } from "components/erp/shared/record-workspace"
import type { ErpResource } from "api/models/erp"

interface InventoryWorkspaceProps {
    initialResource?: string
    section?: string
}

const parameterResource = (key: string, label: string): ErpResource => ({
    key,
    label,
    group: "Technical parameters",
    fields: [
        { name: "code", label: "Code", type: "text", required: true, reference: null, choices: [] },
        { name: "name", label: "Name", type: "text", required: true, reference: null, choices: [] },
        { name: "description", label: "Description", type: "text", required: false, reference: null, choices: [] },
        { name: "active", label: "Active", type: "boolean", required: true, reference: null, choices: [] }
    ]
})

const sectionTabs = {
    "reference-data": [
        { resource: "custody-return-condition-types", label: "Custody return condition" },
        { resource: "sale-return-reason-types", label: "Sale return reason" },
        { resource: "inventory-count-status-types", label: "Inventory count status" },
        { resource: "inventory-count-result-types", label: "Inventory count result" },
        { resource: "reservation-status-types", label: "Reservation status" }
    ],
    "technical-parameters": [
        { resource: "calibers", label: "Calibers", definition: parameterResource("calibers", "Calibers") },
        { resource: "ammunition-types", label: "Ammunition types", definition: parameterResource("ammunition-types", "Ammunition types") },
        { resource: "projectile-types", label: "Projectile types", definition: parameterResource("projectile-types", "Projectile types") },
        { resource: "case-types", label: "Case types", definition: parameterResource("case-types", "Case types") },
        { resource: "primer-types", label: "Primer types", definition: parameterResource("primer-types", "Primer types") },
        { resource: "grenade-types", label: "Grenade types", definition: parameterResource("grenade-types", "Grenade types") },
        { resource: "agents", label: "Agents", definition: parameterResource("agents", "Agents") },
        { resource: "compositions", label: "Compositions", definition: parameterResource("compositions", "Compositions") },
        { resource: "protection-types", label: "Protection types", definition: parameterResource("protection-types", "Protection types") },
        { resource: "protection-levels", label: "Protection levels", definition: parameterResource("protection-levels", "Protection levels") },
        { resource: "materials", label: "Materials", definition: parameterResource("materials", "Materials") },
        { resource: "sizes", label: "Sizes", definition: parameterResource("sizes", "Sizes") },
        { resource: "cartridge-types", label: "Cartridge types", definition: parameterResource("cartridge-types", "Cartridge types") },
        { resource: "optical-types", label: "Optical types", definition: parameterResource("optical-types", "Optical types") },
        { resource: "shield-types", label: "Shield types", definition: parameterResource("shield-types", "Shield types") },
        { resource: "locking-mechanisms", label: "Locking mechanisms", definition: parameterResource("locking-mechanisms", "Locking mechanisms") },
        { resource: "component-types", label: "Component types", definition: parameterResource("component-types", "Component types") },
        { resource: "compatibilities", label: "Compatibilities", definition: parameterResource("compatibilities", "Compatibilities") },
        { resource: "interfaces", label: "Interfaces", definition: parameterResource("interfaces", "Interfaces") }
    ],
    catalog: [
        { resource: "item-categories", label: "Categories" },
        { resource: "armament-types", label: "Types" },
        { resource: "armament-classifications", label: "Classifications" },
        { resource: "brands", label: "Brands" },
        { resource: "item-models", label: "Models" }
    ],
    specifications: [
        { resource: "technical-characteristics", label: "Technical characteristics" }
    ],
    equipment: [
        { resource: "firearm-specifications", label: "Firearm" },
        { resource: "ammunition-specifications", label: "Ammunition" },
        { resource: "grenade-specifications", label: "Grenade" },
        { resource: "spray-specifications", label: "Spray" },
        { resource: "ballistic-protection-specifications", label: "Ballistic protection" },
        { resource: "electrical-device-specifications", label: "Electrical device" },
        { resource: "optical-specifications", label: "Optical" },
        { resource: "helmet-specifications", label: "Helmet" },
        { resource: "shield-specifications", label: "Shield" },
        { resource: "restraint-specifications", label: "Handcuffs/restraints" },
        { resource: "accessory-component-specifications", label: "Accessories/components" },
        { resource: "tactical-equipment-specifications", label: "Tactical equipment" },
        { resource: "regulatory-controls", label: "Regulatory controls" }
    ],
    compliance: [
        { resource: "expiration-controls", label: "Expiration controls" },
        { resource: "certifications", label: "Certifications" },
        { resource: "recalls", label: "Recalls" }
    ],
    inventory: [
        { resource: "stock-locations", label: "Stock locations" },
        { resource: "equipment-sets", label: "Equipment set" },
        { resource: "individual-assets", label: "Individual assets" },
        { resource: "stock-lots", label: "Stock lots" },
        { resource: "physical-inventory", label: "Physical inventory", content: <InventoryCountPanel /> }
    ],
    stock: [
        { resource: "stock-balances", label: "Balances" },
        { resource: "stock-movements", label: "Movements" }
    ]
}

export function InventoryWorkspace({ initialResource, section }: InventoryWorkspaceProps) {
    const tabs = section ? sectionTabs[section as keyof typeof sectionTabs] : undefined
    const firstTabResource = tabs?.[0]?.resource
    const requestedResource =
        initialResource && tabs?.some(tab => tab.resource === initialResource)
            ? initialResource
            : firstTabResource ?? initialResource ?? "item-categories"

    return (
        <RecordWorkspace
            key={`${section ?? "all"}:${requestedResource}`}
            module="inventory"
            title="Assets and inventory"
            initialResource={requestedResource}
            description="Manage equipment models, type-specific technical specifications, identification, individual assets, controlled lots, locations, conditions and immutable stock movement history."
            showNavigation={false}
            tabs={tabs}
        />
    )
}