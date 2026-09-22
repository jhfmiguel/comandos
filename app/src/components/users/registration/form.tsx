"use client";

import React from "react";

import { useRouter } from "next/navigation";
import { useFormik } from "formik";

import { Button } from "components/common/button";

import { User } from "api/models/users";

import {
    Input,
    InputCPF,
    InputDate,
    InputPhone
} from "components";

import {
    convertToIsoLocalDateTime,
    duplicateCPF,
    getFormattedInitialValues,
    getValidationScheme
} from "./validationSchema";

import { convertToIsoDate } from "utils/date";
import { formatOnlyNumbers } from "utils/numeric";

export interface UserFormProps {
    user: User;
    onSubmit: (user: User) => void | Promise<void>;
}

export const formikScheme: User = {
    creationDate: "",
    name: "",
    cpf: "",
    birth: "",
    address: "",
    email: "",
    phone: ""
};

export const UserForm: React.FC<UserFormProps> = ({
    user,
    onSubmit
}) => {
    const router = useRouter();

    const formik = useFormik<User>({
        initialValues: getFormattedInitialValues(user),
        enableReinitialize: true,
        validationSchema: getValidationScheme(user),
        validateOnChange: false,
        validateOnBlur: true,

        onSubmit: async (values, actions) => {
            const cleanCpf = formatOnlyNumbers(
                values.cpf
            );

            const isoBirth = convertToIsoDate(
                values.birth
            );

            const isoCreationDate =
                convertToIsoLocalDateTime(
                    values.creationDate
                );

            const submittedValues: User = {
                ...values,
                cpf: cleanCpf,
                birth: isoBirth,
                creationDate:
                    isoCreationDate as User["creationDate"]
            };

            try {
                await Promise.resolve(
                    onSubmit(submittedValues)
                );
            } catch {
                actions.setFieldError(
                    "cpf",
                    duplicateCPF
                );
            } finally {
                actions.setSubmitting(false);
            }
        }
    });

    const handleBack = (): void => {
        router.push("/queries/users");
    };

    const handleCpfBlur = async (): Promise<void> => {
        const cpf = formik.values.cpf?.trim();

        if (!cpf) {
            await formik.setFieldTouched(
                "cpf",
                false,
                false
            );

            formik.setFieldError(
                "cpf",
                undefined
            );

            return;
        }

        await formik.setFieldTouched(
            "cpf",
            true,
            true
        );
    };

    return (
        <>
            <form onSubmit={formik.handleSubmit}>
                {formik.values.id && (
                    <div className="formgrid grid">
                        <Input
                            id="id"
                            name="id"
                            label="Id:"
                            columnClasses="col-12 md:col-6"
                            value={formik.values.id}
                            autoComplete="off"
                            disabled
                        />

                        <Input
                            id="creationDate"
                            name="creationDate"
                            label="Creation Date:"
                            columnClasses="col-12 md:col-6"
                            value={
                                formik.values
                                    .creationDate
                            }
                            autoComplete="off"
                            disabled
                        />
                    </div>
                )}

                <div className="formgrid grid">
                    <Input
                        id="name"
                        name="name"
                        label="Name: *"
                        columnClasses="col-12"
                        onChange={
                            formik.handleChange
                        }
                        value={
                            formik.values.name
                        }
                        error={
                            formik.touched.name
                                ? formik.errors.name
                                : undefined
                        }
                        autoComplete="off"
                    />
                </div>

                <div className="formgrid grid">
                    <InputCPF
                        id="cpf"
                        name="cpf"
                        label="CPF: *"
                        columnClasses="col-12 md:col-6"
                        onChange={
                            formik.handleChange
                        }
                        onBlur={
                            handleCpfBlur
                        }
                        value={
                            formik.values.cpf
                        }
                        error={
                            formik.touched.cpf
                                ? formik.errors.cpf
                                : undefined
                        }
                        autoComplete="off"
                    />

                    <InputDate
                        id="birth"
                        name="birth"
                        label="Birth: *"
                        columnClasses="col-12 md:col-6"
                        onChange={
                            formik.handleChange
                        }
                        value={
                            formik.values.birth
                        }
                        error={
                            formik.touched.birth
                                ? formik.errors.birth
                                : undefined
                        }
                        autoComplete="off"
                    />
                </div>

                <div className="formgrid grid">
                    <Input
                        id="address"
                        name="address"
                        label="Address: *"
                        columnClasses="col-12"
                        onChange={
                            formik.handleChange
                        }
                        value={
                            formik.values.address
                        }
                        error={
                            formik.touched.address
                                ? formik.errors
                                      .address
                                : undefined
                        }
                        autoComplete="off"
                    />
                </div>

                <div className="formgrid grid">
                    <Input
                        id="email"
                        name="email"
                        label="E-mail: *"
                        columnClasses="col-12 md:col-6"
                        onChange={
                            formik.handleChange
                        }
                        value={
                            formik.values.email
                        }
                        error={
                            formik.touched.email
                                ? formik.errors.email
                                : undefined
                        }
                        autoComplete="off"
                    />

                    <InputPhone
                        id="phone"
                        name="phone"
                        label="Phone: *"
                        columnClasses="col-12 md:col-6"
                        onChange={
                            formik.handleChange
                        }
                        value={
                            formik.values.phone
                        }
                        error={
                            formik.touched.phone
                                ? formik.errors.phone
                                : undefined
                        }
                        autoComplete="off"
                    />
                </div>

                <div
                    style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                        marginTop: "1rem"
                    }}
                >
                    <Button
                        type="submit"
                        className="registration-yellow-button"
                        disabled={
                            formik.isSubmitting
                        }
                    >
                        {formik.isSubmitting
                            ? "Saving..."
                            : formik.values.id
                              ? "Update"
                              : "Save"}
                    </Button>

                    <Button
                        type="button"
                        className="registration-yellow-button"
                        disabled={
                            formik.isSubmitting
                        }
                        onClick={handleBack}
                    >
                        Back
                    </Button>
                </div>
            </form>

            <style jsx global>{`
                .registration-yellow-button {
                    background-color: #ff9900 !important;
                    border-color: #ff9900 !important;
                    color: #1f1f1f !important;
                    font-weight: 600;
                    transition:
                        background-color 0.2s ease,
                        border-color 0.2s ease,
                        box-shadow 0.2s ease;
                }

                .registration-yellow-button:hover:not(:disabled) {
                    background-color: #e68a00 !important;
                    border-color: #e68a00 !important;
                    color: #1f1f1f !important;
                }

                .registration-yellow-button:active:not(:disabled) {
                    background-color: #cc7a00 !important;
                    border-color: #cc7a00 !important;
                    color: #1f1f1f !important;
                }

                .registration-yellow-button:focus {
                    background-color: #ff9900 !important;
                    border-color: #ff9900 !important;
                    color: #1f1f1f !important;
                    box-shadow:
                        0 0 0 0.2rem
                        rgba(
                            255,
                            153,
                            0,
                            0.3
                        ) !important;
                }

                .registration-yellow-button:disabled {
                    background-color: #ff9900 !important;
                    border-color: #ff9900 !important;
                    color: #1f1f1f !important;
                    opacity: 0.55;
                    cursor: not-allowed;
                }
            `}</style>
        </>
    );
};
