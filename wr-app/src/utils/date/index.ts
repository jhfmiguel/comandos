import { FormatUtils } from "@4us-dev/utils"

const formatUtils = new FormatUtils()

/**
 * Formats a raw numerical string into a Brazilian Date mask (DD/MM/YYYY).
 * @param value The raw string input from the field
 * @returns A formatted date string
 */
export const formatDate = (value: string): string => {
    
    if (!value) return ''

    // Extracts only the numeric digits from the input
    const cleanData = formatUtils.formatOnlyIntegers(value).slice(0, 8)
    const size = cleanData.length

    if (size <= 2) {
        return cleanData;
    }
    if (size <= 4) {
        return cleanData.substring(0, 2) + '/' + cleanData.substring(2)
    }
    
    // Returns the full mask (DD/MM/YYYY) for lengths greater than 4 up to 8
    return cleanData.substring(0, 2) + '/' + cleanData.substring(2, 4) + '/' + cleanData.substring(4)
}


/**
 * Converts a Brazilian date string (DD/MM/YYYY) into an ISO date string (YYYY-MM-DD).
 * @param dateStr The formatted Brazilian date string (e.g., "16/12/1982")
 * @returns An ISO date string (e.g., "1982-12-16") or an empty string if invalid
 */
export const convertToIsoDate = (dateStr: string | undefined | null): string => {
    if (!dateStr) return '';
    
    // Splits the string by the slash character
    const parts = dateStr.split('/');
    
    // Checks if we have exactly day, month, and year parts
    if (parts.length !== 3) return dateStr;

    const [day, month, year] = parts;
    
    // Returns in the YYYY-MM-DD format expected by Java's LocalDate
    return `${year}-${month}-${day}`;
};

