"use client"

import { InventoryCountPanel } from "components/erp/inventory-counts"
import { RecordWorkspace } from "components/erp/shared/record-workspace"

interface InventoryWorkspaceProps {
    initialResource?: string
    section?: string
}

const sectionTabs = {
    "reference-data": [
        { resource: "custody-return-condition-types", label: "Custody return condition" },
        { resource: "sale-return-reason-types", label: "Sale return reason" },
        { resource: "inventory-count-status-types", label: "Inventory count status" },
        { resource: "inventory-count-result-types", label: "Inventory count result" },
        { resource: "reservation-status-types", label: "Reservation status" },
        { resource: "calibers", label: "Calibers" },
        { resource: "ammunition-types", label: "Ammunition types" },
        { resource: "projectile-types", label: "Projectile types" },
        { resource: "case-types", label: "Case types" },
        { resource: "primer-types", label: "Primer types" },
        { resource: "grenade-types", label: "Grenade types" },
        { resource: "agents", label: "Agents" },
        { resource: "compositions", label: "Compositions" },
        { resource: "protection-types", label: "Protection types" },
        { resource: "protection-levels", label: "Protection levels" },
        { resource: "materials", label: "Materials" },
        { resource: "sizes", label: "Sizes" },
        { resource: "cartridge-types", label: "Cartridge types" },
        { resource: "optical-types", label: "Optical types" },
        { resource: "shield-types", label: "Shield types" },
        { resource: "locking-mechanisms", label: "Locking mechanisms" },
        { resource: "component-types", label: "Component types" },
        { resource: "compatibilities", label: "Compatibilities" },
        { resource: "interfaces", label: "Interfaces" }
    ],
    catalog: [
        { resource: "item-categories", label: "Categories" },
        { resource: "armament-types", label: "Types" },
        { resource: "armament-classifications", label: "Classifications" },
        { resource: "brands", label: "Brands" },
        { resource: "item-models", label: "Models" }
    ],
    specifications: [
        { resource: "technical-characteristics", label: "Technical" },
        { resource: "category-characteristics", label: "Category" },
        { resource: "model-characteristics", label: "Model" },
        { resource: "asset-characteristics", label: "Asset" }
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
        { resource: "recalls", label: "Recalls" },
        { resource: "recall-items", label: "Recall items" }
    ],
    inventory: [
        { resource: "stock-locations", label: "Stock locations" },
        { resource: "equipment-sets", label: "Equipment set" },
        { resource: "equipment-set-components", label: "Equipment set components" },
        { resource: "individual-assets", label: "Individual assets" },
        { resource: "stock-lots", label: "Stock lots" },
        { resource: "physical-inventory", label: "Physical inventory", content: <InventoryCountPanel /> }
    ],
    stock: [
        { resource: "stock-balances", label: "Balances" },
        { resource: "stock-movements", label: "Movements" }
    ]
}

export function InventoryWorkspace({ initialResource = "item-categories", section }: InventoryWorkspaceProps) {
    const tabs = section ? sectionTabs[section as keyof typeof sectionTabs] : undefined
    return (
        <RecordWorkspace
            module="inventory"
            title="Assets and inventory"
            initialResource={tabs?.[0]?.resource ?? initialResource}
            description="Manage equipment models, type-specific technical specifications, identification, individual assets, controlled lots, locations, conditions and immutable stock movement history."
            showNavigation={false}
            tabs={tabs}
        />
    )
}