"use client"

import * as React from "react"
import {
    apiErrorMessage,
    type AsyncState
} from "../data"

export function useAsyncTask<T>() {
    const [state, setState] = React.useState<AsyncState<T>>({
        status: "idle",
        data: null,
        error: null
    })

    const run = React.useCallback(async (
        task: () => Promise<T>,
        fallbackError?: string
    ): Promise<T | null> => {
        setState({
            status: "loading",
            data: null,
            error: null
        })

        try {
            const data = await task()

            setState({
                status: "success",
                data,
                error: null
            })

            return data
        } catch (error) {
            setState({
                status: "error",
                data: null,
                error: apiErrorMessage(error, fallbackError)
            })

            return null
        }
    }, [])

    const reset = React.useCallback(() => {
        setState({
            status: "idle",
            data: null,
            error: null
        })
    }, [])

    return {
        ...state,
        loading: state.status === "loading",
        run,
        reset
    }
}
