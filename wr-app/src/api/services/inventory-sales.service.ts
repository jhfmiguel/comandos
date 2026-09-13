import { httpClient } from "api/http";
import type { 
    InventorySale, 
    SaleRequest, 
    SaleReturn,
    SaleReturnRequest,
    SalesPage, 
    StockOption 
} from "api/models/erp/sales";

const root = "/api/erp/sales";

export const inventorySalesService = {
    
    stock: async (organizationId: number, kind: string, search: string, page: number, signal: AbortSignal, unitId?: number) =>
        (await httpClient.get<SalesPage<StockOption>>(`${root}/stock`, { params: { organizationId, unitId, kind, search, page }, signal })).data,
    
    finalize: async (request: SaleRequest) => (await httpClient.post<InventorySale>(root, request)).data,
    returnItems: async (saleId: number, request: SaleReturnRequest) =>
        (await httpClient.post<SaleReturn>(`${root}/${saleId}/returns`, request)).data,
    
    list: async (organizationId: number, page: number, signal: AbortSignal, unitId?: number) =>
        (await httpClient.get<SalesPage<InventorySale>>(root, { params: { organizationId, unitId, page }, signal })).data
    
};
