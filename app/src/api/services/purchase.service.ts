import { httpClient } from "api/http";
import type {
    CreateProcurementRequest,
    CreatePurchaseRequest,
    PurchaseView
} from "api/models/erp/purchase";

const root = "/api/erp/purchases";

export const purchaseService = {
    create: async (request: CreatePurchaseRequest) =>
        (await httpClient.post<PurchaseView>(root, request)).data,

    list: async (organizationId: number, signal?: AbortSignal) =>
        (await httpClient.get<PurchaseView[]>(root, {
            params: { organizationId },
            signal
        })).data,

    get: async (id: number) =>
        (await httpClient.get<PurchaseView>(`${root}/${id}`)).data,

    configureProcurement: async (id: number, request: CreateProcurementRequest) =>
        (await httpClient.put<PurchaseView>(`${root}/${id}/procurement`, request)).data,

    updateProcurementStatus: async (id: number, status: string) =>
        (await httpClient.put<PurchaseView>(`${root}/${id}/procurement/status`, { status })).data,

    authorize: async (id: number) =>
        (await httpClient.post<PurchaseView>(`${root}/${id}/authorize`)).data,

    order: async (id: number) =>
        (await httpClient.post<PurchaseView>(`${root}/${id}/order`)).data,

    cancel: async (id: number) =>
        (await httpClient.post<PurchaseView>(`${root}/${id}/cancel`)).data
};