import { AxiosResponse } from 'axios'

import { httpClient } from 'api/http'
import { User } from 'api/models/users'
import { Page }  from 'api/models/common/page'


const resourceURL: string = "/api/users"

export const useUserService = () => {

    const saveUser = async ( user: User ) : Promise<User> => {
        
        const response: AxiosResponse<User> = await httpClient.post<User>( resourceURL, user )
        return response.data

    }

    const updateUser = async ( user: User ) : Promise<void> => {

        const url: string = `${resourceURL}/${user.id}`
        await httpClient.put<User>( url, user)

    }

    const loadUser = async ( id: any ): Promise<User> => {
        
        const url: string = `${resourceURL}/${id}`
        const response: AxiosResponse<User> = await httpClient.get( url )
        return response.data

    }

     const deleteUser = async ( id: any ): Promise<void> => {
        
        const url: string = `${resourceURL}/${id}`
        await httpClient.delete( url )

    }

    const findUser = async ( 
        
        name: string = "", 
        cpf: string = "",
        page: number = 0,
        size: number = 0 

    ) : Promise< Page< User > > => {

        const url = `${resourceURL}?name=${name}&cpf=${cpf}&page=${page}&size=${size}`
        const response: AxiosResponse< Page< User > > = await httpClient.get(url)
        return response.data


    }


    return {

        saveUser,
        updateUser,
        loadUser,
        deleteUser,
        findUser
        
    }

}