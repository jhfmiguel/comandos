import axios from "axios"
import * as Yup from "yup"

import { formatOnlyNumbers, validateCPF } from "utils/numeric"
import { User } from "api/models/users"
import { formikScheme } from "./form"

// --- INTERFACES ---
// Defines the paginated response returned by the API
export interface Page<T> {
    content         : Array<T>
    first           : number
    size            : number
    number          : number
    totalElements   : number
}

// Static error message configurations
const requiredField = 'Required field.'
const invalidCPF    = 'Invalid CPF.'
const invalidDate   = 'Invalid date.'
const invalidEmail  = 'Invalid e-mail.'
const invalidPhone  = 'Invalid Phone.'
export const duplicateCPF  = 'This CPF is already registered in the system.'

const yupInitialValidation = Yup.string().trim().required(requiredField)

/**
 * Formats incoming ISO timestamp into a readable text date time representation (dd/mm/yyyy hh:mm)
 */
export const formatIncomingCreationDateTime = (dateStr: string | undefined | null): string => {
    if (!dateStr || dateStr.trim() === '') return '';
    if (dateStr.includes('/') && dateStr.includes(':')) return dateStr;
    
    const parts = dateStr.split('T');
    if (parts.length !== 2) return dateStr;

    const rawDate = parts[0];
    let rawTime = parts[1];

    if (rawTime.includes('.')) {
        rawTime = rawTime.split('.')[0];
    }

    const dateParts = rawDate.split('-');
    if (dateParts.length !== 3) return dateStr;
    const [year, month, day] = dateParts;

    return `${day}/${month}/${year} ${rawTime}`;
};

/**
 * Formats incoming ISO string dates into readable display patterns (dd/mm/yyyy)
 */
export const formatIncomingBirthDate = (dateStr: string | undefined | null): string => {
    if (!dateStr || dateStr.trim() === '') return '';
    if (dateStr.includes('/')) return dateStr;
    
    const parts = dateStr.split('-');
    if (parts.length !== 3) return dateStr;
    
    const [year, month, day] = parts;
    return `${day}/${month}/${year}`;
};

/**
 * Converts user readable date text formats back to database system ISO configurations
 */
export const convertToIsoLocalDateTime = (dateTimeStr: string | undefined | null): string | null => {
    if (!dateTimeStr || dateTimeStr.trim() === '') return null;
    if (dateTimeStr.includes('T')) return dateTimeStr;

    const parts = dateTimeStr.split(' ');
    if (parts.length !== 2) return dateTimeStr;

    const [datePart, timePart] = parts;
    const dateParts = datePart.split('/');
    if (dateParts.length !== 3) return dateTimeStr;

    const [day, month, year] = dateParts;
    return `${year}-${month}-${day}T${timePart}`;
};

/**
 * Generates initial form values by formatting the dynamic user profile data
 */
export const getFormattedInitialValues = (user?: User): User => {
    return {
        ...formikScheme,
        ...user,
        birth: formatIncomingBirthDate(user?.birth),
        creationDate: formatIncomingCreationDateTime(user?.creationDate) 
    };
};

/**
 * Generates the validation schema using the user record context to control duplication rule flows
 */
export const getValidationScheme = (user?: User) => {
    return Yup.object().shape({
        name:       yupInitialValidation,
        cpf:        yupInitialValidation 
                        .max( 14, invalidCPF )
                        .test('cpf-matematico', invalidCPF, function(value) {
                            if (!value || value.trim() === '') return true;
                            return validateCPF(value);
                        })
                        .test('cpf-duplicado-db', duplicateCPF, async function(value) {
                            if (!value || value.trim() === '' || !validateCPF(value)) return true;
                            
                            // If the user already has an ID, skip database duplication checks (Update Mode)
                            if (user?.id) return true; 

                            try {
                                const cleanCpf = formatOnlyNumbers(value);
                                
                                // FIXED: Tipado para receber a estrutura paginada Page<User>
                                const response = await axios.get<Page<User>>(
                                    `http://localhost:8080/api/users`,
                                    {
                                        params: {
                                            cpf: cleanCpf,
                                            page: 0,
                                            size: 1
                                        }
                                    }
                                );
                                
                                // FIXED: Extrai o array de registros de dentro da propriedade content
                                const users = response.data?.content || [];
                                
                                const isDuplicated = users.some(u => formatOnlyNumbers(u.cpf) === cleanCpf);
                                if (isDuplicated) {
                                    return false; 
                                }
                            } catch (err) {
                                console.error("Database duplication check failed:", err);
                            }
                            return true;
                        }),
        birth:      yupInitialValidation.length( 10, invalidDate ), 
        address:    yupInitialValidation.max( 255 ), 
        email:      yupInitialValidation.email( invalidEmail ),
        phone:      yupInitialValidation
                        .min( 14, invalidPhone )
                        .max( 15, invalidPhone ), 
    });
};

