import { httpClient } from "api/http";
import type { CreateCommercialOrder, CommercialSalesDocument, CommercialSalesInvoice } from "api/models/erp/commercial-sales";

const root = "/api/erp/commercial-sales/orders";

export const commercialSalesService = {
    create: async (order: CreateCommercialOrder) => (await httpClient.post<CommercialSalesDocument>(root, order)).data,
    list: async (organizationId: number, unitId?: number) =>
        (await httpClient.get<CommercialSalesDocument[]>(root, { params: { organizationId, unitId } })).data,
    get: async (id: string) => (await httpClient.get<CommercialSalesDocument>(`${root}/${encodeURIComponent(id)}`)).data,
    submit: async (id: string) => (await httpClient.post<CommercialSalesDocument>(`${root}/${encodeURIComponent(id)}/submit`)).data,
    approve: async (id: string) => (await httpClient.post<CommercialSalesDocument>(`${root}/${encodeURIComponent(id)}/approve`)).data,
    confirm: async (id: string) => (await httpClient.post<CommercialSalesDocument>(`${root}/${encodeURIComponent(id)}/confirm`)).data,
    invoice: async (id: string, issuedOn: string, dueOn: string) =>
        (await httpClient.post<CommercialSalesInvoice>(`${root}/${encodeURIComponent(id)}/invoice`, { issuedOn, dueOn })).data,
    deliver: async (id: string, reference: string, lines: Array<{ itemId: string; quantity: string }>) =>
        (await httpClient.post(`${root}/${encodeURIComponent(id)}/deliveries`, { reference, lines, metadata: {} })).data,
    cancel: async (id: string, reason: string) =>
        (await httpClient.post<CommercialSalesDocument>(`${root}/${encodeURIComponent(id)}/cancel`, { reason })).data
};
