export interface ContractRecord {
    id: { value: string };
    number: string;
    subject: string;
    status: "DRAFT" | "ACTIVE" | "SUSPENDED" | "EXPIRED" | "TERMINATED" | "CANCELLED";
    startsAt: string;
    endsAt?: string | null;
    value?: { amount: string; currency: { currencyCode: string } } | null;
    references: Record<string, string>;
}

export interface CreateContractRecord {
    id: string;
    organizationId: number;
    unitId?: number;
    counterpartyId?: string;
    number: string;
    subject: string;
    startsAt: string;
    endsAt?: string;
    value?: string;
    currency?: string;
    partyRole: "CUSTOMER" | "SUPPLIER" | "SERVICE_PROVIDER" | "PARTNER";
    references?: Record<string, string>;
}

export interface ContractAmendment {
    id: string;
    contractId: string;
    sequence: number;
    summary: string;
    effectiveOn: string;
    status: "DRAFT" | "APPROVED" | "APPLIED" | "REJECTED";
}

export interface ContractRenewal {
    id: string;
    contractId: string;
    requestedStartOn: string;
    requestedEndOn: string;
    status: "REQUESTED" | "APPROVED" | "REJECTED" | "COMPLETED";
}

export interface ContractAdjustment {
    id: string;
    contractId: string;
    indexCode: string;
    ratePercent: string;
    baseAmount: string;
    adjustedAmount: string;
    currency: string;
    effectiveOn: string;
    status: "REQUESTED" | "APPROVED" | "APPLIED" | "REJECTED";
}

export interface ContractObligation {
    id: string;
    contractId: string;
    code: string;
    description: string;
    dueOn: string;
    responsibleParty?: string | null;
    status: "OPEN" | "IN_PROGRESS" | "FULFILLED" | "WAIVED";
    evidenceReference?: string | null;
}

export interface ContractRisk {
    id: string;
    contractId: string;
    reference: string;
    category: string;
    description: string;
    likelihood: number;
    impact: number;
    status: string;
    mitigation?: string;
}

export interface LegalMatterRecord {
    id: string;
    reference: string;
    type: "JUDICIAL" | "ADMINISTRATIVE";
    title: string;
    status: string;
    filedOn?: string | null;
    closedOn?: string | null;
    outcome?: string | null;
    contractId?: string | null;
    metadata: Record<string, string>;
}

export interface LegalDeadlineRecord {
    id: string;
    matterId: string;
    code: string;
    kind: "FILING" | "HEARING" | "RESPONSE" | "APPEAL" | "PAYMENT" | "CONTRACTUAL" | "OTHER";
    description: string;
    dueOn: string;
    status: "OPEN" | "COMPLETED" | "CANCELLED";
    completionReference?: string | null;
}

export interface LawFirmRecord { id: string; code: string; name: string; registrationNumber?: string | null; email?: string | null; phone?: string | null; active: boolean; metadata: Record<string, string>; }
export interface PowerOfAttorneyRecord { id: string; reference: string; principalId: string; attorneyName: string; attorneyRegistration?: string | null; scope: string; issuedOn: string; expiresOn?: string | null; status: "ACTIVE" | "REVOKED" | "EXPIRED"; documentReference?: string | null; metadata: Record<string, string>; }
export interface LegalOpinionRecord { id: string; reference: string; matterId?: string | null; contractId?: string | null; title: string; requestedBy: string; author?: string | null; conclusion?: string | null; status: "REQUESTED" | "IN_REVIEW" | "ISSUED" | "WITHDRAWN"; privileged: boolean; metadata: Record<string, string>; }

export interface CreateLegalMatter {
    id: string;
    organizationId: number;
    unitId?: number;
    reference: string;
    type: "JUDICIAL" | "ADMINISTRATIVE";
    title: string;
    jurisdiction?: string;
    contractId?: string;
    lawFirmId?: string;
}
