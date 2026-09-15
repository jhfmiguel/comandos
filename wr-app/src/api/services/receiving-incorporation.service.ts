import axios from "axios"

export interface ReceivingIncorporationInput {
    receivingId: number
    receivingItemId: number
    receivingSerialId?: number
    stockLocationId: number
    assetCode?: string
    lotNumber?: string
    incorporatedBy?: string
    incorporatedAt?: string
    notes?: string
}

const baseUrl = "/api/erp/receiving-incorporations"

export const receivingIncorporationService = {
    list: async (receivingId?: number) => {
        const response = await axios.get(baseUrl, { params: { receivingId } })
        return response.data
    },

    create: async (payload: ReceivingIncorporationInput) => {
        const response = await axios.post(baseUrl, payload)
        return response.data
    }
}