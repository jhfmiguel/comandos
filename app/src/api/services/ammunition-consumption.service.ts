import { httpClient } from "api/http";
import type { AmmunitionConsumption, AmmunitionConsumptionPage, AmmunitionConsumptionRequest, AmmunitionStockOption } from "api/models/erp/ammunition-consumption";

const root = "/api/erp/ammunition-consumptions";
export const ammunitionConsumptionService = {
    stock: async (organizationId: number, unitId: number | undefined, search: string, page: number, signal: AbortSignal) =>
        (await httpClient.get<AmmunitionConsumptionPage<AmmunitionStockOption>>(`${root}/stock`, { params: { organizationId, unitId, search, page }, signal })).data,
    finalize: async (request: AmmunitionConsumptionRequest) => (await httpClient.post<AmmunitionConsumption>(root, request)).data,
    list: async (organizationId: number, unitId: number | undefined, page: number, signal: AbortSignal) =>
        (await httpClient.get<AmmunitionConsumptionPage<AmmunitionConsumption>>(root, { params: { organizationId, unitId, page }, signal })).data
};
