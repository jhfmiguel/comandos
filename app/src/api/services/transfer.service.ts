import { httpClient } from "api/http";
import type { InventoryTransfer, TransferPage, TransferRequest, TransferStockOption } from "api/models/erp/transfer";

const root = "/api/erp/transfers";

export const transferService = {
    stock: async (organizationId: number, sourceUnitId: number, kind: "ASSET" | "LOT", search: string,
        page: number, signal: AbortSignal) => (await httpClient.get<TransferPage<TransferStockOption>>(`${root}/stock`, {
            params: { organizationId, sourceUnitId, kind, search, page }, signal
        })).data,
    finalize: async (request: TransferRequest) => (await httpClient.post<InventoryTransfer>(root, request)).data,
    accept: async (id: number) => (await httpClient.post<InventoryTransfer>(`${root}/${id}/accept`, { requestId: crypto.randomUUID() })).data,
    reject: async (id: number, reason: string) => (await httpClient.post<InventoryTransfer>(`${root}/${id}/reject`, { requestId: crypto.randomUUID(), reason })).data,
    list: async (organizationId: number, unitId: number | undefined, page: number, signal: AbortSignal) =>
        (await httpClient.get<TransferPage<InventoryTransfer>>(root, { params: { organizationId, unitId, page }, signal })).data
};
