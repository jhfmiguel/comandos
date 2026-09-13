import { createErpService } from "./erp.service";

export const coreService = createErpService("core");

export type {

    ErpValue as CoreValue, 
    ErpField as CoreField, 
    ErpResource as CoreResource,
    ErpRecord as CoreRecord, 
    ErpPage as CorePage
    
} from "api/models/erp";
