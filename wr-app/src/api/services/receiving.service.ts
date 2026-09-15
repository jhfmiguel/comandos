import axios from "axios"
import type { ReceivingInput, ReceivingStatus } from "../models/erp/receiving"

const baseUrl = "/api/erp/receivings"

export const receivingService = {
    list: async (acquisitionId?: number) => {
        const response = await axios.get(baseUrl, { params: { acquisitionId } })
        return response.data
    },
    create: async (payload: ReceivingInput) => {
        const response = await axios.post(baseUrl, payload)
        return response.data
    },
    changeStatus: async (id: number, status: ReceivingStatus, notes?: string) => {
        const response = await axios.patch(`${baseUrl}/${id}/status`, { status, notes })
        return response.data
    }
}