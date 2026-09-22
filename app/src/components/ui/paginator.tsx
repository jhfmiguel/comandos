"use client";

import * as React from "react";
import clsx from "clsx";

export interface PaginatorRootChangeEvent { value: number }
export interface PaginatorPage { type:"page"|"ellipsis"; value:number }
export interface PaginatorPagesInstance { paginator:{pages:PaginatorPage[]} }
interface Ctx { page:number; total:number; itemsPerPage:number; pages:number; change:(n:number)=>void }
const C=React.createContext<Ctx|null>(null);

function buildPages(page:number,pages:number):PaginatorPage[]{
 if(pages<=7)return Array.from({length:pages},(_,i)=>({type:"page" as const,value:i+1}));
 const values=new Set([1,pages,page-1,page,page+1].filter(v=>v>=1&&v<=pages));
 const sorted=[...values].sort((a,b)=>a-b); const out:PaginatorPage[]=[]; let prev=0;
 for(const v of sorted){ if(prev&&v-prev>1)out.push({type:"ellipsis",value:prev+1}); out.push({type:"page",value:v}); prev=v; }
 return out;
}
function Root({page,total,itemsPerPage,onPageChange,children,className}:{page:number;total:number;itemsPerPage:number;onPageChange?:(e:PaginatorRootChangeEvent)=>void;children:React.ReactNode;className?:string}){
 const pages=Math.max(1,Math.ceil(total/Math.max(itemsPerPage,1))); const change=(n:number)=>onPageChange?.({value:Math.min(Math.max(n,1),pages)});
 return <C.Provider value={{page,total,itemsPerPage,pages,change}}><nav aria-label="Pagination" className={clsx("comandos-paginator",className)}>{children}</nav></C.Provider>;
}
const Content=(p:React.HTMLAttributes<HTMLDivElement>)=><div className={clsx("comandos-paginator-content",p.className)} {...p}/>;
function NavButton({kind,children,...props}:{kind:"first"|"prev"|"next"|"last";children?:React.ReactNode}&React.ButtonHTMLAttributes<HTMLButtonElement>){
 const c=React.useContext(C)!; const target=kind==="first"?1:kind==="prev"?c.page-1:kind==="next"?c.page+1:c.pages; const disabled=kind==="first"||kind==="prev"?c.page<=1:c.page>=c.pages;
 return <button type="button" disabled={disabled} onClick={()=>c.change(target)} {...props}>{children}</button>;
}
const First=(p:React.ButtonHTMLAttributes<HTMLButtonElement>)=><NavButton kind="first" {...p}/>;
const Prev=(p:React.ButtonHTMLAttributes<HTMLButtonElement>)=><NavButton kind="prev" {...p}/>;
const Next=(p:React.ButtonHTMLAttributes<HTMLButtonElement>)=><NavButton kind="next" {...p}/>;
const Last=(p:React.ButtonHTMLAttributes<HTMLButtonElement>)=><NavButton kind="last" {...p}/>;
function Pages({children}:{children:(instance:PaginatorPagesInstance)=>React.ReactNode}){const c=React.useContext(C)!;return <>{children({paginator:{pages:buildPages(c.page,c.pages)}})}</>}
function Page({value,...props}:{value:number}&React.ButtonHTMLAttributes<HTMLButtonElement>){const c=React.useContext(C)!;return <button type="button" aria-current={c.page===value?"page":undefined} className={clsx(c.page===value&&"is-active",props.className)} onClick={()=>c.change(value)} {...props}>{value}</button>}
const Ellipsis=(p:React.HTMLAttributes<HTMLSpanElement>)=><span {...p}/>;
export const Paginator={Root,Content,First,Prev,Pages,Page,Ellipsis,Next,Last};
