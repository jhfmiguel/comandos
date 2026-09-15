export type ProcurementMethod = "NOT_REQUIRED" | "BIDDING" | "DIRECT_CONTRACTING";
export type BiddingModality = "PREGAO" | "CONCORRENCIA" | "CONCURSO" | "LEILAO" | "DIALOGO_COMPETITIVO";
export type DirectContractingType = "DISPENSA" | "INEXIGIBILIDADE";
export type PurchaseStatus =
    | "DRAFT"
    | "PLANNING"
    | "PROCUREMENT_IN_PROGRESS"
    | "AUTHORIZED"
    | "ORDERED"
    | "PARTIALLY_RECEIVED"
    | "RECEIVED"
    | "CANCELLED";

export interface CreatePurchaseItemRequest {
    itemModelId: number;
    quantity: number;
    unitPrice: number;
    discount: number;
    notes?: string;
}

export interface CreatePurchaseRequest {
    buyerOrganizationId: number;
    supplierOrganizationId?: number;
    purchaseNumber: string;
    purchaseDate: string;
    notes?: string;
    items: CreatePurchaseItemRequest[];
}

export interface CreateProcurementRequest {
    processNumber: string;
    objectDescription: string;
    justification?: string;
    procurementMethod: ProcurementMethod;
    biddingModality?: BiddingModality;
    directContractingType?: DirectContractingType;
    estimatedValue?: number;
    legalBasis?: string;
    supplierChoiceReason?: string;
    priceJustification?: string;
}

export interface PurchaseItemView {
    id: number;
    itemModelId: number;
    itemName: string;
    quantity: number;
    receivedQuantity: number;
    unitPrice: number;
    discount: number;
    total: number;
}

export interface ProcurementView {
    id: number;
    processNumber: string;
    procurementMethod: ProcurementMethod;
    biddingModality?: BiddingModality;
    directContractingType?: DirectContractingType;
    status: string;
    estimatedValue: number;
    legalBasis?: string;
}

export interface PurchaseView {
    id: number;
    buyerOrganizationId: number;
    buyerName: string;
    supplierOrganizationId?: number;
    supplierName?: string;
    purchaseNumber: string;
    purchaseDate: string;
    status: PurchaseStatus;
    subtotal: number;
    discount: number;
    freight: number;
    taxes: number;
    total: number;
    procurement?: ProcurementView;
    items: PurchaseItemView[];
}

export interface ItemModelOption {
    id: number;
    name: string;
    sku?: string;
    listPrice?: number;
}