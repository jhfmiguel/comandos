/**
 * Strips all non-numeric characters from a string.
 * @param value The raw string input (e.g., '123abc45')
 * @returns A string containing only digits (e.g., '12345')
 */
export const formatOnlyNumbers = (value: string | undefined | null): string => {
    if (!value) return ''
    return value.replace(/\D/g, '')
};

/**
 * Validates a Brazilian CPF using the official verification digit algorithm.
 * @param cpf The raw or masked CPF string
 * @returns true if the CPF is mathematically valid, false otherwise
 */
export const validateCPF = (cpf: string | undefined | null): boolean => {
    if (!cpf) return false

    // Strips all formatting characters
    const cleanCPF = cpf.replace(/\D/g, '')

    // CPFs must have exactly 11 digits and cannot be a sequence of identical numbers
    if (cleanCPF.length !== 11 || /^(\d)\1+$/.test(cleanCPF)) {
        return false
    }

    // Validates the first verification digit
    let sum = 0
    for (let i = 1; i <= 9; i++) {
        sum += parseInt(cleanCPF.substring(i - 1, i)) * (11 - i)
    }
    let remainder = (sum * 10) % 11
    if (remainder === 10 || remainder === 11) remainder = 0
    if (remainder !== parseInt(cleanCPF.substring(9, 10))) return false

    // Validates the second verification digit
    sum = 0
    for (let i = 1; i <= 10; i++) {
        sum += parseInt(cleanCPF.substring(i - 1, i)) * (12 - i)
    }
    remainder = (sum * 10) % 11
    if (remainder === 10 || remainder === 11) remainder = 0
    if (remainder !== parseInt(cleanCPF.substring(10, 11))) return false

    return true
}
