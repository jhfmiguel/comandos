import { httpClient } from "api/http";
import type { ContractAdjustment, ContractAmendment, ContractObligation, ContractRecord, ContractRenewal, ContractRisk, CreateContractRecord, CreateLegalMatter, LawFirmRecord, LegalDeadlineRecord, LegalMatterRecord, LegalOpinionRecord, PowerOfAttorneyRecord } from "api/models/erp/contracts";

const root = "/api/erp/contracts";
const scope = (organizationId: number, unitId?: number) => ({ organizationId, ...(unitId ? { unitId } : {}) });

export const contractsService = {
    list: async (organizationId: number, unitId?: number) =>
        (await httpClient.get<ContractRecord[]>(root, { params: scope(organizationId, unitId) })).data,
    create: async (contract: CreateContractRecord) => (await httpClient.post<ContractRecord>(root, contract)).data,
    activate: async (id: string, organizationId: number, unitId?: number) =>
        (await httpClient.post<ContractRecord>(`${root}/${encodeURIComponent(id)}/activate`, {}, { params: scope(organizationId, unitId) })).data,
    amendments: async (id: string, organizationId: number, unitId?: number) =>
        (await httpClient.get<ContractAmendment[]>(`${root}/${encodeURIComponent(id)}/amendments`, { params: scope(organizationId, unitId) })).data,
    requestAmendment: async (id: string, organizationId: number, unitId: number | undefined, body: { summary: string; effectiveOn: string; newEndDate?: string; newValue?: string; currency?: string }) =>
        (await httpClient.post<ContractAmendment>(`${root}/${encodeURIComponent(id)}/amendments`, body, { params: scope(organizationId, unitId) })).data,
    decideAmendment: async (id: string, organizationId: number, unitId: number | undefined, approve: boolean, reason?: string) =>
        (await httpClient.post<ContractAmendment>(`${root}/amendments/${encodeURIComponent(id)}/decision`, { approve, reason }, { params: scope(organizationId, unitId) })).data,
    adjustments: async (id: string, organizationId: number, unitId?: number) =>
        (await httpClient.get<ContractAdjustment[]>(`${root}/${encodeURIComponent(id)}/adjustments`, { params: scope(organizationId, unitId) })).data,
    requestAdjustment: async (id: string, organizationId: number, unitId: number | undefined, body: { indexCode: string; ratePercent: string; effectiveOn: string }) =>
        (await httpClient.post<ContractAdjustment>(`${root}/${encodeURIComponent(id)}/adjustments`, body, { params: scope(organizationId, unitId) })).data,
    decideAdjustment: async (id: string, organizationId: number, unitId: number | undefined, approve: boolean, reason?: string) =>
        (await httpClient.post<ContractAdjustment>(`${root}/adjustments/${encodeURIComponent(id)}/decision`, { approve, reason }, { params: scope(organizationId, unitId) })).data,
    applyDueAdjustments: async (id: string, organizationId: number, unitId?: number) =>
        (await httpClient.post<ContractAdjustment[]>(`${root}/${encodeURIComponent(id)}/adjustments/apply-due`, {}, { params: scope(organizationId, unitId) })).data,
    renewals: async (id: string, organizationId: number, unitId?: number) =>
        (await httpClient.get<ContractRenewal[]>(`${root}/${encodeURIComponent(id)}/renewals`, { params: scope(organizationId, unitId) })).data,
    requestRenewal: async (id: string, organizationId: number, unitId: number | undefined, body: { startOn: string; endOn: string }) =>
        (await httpClient.post<ContractRenewal>(`${root}/${encodeURIComponent(id)}/renewals`, body, { params: scope(organizationId, unitId) })).data,
    decideRenewal: async (id: string, organizationId: number, unitId: number | undefined, approve: boolean, reason?: string) =>
        (await httpClient.post<ContractRenewal>(`${root}/renewals/${encodeURIComponent(id)}/decision`, { approve, reason }, { params: scope(organizationId, unitId) })).data,
    completeRenewal: async (id: string, organizationId: number, unitId?: number) =>
        (await httpClient.post<ContractRenewal>(`${root}/renewals/${encodeURIComponent(id)}/complete`, {}, { params: scope(organizationId, unitId) })).data,
    obligations: async (id: string, organizationId: number, unitId?: number) =>
        (await httpClient.get<ContractObligation[]>(`${root}/${encodeURIComponent(id)}/obligations`, { params: scope(organizationId, unitId) })).data,
    saveObligation: async (id: string, organizationId: number, unitId: number | undefined, body: { id: string; code: string; description: string; dueOn: string; responsibleParty?: string; notes?: string }) =>
        (await httpClient.post<ContractObligation>(`${root}/${encodeURIComponent(id)}/obligations`, body, { params: scope(organizationId, unitId) })).data,
    updateObligation: async (id: string, organizationId: number, unitId: number | undefined, body: { status: ContractObligation["status"]; evidenceReference?: string; notes?: string }) =>
        (await httpClient.post<ContractObligation>(`${root}/obligations/${encodeURIComponent(id)}/status`, body, { params: scope(organizationId, unitId) })).data,
    risks: async (id: string, organizationId: number, unitId?: number) =>
        (await httpClient.get<ContractRisk[]>(`${root}/${encodeURIComponent(id)}/risk-assessments`, { params: scope(organizationId, unitId) })).data,
    assessRisk: async (id: string, organizationId: number, unitId: number | undefined, body: { id: string; reference: string; category: string; description: string; likelihood: number; impact: number; mitigation?: string }) =>
        (await httpClient.post<ContractRisk>(`${root}/${encodeURIComponent(id)}/risk-assessments`, body, { params: scope(organizationId, unitId) })).data,
    matters: async (organizationId: number, unitId?: number) =>
        (await httpClient.get<LegalMatterRecord[]>(`${root}/legal/matters`, { params: scope(organizationId, unitId) })).data,
    createMatter: async (matter: CreateLegalMatter) => (await httpClient.post<LegalMatterRecord>(`${root}/legal/matters`, matter)).data,
    closeMatter: async (id: string, organizationId: number, unitId: number | undefined, body: { closedOn: string; outcome: string }) =>
        (await httpClient.post<LegalMatterRecord>(`${root}/legal/matters/${encodeURIComponent(id)}/close`, body, { params: scope(organizationId, unitId) })).data,
    deadlines: async (id: string, organizationId: number, unitId?: number) =>
        (await httpClient.get<LegalDeadlineRecord[]>(`${root}/legal/matters/${encodeURIComponent(id)}/deadlines`, { params: scope(organizationId, unitId) })).data,
    addDeadline: async (id: string, organizationId: number, unitId: number | undefined, body: { id: string; code: string; kind: LegalDeadlineRecord["kind"]; description: string; dueOn: string; responsibleParty?: string }) =>
        (await httpClient.post<LegalDeadlineRecord>(`${root}/legal/matters/${encodeURIComponent(id)}/deadlines`, body, { params: scope(organizationId, unitId) })).data,
    completeDeadline: async (id: string, organizationId: number, unitId: number | undefined, evidenceReference: string) =>
        (await httpClient.post<LegalDeadlineRecord>(`${root}/legal/deadlines/${encodeURIComponent(id)}/complete`, { reference: evidenceReference }, { params: scope(organizationId, unitId) })).data,
    lawFirms: async (organizationId: number, unitId?: number) =>
        (await httpClient.get<LawFirmRecord[]>(`${root}/legal/law-firms`, { params: scope(organizationId, unitId) })).data,
    createLawFirm: async (body: { id: string; organizationId: number; unitId?: number; code: string; name: string; registrationNumber?: string; email?: string; phone?: string }) =>
        (await httpClient.post<LawFirmRecord>(`${root}/legal/law-firms`, body)).data,
    powersOfAttorney: async (organizationId: number, unitId?: number) =>
        (await httpClient.get<PowerOfAttorneyRecord[]>(`${root}/legal/powers-of-attorney`, { params: scope(organizationId, unitId) })).data,
    createPowerOfAttorney: async (body: { id: string; organizationId: number; unitId?: number; reference: string; principalId: string; attorneyName: string; attorneyRegistration?: string; scope: string; issuedOn: string; expiresOn?: string; documentReference?: string }) =>
        (await httpClient.post<PowerOfAttorneyRecord>(`${root}/legal/powers-of-attorney`, body)).data,
    revokePowerOfAttorney: async (id: string, organizationId: number, unitId: number | undefined, reason: string) =>
        (await httpClient.post<PowerOfAttorneyRecord>(`${root}/legal/powers-of-attorney/${encodeURIComponent(id)}/revoke`, { reason }, { params: scope(organizationId, unitId) })).data,
    opinions: async (organizationId: number, unitId?: number) =>
        (await httpClient.get<LegalOpinionRecord[]>(`${root}/legal/opinions`, { params: scope(organizationId, unitId) })).data,
    requestOpinion: async (body: { id: string; organizationId: number; unitId?: number; reference: string; matterId?: string; contractId?: string; title: string; privileged: boolean }) =>
        (await httpClient.post<LegalOpinionRecord>(`${root}/legal/opinions`, body)).data,
    issueOpinion: async (id: string, organizationId: number, unitId: number | undefined, body: { conclusion: string; documentReference?: string }) =>
        (await httpClient.post<LegalOpinionRecord>(`${root}/legal/opinions/${encodeURIComponent(id)}/issue`, body, { params: scope(organizationId, unitId) })).data
};
