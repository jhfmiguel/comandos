"use client";
import * as React from "react";
import clsx from "clsx";
export const Textarea = React.forwardRef<HTMLTextAreaElement, React.TextareaHTMLAttributes<HTMLTextAreaElement>>(function Textarea({className,...props}, ref) {
  return <textarea ref={ref} className={clsx("comandos-input", className)} {...props} />;
});
