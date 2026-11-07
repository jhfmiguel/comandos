export type CommercialDocumentStatus =
    | "DRAFT" | "SUBMITTED" | "APPROVED" | "REJECTED" | "CONFIRMED"
    | "INVOICED" | "PARTIALLY_DELIVERED" | "DELIVERED" | "CANCELLED";

export interface CommercialSalesLine {
    itemId: string;
    description: string;
    quantity: string;
    unitPrice: string;
    discount: string;
}

export interface CommercialSalesDocument {
    id: string;
    tenant: string;
    reference: string;
    customerId: string;
    type: "ORDER";
    status: CommercialDocumentStatus;
    lines: CommercialSalesLine[];
    currency: string;
    createdAt: string;
    updatedAt: string;
    actorId: string | null;
    rejectionReason: string | null;
    metadata: Record<string, string>;
    total: number;
}

export interface CreateCommercialOrder {
    reference: string;
    customerId: string;
    organizationId: number;
    unitId?: number;
    buyerId: number;
    paymentMethod: string;
    processNumber: string;
    legalBasis: string;
    documentReference: string;
    lines: Array<{ itemId: string; description: string; quantity: string; unitPrice: string }>;
}

export interface CommercialSalesInvoice {
    id: string;
    tenant: string;
    salesDocumentId: string;
    customerId: string;
    issuedOn: string;
    dueOn: string;
    amount: number;
    currency: string;
    status: string;
}

