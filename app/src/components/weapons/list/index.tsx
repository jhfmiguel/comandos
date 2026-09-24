"use client"

import React from "react"
import { useRouter } from "next/navigation"
import { Check, Pencil, Plus, X } from "lucide-react"
import { Trash } from "@primeicons/react"

import { Layout, Loader } from "components"
import { Pagination } from "platform/components/pagination"
import type { Weapon } from "api/models/weapons"
import { useWeaponService, type WeaponSearchFilters } from "api/services/weapon.service"

const EMPTY: WeaponSearchFilters = { sku: "", name: "", price: "", description: "" }

export const WeaponsList: React.FC = () => {
    const router = useRouter()
    const service = useWeaponService()
    const [loading, setLoading] = React.useState(true)
    const [weapons, setWeapons] = React.useState<Weapon[]>([])
    const [filters, setFilters] = React.useState<WeaponSearchFilters>(EMPTY)
    const [totalRecords, setTotalRecords] = React.useState(0)
    const [rows, setRows] = React.useState(10)
    const [page, setPage] = React.useState(0)
    const [editingId, setEditingId] = React.useState<string | null>(null)
    const [draft, setDraft] = React.useState<Partial<Weapon>>({})
    const [deleteId, setDeleteId] = React.useState<string | null>(null)
    const requestRef = React.useRef<AbortController | null>(null)
    const timerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null)

    const load = React.useCallback(async (nextFilters: WeaponSearchFilters, nextPage: number, nextRows: number) => {
        requestRef.current?.abort()
        const controller = new AbortController()
        requestRef.current = controller
        setLoading(true)
        try {
            const data = await service.findWeapon(nextFilters, nextPage, nextRows, controller.signal)
            if (controller.signal.aborted) return
            setWeapons([...(data.content ?? [])])
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

    const changeFilter = (field: keyof WeaponSearchFilters, value: string) => {
        const next = { ...filters, [field]: value }
        setFilters(next)
        setPage(0)
        if (timerRef.current) clearTimeout(timerRef.current)
        timerRef.current = setTimeout(() => void load(next, 0, rows), 400)
    }

    const startEdit = (weapon: Weapon) => {
        if (weapon.id == null) return
        setEditingId(String(weapon.id))
        setDraft({ ...weapon })
    }

    const cancelEdit = () => {
        setEditingId(null)
        setDraft({})
    }

    const saveEdit = async (weapon: Weapon) => {
        if (weapon.id == null) return
        setLoading(true)
        try {
            await service.updateWeapon({ ...weapon, ...draft, id: String(weapon.id) })
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
            await service.deleteWeapon(deleteId)
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

    return (
        <Layout title="Weapons">
            <div className="comandos-list-page">
                {loading && <Loader show />}
                <div className="comandos-list-toolbar">
                    <button type="button" className="registration-yellow-button" onClick={() => router.push("/registrations/weapons")}>
                        <Plus size={18}/> <span>New Weapon</span>
                    </button>
                </div>

                <div className="comandos-native-table-container">
                    <table className="comandos-native-table comandos-edit-table">
                        <thead>
                            <tr><th>ID</th><th>SKU</th><th>Name</th><th>Price</th><th>Description</th><th>Actions</th></tr>
                            <tr className="comandos-filter-row">
                                <th />
                                <th><input className="comandos-input" placeholder="Pesquisar SKU..." value={filters.sku} onChange={e=>changeFilter("sku",e.target.value)} /></th>
                                <th><input className="comandos-input" placeholder="Pesquisar nome..." value={filters.name} onChange={e=>changeFilter("name",e.target.value)} /></th>
                                <th><input className="comandos-input" placeholder="Pesquisar preço..." value={filters.price} onChange={e=>changeFilter("price",e.target.value)} /></th>
                                <th><input className="comandos-input" placeholder="Pesquisar descrição..." value={filters.description} onChange={e=>changeFilter("description",e.target.value)} /></th>
                                <th />
                            </tr>
                        </thead>
                        <tbody>
                            {weapons.map(weapon => {
                                const editing = editingId === String(weapon.id)
                                return <tr key={String(weapon.id)}>
                                    <td>{weapon.id}</td>
                                    <td>{editing ? <input className="comandos-input" value={draft.sku ?? ""} onChange={e=>setDraft(v=>({...v,sku:e.target.value}))}/> : weapon.sku}</td>
                                    <td>{editing ? <input className="comandos-input" value={draft.name ?? ""} onChange={e=>setDraft(v=>({...v,name:e.target.value}))}/> : weapon.name}</td>
                                    <td>{editing ? <input className="comandos-input" type="number" min="0" step="0.01" value={draft.price ?? ""} onChange={e=>setDraft(v=>({...v,price:e.target.value===""?undefined:Number(e.target.value)}))}/> : (weapon.priceFormatted ?? (weapon.price == null ? "" : new Intl.NumberFormat("pt-BR",{style:"currency",currency:"BRL"}).format(weapon.price)))}</td>
                                    <td>{editing ? <input className="comandos-input" value={draft.description ?? ""} onChange={e=>setDraft(v=>({...v,description:e.target.value}))}/> : weapon.description}</td>
                                    <td><div className="comandos-row-actions">
                                        {editing ? <>
                                            <button className="comandos-icon-button comandos-icon-button-success" type="button" aria-label="Save changes" onClick={()=>void saveEdit(weapon)}><Check size={19}/></button>
                                            <button className="comandos-icon-button comandos-icon-button-danger" type="button" aria-label="Cancel editing" onClick={cancelEdit}><X size={19}/></button>
                                        </> : <>
                                            <button className="comandos-icon-button" type="button" aria-label="Edit weapon" onClick={()=>startEdit(weapon)}><Pencil size={19}/></button>
                                            <button className="comandos-icon-button comandos-icon-button-danger" type="button" aria-label="Delete weapon" onClick={()=>weapon.id!=null&&setDeleteId(String(weapon.id))}><Trash size={19}/></button>
                                        </>}
                                    </div></td>
                                </tr>
                            })}
                            {!weapons.length && !loading && <tr><td colSpan={6} className="text-center">No records found.</td></tr>}
                        </tbody>
                    </table>
                </div>

                <Pagination
                    page={page}
                    totalElements={totalRecords}
                    pageSize={rows}
                    onPageChange={(nextPage) => {
                        setPage(nextPage)
                        void load(filters, nextPage, rows)
                    }}
                    onPageSizeChange={(nextRows) => {
                        setRows(nextRows)
                        setPage(0)
                        void load(filters, 0, nextRows)
                    }}
                    labels={{
                        totalRecords: "registros",
                        first: "Primeira página",
                        previous: "Página anterior",
                        next: "Próxima página",
                        last: "Última página"
                    }}
                />
            </div>

            {deleteId !== null && <div className="comandos-dialog-layer">
                <button type="button" className="comandos-dialog-backdrop" aria-label="Close dialog" onClick={()=>setDeleteId(null)} />
                <div role="dialog" aria-modal="true" className="comandos-native-dialog" style={{width:"min(26rem, calc(100vw - 2rem))"}}>
                    <div className="comandos-native-dialog-header"><h2>Delete Weapon</h2></div>
                    <div className="comandos-native-dialog-content">
                        <p>Are you sure you want to delete this weapon?</p>
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
