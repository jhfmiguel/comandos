import { httpClient } from "api/http";
import type { Custody, CustodyEquipmentSetOption, CustodyIssueRequest, CustodyPage, CustodyStockOption } from "api/models/erp/custody";

const root = "/api/erp/custodies";
export const custodyService = {
    stock: async (organizationId: number, unitId: number | undefined, search: string, page: number, signal: AbortSignal) =>
        (await httpClient.get<CustodyPage<CustodyStockOption>>(`${root}/stock`, { params: { organizationId, unitId, search, page }, signal })).data,
    equipmentSets: async (organizationId: number, unitId: number | undefined, search: string, page: number, signal: AbortSignal) =>
        (await httpClient.get<CustodyPage<CustodyEquipmentSetOption>>(`${root}/equipment-sets`, { params: { organizationId, unitId, search, page }, signal })).data,
    issue: async (request: CustodyIssueRequest) => (await httpClient.post<Custody>(root, request)).data,
    list: async (organizationId: number, unitId: number | undefined, page: number, signal: AbortSignal) =>
        (await httpClient.get<CustodyPage<Custody>>(root, { params: { organizationId, unitId, page }, signal })).data,
    returnItems: async (custodyId: number, itemIds: number[], requestId: string, conditionTypeId: number, inspectionNotes: string) =>
        (await httpClient.post<Custody>(`${root}/${custodyId}/returns`, { requestId, itemIds, conditionTypeId,
            inspectionNotes: inspectionNotes.trim() || undefined })).data
};
