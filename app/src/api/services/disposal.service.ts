import { httpClient } from "api/http";
import type { DisposalPage,DisposalProcess,DisposalRequest,DisposalStockOption } from "api/models/erp/disposal";
const root="/api/erp/disposals";
export const disposalService={
 stock:async(organizationId:number,unitId:number|undefined,kind:"ASSET"|"LOT",search:string,page:number,signal:AbortSignal)=>(await httpClient.get<DisposalPage<DisposalStockOption>>(`${root}/stock`,{params:{organizationId,unitId,kind,search,page},signal})).data,
 finalize:async(request:DisposalRequest)=>(await httpClient.post<DisposalProcess>(root,request)).data,
 list:async(organizationId:number,unitId:number|undefined,page:number,signal:AbortSignal)=>(await httpClient.get<DisposalPage<DisposalProcess>>(root,{params:{organizationId,unitId,page},signal})).data
};
