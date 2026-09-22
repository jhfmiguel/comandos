import axios from "axios"

export interface ReceivingIncorporationInput {
    receivingId: number
    receivingItemId: number
    receivingSerialId?: number
    stockLocationId: number
    assetCode?: string
    lotNumber?: string
    quantity?: number
    incorporationValue?: number
    initialCondition?: string
    incorporatedBy?: string
    incorporatedAt?: string
    notes?: string
}

export interface ReceivingIncorporationView {
    id: number
    receivingId: number
    receivingItemId: number
    receivingSerialId?: number
    serialNumber?: string
    stockLocationId: number
    assetCode?: string
    lotNumber?: string
    quantity: number
    incorporationValue: number
    initialCondition: string
    inventoryResource: "assets" | "lots"
    inventoryRecordId: number
    incorporatedBy?: string
    incorporatedAt: string
    notes?: string
}

const baseUrl = "/api/erp/receiving-incorporations"

export const receivingIncorporationService = {
    list: async (receivingId?: number) => {
        const response = await axios.get<ReceivingIncorporationView[]>(baseUrl, { params: { receivingId } })
        return response.data
    },

    create: async (payload: ReceivingIncorporationInput) => {
        const response = await axios.post<ReceivingIncorporationView>(baseUrl, payload)
        return response.data
    }
}
