import axios from "axios"

export interface ReceivingSerialDecision {
    serialId: number
    accepted: boolean
    rejectionReason?: string
}

export interface ReceivingItemDecision {
    receivingItemId: number
    acceptedQuantity: number
    rejectedQuantity: number
    divergenceDescription?: string
    serials?: ReceivingSerialDecision[]
}

export interface ReceivingInspectionInput {
    inspectedAt?: string
    inspector?: string
    provisionalReceipt: boolean
    definitiveReceipt: boolean
    approved: boolean
    nonConformity?: string
    decisionNotes?: string
    items: ReceivingItemDecision[]
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
