"use client"

import React from "react"
import { useRouter } from "next/navigation"
import { Check, ChevronLeft, ChevronRight, Pencil, Plus, X } from "lucide-react"
import { Trash } from "@primeicons/react"

import { Layout, Loader } from "components"
import type { User } from "api/models/users"
import { useUserService } from "api/services/user.service"

interface UserSearchFilters {
    name: string
    cpf: string
    birth: string
    address: string
    email: string
    phone: string
}

const EMPTY: UserSearchFilters = {
    name: "",
    cpf: "",
    birth: "",
    address: "",
    email: "",
    phone: ""
}

const onlyNumbers = (value: unknown) => String(value ?? "").replace(/\D/g, "")
const formatCPF = (value: unknown) => {
    const numbers = onlyNumbers(value).slice(0, 11)
    return numbers
        .replace(/^(\d{3})(\d)/, "$1.$2")
        .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
        .replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, "$1.$2.$3-$4")
}
const formatPhone = (value: unknown) => {
    const numbers = onlyNumbers(value).slice(0, 11)
    if (numbers.length <= 10) return numbers.replace(/^(\d{2})(\d)/, "($1) $2").replace(/(\d{4})(\d)/, "$1-$2")
    return numbers.replace(/^(\d{2})(\d)/, "($1) $2").replace(/(\d{5})(\d)/, "$1-$2")
}
const formatDate = (value?: string) => {
    if (!value) return "-"
    const date = value.split("T")[0]
    const parts = date.split("-")
    return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : value
}

export const UsersList: React.FC = () => {
    const router = useRouter()
    const service = useUserService()
    const [loading, setLoading] = React.useState(true)
    const [users, setUsers] = React.useState<User[]>([])
    const [filters, setFilters] = React.useState<UserSearchFilters>(EMPTY)
    const [totalRecords, setTotalRecords] = React.useState(0)
    const [rows, setRows] = React.useState(10)
    const [page, setPage] = React.useState(0)
    const [editingId, setEditingId] = React.useState<number | null>(null)
    const [draft, setDraft] = React.useState<Partial<User>>({})
    const [deleteId, setDeleteId] = React.useState<number | null>(null)
    const requestRef = React.useRef<AbortController | null>(null)
    const timerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null)

    const load = React.useCallback(async (nextFilters: UserSearchFilters, nextPage: number, nextRows: number) => {
        requestRef.current?.abort()
        const controller = new AbortController()
        requestRef.current = controller
        setLoading(true)
        try {
            const data = await service.findUser(
                nextFilters.name,
                onlyNumbers(nextFilters.cpf),
                nextPage,
                nextRows,
                nextFilters.birth,
                nextFilters.address,
                nextFilters.email,
                onlyNumbers(nextFilters.phone),
                controller.signal
            )
            if (controller.signal.aborted) return
            setUsers([...(data.content ?? [])].sort((a,b)=>Number(a.id??0)-Number(b.id??0)))
            setTotalRecords(data.totalElements ?? 0)
        } finally {
            if (!controller.signal.aborted) setLoading(false)
        }
    }, [service])

    React.useEffect(() => {
        const initialLoad = window.setTimeout(() => {
            void load(EMPTY, 0, 10)
        }, 0)

        return () => {
            window.clearTimeout(initialLoad)
            requestRef.current?.abort()
            if (timerRef.current) clearTimeout(timerRef.current)
        }
    }, [load])

    const changeFilter = (field: keyof UserSearchFilters, value: string) => {
        const next = { ...filters, [field]: value }
        setFilters(next)
        setPage(0)
        if (timerRef.current) clearTimeout(timerRef.current)
        timerRef.current = setTimeout(() => void load(next, 0, rows), 400)
    }

    const startEdit = (user: User) => {
        if (user.id == null) return
        setEditingId(user.id)
        setDraft({ ...user })
    }

    const cancelEdit = () => {
        setEditingId(null)
        setDraft({})
    }

    const saveEdit = async (user: User) => {
        if (user.id == null) return
        setLoading(true)
        try {
            await service.updateUser({ ...user, ...draft, id: user.id })
            cancelEdit()
            await load(filters, page, rows)
        } finally {
            setLoading(false)
        }
    }

    const confirmDelete = async () => {
        if (deleteId == null) return
        setLoading(true)
        try {
            await service.deleteUser(deleteId)
            setDeleteId(null)
            const nextTotal = Math.max(totalRecords - 1, 0)
            const maxPage = Math.max(Math.ceil(nextTotal / rows) - 1, 0)
            const nextPage = Math.min(page, maxPage)
            setPage(nextPage)
            await load(filters, nextPage, rows)
        } finally {
            setLoading(false)
        }
    }

    const totalPages = Math.max(Math.ceil(totalRecords / rows), 1)

    return (
        <Layout title="Users">
            <div className="comandos-list-page">
                {loading && <Loader show />}

                <div className="comandos-list-toolbar">
                    <button type="button" className="registration-yellow-button" onClick={() => router.push("/registrations/users")}>
                        <Plus size={18} /> <span>New User</span>
                    </button>
                </div>

                <div className="comandos-native-table-container">
                    <table className="comandos-native-table comandos-edit-table">
                        <thead>
                            <tr>
                                <th>ID</th><th>Name</th><th>CPF</th><th>Birth</th><th>Address</th><th>Email</th><th>Phone</th><th>Actions</th>
                            </tr>
                            <tr className="comandos-filter-row">
                                <th />
                                <th><input className="comandos-input" value={filters.name} onChange={e=>changeFilter("name",e.target.value)} /></th>
                                <th><input className="comandos-input" value={filters.cpf} onChange={e=>changeFilter("cpf",formatCPF(e.target.value))} /></th>
                                <th><input className="comandos-input" value={filters.birth} onChange={e=>changeFilter("birth",e.target.value)} /></th>
                                <th><input className="comandos-input" value={filters.address} onChange={e=>changeFilter("address",e.target.value)} /></th>
                                <th><input className="comandos-input" value={filters.email} onChange={e=>changeFilter("email",e.target.value)} /></th>
                                <th><input className="comandos-input" value={filters.phone} onChange={e=>changeFilter("phone",formatPhone(e.target.value))} /></th>
                                <th />
                            </tr>
                        </thead>
                        <tbody>
                            {users.map(user => {
                                const editing = editingId === user.id
                                return <tr key={user.id}>
                                    <td>{user.id}</td>
                                    <td>{editing ? <input className="comandos-input" value={draft.name ?? ""} onChange={e=>setDraft(v=>({...v,name:e.target.value}))}/> : user.name}</td>
                                    <td>{editing ? <input className="comandos-input" value={formatCPF(draft.cpf)} onChange={e=>setDraft(v=>({...v,cpf:onlyNumbers(e.target.value)}))}/> : formatCPF(user.cpf)}</td>
                                    <td>{editing ? <input type="date" className="comandos-input" value={String(draft.birth ?? "").split("T")[0]} onChange={e=>setDraft(v=>({...v,birth:e.target.value}))}/> : formatDate(user.birth)}</td>
                                    <td>{editing ? <input className="comandos-input" value={draft.address ?? ""} onChange={e=>setDraft(v=>({...v,address:e.target.value}))}/> : user.address}</td>
                                    <td>{editing ? <input type="email" className="comandos-input" value={draft.email ?? ""} onChange={e=>setDraft(v=>({...v,email:e.target.value}))}/> : user.email}</td>
                                    <td>{editing ? <input className="comandos-input" value={formatPhone(draft.phone)} onChange={e=>setDraft(v=>({...v,phone:onlyNumbers(e.target.value)}))}/> : formatPhone(user.phone)}</td>
                                    <td>
                                        <div className="comandos-row-actions">
                                            {editing ? <>
                                                <button className="comandos-icon-button comandos-icon-button-success" type="button" aria-label="Save changes" onClick={()=>void saveEdit(user)}><Check size={19}/></button>
                                                <button className="comandos-icon-button comandos-icon-button-danger" type="button" aria-label="Cancel editing" onClick={cancelEdit}><X size={19}/></button>
                                            </> : <>
                                                <button className="comandos-icon-button" type="button" aria-label="Edit user" onClick={()=>startEdit(user)}><Pencil size={19}/></button>
                                                <button className="comandos-icon-button comandos-icon-button-danger" type="button" aria-label="Delete user" onClick={()=>user.id!=null&&setDeleteId(user.id)}><Trash size={19}/></button>
                                            </>}
                                        </div>
                                    </td>
                                </tr>
                            })}
                            {!users.length && !loading && <tr><td colSpan={8} className="text-center">No records found.</td></tr>}
                        </tbody>
                    </table>
                </div>

                <div className="comandos-pagination">
                    <div className="comandos-pagination-controls">
                        <button className="comandos-icon-button" type="button" disabled={page===0} onClick={()=>{setPage(page-1);void load(filters,page-1,rows)}}><ChevronLeft size={18}/></button>
                        <span>Page {page+1} of {totalPages}</span>
                        <button className="comandos-icon-button" type="button" disabled={page+1>=totalPages} onClick={()=>{setPage(page+1);void load(filters,page+1,rows)}}><ChevronRight size={18}/></button>
                        <select className="comandos-input comandos-page-size" value={rows} onChange={e=>{const next=Number(e.target.value);setRows(next);setPage(0);void load(filters,0,next)}}>
                            {[10,20,50,100].map(size=><option key={size} value={size}>{size}</option>)}
                        </select>
                    </div>
                    <span>Total records: {totalRecords}</span>
                </div>
            </div>

            {deleteId !== null && <div className="comandos-dialog-layer">
                <button type="button" className="comandos-dialog-backdrop" aria-label="Close dialog" onClick={()=>setDeleteId(null)} />
                <div role="dialog" aria-modal="true" className="comandos-native-dialog" style={{width:"min(26rem, calc(100vw - 2rem))"}}>
                    <div className="comandos-native-dialog-header"><h2>Delete User</h2></div>
                    <div className="comandos-native-dialog-content">
                        <p>Are you sure you want to delete this user?</p>
                        <div className="comandos-dialog-actions">
                            <button type="button" className="comandos-secondary-button" disabled={loading} onClick={()=>setDeleteId(null)}>Cancel</button>
                            <button type="button" className="comandos-red-button comandos-dialog-action-button" disabled={loading} onClick={()=>void confirmDelete()}>{loading?"Deleting...":"Delete"}</button>
                        </div>
                    </div>
                </div>
            </div>}
        </Layout>
    )
}
