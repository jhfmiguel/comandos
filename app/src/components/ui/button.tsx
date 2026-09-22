"use client";

import * as React from "react";
import clsx from "clsx";

export interface ButtonProps extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, "size"> {
  as?: React.ElementType;
  severity?: "secondary" | "success" | "danger" | "info" | "warn" | string;
  variant?: "text" | "outlined" | string;
  size?: "small" | "large";
  iconOnly?: boolean;
  fluid?: boolean;
}

export const Button = React.forwardRef<HTMLElement, ButtonProps>(function Button(
  { as: Component = "button", severity, variant, size, iconOnly, fluid, className, type = "button", ...props },
  ref
) {
  const classes = clsx(
    "comandos-ui-button",
    severity && `comandos-ui-button-${severity}`,
    variant && `comandos-ui-button-${variant}`,
    size && `comandos-ui-button-${size}`,
    iconOnly && "comandos-ui-button-icon-only",
    fluid && "w-full",
    className
  );
  return <Component ref={ref} type={Component === "button" ? type : undefined} className={classes} {...props} />;
});
