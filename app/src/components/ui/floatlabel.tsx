import * as React from "react";
export function FloatLabel({ children, className }: { children: React.ReactNode; className?: string; variant?: string }) {
  return <div className={`comandos-float-label ${className ?? ""}`}>{children}</div>;
}
