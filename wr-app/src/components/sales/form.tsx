"use client";

import * as React from "react";
import { useFormik } from "formik";

import {
    AutoComplete,
    type AutoCompleteCompleteEvent,
    type AutoCompleteValueChangeEvent
} from "@primereact/ui/autocomplete";

import { InputText } from "@primereact/ui/inputtext";
import { Label } from "@primereact/ui/label";
import { Button } from "@primereact/ui/button";
import { Dialog } from "@primereact/ui/dialog";
import { Plus } from "@primeicons/react/plus";
import { Minus } from "@primeicons/react/minus";
import { Trash } from "@primeicons/react/trash";
import { FloatLabel } from "@primereact/ui/floatlabel";
import { DataTable } from "@primereact/ui/datatable";
import { Select, type SelectValueChangeEvent } from "@primereact/ui/select";


import type { Sale, SaleItem } from "api/models/sales";
import type { User } from "api/models/users";
import type { Weapon } from "api/models/weapons";
import { useUserService, useWeaponService } from "api/services";


interface SalesFormProps {

    onSubmit: ( sale: Sale ) => void;

}

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
});

const paymentMethods = ["Cash", "Pix", "Credit Card", "Debit Card", "Bank Transfer"];

const calculateTotal = (items: SaleItem[] = []): number =>
    items.reduce((total, item) => total + Math.round((item.price ?? 0) * 100) * item.quantity, 0) / 100;


const formScheme: Sale = {

    user: {},
    weapons: [],
    total: 0,
    paymentMethod: ""

};


export const SalesForm: React.FC< SalesFormProps > = ({

    onSubmit

}) => {

    const { findUser } = useUserService();

    const { loadWeapon, findWeapon } = useWeaponService();

    const [ filteredUsers, setFilteredUsers ] = React.useState< User[] >( [] );
    const [code, setCode] = React.useState< string >('');
    const [weapon, setWeapon] = React.useState< Weapon | null >(null);
    const [quantity, setQuantity] = React.useState< string >('');
    const [itemError, setItemError] = React.useState('');
    const [itemToDelete, setItemToDelete] = React.useState<SaleItem | null>(null);
    const [loadingWeapon, setLoadingWeapon] = React.useState(false);
    const addingWeapon = React.useRef(false);
    const [filteredWeapons, setFilteredWeapons] = React.useState<Weapon[]>([]);
    const weaponRequest = React.useRef(0);
    const codeTimer = React.useRef<ReturnType<typeof setTimeout> | null>(null);

    React.useEffect(() => () => {
        if (codeTimer.current) clearTimeout(codeTimer.current);
        weaponRequest.current++;
    }, []);

    const handleCodeInput = (value: string): void => {
        const request = ++weaponRequest.current;
        if (codeTimer.current) clearTimeout(codeTimer.current);
        setCode(value);
        setWeapon(null);
        setFilteredWeapons([]);
        setItemError('');
        if (!value.trim()) return;
        codeTimer.current = setTimeout(async () => {
            try {
                const result = await loadWeapon(encodeURIComponent(value.trim()));
                if (request === weaponRequest.current) setWeapon(result);
            } catch {
                if (request === weaponRequest.current) {
                    setItemError('Unable to load the weapon. Check the code and try again.');
                }
            }
        }, 300);
    };

    const searchWeapons = async (event: AutoCompleteCompleteEvent): Promise<void> => {
        const request = ++weaponRequest.current;
        if (codeTimer.current) clearTimeout(codeTimer.current);
        try {
            const result = await findWeapon({ name: event.query.trim(), sku: '', price: '', description: '' }, 0, 20);
            if (request === weaponRequest.current) setFilteredWeapons(result.content);
        } catch {
            if (request === weaponRequest.current) {
                setFilteredWeapons([]);
                setItemError('Unable to search weapons. Try again.');
            }
        }
    };

    const handleWeaponChange = (event: AutoCompleteValueChangeEvent): void => {
        ++weaponRequest.current;
        if (codeTimer.current) clearTimeout(codeTimer.current);
        const selected = event.value as unknown;
        const nextWeapon = selected && typeof selected === 'object' && 'id' in selected
            ? selected as Weapon : null;
        setWeapon(nextWeapon);
        setCode(nextWeapon?.id == null ? '' : String(nextWeapon.id));
        setItemError('');
    };

    const formik = useFormik< Sale >({

        initialValues: formScheme,
        onSubmit: (values) => onSubmit({ ...values, total: calculateTotal(values.weapons) })

    });

    const itemCount = (formik.values.weapons ?? []).reduce((count, item) => count + item.quantity, 0);
    const saleTotal = calculateTotal(formik.values.weapons);

    const changeItemQuantity = (id: SaleItem['id'], delta: number): void => {
        void formik.setValues(values => ({
            ...values,
            weapons: (values.weapons ?? []).map(item => {
                const nextQuantity = item.quantity + delta;
                return item.id === id && Number.isSafeInteger(nextQuantity) && nextQuantity >= 1
                    ? { ...item, quantity: nextQuantity } : item;
            })
        }));
    };

    const removeItem = (id: SaleItem['id']): void => {
        void formik.setValues(values => ({
            ...values,
            weapons: (values.weapons ?? []).filter(item => item.id !== id)
        }));
        setItemError('');
    };

    const searchUsers = async ( event: AutoCompleteCompleteEvent ): Promise< void > => {

        try {

            const response = await findUser( event.query.trim(), "", 0, 20 );

            setFilteredUsers( response.content );

        } catch ( error ) {

            console.error(
                "Error searching users:",
                error
            );

            setFilteredUsers( [] );

        }

    };


    const handleUserChange = ( event: AutoCompleteValueChangeEvent ): void => {

        const selectedUser = event.value as unknown as User;

        void formik.setFieldValue( "user", selectedUser ?? {} );

    };


const addWeapon = async (): Promise<void> => {
    if (addingWeapon.current) return;
    const amount = quantity.trim() === '' ? 1 : Number(quantity);

    if (
        !code.trim() ||
        !Number.isSafeInteger(amount) ||
        amount <= 0
    ) {
        setItemError('Enter a code and a positive whole quantity.');
        return;
    }

    addingWeapon.current = true;
    ++weaponRequest.current;
    if (codeTimer.current) clearTimeout(codeTimer.current);
    setLoadingWeapon(true);
    setItemError('');
    try {
    const selectedWeapon = await loadWeapon(encodeURIComponent(code.trim()));
    if (!selectedWeapon?.id || !selectedWeapon.name) {
        setItemError('No weapon found for this code.');
        return;
    }
    setWeapon(selectedWeapon);
    const items = formik.values.weapons ?? [];
    const weaponId = selectedWeapon.id;

    if (items.some(item => item.id === weaponId)) {
        setItemError('This weapon is already in the sale.');
        return;
    }

    void formik.setFieldValue('weapons', [
        ...items,
        {
            ...selectedWeapon,
            id: weaponId,
            quantity: amount
        }
    ]);

    setCode('');
    setWeapon(null);
    setQuantity('');
    setItemError('');
    } catch {
        setWeapon(null);
        setItemError('Unable to load the weapon. Check the code and try again.');
    } finally {
        addingWeapon.current = false;
        setLoadingWeapon(false);
    }
};

    return (

        <form onSubmit={ formik.handleSubmit } className="sales-form">

            <div className="formgrid grid">

                <div className="field col-12">

                    <AutoComplete.Root
                        options={ filteredUsers }
                        optionKey="id"
                        optionLabel="name"
                        value={ formik.values.user }
                        forceSelection
                        delay={ 300 }
                        minLength={ 1 }
                        onComplete={ searchUsers }
                        onValueChange={ handleUserChange }
                        className="w-full"
                    >

                        <FloatLabel variant="in" className="w-full">
                            <AutoComplete.Input
                                as={ InputText }
                                id="user"
                                name="user"
                                className="w-full"
                            />
                            <Label htmlFor="user">User</Label>
                        </FloatLabel>

                        <AutoComplete.Portal>

                            <AutoComplete.Positioner>

                                <AutoComplete.Popup>

                                    <AutoComplete.List style={{ maxHeight: "14rem" }}>

                                        { filteredUsers.map(( user, index ) => (

                                            <AutoComplete.Option
                                                key={ String( user.id ) }
                                                index={ index }
                                                uKey={ String( user.id ) }
                                            >
                                                { user.name }
                                            </AutoComplete.Option>

                                        ))}

                                        <AutoComplete.Empty className="text-sm">No user found</AutoComplete.Empty>

                                    </AutoComplete.List>

                                </AutoComplete.Popup>

                            </AutoComplete.Positioner>

                        </AutoComplete.Portal>

                    </AutoComplete.Root>

                </div>

                <div className="field col-2">
                    <FloatLabel variant="in" className="w-full">
                        <InputText 
                            value = { code } 
                            onInput = { 
                                ( e: React.FormEvent< HTMLInputElement > ) => {
                                    handleCodeInput(e.currentTarget.value);
                                }
                            } 
                            id = "code" 
                            name="code"
                            disabled={loadingWeapon}
                            onKeyDown={(event: React.KeyboardEvent<HTMLInputElement>) => {
                                if (event.key === 'Enter') {
                                    event.preventDefault();
                                    void addWeapon();
                                } else if (event.key === 'Tab' && !event.shiftKey && code.trim()) {
                                    void addWeapon();
                                }
                            }}
                            className="w-full"
                        />
                        <Label htmlFor="code">Code</Label>
                    </FloatLabel>
                </div>

                <div className="field col" style={{ minWidth: 0 }}>
                    <AutoComplete.Root
                        options={filteredWeapons}
                        optionKey="id"
                        optionLabel="name"
                        value={weapon}
                        forceSelection
                        delay={300}
                        minLength={1}
                        disabled={loadingWeapon}
                        onComplete={searchWeapons}
                        onValueChange={handleWeaponChange}
                        className="w-full"
                    >
                    <FloatLabel variant="in" className="w-full">
                        <AutoComplete.Input
                            as={InputText}
                            id="weapon"
                            name="weapon"
                            className="w-full"
                        />
                        <Label htmlFor="weapon">Weapon</Label>
                    </FloatLabel>
                    <AutoComplete.Portal>
                        <AutoComplete.Positioner>
                            <AutoComplete.Popup>
                                <AutoComplete.List style={{ maxHeight: '14rem' }}>
                                    {filteredWeapons.map((item, index) => (
                                        <AutoComplete.Option key={String(item.id)} index={index} uKey={String(item.id)}>
                                            {item.name}
                                        </AutoComplete.Option>
                                    ))}
                                    <AutoComplete.Empty>No weapon found</AutoComplete.Empty>
                                </AutoComplete.List>
                            </AutoComplete.Popup>
                        </AutoComplete.Positioner>
                    </AutoComplete.Portal>
                    </AutoComplete.Root>
                </div>

                <div className="field col-2">
                    <FloatLabel variant="in" className="w-full">
                        <InputText
                            id="quantity"
                            name="quantity"
                            disabled={loadingWeapon}
                            type="number"
                            min={1}
                            step={1}
                            value={ quantity }
                            onInput={ (e: React.FormEvent<HTMLInputElement>) => setQuantity(e.currentTarget.value) }
                            className="w-full"
                        />
                        <Label htmlFor="quantity">Quantity</Label>
                    </FloatLabel>
                </div>

                <div className="field col-fixed flex align-items-end">
                    <Button
                        type="button"
                        onClick={addWeapon}
                        disabled={loadingWeapon}
                        className="registration-yellow-button"
                        style={{ height: "3.5rem", gap: "0.5rem" }}
                    >
                        <Plus size={18} />
                        <span>Add</span>
                    </Button>
                </div>

                {itemError && (
                    <div className="col-12" role="alert">{itemError}</div>
                )}

                <div className="col-12 my-4">
                    <hr className="border-none border-top-1 surface-border mt-0 mb-4" />
                    <DataTable.Root
                        data={formik.values.weapons ?? []}
                        dataKey="id"
                        stripedRows
                        style={{ width: "100%" }}
                    >
                        <DataTable.TableContainer style={{ width: "100%", overflowX: "auto" }}>
                            <DataTable.Table
                                aria-label="Weapons in this sale"
                                style={{ width: "100%", minWidth: "1250px", tableLayout: "auto" }}
                            >
                                <DataTable.THead>
                                    <DataTable.THeadRow>
                                        <DataTable.THeadCell style={{ textAlign: "center" }}>Code</DataTable.THeadCell>
                                        <DataTable.THeadCell style={{ textAlign: "center" }}>SKU</DataTable.THeadCell>
                                        <DataTable.THeadCell style={{ textAlign: "center" }}>Weapon</DataTable.THeadCell>
                                        <DataTable.THeadCell style={{ textAlign: "center" }}>Unit Price</DataTable.THeadCell>
                                        <DataTable.THeadCell style={{ textAlign: "center" }}>Quantity</DataTable.THeadCell>
                                        <DataTable.THeadCell style={{ textAlign: "center" }}>Total</DataTable.THeadCell>
                                        <DataTable.THeadCell style={{ width: "10rem", textAlign: "center" }}>Delete</DataTable.THeadCell>
                                    </DataTable.THeadRow>
                                </DataTable.THead>
                                <DataTable.TBody>
                                    {({ item: rowData, index }) => {
                                        const item = rowData as unknown as SaleItem;
                                        return (
                                        <DataTable.Row key={item.id} index={index}>
                                            <DataTable.Cell>{item.id}</DataTable.Cell>
                                            <DataTable.Cell>{item.sku || "—"}</DataTable.Cell>
                                            <DataTable.Cell>{item.name}</DataTable.Cell>
                                            <DataTable.Cell style={{ textAlign: "right" }}>
                                                {item.price == null ? "—" : currencyFormatter.format(item.price)}
                                            </DataTable.Cell>
                                            <DataTable.Cell>
                                                <div className="flex align-items-center justify-content-center gap-2">
                                                    <Button
                                                        type="button"
                                                        variant="text"
                                                        severity="secondary"
                                                        title="Decrease quantity"
                                                        aria-label={`Decrease quantity of ${item.name}`}
                                                        disabled={loadingWeapon || item.quantity <= 1}
                                                        onClick={() => changeItemQuantity(item.id, -1)}
                                                    >
                                                        <Minus size={20} />
                                                    </Button>
                                                    <span aria-live="polite">{item.quantity}</span>
                                                    <Button
                                                        type="button"
                                                        variant="text"
                                                        severity="success"
                                                        title="Increase quantity"
                                                        aria-label={`Increase quantity of ${item.name}`}
                                                        disabled={loadingWeapon || item.quantity >= Number.MAX_SAFE_INTEGER}
                                                        onClick={() => changeItemQuantity(item.id, 1)}
                                                    >
                                                        <Plus size={20} />
                                                    </Button>
                                                </div>
                                            </DataTable.Cell>
                                            <DataTable.Cell style={{ textAlign: "right" }}>
                                                {item.price == null ? "—" : currencyFormatter.format(item.price * item.quantity)}
                                            </DataTable.Cell>
                                            <DataTable.Cell style={{ textAlign: "center" }}>
                                                <Button
                                                    type="button"
                                                    variant="text"
                                                    severity="danger"
                                                    title="Delete item"
                                                    aria-label={`Delete ${item.name}`}
                                                    disabled={loadingWeapon}
                                                    onClick={() => setItemToDelete(item)}
                                                >
                                                    <Trash size={20} />
                                                </Button>
                                            </DataTable.Cell>
                                        </DataTable.Row>
                                        );
                                    }}
                                </DataTable.TBody>
                            </DataTable.Table>
                        </DataTable.TableContainer>
                        {!formik.values.weapons?.length && (
                            <p className="text-center mt-5">No weapons added to the sale.</p>
                        )}
                    </DataTable.Root>
                    <hr className="border-none border-top-1 surface-border mt-4 mb-0" />
                </div>

                <div className="field col-12 md:col-6 mt-3">
                    <FloatLabel variant="in" className="w-full">
                        <Select.Root
                            name="paymentMethod"
                            options={paymentMethods}
                            value={formik.values.paymentMethod || null}
                            onValueChange={(event: SelectValueChangeEvent) => {
                                void formik.setFieldValue("paymentMethod", event.value);
                            }}
                            className="w-full"
                        >
                            <Select.Trigger id="paymentMethod" aria-labelledby="paymentMethodLabel">
                                <Select.Value className="sales-payment-value" />
                                <Select.Indicator />
                            </Select.Trigger>
                            <Select.Portal>
                                <Select.Positioner>
                                    <Select.Popup>
                                        <Select.List>
                                            {paymentMethods.map((method, index) => (
                                                <Select.Option key={method} option={method} index={index} className="sales-payment-option">
                                                    {method}
                                                </Select.Option>
                                            ))}
                                        </Select.List>
                                    </Select.Popup>
                                </Select.Positioner>
                            </Select.Portal>
                        </Select.Root>
                        <Label id="paymentMethodLabel" htmlFor="paymentMethod">Payment Method</Label>
                    </FloatLabel>
                </div>

                <div className="field col-6 md:col-3 mt-3">
                    <FloatLabel variant="in" className="w-full">
                        <InputText id="itemCount" value={String(itemCount)} readOnly className="w-full" />
                        <Label htmlFor="itemCount">Items</Label>
                    </FloatLabel>
                </div>

                <div className="field col-6 md:col-3 mt-3">
                    <FloatLabel variant="in" className="w-full">
                        <InputText id="saleTotal" value={currencyFormatter.format(saleTotal)} readOnly className="w-full" />
                        <Label htmlFor="saleTotal">Total</Label>
                    </FloatLabel>
                </div>

                <div className="col-12 flex justify-content-end">
                    <Button type="submit" className="registration-yellow-button" disabled={!formik.values.weapons?.length}>
                        Finalize
                    </Button>
                </div>

            </div>

            <Dialog.Root
                open={itemToDelete !== null}
                onOpenChange={(event: { value?: boolean }) => {
                    if (!event.value) setItemToDelete(null);
                }}
            >
                <Dialog.Portal>
                    <Dialog.Backdrop />
                    <Dialog.Positioner>
                        <Dialog.Popup style={{ width: "26rem" }}>
                            <Dialog.Header>
                                <Dialog.Title>Delete Item</Dialog.Title>
                            </Dialog.Header>
                            <Dialog.Content>
                                <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                                    <p style={{ margin: 0 }}>
                                        Are you sure you want to delete this item?
                                    </p>
                                    <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.5rem" }}>
                                        <Button type="button" severity="secondary" onClick={() => setItemToDelete(null)}>
                                            Cancel
                                        </Button>
                                        <Button
                                            type="button"
                                            severity="danger"
                                            onClick={() => {
                                                if (itemToDelete) removeItem(itemToDelete.id);
                                                setItemToDelete(null);
                                            }}
                                        >
                                            Delete
                                        </Button>
                                    </div>
                                </div>
                            </Dialog.Content>
                        </Dialog.Popup>
                    </Dialog.Positioner>
                </Dialog.Portal>
            </Dialog.Root>
        </form>

    );

};
