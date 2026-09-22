import { httpClient } from "api/http"
import type {
    ReceivingInput,
    ReceivingStatus,
    ReceivingView
} from "../models/erp/receiving"

const baseUrl = "/api/erp/receivings"

export const receivingService = {
    list: async (acquisitionId?: number) => {
        const response = await httpClient.get<ReceivingView[]>(baseUrl, {
            params: { acquisitionId }
        })
        return response.data
    },

    get: async (id: number) => {
        const response = await httpClient.get<ReceivingView>(`${baseUrl}/${id}`)
        return response.data
    },

    create: async (payload: ReceivingInput) => {
        const response = await httpClient.post<ReceivingView>(baseUrl, payload)
        return response.data
    },

    changeStatus: async (id: number, status: ReceivingStatus, notes?: string) => {
        const response = await httpClient.patch<ReceivingView>(
            `${baseUrl}/${id}/status`,
            { status, notes }
        )
        return response.data
    }
}
