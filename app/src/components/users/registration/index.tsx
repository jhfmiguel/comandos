"use client"

import { useState } from "react"
import axios from "axios"
import { Layout } from "components"
import { UserForm } from "./form"
import { User } from "api/models/users"
import { useUserService } from "api/services"
import { Alert } from "components/common/message"

// NEW: Local helper function to re-apply the Brazilian CPF mask upon server response layout initialization
const applyCpfMask = (value: string | undefined | null): string => {
    if (!value) return '';
    const clean = value.replace(/\D/g, '');
    if (clean.length !== 11) return value;
    return clean.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
};

export const UserRegistration: React.FC = () => {

    const service = useUserService()
    
    const [ user, setUser ] = useState< User >({} as User)
    const [ message, setMessage ] = useState< Array< Alert > >([])

    const handleSubmit = async ( user: User ) => {
        try {
            if ( user.id ) {
                await service.updateUser( user )
                setMessage( [{ type: 'success', text: 'User successfully updated' }] )
            } else {
                const savedUser = await service.saveUser( user )
                
                // FIXED: Re-applies formatting layout to the raw database string before binding to Formik re-initializers
                if (savedUser) {
                    savedUser.cpf = applyCpfMask(savedUser.cpf);
                }

                setUser( savedUser ) 
                setMessage( [{ type: 'success', text: 'User successfully registered' }] )
            }
        } catch (error: unknown) {
            console.error("[DEBUG] Network server transmission error caught:", error);
            
            const status = axios.isAxiosError(error) ? error.response?.status : undefined;
            if ( status === 500 || status === 409 || status === 400 ) {
                throw new Error("CPF_DUPLICATED")
            }

            setMessage( [{ type: 'danger', text: 'An error occurred while saving the user.' }] )
            throw error
        }
    }

    return (
       <Layout title='Users' message={ message }>
            <UserForm 
                user = { user } 
                onSubmit = { handleSubmit } 
            />
       </Layout>
    )
}
