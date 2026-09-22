export type ErpValue = string | number | boolean | null;
export interface ErpField {
    name: string;
    label: string;
    type: "text" | "email" | "password" | "date" | "datetime-local" | "decimal" | "integer" | "boolean" | "choice" | "reference";
    required: boolean;
    reference: string | null;
    choices: string[];
    readOnly?: boolean;
    createOnly?: boolean;
}
export interface ErpResource {
    key: string;
    label: string;
    group: string;
    fields: ErpField[];
    readOnly?: boolean;
    actions?: string[];
}
export interface ErpRecord {
    id: number;
    version: number;
    label: string;
    referenceLabels: Record<string, string>;
    [key: string]: ErpValue | Record<string, string>;
}
export interface ErpPage {
    content: ErpRecord[];
    totalElements: number;
    page: number;
    size: number;
}
