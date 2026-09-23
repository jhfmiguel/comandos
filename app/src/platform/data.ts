import axios from "axios"

export interface PageResult<T> {
    content: T[]
    totalElements: number
    size: number
    number?: number
    totalPages?: number
}

export type AsyncStatus = "idle" | "loading" | "success" | "error"

export interface AsyncState<T> {
    status: AsyncStatus
    data: T | null
    error: string | null
}

export const idleState = <T>(): AsyncState<T> => ({
    status: "idle",
    data: null,
    error: null
})

export function apiErrorMessage(
    error: unknown,
    fallback = "Unable to complete the request. Check the API connection and try again."
): string {
    if (!axios.isAxiosError(error)) return fallback

    const data = error.response?.data

    if (typeof data?.detail === "string" && data.detail.trim()) {
        return data.detail
    }

    if (typeof data?.message === "string" && data.message.trim()) {
        return data.message
    }

    if (typeof error.message === "string" && error.message.trim()) {
        return error.message
    }

    return fallback
}

export function pageCount(totalElements: number, size: number): number {
    if (size <= 0) return 1
    return Math.max(Math.ceil(totalElements / size), 1)
}
