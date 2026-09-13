export interface AmmunitionStockOption {
    balanceId: number; lotId: number; sku: string; modelName: string; lotNumber: string; locationName: string;
    unitOfMeasure: string; available: number; validUntil: string | null;
}
export interface AmmunitionConsumptionItem extends AmmunitionStockOption {
    id: number; quantity: number; result: string; movementId: number;
}
export interface AmmunitionConsumption {
    id: number; organizationId: number; organizationName: string; unitId: number | null; unitName: string | null;
    responsibleId: number; responsibleName: string; authorizerId: number; authorizerName: string; purpose: string;
    status: string; consumedAt: string; finalizedById: number | null; finalizedByLogin: string | null;
    items: AmmunitionConsumptionItem[];
}
export interface AmmunitionConsumptionRequest {
    requestId: string; organizationId: number; unitId?: number; responsibleId: number; authorizerId: number;
    purpose: string; items: { balanceId: number; quantity: number; result: string }[];
}
export interface AmmunitionConsumptionPage<T> { content: T[]; totalElements: number; page: number; size: number }
