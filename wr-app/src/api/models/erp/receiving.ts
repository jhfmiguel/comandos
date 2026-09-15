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
    | "DEFINITIVELY_ACCEPTED"
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
    notes?: string
    items: ReceivingItemInput[]
}