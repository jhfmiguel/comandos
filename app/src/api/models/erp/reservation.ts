export interface ReservationStock{kind:"ASSET"|"LOT";stockId:number;code:string;modelName:string;sku:string;locationName:string;unitOfMeasure:string;available:string}export interface Reservation{ id:number;organizationId:number;organizationName:string;unitId:number|null;unitName:string|null;purpose:string;startsAt:string;endsAt:string;statusCode:string;statusName:string;createdOn:string;releasedAt:string|null;operatorLogin:string|null;items:{id:number;assetId:number|null;lotId:number|null;modelName:string;sku:string;stockCode:string;locationName:string;unitOfMeasure:string;quantity:string}[]}export interface ReservationPage<T>{content:T[];totalElements:number;page:number;size:number}
export interface ReservationRequest {
    requestId: string;
    organizationId: number;
    unitId?: number;
    purpose: string;
    startsAt: string;
    endsAt: string;
    items: { assetId?: number; balanceId?: number; quantity: string }[];
}
