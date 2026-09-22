"use client";

import {
    useEffect,
    useState
} from "react";

import {
    useRouter,
    useSearchParams
} from "next/navigation";

import * as yup from "yup";

import { Button as PrimeButton } from "@primereact/ui/button";

import {
    Layout,
    Input,
    InputMoney,
    Textarea
} from "components";

import { Alert } from "components/common/message";
import { useComandosPreferences } from "components/settings/preferences-provider";

import { Weapon } from "api/models/weapons";
import { useWeaponService } from "api/services";

import {
    convertToBigDecimal,
    formatReal
} from "utils/money";

const requiredField = "Required field.";
const moreThanTen = "The value must be greater than 10 characters.";
const moreThanZero = "The value must be greater than zero.";

const validationSchema = yup.object().shape({
    sku: yup
        .string()
        .trim()
        .required(requiredField),

    price: yup
        .number()
        .required(requiredField)
        .moreThan(0, moreThanZero),

    name: yup
        .string()
        .trim()
        .required(requiredField),

    description: yup
        .string()
        .trim()
        .required(requiredField)
        .min(11, moreThanTen)
});

interface MessageErrors {
    sku?: string;
    price?: string;
    name?: string;
    description?: string;
}

export const WeaponsRegistration: React.FC = () => {
    const router = useRouter();
    const { locale } = useComandosPreferences();
    const searchParams = useSearchParams();

    const service = useWeaponService();

    const queryId = searchParams.get("id");

    const [message, setMessage] = useState<Alert[]>([]);
    const [messageError, setMessageError] = useState<MessageErrors>({});
    const [saving, setSaving] = useState<boolean>(false);

    const [id, setId] = useState<string | undefined>("");
    const [creationDate, setCreationDate] = useState<string | undefined>("");
    const [sku, setSku] = useState<string>("");
    const [price, setPrice] = useState<string>("");
    const [name, setName] = useState<string>("");
    const [description, setDescription] = useState<string>("");

    useEffect(() => {
        if (!queryId) {
            return;
        }

        service
            .loadWeapon(queryId)
            .then((weaponFound) => {
                setId(weaponFound.id);
                setCreationDate(weaponFound.creationDate ?? "");
                setSku(weaponFound.sku ?? "");

                if (weaponFound.price) {
                    const priceAsString =
                        typeof weaponFound.price === "number"
                            ? (weaponFound.price * 100).toFixed(0)
                            : weaponFound.price;

                    setPrice(
                        formatReal(
                            priceAsString.toString()
                        )
                    );
                } else {
                    setPrice("");
                }

                setName(weaponFound.name ?? "");
                setDescription(weaponFound.description ?? "");
            })
            .catch((error) => {
                console.error(
                    "Failed to load weapon:",
                    error
                );

                setMessage([
                    {
                        type: "danger",
                        text: "An error occurred while loading the weapon."
                    }
                ]);
            });

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [queryId]);

    const submit = async (): Promise<void> => {
        const numericPrice = convertToBigDecimal(price, locale);

        const weapon: Weapon = {
            id,
            sku,
            price: numericPrice,
            name,
            description
        };

        setSaving(true);

        try {
            await validationSchema.validate(
                weapon,
                {
                    abortEarly: true
                }
            );

            setMessageError({});

            if (id) {
                await service.updateWeapon(weapon);

                setMessage([
                    {
                        type: "success",
                        text: "Weapon successfully updated."
                    }
                ]);

                return;
            }

            const weaponResponse =
                await service.saveWeapon(weapon);

            setId(weaponResponse.id);
            setCreationDate(weaponResponse.creationDate);

            setMessage([
                {
                    type: "success",
                    text: "Weapon successfully registered."
                }
            ]);
        } catch (error) {
            if (error instanceof yup.ValidationError) {
                const field =
                    error.path as keyof MessageErrors | undefined;

                if (field) {
                    setMessageError({
                        [field]: error.message
                    });
                }

                return;
            }

            console.error(
                "Failed to save weapon:",
                error
            );

            setMessage([
                {
                    type: "danger",
                    text: "An error occurred while saving the weapon."
                }
            ]);
        } finally {
            setSaving(false);
        }
    };

    const handleBack = (): void => {
        router.push("/queries/weapons");
    };

    return (
        <>
            <Layout
                title="Weapons Registration"
                message={message}
            >
                {id && (
                    <div className="formgrid grid">
                        <Input
                            label="Code"
                            columnClasses="col-12 md:col-6"
                            value={id}
                            id="inputIdCode"
                            disabled
                        />

                        <Input
                            label="Registration Date"
                            columnClasses="col-12 md:col-6"
                            value={creationDate}
                            id="inputRegistrationDate"
                            disabled
                        />
                    </div>
                )}

                <div className="formgrid grid">
                    <Input
                        label="SKU *"
                        columnClasses="col-12 md:col-6"
                        onChange={(event) => {
                            setSku(event.target.value);
                        }}
                        value={sku}
                        id="inputSku"
                        placeholder="Type the SKU"
                        error={messageError.sku}
                    />

                    <InputMoney
                        label="Price *"
                        columnClasses="col-12 md:col-6"
                        onChange={(event) => {
                            setPrice(event.target.value);
                        }}
                        value={price}
                        id="inputPrice"
                        placeholder="Type the price"
                        maxLength={18}
                        error={messageError.price}
                    />
                </div>

                <div className="formgrid grid">
                    <Input
                        label="Name *"
                        columnClasses="col-12"
                        onChange={(event) => {
                            setName(event.target.value);
                        }}
                        value={name}
                        id="inputName"
                        placeholder="Type the name"
                        error={messageError.name}
                    />
                </div>

                <div className="formgrid grid">
                    <Textarea
                        label="Description"
                        columnClasses="col-12"
                        onChange={(event) => {
                            setDescription(event.target.value);
                        }}
                        value={description}
                        id="textareaDescription"
                        placeholder="Type the description"
                        error={messageError.description}
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
                    <PrimeButton
                        type="button"
                        className="registration-yellow-button"
                        disabled={saving}
                        onClick={submit}
                    >
                        {saving
                            ? "Saving..."
                            : id
                              ? "Update"
                              : "Save"}
                    </PrimeButton>

                    <PrimeButton
                        type="button"
                        className="registration-yellow-button"
                        disabled={saving}
                        onClick={handleBack}
                    >
                        Back
                    </PrimeButton>
                </div>
            </Layout>

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
                        rgba(255, 153, 0, 0.3) !important;
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
