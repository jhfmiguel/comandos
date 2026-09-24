"use client";

import * as React from "react";
import { PostalCodeField } from "components/erp/core/postal-code-field";
import styles from "./workspace.module.css";

export type AddressDraft = {
    type: string;
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
    type: string;
    countryCode: string;
    number: string;
    whatsapp: boolean;
    primaryPhone: boolean;
};

export type EmailDraft = {
    type: string;
    email: string;
    primaryEmail: boolean;
};

const newAddress = (primary: boolean): AddressDraft => ({
    type: "RESIDENTIAL",
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
    type: "MOBILE",
    countryCode: "+55",
    number: "",
    whatsapp: false,
    primaryPhone: primary
});

const newEmail = (primary: boolean): EmailDraft => ({
    type: "PERSONAL",
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

    if (countryDigits !== "55") {
        return digits;
    }

    const brazilian = digits.slice(0, 11);
    if (brazilian.length <= 2) return brazilian ? `(${brazilian}` : "";

    const area = brazilian.slice(0, 2);
    const local = brazilian.slice(2);

    if (local.length <= 4) return `(${area}) ${local}`;
    if (local.length <= 8) return `(${area}) ${local.slice(0, 4)}-${local.slice(4)}`;

    return `(${area}) ${local.slice(0, 5)}-${local.slice(5, 9)}`;
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
    const updateAddress = (index: number, patch: Partial<AddressDraft>) => {
        setAddresses(current => current.map((item, itemIndex) => {
            if (itemIndex !== index) {
                return patch.primaryAddress ? { ...item, primaryAddress: false } : item;
            }
            return { ...item, ...patch };
        }));
    };

    const updatePhone = (index: number, patch: Partial<PhoneDraft>) => {
        setPhones(current => current.map((item, itemIndex) => {
            if (itemIndex !== index) {
                return patch.primaryPhone ? { ...item, primaryPhone: false } : item;
            }
            return { ...item, ...patch };
        }));
    };

    const updateEmail = (index: number, patch: Partial<EmailDraft>) => {
        setEmails(current => current.map((item, itemIndex) => {
            if (itemIndex !== index) {
                return patch.primaryEmail ? { ...item, primaryEmail: false } : item;
            }
            return { ...item, ...patch };
        }));
    };

    return (
        <div className={styles.contactSections}>
            <section className={styles.contactSection}>
                <div className={styles.contactHeader}>
                    <div>
                        <h3>Endereços</h3>
                        <small>Adicione vários endereços. No Brasil, o CEP consulta e preenche os dados; no exterior, use país e código postal livre.</small>
                    </div>
                    <button type="button" className="comandos-secondary-button"
                        onClick={() => setAddresses(current => [...current, newAddress(current.length === 0)])}>
                        Adicionar endereço
                    </button>
                </div>

                {addresses.map((address, index) => {
                    const prefix = "person-address-" + index;
                    return (
                        <div className={styles.contactCard} key={prefix}>
                            <div className={styles.contactCardHeader}>
                                <strong>Endereço {index + 1}</strong>
                                <button type="button" className="comandos-secondary-button"
                                    onClick={() => setAddresses(current => current.filter((_, itemIndex) => itemIndex !== index))}>
                                    Remover
                                </button>
                            </div>
                            <div className={styles.contactGrid}>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-type"}>Tipo *</label>
                                    <select id={prefix + "-type"} required value={address.type}
                                        onChange={event => updateAddress(index, { type: event.target.value })}>
                                        <option value="RESIDENTIAL">Residencial</option>
                                        <option value="BUSINESS">Comercial</option>
                                        <option value="MAILING">Correspondência</option>
                                        <option value="OTHER">Outro</option>
                                    </select>
                                </div>

                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-foreign"}>Endereço no exterior</label>
                                    <input id={prefix + "-foreign"} type="checkbox" checked={address.foreignAddress}
                                        onChange={event => updateAddress(index, {
                                            foreignAddress: event.target.checked,
                                            country: event.target.checked ? "" : "Brasil",
                                            postalCode: ""
                                        })} />
                                </div>

                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-country"}>País{address.foreignAddress ? " *" : ""}</label>
                                    <input id={prefix + "-country"} required={address.foreignAddress}
                                        value={address.country} readOnly={!address.foreignAddress}
                                        onChange={event => updateAddress(index, { country: event.target.value })} />
                                </div>

                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-postal"}>{address.foreignAddress ? "Código postal" : "CEP *"}</label>
                                    {address.foreignAddress ? (
                                        <input id={prefix + "-postal"} value={address.postalCode} maxLength={30}
                                            onChange={event => updateAddress(index, { postalCode: event.target.value })} />
                                    ) : (
                                        <PostalCodeField id={prefix + "-postal"} value={address.postalCode}
                                            onChange={value => updateAddress(index, { postalCode: value })}
                                            onResolved={resolved => updateAddress(index, {
                                                street: resolved.street || address.street,
                                                district: resolved.district || address.district,
                                                city: resolved.city || address.city,
                                                state: resolved.state || address.state,
                                                country: "Brasil"
                                            })} />
                                    )}
                                </div>

                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-street"}>Logradouro *</label>
                                    <input id={prefix + "-street"} required value={address.street}
                                        onChange={event => updateAddress(index, { street: event.target.value })} />
                                </div>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-number"}>Número *</label>
                                    <input id={prefix + "-number"} required value={address.number}
                                        onChange={event => updateAddress(index, { number: event.target.value })} />
                                </div>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-complement"}>Complemento</label>
                                    <input id={prefix + "-complement"} value={address.complement}
                                        onChange={event => updateAddress(index, { complement: event.target.value })} />
                                </div>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-district"}>Bairro / Distrito</label>
                                    <input id={prefix + "-district"} value={address.district}
                                        onChange={event => updateAddress(index, { district: event.target.value })} />
                                </div>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-city"}>Cidade *</label>
                                    <input id={prefix + "-city"} required value={address.city}
                                        onChange={event => updateAddress(index, { city: event.target.value })} />
                                </div>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-state"}>
                                        {address.foreignAddress ? "Estado / Província / Região *" : "UF *"}
                                    </label>
                                    <input id={prefix + "-state"} required value={address.state}
                                        maxLength={address.foreignAddress ? 120 : 2}
                                        onChange={event => updateAddress(index, { state: event.target.value })} />
                                </div>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-primary"}>Endereço principal</label>
                                    <input id={prefix + "-primary"} type="checkbox" checked={address.primaryAddress}
                                        onChange={event => updateAddress(index, { primaryAddress: event.target.checked })} />
                                </div>
                            </div>
                        </div>
                    );
                })}
            </section>

            <section className={styles.contactSection}>
                <div className={styles.contactHeader}>
                    <div>
                        <h3>Telefones</h3>
                        <small>Adicione vários telefones, marque o principal e indique se possui WhatsApp.</small>
                    </div>
                    <button type="button" className="comandos-secondary-button"
                        onClick={() => setPhones(current => [...current, newPhone(current.length === 0)])}>
                        Adicionar telefone
                    </button>
                </div>

                {phones.map((phone, index) => {
                    const prefix = "person-phone-" + index;
                    return (
                        <div className={styles.contactCard} key={prefix}>
                            <div className={styles.contactCardHeader}>
                                <strong>Telefone {index + 1}</strong>
                                <button type="button" className="comandos-secondary-button"
                                    onClick={() => setPhones(current => current.filter((_, itemIndex) => itemIndex !== index))}>
                                    Remover
                                </button>
                            </div>
                            <div className={styles.contactGrid}>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-type"}>Tipo *</label>
                                    <select id={prefix + "-type"} required value={phone.type}
                                        onChange={event => updatePhone(index, { type: event.target.value })}>
                                        <option value="MOBILE">Celular</option>
                                        <option value="WHATSAPP">WhatsApp</option>
                                        <option value="HOME">Residencial</option>
                                        <option value="WORK">Comercial</option>
                                        <option value="OTHER">Outro</option>
                                    </select>
                                </div>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-country"}>Código do país *</label>
                                    <input id={prefix + "-country"} required inputMode="tel" value={phone.countryCode} maxLength={5}
                                        placeholder="+55"
                                        onChange={event => {
                                            const countryCode = maskCountryCode(event.target.value);
                                            updatePhone(index, {
                                                countryCode,
                                                number: maskPhoneNumber(phone.number, countryCode)
                                            });
                                        }} />
                                </div>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-number"}>Telefone *</label>
                                    <input id={prefix + "-number"} required type="tel" inputMode="tel"
                                        value={maskPhoneNumber(phone.number, phone.countryCode)}
                                        maxLength={20}
                                        placeholder={phone.countryCode.replace(/\D/g, "") === "55" ? "(00) 00000-0000" : "Somente números"}
                                        onChange={event => updatePhone(index, {
                                            number: maskPhoneNumber(event.target.value, phone.countryCode)
                                        })} />
                                </div>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-whatsapp"}>WhatsApp</label>
                                    <input id={prefix + "-whatsapp"} type="checkbox" checked={phone.whatsapp}
                                        onChange={event => updatePhone(index, { whatsapp: event.target.checked })} />
                                </div>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-primary"}>Telefone principal</label>
                                    <input id={prefix + "-primary"} type="checkbox" checked={phone.primaryPhone}
                                        onChange={event => updatePhone(index, { primaryPhone: event.target.checked })} />
                                </div>
                            </div>
                        </div>
                    );
                })}
            </section>

            <section className={styles.contactSection}>
                <div className={styles.contactHeader}>
                    <div>
                        <h3>E-mails</h3>
                        <small>Adicione vários e-mails e defina qual é o principal.</small>
                    </div>
                    <button type="button" className="comandos-secondary-button"
                        onClick={() => setEmails(current => [...current, newEmail(current.length === 0)])}>
                        Adicionar e-mail
                    </button>
                </div>

                {emails.map((email, index) => {
                    const prefix = "person-email-" + index;
                    return (
                        <div className={styles.contactCard} key={prefix}>
                            <div className={styles.contactCardHeader}>
                                <strong>E-mail {index + 1}</strong>
                                <button type="button" className="comandos-secondary-button"
                                    onClick={() => setEmails(current => current.filter((_, itemIndex) => itemIndex !== index))}>
                                    Remover
                                </button>
                            </div>
                            <div className={styles.contactGrid}>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-type"}>Tipo *</label>
                                    <select id={prefix + "-type"} required value={email.type}
                                        onChange={event => updateEmail(index, { type: event.target.value })}>
                                        <option value="PERSONAL">Pessoal</option>
                                        <option value="WORK">Profissional</option>
                                        <option value="OTHER">Outro</option>
                                    </select>
                                </div>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-address"}>E-mail *</label>
                                    <input id={prefix + "-address"} required type="email" value={email.email} maxLength={255}
                                        onChange={event => updateEmail(index, { email: event.target.value })} />
                                </div>
                                <div className={styles.field}>
                                    <label htmlFor={prefix + "-primary"}>E-mail principal</label>
                                    <input id={prefix + "-primary"} type="checkbox" checked={email.primaryEmail}
                                        onChange={event => updateEmail(index, { primaryEmail: event.target.checked })} />
                                </div>
                            </div>
                        </div>
                    );
                })}
            </section>
        </div>
    );
}
