export interface CustodyStockOption {
    assetId: number; assetCode: string; serialNumber: string | null; modelName: string; locationName: string;
}
export interface CustodyEquipmentSetOption { equipmentSetId: number; code: string; name: string; componentCount: number }
export interface CustodyItem {
    id: number; assetId: number | null; balanceId: number | null; equipmentSetId: number | null;
    equipmentSetCode: string | null; equipmentSetName: string | null; componentRole: string | null;
    assetCode: string; serialNumber: string | null; modelName: string; locationName: string;
    quantity: string; issueMovementId: number; returnedAt: string | null;
    returnConditionTypeId: number | null; returnConditionName: string | null; returnBlocksAvailability: boolean | null;
    inspectionNotes: string | null; returnInspectionId: number | null; maintenanceWorkOrderId: number | null;
}
export interface CustodyReturn {
    id: number; returnedAt: string; returnedById: number | null; returnedByLogin: string | null; itemIds: number[];
}
export interface Custody {
    id: number; organizationId: number; organizationName: string; unitId: number | null; unitName: string | null;
    recipientId: number; recipientName: string; authorizerId: number; authorizerName: string; purpose: string;
    status: string; deliveredAt: string; dueAt: string | null; completedAt: string | null;
    issuedById: number | null; issuedByLogin: string | null; items: CustodyItem[]; returns: CustodyReturn[];
}
export interface CustodyIssueRequest {
    requestId: string; organizationId: number; unitId?: number; recipientId: number; authorizerId: number;
    purpose: string; dueAt?: string; assetIds: number[]; equipmentSetIds: number[];
}
export interface CustodyPage<T> { content: T[]; totalElements: number; page: number; size: number }
