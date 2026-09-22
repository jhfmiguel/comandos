import * as React from "react";
export const Avatar={
 Root:({children,className,...props}:{children?:React.ReactNode;className?:string;shape?:string})=><span className={className} {...props}>{children}</span>,
 Fallback:({children,...props}:React.HTMLAttributes<HTMLSpanElement>)=><span {...props}>{children}</span>
};
