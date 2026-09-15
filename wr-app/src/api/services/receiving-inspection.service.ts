import axios from "axios"

export interface ReceivingInspectionInput {
    inspectedAt?: string
    inspector?: string
    provisionalReceipt: boolean
    definitiveReceipt: boolean
    approved: boolean
    nonConformity?: string
    decisionNotes?: string
}

const baseUrl = "/api/erp/receiving-inspections"

export const receivingInspectionService = {
    list: async (receivingId?: number) => {
        const response = await axios.get(baseUrl, { params: { receivingId } })
        return response.data
    },

    create: async (receivingId: number, payload: ReceivingInspectionInput) => {
        const response = await axios.post(`${baseUrl}/${receivingId}`, payload)
        return response.data
    }
}