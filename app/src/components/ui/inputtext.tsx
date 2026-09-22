"use client";

import * as React from "react";
import clsx from "clsx";

export interface InputTextProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, "size"> {
  size?: "small" | "large";
  fluid?: boolean;
  invalid?: boolean;
}

export const InputText = React.forwardRef<HTMLInputElement, InputTextProps>(function InputText(
  { size, fluid, invalid, className, ...props },
  ref
) {
  return <input ref={ref} className={clsx("comandos-input", size && `comandos-input-${size}`, fluid && "w-full", invalid && "comandos-input-invalid", className)} {...props} />;
});
