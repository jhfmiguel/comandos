import { User } from "../users" 
import { Weapon } from "../weapons" 

export interface SaleItem extends Weapon {
    quantity: number
}

export interface Sale {
    
    user?: User
    weapons?: Array<SaleItem>
    paymentMethod?: string
    total: number

}
