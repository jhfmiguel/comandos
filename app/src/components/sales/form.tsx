"use client"

import * as React from "react"
import { useFormik } from "formik"
import { Minus, Plus } from "lucide-react"
import { Trash } from "@primeicons/react"

import type { Sale, SaleItem } from "api/models/sales"
import type { User } from "api/models/users"
import type { Weapon } from "api/models/weapons"
import { useUserService, useWeaponService } from "api/services"
import { Message } from "components/common/message"
import { ComandosSelectField } from "components/common/select-field"

interface SalesFormProps {
    onSubmit: (sale: Sale) => Promise<void>
    saleCompleted: boolean
    onNewSale: () => void
}

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
})

const paymentMethods = [
    { label: "Cash", value: "CASH" },
    { label: "Pix", value: "PIX" },
    { label: "Credit Card", value: "CREDIT_CARD" },
    { label: "Debit Card", value: "DEBIT_CARD" },
    { label: "Bank Transfer", value: "BANK_TRANSFER" }
]

const calculateTotal = (items: SaleItem[] = []): number =>
    items.reduce(
        (total, item) =>
            total + Math.round((item.price ?? 0) * 100) * item.quantity,
        0
    ) / 100

const formScheme: Sale = {
    user: {},
    weapons: [],
    total: 0,
    paymentMethod: ""
}

export const SalesForm: React.FC<SalesFormProps> = ({
    onSubmit,
    saleCompleted,
    onNewSale
}) => {
    const { findUser } = useUserService()
    const { loadWeapon, findWeapon } = useWeaponService()

    const [filteredUsers, setFilteredUsers] = React.useState<User[]>([])
    const [userQuery, setUserQuery] = React.useState("")
    const [userOpen, setUserOpen] = React.useState(false)

    const [code, setCode] = React.useState("")
    const [weaponName, setWeaponName] = React.useState("")
    const [quantity, setQuantity] = React.useState("")
    const [itemError, setItemError] = React.useState("")
    const [itemToDelete, setItemToDelete] = React.useState<SaleItem | null>(null)
    const [loadingWeapon, setLoadingWeapon] = React.useState(false)
    const [filteredWeapons, setFilteredWeapons] = React.useState<Weapon[]>([])
    const [weaponOpen, setWeaponOpen] = React.useState(false)

    const addingWeapon = React.useRef(false)
    const weaponRequest = React.useRef(0)
    const codeTimer = React.useRef<ReturnType<typeof setTimeout> | null>(null)
    const userTimer = React.useRef<ReturnType<typeof setTimeout> | null>(null)

    React.useEffect(() => () => {
        if (codeTimer.current) clearTimeout(codeTimer.current)
        if (userTimer.current) clearTimeout(userTimer.current)
        weaponRequest.current++
    }, [])

    const formik = useFormik<Sale>({
        initialValues: formScheme,
        validate: (values) => (
            values.paymentMethod ? {} : { paymentMethod: "Select a payment method." }
        ),
        onSubmit: async (values) => {
            if (saleCompleted) return

            if (
                !values.user?.id ||
                !values.paymentMethod ||
                !values.weapons?.length ||
                values.weapons.some((item) => item.id == null)
            ) {
                return
            }

            const saleItems = values.weapons.map((item) => ({
                weapon: { id: item.id },
                quantity: item.quantity
            }))

            await onSubmit({
                ...values,
                weapons: saleItems as unknown as SaleItem[],
                total: calculateTotal(values.weapons)
            })
        }
    })

    const searchUsers = (query: string): void => {
        setUserQuery(query)
        setUserOpen(Boolean(query.trim()))
        void formik.setFieldValue("user", {})

        if (userTimer.current) clearTimeout(userTimer.current)
        if (!query.trim()) {
            setFilteredUsers([])
            return
        }

        userTimer.current = setTimeout(async () => {
            try {
                const response = await findUser(query.trim(), "", 0, 20)
                setFilteredUsers(response.content)
                setUserOpen(true)
            } catch {
                setFilteredUsers([])
            }
        }, 300)
    }

    const selectUser = (user: User): void => {
        void formik.setFieldValue("user", user)
        setUserQuery(user.name ?? "")
        setFilteredUsers([])
        setUserOpen(false)
    }

    const handleCodeInput = (value: string, immediate = false): void => {
        const request = ++weaponRequest.current
        if (codeTimer.current) clearTimeout(codeTimer.current)

        setCode(value)
        setWeaponName("")
        setFilteredWeapons([])
        setWeaponOpen(false)
        setItemError("")

        if (!value.trim()) return

        codeTimer.current = setTimeout(async () => {
            try {
                const result = await loadWeapon(encodeURIComponent(value.trim()))
                if (request === weaponRequest.current) {
                    setWeaponName(result.name ?? "")
                    setFilteredWeapons([result])
                }
            } catch {
                if (request === weaponRequest.current) {
                    setItemError("Unable to load the weapon. Check the code and try again.")
                }
            }
        }, immediate ? 0 : 300)
    }

    const searchWeapons = (query: string): void => {
        setWeaponName(query)
        setWeaponOpen(Boolean(query.trim()))
        const request = ++weaponRequest.current

        if (!query.trim()) {
            setFilteredWeapons([])
            return
        }

        if (codeTimer.current) clearTimeout(codeTimer.current)
        codeTimer.current = setTimeout(async () => {
            try {
                const result = await findWeapon(
                    { name: query.trim(), sku: "", price: "", description: "" },
                    0,
                    20
                )

                if (request === weaponRequest.current) {
                    setFilteredWeapons(result.content)
                    setWeaponOpen(true)
                }
            } catch {
                if (request === weaponRequest.current) {
                    setFilteredWeapons([])
                    setItemError("Unable to search weapons. Try again.")
                }
            }
        }, 300)
    }

    const selectWeapon = (selected: Weapon): void => {
        ++weaponRequest.current
        if (codeTimer.current) clearTimeout(codeTimer.current)
        setWeaponName(selected.name ?? "")
        setCode(selected.id == null ? "" : String(selected.id))
        setFilteredWeapons([])
        setWeaponOpen(false)
        setItemError("")
    }

    const itemCount = (formik.values.weapons ?? []).reduce(
        (count, item) => count + item.quantity,
        0
    )
    const saleTotal = calculateTotal(formik.values.weapons)
    const paymentMethodError =
        (formik.touched.paymentMethod || formik.submitCount > 0)
            ? formik.errors.paymentMethod
            : undefined

    const canFinalize = Boolean(
        formik.values.user?.id &&
        formik.values.weapons?.length &&
        formik.values.weapons.every((item) => item.id != null)
    )

    const changeItemQuantity = (id: SaleItem["id"], delta: number): void => {
        void formik.setValues((values) => ({
            ...values,
            weapons: (values.weapons ?? []).map((item) => {
                const nextQuantity = item.quantity + delta
                return item.id === id &&
                    Number.isSafeInteger(nextQuantity) &&
                    nextQuantity >= 1
                    ? { ...item, quantity: nextQuantity }
                    : item
            })
        }))
    }

    const removeItem = (id: SaleItem["id"]): void => {
        void formik.setValues((values) => ({
            ...values,
            weapons: (values.weapons ?? []).filter((item) => item.id !== id)
        }))
        setItemError("")
    }

    const addWeapon = async (): Promise<void> => {
        if (addingWeapon.current) return

        const amount = quantity.trim() === "" ? 1 : Number(quantity)

        if (!code.trim() || !Number.isSafeInteger(amount) || amount <= 0) {
            setItemError("Enter a code and a positive whole quantity.")
            return
        }

        addingWeapon.current = true
        ++weaponRequest.current
        if (codeTimer.current) clearTimeout(codeTimer.current)
        setLoadingWeapon(true)
        setItemError("")

        try {
            const selectedWeapon = await loadWeapon(encodeURIComponent(code.trim()))

            if (!selectedWeapon?.id || !selectedWeapon.name) {
                setItemError("No weapon found for this code.")
                return
            }

            setWeaponName(selectedWeapon.name)

            const items = formik.values.weapons ?? []
            const weaponId = selectedWeapon.id

            if (items.some((item) => item.id === weaponId)) {
                setItemError("This weapon is already in the sale.")
                return
            }

            void formik.setFieldValue("weapons", [
                ...items,
                {
                    ...selectedWeapon,
                    id: weaponId,
                    quantity: amount
                }
            ])

            setCode("")
                setWeaponName("")
            setQuantity("")
            setItemError("")
        } catch {
                setWeaponName("")
            setItemError("Unable to load the weapon. Check the code and try again.")
        } finally {
            addingWeapon.current = false
            setLoadingWeapon(false)
        }
    }

    const resetSale = (): void => {
        ++weaponRequest.current
        if (codeTimer.current) clearTimeout(codeTimer.current)
        if (userTimer.current) clearTimeout(userTimer.current)

        formik.resetForm()
        setCode("")
        setWeaponName("")
        setQuantity("")
        setItemError("")
        setFilteredUsers([])
        setFilteredWeapons([])
        setUserQuery("")
        setUserOpen(false)
        setWeaponOpen(false)
        setItemToDelete(null)
        onNewSale()
    }

    return (
        <form onSubmit={formik.handleSubmit} className="sales-form">
            <div className="formgrid grid">
                <div className="field col-12 comandos-native-autocomplete">
                    <label htmlFor="user" className="comandos-float-label">User</label>
                    <input
                        id="user"
                        name="user"
                        className="comandos-input w-full"
                        autoComplete="off"
                        value={userQuery}
                        onChange={(event) => searchUsers(event.target.value)}
                        onFocus={() => setUserOpen(Boolean(userQuery.trim()))}
                        onBlur={() => window.setTimeout(() => setUserOpen(false), 120)}
                    />
                    {userOpen && (
                        <div className="comandos-autocomplete-popup" role="listbox">
                            {filteredUsers.length ? filteredUsers.map((user) => (
                                <button
                                    key={String(user.id)}
                                    type="button"
                                    role="option"
                                    aria-selected="false"
                                    className="comandos-autocomplete-option"
                                    onMouseDown={(event) => event.preventDefault()}
                                    onClick={() => selectUser(user)}
                                >
                                    {user.name}
                                </button>
                            )) : (
                                <div className="comandos-autocomplete-empty">No user found</div>
                            )}
                        </div>
                    )}
                </div>

                <div className="field col-2">
                    <label htmlFor="code" className="comandos-float-label">Code</label>
                    <input
                        value={code}
                        onChange={(event) => handleCodeInput(event.target.value)}
                        id="code"
                        name="code"
                        disabled={loadingWeapon}
                        onKeyDown={(event) => {
                            if (event.key === "Enter") {
                                event.preventDefault()
                                void addWeapon()
                            } else if (
                                event.key === "Tab" &&
                                !event.shiftKey &&
                                code.trim()
                            ) {
                                handleCodeInput(code, true)
                            }
                        }}
                        className="comandos-input w-full"
                    />
                </div>

                <div className="field col comandos-native-autocomplete" style={{ minWidth: 0 }}>
                    <label htmlFor="weapon" className="comandos-float-label">Weapon</label>
                    <input
                        id="weapon"
                        name="weapon"
                        className="comandos-input w-full"
                        autoComplete="off"
                        disabled={loadingWeapon}
                        value={weaponName}
                        onChange={(event) => searchWeapons(event.target.value)}
                        onFocus={() => setWeaponOpen(Boolean(weaponName.trim()))}
                        onBlur={() => window.setTimeout(() => setWeaponOpen(false), 120)}
                    />
                    {weaponOpen && (
                        <div className="comandos-autocomplete-popup" role="listbox">
                            {filteredWeapons.length ? filteredWeapons.map((item) => (
                                <button
                                    key={String(item.id)}
                                    type="button"
                                    role="option"
                                    aria-selected="false"
                                    className="comandos-autocomplete-option"
                                    onMouseDown={(event) => event.preventDefault()}
                                    onClick={() => selectWeapon(item)}
                                >
                                    {item.name}
                                </button>
                            )) : (
                                <div className="comandos-autocomplete-empty">No weapon found</div>
                            )}
                        </div>
                    )}
                </div>

                <div className="field col-2">
                    <label htmlFor="quantity" className="comandos-float-label">Quantity</label>
                    <input
                        id="quantity"
                        name="quantity"
                        disabled={loadingWeapon}
                        type="number"
                        min={1}
                        step={1}
                        value={quantity}
                        onChange={(event) => setQuantity(event.target.value)}
                        className="comandos-input w-full"
                    />
                </div>

                <div className="field col-fixed flex align-items-end">
                    <button
                        type="button"
                        onClick={() => void addWeapon()}
                        disabled={loadingWeapon}
                        className="registration-yellow-button"
                        style={{ height: "3.5rem", gap: "0.5rem" }}
                    >
                        <Plus size={18} />
                        <span>Add</span>
                    </button>
                </div>

                {itemError && (
                    <div className="col-12">
                        <Message
                            type="error"
                            text={itemError}
                            onClose={() => setItemError("")}
                        />
                    </div>
                )}

                <div className="col-12 my-4">
                    <hr className="border-none border-top-1 surface-border mt-0 mb-4" />
                    <div className="comandos-native-table-container">
                        <table className="comandos-native-table" aria-label="Weapons in this sale">
                            <thead>
                                <tr>
                                    <th>Code</th>
                                    <th>SKU</th>
                                    <th>Weapon</th>
                                    <th>Unit Price</th>
                                    <th>Quantity</th>
                                    <th>Total</th>
                                    <th>Delete</th>
                                </tr>
                            </thead>
                            <tbody>
                                {(formik.values.weapons ?? []).map((item) => (
                                    <tr key={String(item.id)}>
                                        <td>{item.id}</td>
                                        <td>{item.sku || "—"}</td>
                                        <td>{item.name}</td>
                                        <td className="text-right">
                                            {item.price == null
                                                ? "—"
                                                : currencyFormatter.format(item.price)}
                                        </td>
                                        <td>
                                            <div className="flex align-items-center justify-content-center gap-2">
                                                <button
                                                    type="button"
                                                    className="comandos-icon-button"
                                                    title="Decrease quantity"
                                                    aria-label={`Decrease quantity of ${item.name}`}
                                                    disabled={loadingWeapon || item.quantity <= 1}
                                                    onClick={() => changeItemQuantity(item.id, -1)}
                                                >
                                                    <Minus size={20} />
                                                </button>
                                                <span aria-live="polite">{item.quantity}</span>
                                                <button
                                                    type="button"
                                                    className="comandos-icon-button comandos-icon-button-success"
                                                    title="Increase quantity"
                                                    aria-label={`Increase quantity of ${item.name}`}
                                                    disabled={loadingWeapon || item.quantity >= Number.MAX_SAFE_INTEGER}
                                                    onClick={() => changeItemQuantity(item.id, 1)}
                                                >
                                                    <Plus size={20} />
                                                </button>
                                            </div>
                                        </td>
                                        <td className="text-right">
                                            {item.price == null
                                                ? "—"
                                                : currencyFormatter.format(item.price * item.quantity)}
                                        </td>
                                        <td className="text-center">
                                            <button
                                                type="button"
                                                className="comandos-icon-button comandos-icon-button-danger"
                                                title="Delete item"
                                                aria-label={`Delete ${item.name}`}
                                                disabled={loadingWeapon}
                                                onClick={() => setItemToDelete(item)}
                                            >
                                                <Trash size={20} />
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {!formik.values.weapons?.length && (
                        <p className="text-center mt-5">No weapons added to the sale.</p>
                    )}

                    <hr className="border-none border-top-1 surface-border mt-4 mb-0" />
                </div>

                <div className="field col-12 md:col-6 mt-3">
                    <ComandosSelectField
                        id="paymentMethod"
                        name="paymentMethod"
                        label="Payment Method"
                        required
                        value={formik.values.paymentMethod || ""}
                        options={paymentMethods}
                        invalid={Boolean(paymentMethodError)}
                        describedBy={paymentMethodError ? "paymentMethodError" : undefined}
                        onChange={value => {
                            void formik.setFieldValue("paymentMethod", value)
                        }}
                        onBlur={() => {
                            void formik.setFieldTouched("paymentMethod", true)
                        }}
                    />
                    {paymentMethodError && (
                        <div id="paymentMethodError" className="mt-2">
                            <Message type="error" text={paymentMethodError} />
                        </div>
                    )}
                </div>

                <div className="field col-6 md:col-3 mt-3">
                    <label htmlFor="itemCount" className="comandos-float-label">Items</label>
                    <input
                        id="itemCount"
                        value={String(itemCount)}
                        readOnly
                        className="comandos-input w-full"
                    />
                </div>

                <div className="field col-6 md:col-3 mt-3">
                    <label htmlFor="saleTotal" className="comandos-float-label">Total</label>
                    <input
                        id="saleTotal"
                        value={currencyFormatter.format(saleTotal)}
                        readOnly
                        className="comandos-input w-full"
                    />
                </div>

                <div className="col-12 flex justify-content-end">
                    {saleCompleted ? (
                        <button
                            type="button"
                            className="registration-yellow-button"
                            onClick={resetSale}
                        >
                            <Plus size={18} />
                            <span>Sale</span>
                        </button>
                    ) : (
                        <button
                            type="submit"
                            className="registration-yellow-button"
                            disabled={!canFinalize || formik.isSubmitting || loadingWeapon}
                        >
                            Finalize
                        </button>
                    )}
                </div>
            </div>

            {itemToDelete && (
                <div className="comandos-dialog-layer">
                    <button
                        type="button"
                        className="comandos-dialog-backdrop"
                        aria-label="Close dialog"
                        onClick={() => setItemToDelete(null)}
                    />
                    <div
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="delete-sale-item-title"
                        className="comandos-native-dialog"
                        style={{ width: "min(26rem, calc(100vw - 2rem))" }}
                    >
                        <div className="comandos-native-dialog-header">
                            <h2 id="delete-sale-item-title">Delete Item</h2>
                        </div>
                        <div className="comandos-native-dialog-content">
                            <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                                <p style={{ margin: 0 }}>
                                    Are you sure you want to delete this item?
                                </p>
                                <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.5rem" }}>
                                    <button
                                        type="button"
                                        className="comandos-secondary-button"
                                        onClick={() => setItemToDelete(null)}
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        type="button"
                                        className="comandos-red-button comandos-dialog-action-button"
                                        onClick={() => {
                                            removeItem(itemToDelete.id)
                                            setItemToDelete(null)
                                        }}
                                    >
                                        Delete
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </form>
    )
}
