import React from "react"
import { useFormik } from "formik"

import { User } from "api/models/users"
import { Input, InputCPF, InputPhone, InputDate, Button } from "components"
// Imported the new dynamic functions instead of static values
import { getFormattedInitialValues, convertToIsoLocalDateTime, duplicateCPF, getValidationScheme } from "./validationSchema"

import { convertToIsoDate } from "utils/date"
import { formatOnlyNumbers } from "utils/numeric"

export interface UserFormProps {
    user: User;
    onSubmit: ( user: User ) => void | Promise<void>;
}

export const formikScheme: User = {
    creationDate:   '',
    name:           '',
    cpf:            '',
    birth:          '',
    address:        '',
    email:          '',
    phone:          ''
}

export const UserForm: React.FC< UserFormProps > = ({
    user,
    onSubmit
}) => {

    const formik = useFormik< User >( {
        // Passed the dynamic 'user' prop into the configuration functions
        initialValues: getFormattedInitialValues(user), 
        enableReinitialize: true, 
        validationSchema: getValidationScheme(user),
        validateOnChange: false, 
        validateOnBlur: true,    
        onSubmit: async (values, actions) => {
            const cleanCpf = formatOnlyNumbers(values.cpf);
            const isoBirth = convertToIsoDate(values.birth);
            const isoCreationDate = convertToIsoLocalDateTime(values.creationDate);

            const submittedValues = {
                ...values,
                cpf: cleanCpf,
                birth: isoBirth,
                creationDate: isoCreationDate as any
            };
            
            try {
                await Promise.resolve(onSubmit(submittedValues));
            } catch (error: any) {
                actions.setFieldError('cpf', duplicateCPF);
            } finally {
                actions.setSubmitting(false);
            }
        },
    } )


    return (

        <form onSubmit = { formik.handleSubmit }>

            { formik.values.id &&
            <div className='columns'>
                <Input
                    id = 'id'
                    name = 'id'
                    label = 'Id:'
                    columnClasses = 'is-half'
                    value = { formik.values.id }
                    autoComplete = 'off'
                    disabled
                />

                <Input
                    id = 'creationDate'
                    name = 'creationDate'
                    label = 'Creation Date:'
                    columnClasses = 'is-half'
                    value = { formik.values.creationDate }
                    autoComplete = 'off'
                    disabled
                />
            </div>
            }

            <div className='columns'>
                <Input
                    id = 'name'
                    name = 'name'
                    label = 'Name: *'
                    columnClasses = 'is-full'
                    onChange = { formik.handleChange }
                    value = { formik.values.name }
                    error = { formik.touched.name ? formik.errors.name : undefined } 
                    autoComplete = 'off'
                />
            </div>

            <div className='columns'>
                <InputCPF
                    id = 'cpf'
                    name = 'cpf'
                    label = 'CPF: *'
                    columnClasses = 'is-half'
                    onChange = { formik.handleChange } 
                    onBlur = { formik.handleBlur } 
                    value = { formik.values.cpf }
                    error = { formik.touched.cpf ? formik.errors.cpf : undefined } 
                    autoComplete = 'off'
                />

                <InputDate
                    id = 'birth'
                    name = 'birth'
                    label = 'Birth: *'
                    columnClasses = 'is-half'
                    onChange = { formik.handleChange }
                    value = { formik.values.birth }
                    error = { formik.touched.birth ? formik.errors.birth : undefined } 
                    autoComplete = 'off'
                />
            </div>

            <div className='columns'>
                <Input
                    id = 'address'
                    name = 'address'
                    label = 'Address: *'
                    columnClasses = 'is-full'
                    onChange = { formik.handleChange }
                    value = { formik.values.address }
                    error = { formik.touched.address ? formik.errors.address : undefined } 
                    autoComplete = 'off'
                />
            </div>

            <div className='columns'>
                <Input
                    id = 'email'
                    name = 'email'
                    label = 'E-mail: *'
                    columnClasses = 'is-half'
                    onChange = { formik.handleChange }
                    value = { formik.values.email }
                    error = { formik.touched.email ? formik.errors.email : undefined } 
                    autoComplete = 'off'
                />

                <InputPhone
                    id = 'phone'
                    name = 'phone'
                    label = 'Phone: *'
                    columnClasses = 'is-half'
                    onChange = { formik.handleChange }
                    value = { formik.values.phone }
                    error = { formik.touched.phone ? formik.errors.phone : undefined }
                    autoComplete = 'off'
                />
            </div>

            <div className = 'field is-grouped'>
                <div className = 'control is-link'>
                    <Button
                        label={formik.values.id ? 'Update' : 'Save'}
                        type='submit'
                        disabled={formik.isSubmitting} 
                        columnClasses={""}
                    />
                </div>
            </div>

        </form>
    )
}
