"use client";

import * as React from "react";
import clsx from "clsx";

interface SidebarChangeEvent { value?: boolean; originalEvent?: React.SyntheticEvent }
interface Ctx{open:boolean;toggle:(event?:React.SyntheticEvent)=>void}
const C=React.createContext<Ctx|null>(null);

function Root({open,onOpenChange,children,className,...props}:{open:boolean;onOpenChange?:(e:SidebarChangeEvent)=>void;children:React.ReactNode;className?:string;collapsible?:string;id?:string}){
 const toggle=(originalEvent?:React.SyntheticEvent)=>onOpenChange?.({value:!open,originalEvent});
 return <C.Provider value={{open,toggle}}><div {...props} className={clsx(className)} data-state={open?"expanded":"collapsed"}>{children}</div></C.Provider>;
}
const passthrough=(tag:"div"|"aside"|"header"|"main"|"footer")=>React.forwardRef<any,any>(function P({children,className,...props},ref){return React.createElement(tag,{ref,className,...props},children)});
const Spacer=passthrough("div"), Aside=passthrough("aside"), Panel=passthrough("div"), Header=passthrough("header"), Content=passthrough("div"), Group=passthrough("div"), GroupContent=passthrough("div"), Footer=passthrough("footer"), Rail=passthrough("div"), Layout=passthrough("div"), Main=passthrough("main"), Menu=passthrough("div"), MenuItem=passthrough("div"), MenuSub=passthrough("div"), MenuSubItem=passthrough("div");
const GroupLabel=passthrough("div");

function polymorphic(defaultTag:React.ElementType){
 return React.forwardRef<any,any>(function Poly({as:Component=defaultTag,children,isActive,...props},ref){return <Component ref={ref} data-active={isActive||undefined} {...props}>{children}</Component>})
}
const MenuButton=polymorphic("button"), MenuSubButton=polymorphic("button");
const Trigger=React.forwardRef<any,any>(function Trigger({as:Component="button",children,onClick,...props},ref){const c=React.useContext(C)!;return <Component ref={ref} {...props} onClick={(e:any)=>{onClick?.(e);if(!e.defaultPrevented)c.toggle(e)}}>{children}</Component>});

export const Sidebar={Root,Spacer,Aside,Panel,Header,Content,Group,GroupLabel,GroupContent,Footer,Rail,Layout,Main,Menu,MenuItem,MenuButton,MenuSub,MenuSubItem,MenuSubButton,Trigger};
