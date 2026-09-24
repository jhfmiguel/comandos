"use client";

import * as React from "react";
import { ChevronDown, Plus, Trash2 } from "lucide-react";
import { ComandosSelectField } from "components/common/select-field";
import { PostalCodeField } from "components/erp/core/postal-code-field";
import { createErpService } from "api/services/erp.service";
import type { ErpRecord } from "api/models/erp";
import styles from "./workspace.module.css";

const core = createErpService("core");

export type AddressDraft = {
    contactTypeId: string;
    foreignAddress: boolean;
    country: string;
    postalCode: string;
    street: string;
    number: string;
    complement: string;
    district: string;
    city: string;
    state: string;
    primaryAddress: boolean;
};

export type PhoneDraft = {
    contactTypeId: string;
    countryCode: string;
    number: string;
    whatsapp: boolean;
    primaryPhone: boolean;
};

export type EmailDraft = {
    contactTypeId: string;
    email: string;
    primaryEmail: boolean;
};

const newAddress = (primary: boolean): AddressDraft => ({
    contactTypeId: "",
    foreignAddress: false,
    country: "Brasil",
    postalCode: "",
    street: "",
    number: "",
    complement: "",
    district: "",
    city: "",
    state: "",
    primaryAddress: primary
});

const newPhone = (primary: boolean): PhoneDraft => ({
    contactTypeId: "",
    countryCode: "+55",
    number: "",
    whatsapp: false,
    primaryPhone: primary
});

const newEmail = (primary: boolean): EmailDraft => ({
    contactTypeId: "",
    email: "",
    primaryEmail: primary
});

function maskCountryCode(value: string): string {
    const digits = value.replace(/\D/g, "").slice(0, 4);
    return digits ? `+${digits}` : "";
}

function maskPhoneNumber(value: string, countryCode: string): string {
    const digits = value.replace(/\D/g, "").slice(0, 15);
    const countryDigits = countryCode.replace(/\D/g, "");

    if (countryDigits !== "55") return digits;

    const brazilian = digits.slice(0, 11);
    if (brazilian.length <= 2) return brazilian ? `(${brazilian}` : "";

    const area = brazilian.slice(0, 2);
    const local = brazilian.slice(2);

    if (local.length <= 4) return `(${area}) ${local}`;
    if (local.length <= 8) return `(${area}) ${local.slice(0, 4)}-${local.slice(4)}`;

    return `(${area}) ${local.slice(0, 5)}-${local.slice(5, 9)}`;
}


type ContactPanelKey = "addresses" | "phones" | "emails";

function ContactAccordionPanel({
    value,
    title,
    activePanel,
    onToggle,
    children
}: React.PropsWithChildren<{
    value: ContactPanelKey;
    title: string;
    activePanel: ContactPanelKey | null;
    onToggle: (value: ContactPanelKey) => void;
}>) {
    const open = activePanel === value;
    const triggerId = `person-contact-${value}-trigger`;
    const contentId = `person-contact-${value}-content`;

    return (
        <section
            className={styles.contactAccordionPanel}
            data-open={open ? "true" : "false"}
        >
            <h3 className={styles.contactAccordionHeading}>
                <div
                    id={triggerId}
                    role="button"
                    tabIndex={0}
                    className={styles.contactAccordionTrigger}
                    aria-expanded={open}
                    aria-controls={contentId}
                    onClick={() => onToggle(value)}
                    onKeyDown={(event) => {
                        if (event.key === "Enter" || event.key === " ") {
                            event.preventDefault();
                            onToggle(value);
                        }
                    }}
                >
                    <span>{title}</span>
                    <ChevronDown
                        size={18}
                        aria-hidden="true"
                        className={styles.contactAccordionChevron}
                    />
                </div>
            </h3>

            <div
                id={contentId}
                role="region"
                aria-labelledby={triggerId}
                className={styles.contactAccordionContent}
                hidden={!open}
            >
                <div className={styles.contactAccordionContentInner}>
                    {children}
                </div>
            </div>
        </section>
    );
}

export function PersonContactsEditor({
    addresses,
    setAddresses,
    phones,
    setPhones,
    emails,
    setEmails
}: {
    addresses: AddressDraft[];
    setAddresses: React.Dispatch<React.SetStateAction<AddressDraft[]>>;
    phones: PhoneDraft[];
    setPhones: React.Dispatch<React.SetStateAction<PhoneDraft[]>>;
    emails: EmailDraft[];
    setEmails: React.Dispatch<React.SetStateAction<EmailDraft[]>>;
}) {
    const [activePanel, setActivePanel] = React.useState<ContactPanelKey | null>("addresses");
    const [contactTypes, setContactTypes] = React.useState<ErpRecord[]>([]);
    const [contactTypesError, setContactTypesError] = React.useState("");
    const [countryOptions, setCountryOptions] = React.useState<Array<{ value: string; label: string }>>([]);
    const [countriesLoading, setCountriesLoading] = React.useState(false);
    const [countriesError, setCountriesError] = React.useState("");

    React.useEffect(() => {
        const controller = new AbortController();

        core.list("contact-types", "", 0, controller.signal, undefined, {}, 100)
            .then(result => {
                if (controller.signal.aborted) return;
                setContactTypes(
                    result.content
                        .filter(option => option.active !== false)
                        .sort((left, right) =>
                            String(left.name ?? left.label ?? "")
                                .localeCompare(String(right.name ?? right.label ?? ""), "pt-BR")
                        )
                );
                setContactTypesError("");
            })
            .catch(() => {
                if (!controller.signal.aborted) {
                    setContactTypes([]);
                    setContactTypesError("Não foi possível carregar os tipos de contato.");
                }
            });

        return () => controller.abort();
    }, []);


    React.useEffect(() => {
        if (!addresses.some(address => address.foreignAddress) || countryOptions.length > 0 || countriesLoading) {
            return;
        }

        const controller = new AbortController();
        setCountriesLoading(true);

        fetch("/api/countries", {
            signal: controller.signal,
            headers: { Accept: "application/json" }
        })
            .then(async response => {
                if (!response.ok) {
                    const payload = await response.json().catch(() => null) as { message?: string } | null;
                    throw new Error(payload?.message || "Não foi possível carregar os países.");
                }

                return response.json() as Promise<Array<{ value: string; label: string }>>;
            })
            .then(options => {
                if (controller.signal.aborted) return;
                setCountryOptions(options);
                setCountriesError("");
            })
            .catch(error => {
                if (!controller.signal.aborted) {
                    setCountryOptions([]);
                    setCountriesError(error instanceof Error ? error.message : "Não foi possível carregar os países.");
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setCountriesLoading(false);
                }
            });

        return () => controller.abort();
    }, [addresses, countryOptions.length, countriesLoading]);

    React.useEffect(() => {
        const frame = requestAnimationFrame(() => {
            document.dispatchEvent(new Event("comandos:float-label-refresh"));
        });
        return () => cancelAnimationFrame(frame);
    }, [addresses, phones, emails]);

    const togglePanel = (value: ContactPanelKey) => {
        setActivePanel(current => current === value ? null : value);
    };

    const contactTypeOptions = (field: "addressEnabled" | "phoneEnabled" | "emailEnabled") =>
        contactTypes
            .filter(option => option[field] === true)
            .map(option => ({
                value: String(option.id),
                label: String(option.name ?? option.label ?? option.code ?? option.id)
            }));

    const updateAddress = (index: number, patch: Partial<AddressDraft>) => {
        setAddresses(current => current.map((item, itemIndex) => {
            if (itemIndex !== index && patch.primaryAddress) {
                return { ...item, primaryAddress: false };
            }
            return itemIndex === index ? { ...item, ...patch } : item;
        }));
    };

    const updatePhone = (index: number, patch: Partial<PhoneDraft>) => {
        setPhones(current => current.map((item, itemIndex) => {
            if (itemIndex !== index && patch.primaryPhone) {
                return { ...item, primaryPhone: false };
            }
            return itemIndex === index ? { ...item, ...patch } : item;
        }));
    };

    const updateEmail = (index: number, patch: Partial<EmailDraft>) => {
        setEmails(current => current.map((item, itemIndex) => {
            if (itemIndex !== index && patch.primaryEmail) {
                return { ...item, primaryEmail: false };
            }
            return itemIndex === index ? { ...item, ...patch } : item;
        }));
    };

    return (
        <div className={styles.contactAccordion}>
            {contactTypesError && (
                <small className={styles.contactTypeError}>{contactTypesError}</small>
            )}
            {countriesError && addresses.some(address => address.foreignAddress) && (
                <small className={styles.contactTypeError}>{countriesError}</small>
            )}
            <ContactAccordionPanel
                value="addresses"
                title="Endereços"
                activePanel={activePanel}
                onToggle={togglePanel}
            >
                <div className={styles.contactHeader}>
                    <small>Edite diretamente na tabela e marque um endereço principal.</small>
                    <button
                        type="button"
                        className={`registration-yellow-button ${styles.contactAddButton}`}
                        onClick={() => setAddresses(current => [...current, newAddress(current.length === 0)])}
                    >
                        <Plus size={16} />
                        <span>Adicionar endereço</span>
                    </button>
                </div>

                <div className={`comandos-native-table-container ${styles.contactTableScroll} ${styles.addressTableScroll}`}>
                    <table className={`comandos-native-table ${styles.contactTable} ${styles.addressTable}`}>
                        <thead>
                            <tr>
                                <th>Tipo</th>
                                <th>Exterior</th>
                                <th>País</th>
                                <th className={styles.addressPostalCell}>CEP</th>
                                <th>Logradouro</th>
                                <th className={styles.addressNumberCell}>Número</th>
                                <th>Complemento</th>
                                <th>Bairro / Distrito</th>
                                <th>Cidade</th>
                                <th className={styles.addressStateCell}>Estado</th>
                                <th>Principal</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {addresses.map((address, index) => {
                                const prefix = `person-address-${index}`;
                                return (
                                    <tr key={prefix}>
                                        <td>
                                            <ComandosSelectField
                                                id={`person-address-type-${index}`}
                                                label="Tipo"
                                                required
                                                value={address.contactTypeId}
                                                options={contactTypeOptions("addressEnabled")}
                                                onChange={value => updateAddress(index, { contactTypeId: value })}
                                            />
                                        </td>
                                        <td className={`${styles.contactBooleanCell} ${styles.contactBooleanField}`}>
                                            <span className={styles.contactBooleanLabel}>Exterior</span>
                                            <input
                                                aria-label={`Endereço ${index + 1} no exterior`}
                                                type="checkbox"
                                                checked={address.foreignAddress}
                                                onChange={event => updateAddress(index, {
                                                    foreignAddress: event.target.checked,
                                                    country: event.target.checked ? "" : "Brasil",
                                                    postalCode: ""
                                                })}
                                            />
                                        </td>
                                        <td>
                                            {address.foreignAddress ? (
                                                <ComandosSelectField
                                                    id={`person-address-country-${index}`}
                                                    label="País"
                                                    required
                                                    value={address.country}
                                                    options={countryOptions}
                                                    placeholder={countriesLoading ? "Carregando países..." : "Selecione o país"}
                                                    disabled={countriesLoading && countryOptions.length === 0}
                                                    onChange={value => updateAddress(index, { country: value })}
                                                />
                                            ) : (
                                                <input
                                                    aria-label={`País do endereço ${index + 1}`}
                                                    value={address.country}
                                                    readOnly
                                                />
                                            )}
                                        </td>
                                        <td className={styles.addressPostalCell}>
                                            {address.foreignAddress ? (
                                                <input
                                                    aria-label={`Código postal do endereço ${index + 1}`}
                                                    value={address.postalCode}
                                                    maxLength={30}
                                                    onChange={event => updateAddress(index, { postalCode: event.target.value })}
                                                />
                                            ) : (
                                                <PostalCodeField
                                                    id={`${prefix}-postal`}
                                                    compact
                                                    value={address.postalCode}
                                                    onChange={value => updateAddress(index, { postalCode: value })}
                                                    onResolved={resolved => updateAddress(index, {
                                                        street: resolved.street || address.street,
                                                        district: resolved.district || address.district,
                                                        city: resolved.city || address.city,
                                                        state: resolved.state || address.state,
                                                        country: "Brasil"
                                                    })}
                                                />
                                            )}
                                        </td>
                                        <td>
                                            <input
                                                aria-label={`Logradouro do endereço ${index + 1}`}
                                                required
                                                value={address.street}
                                                onChange={event => updateAddress(index, { street: event.target.value })}
                                            />
                                        </td>
                                        <td className={styles.addressNumberCell}>
                                            <input
                                                aria-label={`Número do endereço ${index + 1}`}
                                                required
                                                value={address.number}
                                                onChange={event => updateAddress(index, { number: event.target.value })}
                                            />
                                        </td>
                                        <td>
                                            <input
                                                aria-label={`Complemento do endereço ${index + 1}`}
                                                value={address.complement}
                                                onChange={event => updateAddress(index, { complement: event.target.value })}
                                            />
                                        </td>
                                        <td>
                                            <input
                                                aria-label={`Bairro do endereço ${index + 1}`}
                                                value={address.district}
                                                onChange={event => updateAddress(index, { district: event.target.value })}
                                            />
                                        </td>
                                        <td>
                                            <input
                                                aria-label={`Cidade do endereço ${index + 1}`}
                                                required
                                                value={address.city}
                                                onChange={event => updateAddress(index, { city: event.target.value })}
                                            />
                                        </td>
                                        <td className={styles.addressStateCell}>
                                            <input
                                                aria-label={`Estado do endereço ${index + 1}`}
                                                required
                                                value={address.state}
                                                maxLength={address.foreignAddress ? 120 : 2}
                                                onChange={event => updateAddress(index, { state: event.target.value })}
                                            />
                                        </td>
                                        <td className={`${styles.contactBooleanCell} ${styles.contactBooleanField}`}>
                                            <span className={styles.contactBooleanLabel}>Principal</span>
                                            <input
                                                aria-label={`Endereço ${index + 1} principal`}
                                                type="radio"
                                                name="person-primary-address"
                                                checked={address.primaryAddress}
                                                onChange={() => updateAddress(index, { primaryAddress: true })}
                                            />
                                        </td>
                                        <td className={styles.contactActionCell}>
                                            <button
                                                type="button"
                                                className="comandos-icon-button comandos-icon-button-danger"
                                                data-comandos-contact-delete="true"
                                                data-severity="danger"
                                                aria-label={`Remover endereço ${index + 1}`}
                                                onClick={() => setAddresses(current => current.filter((_, itemIndex) => itemIndex !== index))}
                                            >
                                                <Trash2 size={18} />
                                            </button>
                                        </td>
                                    </tr>
                                );
                            })}
                            {!addresses.length && (
                                <tr>
                                    <td colSpan={12} className={styles.contactEmpty}>
                                        Nenhum endereço adicionado.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </ContactAccordionPanel>

            <ContactAccordionPanel
                value="phones"
                title="Telefones"
                activePanel={activePanel}
                onToggle={togglePanel}
            >
                <div className={styles.contactHeader}>
                    <small>Edite diretamente na tabela, indique WhatsApp e selecione o telefone principal.</small>
                    <button
                        type="button"
                        className={`registration-yellow-button ${styles.contactAddButton}`}
                        onClick={() => setPhones(current => [...current, newPhone(current.length === 0)])}
                    >
                        <Plus size={16} />
                        <span>Adicionar telefone</span>
                    </button>
                </div>

                <div className={`comandos-native-table-container ${styles.contactTableScroll} ${styles.phoneTableScroll}`}>
                    <table className={`comandos-native-table ${styles.contactTable} ${styles.phoneTable}`}>
                        <thead>
                            <tr>
                                <th>Tipo</th>
                                <th>Código do país</th>
                                <th>Telefone</th>
                                <th>WhatsApp</th>
                                <th>Principal</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {phones.map((phone, index) => (
                                <tr key={`person-phone-${index}`}>
                                    <td>
                                        <ComandosSelectField
                                            id={`person-phone-type-${index}`}
                                            label="Tipo"
                                            required
                                            value={phone.contactTypeId}
                                            options={contactTypeOptions("phoneEnabled")}
                                            onChange={value => updatePhone(index, { contactTypeId: value })}
                                        />
                                    </td>
                                    <td>
                                        <input
                                            aria-label={`Código do país do telefone ${index + 1}`}
                                            required
                                            inputMode="tel"
                                            value={phone.countryCode}
                                            maxLength={5}
                                            placeholder="+55"
                                            onChange={event => {
                                                const countryCode = maskCountryCode(event.target.value);
                                                updatePhone(index, {
                                                    countryCode,
                                                    number: maskPhoneNumber(phone.number, countryCode)
                                                });
                                            }}
                                        />
                                    </td>
                                    <td>
                                        <input
                                            aria-label={`Telefone ${index + 1}`}
                                            required
                                            type="tel"
                                            inputMode="tel"
                                            value={maskPhoneNumber(phone.number, phone.countryCode)}
                                            maxLength={20}
                                            placeholder={phone.countryCode.replace(/\D/g, "") === "55" ? "(00) 00000-0000" : "Somente números"}
                                            onChange={event => updatePhone(index, {
                                                number: maskPhoneNumber(event.target.value, phone.countryCode)
                                            })}
                                        />
                                    </td>
                                    <td className={`${styles.contactBooleanCell} ${styles.contactBooleanField}`}>
                                        <span className={styles.contactBooleanLabel}>WhatsApp</span>
                                        <input
                                            aria-label={`Telefone ${index + 1} possui WhatsApp`}
                                            type="checkbox"
                                            checked={phone.whatsapp}
                                            onChange={event => updatePhone(index, { whatsapp: event.target.checked })}
                                        />
                                    </td>
                                    <td className={`${styles.contactBooleanCell} ${styles.contactBooleanField}`}>
                                        <span className={styles.contactBooleanLabel}>Principal</span>
                                        <input
                                            aria-label={`Telefone ${index + 1} principal`}
                                            type="radio"
                                            name="person-primary-phone"
                                            checked={phone.primaryPhone}
                                            onChange={() => updatePhone(index, { primaryPhone: true })}
                                        />
                                    </td>
                                    <td className={styles.contactActionCell}>
                                        <button
                                            type="button"
                                            className="comandos-icon-button comandos-icon-button-danger"
                                            data-comandos-contact-delete="true"
                                            data-severity="danger"
                                            aria-label={`Remover telefone ${index + 1}`}
                                            onClick={() => setPhones(current => current.filter((_, itemIndex) => itemIndex !== index))}
                                        >
                                            <Trash2 size={18} />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {!phones.length && (
                                <tr>
                                    <td colSpan={6} className={styles.contactEmpty}>
                                        Nenhum telefone adicionado.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </ContactAccordionPanel>

            <ContactAccordionPanel
                value="emails"
                title="E-mails"
                activePanel={activePanel}
                onToggle={togglePanel}
            >
                <div className={styles.contactHeader}>
                    <small>Edite diretamente na tabela e selecione o e-mail principal.</small>
                    <button
                        type="button"
                        className={`registration-yellow-button ${styles.contactAddButton}`}
                        onClick={() => setEmails(current => [...current, newEmail(current.length === 0)])}
                    >
                        <Plus size={16} />
                        <span>Adicionar e-mail</span>
                    </button>
                </div>

                <div className={`comandos-native-table-container ${styles.contactTableScroll} ${styles.emailTableScroll}`}>
                    <table className={`comandos-native-table ${styles.contactTable} ${styles.emailTable}`}>
                        <thead>
                            <tr>
                                <th>Tipo</th>
                                <th>E-mail</th>
                                <th>Principal</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {emails.map((email, index) => (
                                <tr key={`person-email-${index}`}>
                                    <td>
                                        <ComandosSelectField
                                            id={`person-email-type-${index}`}
                                            label="Tipo"
                                            required
                                            value={email.contactTypeId}
                                            options={contactTypeOptions("emailEnabled")}
                                            onChange={value => updateEmail(index, { contactTypeId: value })}
                                        />
                                    </td>
                                    <td>
                                        <input
                                            aria-label={`E-mail ${index + 1}`}
                                            required
                                            type="email"
                                            value={email.email}
                                            maxLength={255}
                                            onChange={event => updateEmail(index, { email: event.target.value })}
                                        />
                                    </td>
                                    <td className={`${styles.contactBooleanCell} ${styles.contactBooleanField}`}>
                                        <span className={styles.contactBooleanLabel}>Principal</span>
                                        <input
                                            aria-label={`E-mail ${index + 1} principal`}
                                            type="radio"
                                            name="person-primary-email"
                                            checked={email.primaryEmail}
                                            onChange={() => updateEmail(index, { primaryEmail: true })}
                                        />
                                    </td>
                                    <td className={styles.contactActionCell}>
                                        <button
                                            type="button"
                                            className="comandos-icon-button comandos-icon-button-danger"
                                            data-comandos-contact-delete="true"
                                            data-severity="danger"
                                            aria-label={`Remover e-mail ${index + 1}`}
                                            onClick={() => setEmails(current => current.filter((_, itemIndex) => itemIndex !== index))}
                                        >
                                            <Trash2 size={18} />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {!emails.length && (
                                <tr>
                                    <td colSpan={4} className={styles.contactEmpty}>
                                        Nenhum e-mail adicionado.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </ContactAccordionPanel>
        </div>
    );
}
