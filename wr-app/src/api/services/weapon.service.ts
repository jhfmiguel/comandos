import { httpClient } from 'api/http'
import { Weapon } from 'api/models/weapons'

import { AxiosResponse } from 'axios'

const resourceURL: string = "api/weapons"

export const useWeaponService = () => {

    const saveWeapon = async ( weapon: Weapon ) : Promise<Weapon> => {
        
        const response: AxiosResponse<Weapon> = await httpClient.post<Weapon>( resourceURL, weapon )
        return response.data

    }

    const updateWeapon = async ( weapon: Weapon ) : Promise<void> => {

        const url: string = `${resourceURL}/${weapon.id}`
        await httpClient.put<Weapon>( url, weapon)

    }

    const loadWeapon = async ( id: any ): Promise<Weapon> => {
        
        const url: string = `${resourceURL}/${id}`
        const response: AxiosResponse<Weapon> = await httpClient.get( url )
        return response.data

    }

     const deleteWeapon = async ( id: any ): Promise<void> => {
        
        const url: string = `${resourceURL}/${id}`
        await httpClient.delete( url )

    }

    return {

        saveWeapon,
        updateWeapon,
        loadWeapon,
        deleteWeapon
        
    }

}