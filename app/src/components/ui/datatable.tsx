"use client";

import * as React from "react";
import clsx from "clsx";

export const FilterMatchMode={Contains:"contains"} as const;
export type DataTableFilterMeta=Record<string,{value:unknown;matchMode?:string}>;
export interface DataTableFilterInstance{value:unknown;onChange:(event:unknown,value:unknown)=>void}
export interface DataTablePaginationInstance{rows?:number}
export interface DataTableEditingEvent{value:Record<string,boolean>}
export interface DataTableRowEditEvent{data:Record<string,unknown>;index?:number}

interface RootContext {
 data:unknown[]; rows?:number; filters:DataTableFilterMeta; onFilter?:(e:{filters:DataTableFilterMeta})=>void;
 editingKeys:Record<string,boolean>; onEditingKeysChange?:(e:DataTableEditingEvent)=>void;
 onRowEditSave?:(e:DataTableRowEditEvent)=>void|Promise<void>; onRowEditCancel?:(e:DataTableRowEditEvent)=>void;
}
const RootC=React.createContext<RootContext|null>(null);
const RowC=React.createContext<{rowKey:string;rowData:Record<string,unknown>;index?:number}|null>(null);

function Root({data=[],rows,filters={},onFilter,editingKeys={},onEditingKeysChange,onRowEditSave,onRowEditCancel,children,style,className}:{
 data?:unknown[];rows?:number;filters?:DataTableFilterMeta;onFilter?:(e:{filters:DataTableFilterMeta})=>void;
 editingKeys?:Record<string,boolean>;onEditingKeysChange?:(e:DataTableEditingEvent)=>void;onRowEditSave?:(e:DataTableRowEditEvent)=>void|Promise<void>;onRowEditCancel?:(e:DataTableRowEditEvent)=>void;
 children:React.ReactNode;style?:React.CSSProperties;className?:string;[key:string]:unknown
}){
 return <RootC.Provider value={{data,rows,filters,onFilter,editingKeys,onEditingKeysChange,onRowEditSave,onRowEditCancel}}><div style={style} className={clsx("comandos-datatable",className)}>{children}</div></RootC.Provider>;
}
const TableContainer=(p:React.HTMLAttributes<HTMLDivElement>)=><div {...p}/>;
const Table=(p:React.TableHTMLAttributes<HTMLTableElement>)=><table className={clsx("comandos-table",p.className)} {...p}/>;
const THead=(p:React.HTMLAttributes<HTMLTableSectionElement>)=><thead {...p}/>;
const THeadRow=(p:React.HTMLAttributes<HTMLTableRowElement>)=><tr {...p}/>;
const THeadCell=(p:React.ThHTMLAttributes<HTMLTableCellElement>)=><th {...p}/>;
function Filter({field,children}:{field:string;children:(i:DataTableFilterInstance)=>React.ReactNode;display?:string;dataType?:string}){
 const c=React.useContext(RootC)!; const value=c.filters?.[field]?.value??null;
 const onChange=(event:unknown,next:unknown)=>{const filters={...c.filters,[field]:{...(c.filters?.[field]??{}),value:next}};c.onFilter?.({filters});};
 return <>{children({value,onChange})}</>;
}
function TBody({children}:{children:(x:{item:unknown;index:number})=>React.ReactNode}){const c=React.useContext(RootC)!;return <tbody>{c.data.map((item,index)=>children({item,index}))}</tbody>}
const Row=(p:React.HTMLAttributes<HTMLTableRowElement>&{index?:number})=>{const{index,...rest}=p;return <tr {...rest}/>};
const Cell=(p:React.TdHTMLAttributes<HTMLTableCellElement>)=><td {...p}/>;
function Pagination({children}:{children:(i:DataTablePaginationInstance)=>React.ReactNode}){const c=React.useContext(RootC)!;return <>{children({rows:c.rows})}</>}

function RowEditor({rowKey,rowData,index,children}:{rowKey:string|number;rowData:Record<string,unknown>;index?:number;children:React.ReactNode}){
 return <RowC.Provider value={{rowKey:String(rowKey),rowData,index}}>{children}</RowC.Provider>;
}
function CellEditor({children}:{field?:string;rowIndex?:number;rowData?:Record<string,unknown>;children:React.ReactNode}){return <>{children}</>}
function CellEditorDisplay({children}:{children:React.ReactNode}){const root=React.useContext(RootC)!;const row=React.useContext(RowC);return row&&root.editingKeys[row.rowKey]?null:<>{children}</>}
function CellEditorContent({children}:{children:React.ReactNode}){const root=React.useContext(RootC)!;const row=React.useContext(RowC);return row&&root.editingKeys[row.rowKey]?<>{children}</>:null}

function EditorButton({action,as:Component="button",children,...props}:{action:"init"|"save"|"cancel";as?:React.ElementType;children?:React.ReactNode;[key:string]:unknown}){
 const root=React.useContext(RootC)!;const row=React.useContext(RowC)!;
 const onClick=async(e:React.MouseEvent)=>{(props.onClick as ((e:React.MouseEvent)=>void)|undefined)?.(e);if(e.defaultPrevented)return;
  if(action==="init"){root.onEditingKeysChange?.({value:{...root.editingKeys,[row.rowKey]:true}});}
  if(action==="save"){await root.onRowEditSave?.({data:row.rowData,index:row.index});const next={...root.editingKeys};delete next[row.rowKey];root.onEditingKeysChange?.({value:next});}
  if(action==="cancel"){root.onRowEditCancel?.({data:row.rowData,index:row.index});const next={...root.editingKeys};delete next[row.rowKey];root.onEditingKeysChange?.({value:next});}
 };
 return <Component type="button" {...props} onClick={onClick}>{children}</Component>;
}
const RowEditorInit=(p:any)=><EditorButton action="init" {...p}/>;
const RowEditorSave=(p:any)=><EditorButton action="save" {...p}/>;
const RowEditorCancel=(p:any)=><EditorButton action="cancel" {...p}/>;

export const DataTable={Root,TableContainer,Table,THead,THeadRow,THeadCell,Filter,TBody,Row,Cell,Pagination,RowEditor,CellEditor,CellEditorDisplay,CellEditorContent,RowEditorInit,RowEditorSave,RowEditorCancel};
