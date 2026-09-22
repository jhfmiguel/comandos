import * as React from "react";
export const InputGroup = {
  Root: ({children, ...props}: React.HTMLAttributes<HTMLDivElement>) => <div {...props}>{children}</div>,
  Addon: ({children, ...props}: React.HTMLAttributes<HTMLSpanElement>) => <span {...props}>{children}</span>
};
