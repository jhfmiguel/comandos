export const convertToBigDecimal = (value: string | number | undefined | null): number => {
    if (!value) return 0;
    
    // Ensures the input is treated as a string before applying replacements
    const stringValue = String(value).replace(/\D/g, '');
    
    // Converts the raw cents string into a proper decimal number (e.g., "150075" -> 1500.75)
    return Number(stringValue) / 100;
}


export const formatReal = (value: string | number | undefined | null): string => {
    
    if (!value) return '0,00';

    // Cleans the input to extract only digits and converts it to a standard cents value
    const cleanDigits = String(value).replace(/\D/g, '');
    const numericValue = Number(cleanDigits) / 100;

    // Uses the native browser/Node API to format currency perfectly according to pt-BR standards
    return new Intl.NumberFormat('pt-BR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(numericValue);

}

