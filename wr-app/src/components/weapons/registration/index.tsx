'use client';

import { useState, useEffect } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import * as yup from 'yup'

import { Layout, Input, Textarea, Button, Message } from 'components'
import { useWeaponService } from 'api/services'
import { Weapon } from 'api/models/weapons'
import { convertToBigDecimal, formatReal } from 'utils/money'
import { Alert } from 'components/common/message'


// --- YUP ---

const requiredField = "Required field."
const moreThanTen = "The value must be greater than 10 characters."
const moreThanZero = "The value must be greater than zero."

const validationSchema = yup.object().shape( {
    
    sku: yup
        .string()
        .trim()
        .required( requiredField ),
    
    price: yup
        .number()
        .required( requiredField )
        .moreThan( 0, moreThanZero ),
    
    name: yup
        .string()
        .trim()
        .required( requiredField ),

    description: yup
        .string()
        .trim()
        .required( requiredField )
        .min( 11, moreThanTen )

} )


// --- MESSAGE ERRORS INTERFACE

interface MessageErrors {
    sku?: string;
    price?: string;
    name?: string;
    description?: string
}

// ---


export const WeaponsRegistration: React.FC = () => {

    
    // --- CAPTURE THE ID FROM THE URL ---

    const router = useRouter()
    // Hook to read URL parameters
    const searchParams = useSearchParams() 
    
    // Captures the ID from the URL (?id=your-id)
    const queryId = searchParams.get('id')


    const service = useWeaponService()

    const [ message, setMessage ] = useState<Array<Alert>>([])
    const [ messageError, setMessageError ] = useState<MessageErrors>({})


    const [ id, setId ] = useState<string | undefined>('')
    const [ creationDate, setCreationDate ] = useState<string | undefined>('')
    
    const [ sku, setSku ] = useState<string>('')
    const [ price, setPrice ] = useState<string>('')
    const [ name, setName ] = useState<string>('')
    const [ description, setDescription ] = useState<string>('')


    // --- LOAD THE WEAPON DATA TO UPDATE ---

    useEffect( () => {

         if (queryId){

             service
                .loadWeapon( queryId )
                .then( weaponFound => {
                    setId( weaponFound.id )
                    setCreationDate( weaponFound.creationDate ?? '' )
                    setSku( weaponFound.sku ?? '' )
                    setPrice( formatReal( `${weaponFound.price}` ) )
                    setName( weaponFound.name ?? '' )
                    setDescription( weaponFound.description ?? '' )
                } )
            }
 
        }, [ queryId ] )

    
    // --- SUBMIT FUNCTION ---
    
    const submit = () => {
        const weapon: Weapon = {
            id,
            sku, 
            price: convertToBigDecimal( price ), 
            name, 
            description
        }

        validationSchema.validate( weapon ).then( obj => {

            setMessageError( {} );

            if( id ) {
    
                service
                .updateWeapon( weapon )
                .then( response => {
                    setMessage( [{
                        type: "success",
                        text: "Weapon successfully updated."
                    }] )
                })
    
            } else {
    
                service
                .saveWeapon( weapon )
                .then( weaponResponse => {
                    setId( weaponResponse.id )
                    setCreationDate( weaponResponse.creationDate )
                    setMessage( [{
                        type: "success",
                        text: "Weapon successfully registered."
                    }] )
                } )
    
            }

        } ).catch( err => {

            const field = err.path;
            const message = err.message;

            setMessageError( {
                [field] : message
            } )

        } )

    }

    // ---

    return (

        <Layout 
            title = "Weapons Registration"
            message = { message }
        >
            
        { id &&
        
            <div className = "columns">

                <Input 
                    label = "Code"
                    columnClasses = "is-half"
                    value= { id }
                    id = "inputId"
                    disabled   
                />

                <Input 
                    label = "Registration Date"
                    columnClasses = "is-half"
                    value = { creationDate } 
                    id = "inputRegistrationDate"
                    disabled   
                />
            </div>

        }

            <div className = "columns">

                <Input 
                    label = "SKU *"
                    columnClasses = "is-half"
                    onChange = { setSku }
                    value= {sku}
                    id = "inputSku"
                    placeholder = "Type the SKU"
                    error = { messageError.sku }         
                />

                <Input 
                    label = "Price *"
                    columnClasses = "is-half"
                    onChange = { setPrice }
                    value = { price } 
                    id = "inputPrice"
                    placeholder = "Type the price"
                    currency
                    maxLength={18}
                    error = { messageError.price }       
                />

            </div>

            <div className = "columns">

                <Input 
                    label = "Name *"
                    columnClasses = "is-full"
                    onChange = { setName }
                    value = { name }
                    id = "inputName"
                    placeholder = "Type the name"
                    error = { messageError.name }         
                />

            </div>

            <div className = "columns">

                <Textarea 
                    label = "Description"
                    columnClasses = "is-full"
                    onChange = { setDescription }
                    value = { description }
                    id = "textareaDescription"
                    placeholder = "Type the description"
                    error = { messageError.description }
                />

            </div>


            <div className = "field is-grouped">
                
                <Button
                    label={id ? "Update" : "Save"}
                    onClick = { submit }    
                />

                <Link href = "/queries/weapons">
                    <Button 
                        label = "Back"   
                    />
                </Link>
                
            </div>

        </Layout>

    )

}