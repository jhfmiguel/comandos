"use client";

import * as React from "react";
import clsx from "clsx";

export interface TabsRootChangeEvent { value: string | number | null }

type TabsValue = string | number | null;
interface TabsContextValue { value: TabsValue; setValue: (value: TabsValue) => void }
const TabsContext = React.createContext<TabsContextValue | null>(null);

function Root({ value, defaultValue, onValueChange, children, className }: {
  value?: TabsValue; defaultValue?: TabsValue; onValueChange?: (event: TabsRootChangeEvent) => void;
  children: React.ReactNode; className?: string; selectOnFocus?: boolean;
}) {
  const [internal, setInternal] = React.useState<TabsValue>(defaultValue ?? null);
  const current = value !== undefined ? value : internal;
  const setValue = (next: TabsValue) => {
    if (value === undefined) setInternal(next);
    onValueChange?.({ value: next });
  };
  return <TabsContext.Provider value={{value:current,setValue}}><div className={clsx("comandos-tabs",className)}>{children}</div></TabsContext.Provider>;
}
const List=(p:React.HTMLAttributes<HTMLDivElement>)=><div role="tablist" {...p}/>;
const Content=(p:React.HTMLAttributes<HTMLDivElement>)=><div className={clsx("comandos-tabs-list-content",p.className)} {...p}/>;
function Tab({value,children,className,...props}:{value:string|number;children?:React.ReactNode;className?:string}&Omit<React.ButtonHTMLAttributes<HTMLButtonElement>,"value">){
 const ctx=React.useContext(TabsContext)!; const active=ctx.value===value;
 return <button type="button" role="tab" aria-selected={active} className={clsx("comandos-tab",active&&"comandos-tab-active",className)} onClick={()=>ctx.setValue(value)} {...props}>{children}</button>;
}
const Indicator=(p:React.HTMLAttributes<HTMLSpanElement>)=><span aria-hidden="true" className={clsx("comandos-tabs-indicator",p.className)} {...p}/>;
const Prev=(p:React.ButtonHTMLAttributes<HTMLButtonElement>)=><button type="button" className={clsx("comandos-tabs-nav",p.className)} {...p}/>;
const Next=Prev;
const Panels=(p:React.HTMLAttributes<HTMLDivElement>)=><div {...p}/>;
function Panel({value,children,...props}:{value:string|number;children?:React.ReactNode}&React.HTMLAttributes<HTMLDivElement>){
 const ctx=React.useContext(TabsContext)!; if(ctx.value!==value)return null; return <div role="tabpanel" {...props}>{children}</div>;
}
export const Tabs={Root,List,Content,Tab,Indicator,Prev,Next,Panels,Panel};
