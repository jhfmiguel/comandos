"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import * as yup from "yup"

import { Layout, Input, InputMoney, Textarea, Button } from "components"
import { useWeaponService } from "api/services"
import { Weapon } from "api/models/weapons"
import { convertToBigDecimal, formatReal } from "utils/money"
import { Alert } from "components/common/message"


// --- YUP VALIDATION SCHEMA ---
const requiredField = 'Required field.'
const moreThanTen = 'The value must be greater than 10 characters.'
const moreThanZero = 'The value must be greater than zero.'

const validationSchema = yup.object().shape({
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
})


// --- MESSAGE ERRORS INTERFACE ---
interface MessageErrors {
    sku?: string;
    price?: string;
    name?: string;
    description?: string;
}


export const WeaponsRegistration: React.FC = () => {

    const router = useRouter()
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
                    
                    // 💡 Correção: Garante que o número puro vindo do banco (ex: 1500.00) 
                    // seja formatado com o padrão correto de string para alimentar o seu InputMoney
                    if (weaponFound.price) {
                        const priceAsStr = typeof weaponFound.price === 'number' 
                            ? (weaponFound.price * 100).toFixed(0) 
                            : weaponFound.price;
                        setPrice( formatReal( priceAsStr ) )
                    } else {
                        setPrice('')
                    }
                    
                    setName( weaponFound.name ?? '' )
                    setDescription( weaponFound.description ?? '' )
                } )
            }
        }, [ queryId ] )

    
    // --- SUBMIT FUNCTION ---
    const submit = () => {
        // Converte o texto digitado na máscara para um formato numérico válido (ex: 1250.50)
        const numericPrice = convertToBigDecimal( price )

        const weapon: Weapon = {
            id,
            sku, 
            price: numericPrice, 
            name, 
            description
        }

        // Valida o objeto montado já com o preço limpo convertido para número
        validationSchema.validate( weapon ).then( obj => {

            setMessageError( {} );

            if( id ) {
                service
                .updateWeapon( weapon )
                .then( response => {
                    setMessage( [{
                        type: 'success',
                        text: 'Weapon successfully updated.'
                    }] )
                })
            } else {
                service
                .saveWeapon( weapon )
                .then( weaponResponse => {
                    setId( weaponResponse.id )
                    setCreationDate( weaponResponse.creationDate )
                    setMessage( [{
                        type: 'success',
                        text: 'Weapon successfully registered.'
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

    return (
        <Layout 
            title = 'Weapons Registration'
            message = { message }
        >
            
        { id &&
            <div className = 'columns'>
                <Input 
                    label = 'Code'
                    columnClasses = 'is-half'
                    value= { id }
                    id = 'inputIdCode' // 💡 Corrigido ID estático duplicado
                    disabled   
                />

                <Input 
                    label = 'Registration Date'
                    columnClasses = 'is-half'
                    value = { creationDate } 
                    id = 'inputRegistrationDate'
                    disabled   
                />
            </div>
        }

            <div className = 'columns'>
                <Input 
                    label = 'SKU *'
                    columnClasses = 'is-half'
                    onChange = { e => setSku(e.target.value) }
                    value= {sku}
                    id = 'inputSku'
                    placeholder = 'Type the SKU'
                    error = { messageError.sku }         
                />

                <InputMoney 
                    label = 'Price *'
                    columnClasses = 'is-half'
                    onChange = { e => setPrice(e.target.value) }
                    value = { price } 
                    id = 'inputPrice'
                    placeholder = 'Type the price'
                    maxLength={18}
                    error = { messageError.price }  
                    currency={true}      
                />
            </div>

            <div className = 'columns'>
                <Input 
                    label = 'Name *'
                    columnClasses = 'is-full'
                    onChange = { e => setName(e.target.value) }
                    value = { name }
                    id = 'inputName'
                    placeholder = 'Type the name'
                    error = { messageError.name }         
                />
            </div>

            <div className = 'columns'>
                <Textarea 
                    label = 'Description'
                    columnClasses = 'is-full'
                    onChange = { e => setDescription(e.target.value) }
                    value = { description }
                    id = 'textareaDescription'
                    placeholder = 'Type the description'
                    error = { messageError.description }
                />
            </div>

            <div className = 'field is-grouped'>
                <Button
                    label = { id ? 'Update' : 'Save' }
                    onClick = { submit }
                    columnClasses = "is-success"   
                />

                <Link href = '/queries/weapons'>
                    <Button 
                        label = 'Back'
                        columnClasses = "is-text"    
                    />
                </Link>
            </div>
        </Layout>
    )
}
