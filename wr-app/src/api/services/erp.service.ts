import { httpClient } from "api/http";
import type { ErpPage, ErpRecord, ErpResource, ErpValue } from "api/models/erp";

export type ErpModule = "core" | "inventory";

export function createErpService(module: ErpModule) {
    
    const root = `/api/erp/${module}`;
    const path = (resource: string) => resource.includes("/") ? `/api/erp/${resource}` : `${root}/${resource}`;
    
    return {

        catalog: async (signal?: AbortSignal) =>
            (await httpClient.get<ErpResource[]>(`${root}/catalog`, { signal })).data,
        
        list: async (
            resource: string,
            search = "",
            page = 0,
            signal?: AbortSignal,
            organizationId?: ErpValue,
            filters: Record<string, string> = {}
        ) =>
            (
                await httpClient.get<ErpPage>(
                    path(resource),
                    {
                        params: {
                            search,
                            page,
                            size: 10,
                            organizationId:
                                organizationId ||
                                undefined,
                            ...Object.fromEntries(
                                Object.entries(filters)
                                    .filter(([, value]) =>
                                        value.trim() !== ""
                                    )
                                    .map(([field, value]) => [
                                        `filter.${field}`,
                                        value.trim()
                                    ])
                            )
                        },
                        signal
                    }
                )
            ).data,
        
        save: async (resource: string, values: Record<string, ErpValue>, id?: number) =>
            id == null
                ? (await httpClient.post<ErpRecord>(path(resource), values)).data
                : (await httpClient.put<ErpRecord>(`${path(resource)}/${id}`, values)).data,
       
        remove: async (resource: string, record: ErpRecord) => {
            await httpClient.delete(`${path(resource)}/${record.id}`, { params: { version: record.version } });
        }
    };
}

export type ErpService = ReturnType<typeof createErpService>;
