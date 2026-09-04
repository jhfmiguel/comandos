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
import { FloatLabel } from '@primereact/ui/floatlabel';


import type { Sale } from "api/models/sales";
import type { User } from "api/models/users";
import { useUserService } from "api/services/user.service";


interface SalesFormProps {

    onSubmit: ( sale: Sale ) => void;

}


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

    const [ filteredUsers, setFilteredUsers ] = React.useState< User[] >( [] );
    const [code, setCode] = React.useState< String >('');

    const formik = useFormik< Sale >({

        initialValues: formScheme,
        onSubmit

    });


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


    return (

        <form onSubmit={ formik.handleSubmit }>

            <div className="w-full">

                <div className="field">

                    <Label htmlFor="user">
                        User
                    </Label>

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
                        className="w-full mt-2"
                    >

                        <AutoComplete.Input
                            as={ InputText }
                            id="user"
                            name="user"
                            placeholder="Enter the username"
                            className="comandos-input w-full"
                        />

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

                <div className="flex flex-wrap justify-center">
                    <FloatLabel>
                        <InputText 
                            value = { code } 
                            onInput = { 
                                ( e: React.FormEvent< HTMLInputElement > ) => setCode ( e.currentTarget.value ) 
                            } 
                            id = "code" 
                        />
                        <Label htmlFor = "code">Code</Label>
                    </FloatLabel>
                </div>

                <div className = "flex justify-center mt-2">
                    <Button type = "submit" className = "prime-yellow-button">Send</Button>
                </div>

            </div>

        </form>

    );

};