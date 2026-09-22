"use client";

import * as React from "react";
interface ChangeEvent{value:boolean}
interface Ctx{open:boolean;setOpen:(v:boolean)=>void}
const C=React.createContext<Ctx|null>(null);
function Root({open,onOpenChange,children,className}:{open:boolean;onOpenChange?:(e:ChangeEvent)=>void;children:React.ReactNode;className?:string}){const setOpen=(v:boolean)=>onOpenChange?.({value:v});return <C.Provider value={{open,setOpen}}><div className={`comandos-popup-menu-root ${className??""}`}>{children}</div></C.Provider>}
function Trigger({as:Component="button",children,onClick,...props}:{as?:React.ElementType;children?:React.ReactNode;onClick?:(e:any)=>void;[key:string]:unknown}){const c=React.useContext(C)!;return <Component {...props} onClick={(e:any)=>{onClick?.(e);c.setOpen(!c.open)}}>{children}</Component>}
function Portal({children}:{children:React.ReactNode}){const c=React.useContext(C)!;return c.open?<>{children}</>:null}
const Positioner=({children,className,...props}:React.HTMLAttributes<HTMLDivElement>)=><div className={className} style={{position:"absolute",bottom:"100%",left:0,marginBottom:4,...props.style}} {...props}>{children}</div>;
const Popup=(p:React.HTMLAttributes<HTMLDivElement>)=><div {...p}/>;
const List=(p:React.HTMLAttributes<HTMLDivElement>)=><div role="menu" {...p}/>;
const Label=(p:React.HTMLAttributes<HTMLDivElement>)=><div {...p}/>;
const Separator=(p:React.HTMLAttributes<HTMLHRElement>)=><hr {...p}/>;
function Item({as:Component="button",children,onClick,...props}:{as?:React.ElementType;children?:React.ReactNode;onClick?:(e:any)=>void;[key:string]:unknown}){const c=React.useContext(C)!;return <Component role="menuitem" {...props} onClick={(e:any)=>{onClick?.(e);c.setOpen(false)}}>{children}</Component>}
export interface MenuRootOpenChangeEvent{value:boolean}
export const Menu={Root,Trigger,Portal,Positioner,Popup,List,Label,Separator,Item};
