export type ReceivingSourceType =
    | "ACQUISITION"
    | "TRANSFER"
    | "RETURN"
    | "LOAN"
    | "DONATION"
    | "MAINTENANCE_RETURN"
    | "OTHER"

export type ReceivingStatus =
    | "EXPECTED"
    | "PARTIALLY_RECEIVED"
    | "RECEIVED"
    | "UNDER_INSPECTION"
    | "PROVISIONALLY_ACCEPTED"
    | "PROVISIONALLY_PARTIALLY_ACCEPTED"
    | "DEFINITIVELY_ACCEPTED"
    | "DEFINITIVELY_PARTIALLY_ACCEPTED"
    | "PARTIALLY_REJECTED"
    | "REJECTED"
    | "CANCELLED"

export interface ReceivingItemInput {
    acquisitionItemId?: number
    itemModelId?: number
    expectedQuantity?: number
    receivedQuantity: number
    lotNumber?: string
    manufactureDate?: string
    expirationDate?: string
    conditionDescription?: string
    notes?: string
    serialNumbers?: string[]
}

export interface ReceivingInput {
    sourceType: ReceivingSourceType
    acquisitionId?: number
    receivingOrganizationId?: number
    receivingUnit?: string
    receivingLocation?: string
    deliveryDocumentNumber?: string
    invoiceNumber?: string
    receivedAt?: string
    receivedBy?: string
    physicalChecked: boolean
    documentsChecked: boolean
    notes?: string
    items: ReceivingItemInput[]
}

export interface ReceivingSerialView {
    id: number
    serialNumber: string
    manufacturerCode?: string
    assetCode?: string
    accepted: boolean
    rejectionReason?: string
}

export interface ReceivingItemView {
    id: number
    acquisitionItemId?: number
    itemModelId?: number
    itemName?: string
    acquiredQuantity?: number
    expectedQuantity?: number
    receivedQuantity: number
    acceptedQuantity: number
    rejectedQuantity: number
    acquisitionPendingQuantity?: number
    lotNumber?: string
    manufactureDate?: string
    expirationDate?: string
    conditionDescription?: string
    divergenceDescription?: string
    notes?: string
    serials: ReceivingSerialView[]
}

export interface ReceivingView {
    id: number
    sourceType: ReceivingSourceType
    acquisitionId?: number
    acquisitionNumber?: string
    receivingOrganizationId?: number
    receivingOrganizationName?: string
    receivingUnit?: string
    receivingLocation?: string
    deliveryDocumentNumber?: string
    invoiceNumber?: string
    receivedAt?: string
    receivedBy?: string
    physicalChecked: boolean
    documentsChecked: boolean
    status: ReceivingStatus
    notes?: string
    items: ReceivingItemView[]
}
