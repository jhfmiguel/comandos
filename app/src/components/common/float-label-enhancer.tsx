"use client"

import { FloatLabelEnhancer as SharedFloatLabelEnhancer } from "@faria-miguel/ui/float-label-enhancer"

export function FloatLabelEnhancer() {
    return (
        <SharedFloatLabelEnhancer
            fieldSelectors={[
                ".field",
                ".registration-field",
                "[data-comandos-field]",
                ".comandos-field",
                "td",
                "th"
            ]}
            ignoreAttribute="data-comandos-no-float"
        />
    )
}
