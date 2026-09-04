import { User } from "../users" 
import { Weapon } from "../weapons" 

export interface Sale {
    
    user?: User
    weapons?: Array<Weapon>
    paymentMethod?: string
    total: number

}