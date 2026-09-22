"use client";

import * as React from "react";

export interface AutoCompleteCompleteEvent { query: string }
export interface AutoCompleteValueChangeEvent { value: unknown }

type Option=Record<string,unknown>;
interface Ctx {
 options:Option[]; optionLabel:string; inputValue:string; disabled?:boolean;
 onInput:(query:string)=>void; onValue:(value:unknown)=>void; open:boolean; setOpen:(v:boolean)=>void;
}
const C=React.createContext<Ctx|null>(null);

function Root({options=[],optionLabel="label",inputValue="",onInputValueChange,onComplete,onValueChange,disabled,children,className}:{
 options?:Option[];optionKey?:string;optionLabel?:string;value?:unknown;inputValue?:string;
 onInputValueChange?:(e:{query:string})=>void;onComplete?:(e:AutoCompleteCompleteEvent)=>void|Promise<void>;
 onValueChange?:(e:AutoCompleteValueChangeEvent)=>void;forceSelection?:boolean;delay?:number;minLength?:number;disabled?:boolean;children:React.ReactNode;className?:string
}){
 const [open,setOpen]=React.useState(false); const timer=React.useRef<ReturnType<typeof setTimeout>|null>(null);
 const onInput=(query:string)=>{onInputValueChange?.({query});setOpen(Boolean(query));if(timer.current)clearTimeout(timer.current);timer.current=setTimeout(()=>{void onComplete?.({query})},250)};
 const onValue=(v:unknown)=>{onValueChange?.({value:v});const label=v&&typeof v==="object"?String((v as Option)[optionLabel]??""):"";onInputValueChange?.({query:label});setOpen(false)};
 return <C.Provider value={{options,optionLabel,inputValue,disabled,onInput,onValue,open,setOpen}}><div className={`comandos-autocomplete ${className??""}`}>{children}</div></C.Provider>;
}
function Input({as:Component="input",...props}:{as?:React.ElementType;[key:string]:unknown}){
 const c=React.useContext(C)!;
 return <Component {...props} value={c.inputValue} disabled={c.disabled} autoComplete="off" onFocus={()=>c.setOpen(Boolean(c.inputValue||c.options.length))} onChange={(e:React.ChangeEvent<HTMLInputElement>)=>c.onInput(e.target.value)} />;
}
const Portal=({children}:{children?:React.ReactNode})=><>{children}</>;
const Positioner=Portal;
function Popup({children,...props}:React.HTMLAttributes<HTMLDivElement>){const c=React.useContext(C)!;if(!c.open)return null;return <div className="comandos-autocomplete-popup" {...props}>{children}</div>}
const List=({children,...props}:React.HTMLAttributes<HTMLDivElement>)=><div role="listbox" {...props}>{children}</div>;
function Option({index,children,...props}:{index:number;uKey?:string;children?:React.ReactNode}&React.HTMLAttributes<HTMLButtonElement>){const c=React.useContext(C)!;const item=c.options[index];return <button type="button" role="option" {...props} onMouseDown={(e)=>e.preventDefault()} onClick={()=>c.onValue(item)}>{children}</button>}
function Empty({children,...props}:React.HTMLAttributes<HTMLDivElement>){const c=React.useContext(C)!;return c.options.length?null:<div {...props}>{children}</div>}
export const AutoComplete={Root,Input,Portal,Positioner,Popup,List,Option,Empty};
