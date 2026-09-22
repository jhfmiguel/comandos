"use client";

import * as React from "react";

export interface SelectValueChangeEvent { value: unknown }
type Option = Record<string, unknown>;
interface SelectCtx {
  options: Option[];
  optionLabel: string;
  optionValue: string;
  value: unknown;
  onValueChange?: (event: SelectValueChangeEvent) => void;
  name?: string;
  invalid?: boolean;
}
const C=React.createContext<SelectCtx|null>(null);
function Root({options=[],optionLabel="label",optionValue="value",value,onValueChange,name,invalid,children,className}:{
 options?:Option[];optionLabel?:string;optionValue?:string;value?:unknown;onValueChange?:(e:SelectValueChangeEvent)=>void;name?:string;invalid?:boolean;children:React.ReactNode;className?:string;[key:string]:unknown
}){return <C.Provider value={{options,optionLabel,optionValue,value,onValueChange,name,invalid}}><div className={className}>{children}</div></C.Provider>}
function Trigger({className,...props}:React.SelectHTMLAttributes<HTMLSelectElement>){
 const c=React.useContext(C)!;
 return <select {...props} name={c.name} value={c.value==null?"":String(c.value)} aria-invalid={c.invalid||props["aria-invalid"]} className={`comandos-input ${className??""}`} onChange={(e)=>c.onValueChange?.({value:e.target.value})}>
  <option value="" />
  {c.options.map((o,i)=><option key={String(o[c.optionValue]??i)} value={String(o[c.optionValue]??"")}>{String(o[c.optionLabel]??"")}</option>)}
 </select>;
}
const Empty=()=>null; const Value=Empty; const Indicator=Empty; const Portal=({children}:{children?:React.ReactNode})=><>{children}</>; const Positioner=Portal; const Popup=Portal; const List=()=>null;
export const Select={Root,Trigger,Value,Indicator,Portal,Positioner,Popup,List};
