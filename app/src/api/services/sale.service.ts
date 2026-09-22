import { httpClient } from "api/http"
import { Sale } from "../models/sales"

const resourceURL = "/api/sales"

export const useSaleService = () => {

    const finalizeSale = async (sale: Sale) : Promise<void> => {
        await httpClient.post< Sale >( resourceURL, sale );
    }

    return finalizeSale

}
