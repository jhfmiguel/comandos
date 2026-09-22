export interface TransferStockOption {
    kind: "ASSET" | "LOT";
    stockId: number;
    code: string;
    modelName: string;
    sku: string;
    locationName: string;
    unitOfMeasure: string;
    available: string;
}

export interface TransferLine {
    id: number;
    assetId: number | null;
    lotId: number | null;
    modelName: string;
    sku: string;
    stockCode: string;
    sourceLocationName: string;
    destinationLocationName: string;
    unitOfMeasure: string;
    quantity: string;
    outMovementId: number;
    inMovementId: number;
}

export interface InventoryTransfer {
    id: number;
    organizationId: number;
    organizationName: string;
    sourceUnitId: number;
    sourceUnitName: string;
    destinationUnitId: number;
    destinationUnitName: string;
    destinationLocationId: number;
    destinationLocationName: string;
    purpose: string;
    status: string;
    sentAt: string;
    finalizedById: number | null;
    finalizedByLogin: string | null;
    approvedById: number | null;
    approvedByLogin: string | null;
    approvedAt: string | null;
    rejectedById: number | null;
    rejectedByLogin: string | null;
    rejectedAt: string | null;
    rejectionReason: string | null;
    items: TransferLine[];
}

export interface TransferRequest {
    requestId: string;
    organizationId: number;
    sourceUnitId: number;
    destinationUnitId: number;
    destinationLocationId: number;
    purpose: string;
    items: { assetId?: number; balanceId?: number; quantity: number }[];
}

export interface TransferPage<T> {
    content: T[];
    totalElements: number;
    page: number;
    size: number;
}
