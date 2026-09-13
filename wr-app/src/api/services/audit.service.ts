import { httpClient } from "api/http";

export interface AuditSummary {
    id: number;
    occurredAt: string;
    actorId: number | null;
    actorLogin: string | null;
    actorType: string;
    resource: string;
    recordId: number;
    action: string;
}
export interface AuditPage { content: AuditSummary[]; totalElements: number; page: number; size: number }
export interface AuditDetail { event: AuditSummary; before: unknown; after: unknown }
export interface AuditFilters { resource: string; recordId: string; action: string; actor: string; from: string; until: string }

export const auditService = {
    list: async (filters: AuditFilters, page: number, signal: AbortSignal) =>
        (await httpClient.get<AuditPage>("/api/erp/audit", { signal, params: {
            resource: filters.resource || undefined, recordId: filters.recordId || undefined,
            action: filters.action || undefined, actor: filters.actor || undefined,
            from: filters.from ? new Date(filters.from).toISOString() : undefined,
            until: filters.until ? new Date(filters.until).toISOString() : undefined, page
        } })).data,
    get: async (id: number, signal: AbortSignal) => (await httpClient.get<AuditDetail>(`/api/erp/audit/${id}`, { signal })).data
};
