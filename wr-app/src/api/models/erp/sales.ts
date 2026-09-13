export interface StockOption {
    kind: "ASSET" | "LOT";
    stockId: number;
    code: string;
    modelName: string;
    sku: string;
    locationName: string;
    unitOfMeasure: string;
    available: string;
    unitPrice: string;
}

export interface SaleRequest {
    requestId: string;
    organizationId: number;
    unitId?: number;
    buyerId: number;
    paymentMethod: string;
    items: { assetId?: number; balanceId?: number; quantity: string; expectedUnitPrice: string }[];
}

export interface InventorySaleLine {
    id: number;
    assetId: number | null;
    modelName: string;
    stockCode: string;
    locationName: string;
    unitOfMeasure: string;
    quantity: string;
    unitPrice: string;
    subtotal: string;
    movementId: number;
}

export interface SaleReturn {
    id: number; reasonId: number; reasonCode: string; reasonName: string; cancellation: boolean;
    notes: string; returnedAt: string; refundAmount: string; refundReference: string | null;
    operatorLogin: string | null;
    items: { id: number; saleItemId: number; stockCode: string; modelName: string; quantity: string; refundAmount: string; movementId: number }[];
}

export interface SaleReturnRequest {
    requestId: string;
    reasonId: number;
    notes: string;
    refundReference: string | null;
    cancellation: boolean;
    items: { saleItemId: number; quantity: string }[];
}

export interface InventorySale {
    id: number;
    organizationId: number;
    organizationName: string;
    unitId: number | null;
    unitName: string | null;
    buyerId: number;
    buyerName: string;
    paymentMethod: string;
    status: string;
    finalizedAt: string;
    finalizedById: number | null;
    finalizedByLogin: string | null;
    total: string;
    items: InventorySaleLine[];
    returns: SaleReturn[];
}

export interface SalesPage<T> { content: T[]; totalElements: number; page: number; size: number }
