import { httpClient } from "api/http";
import type { Donation,DonationPage,DonationRequest,DonationStockOption } from "api/models/erp/donation";
const root="/api/erp/donations";
export const donationService={
 stock:async(organizationId:number,unitId:number|undefined,kind:"ASSET"|"LOT",search:string,page:number,signal:AbortSignal)=>(await httpClient.get<DonationPage<DonationStockOption>>(`${root}/stock`,{params:{organizationId,unitId,kind,search,page},signal})).data,
 finalize:async(request:DonationRequest)=>(await httpClient.post<Donation>(root,request)).data,
 list:async(organizationId:number,unitId:number|undefined,page:number,signal:AbortSignal)=>(await httpClient.get<DonationPage<Donation>>(root,{params:{organizationId,unitId,page},signal})).data
};
