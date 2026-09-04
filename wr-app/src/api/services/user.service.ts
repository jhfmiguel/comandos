import type { AxiosResponse } from "axios";

import { httpClient } from "api/http";
import type { Page } from "api/models/common/page";
import type { User } from "api/models/users";


const resourceURL = "api/users";


export const useUserService = () => {
  

    const saveUser = async ( user: User ): Promise<User> => {
        
        const response: AxiosResponse< User > = await httpClient.post< User >( resourceURL, user );
        return response.data;

    };


    const updateUser = async ( user: User ): Promise<void> => {
        
        if (user.id === undefined) { throw new Error( "The user ID is required." ); }
        await httpClient.put< User >( `${resourceURL}/${user.id}`, user);
        
    };


    const loadUser = async ( id: string | number ): Promise< User > => {
        
        const response: AxiosResponse< User > = await httpClient.get< User >( `${resourceURL}/${id}` );
        return response.data;
    
    };


    const deleteUser = async ( id: string | number ): Promise<void> => {
        
        await httpClient.delete( `${resourceURL}/${id}` );
    
    };


    const findUser = async (
        
        name: string = "",
        cpf: string = "",
        page: number = 0,
        size: number = 20,
        birth: string = "",
        address: string = "",
        email: string = "",
        phone: string = ""
        
    ): Promise<Page<User>> => {
        
        const formattedBirth = /^\d{4}-\d{2}-\d{2}$/.test(birth)
            ? birth.split("-").reverse().join("/")
            : birth;

        const response: AxiosResponse<Page<User>> = await httpClient.get< Page< User >>( resourceURL,
            {
                params: 
                {
                    name,
                    cpf,
                    birth: formattedBirth,
                    address,
                    email,
                    phone,
                    page,
                    size
                }
            }
        );

        return response.data;
    };


    return {

        saveUser,
        updateUser,
        loadUser,
        deleteUser,
        findUser

    };
    

};