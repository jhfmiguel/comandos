"use client";

import { useEffect, useRef, useState } from "react";
import { httpClient } from "api/http";
import axios from "axios";

function maskPostalCode(value: string): string {
    const digits = value.replace(/\D/g, "").slice(0, 8);
    return digits.length > 5
        ? `${digits.slice(0, 5)}-${digits.slice(5)}`
        : digits;
}

export function PostalCodeField({ id = "core-postalCode", value, required = true, onChange, onResolved }: {
    id?: string;
    value: string;
    required?: boolean;
    onChange: (value: string) => void;
    onResolved: (address: Record<string, string>) => void;
}) {
    const [edited, setEdited] = useState(false);
    const [status, setStatus] = useState("");
    const [retry, setRetry] = useState(0);
    const resolved = useRef(onResolved);
    useEffect(() => { resolved.current = onResolved; }, [onResolved]);

    useEffect(() => {
        if (!edited) return;
        const controller = new AbortController();
        const timer = setTimeout(async () => {
            if (!/^[0-9]{5}-?[0-9]{3}$/.test(value)) {
                setStatus("CEP inválido. Informe oito dígitos.");
                return;
            }
            setStatus("Consultando CEP…");
            try {
                const response = await httpClient.get<Record<string, string>>(
                    `/api/erp/core/postal-codes/${value.replace("-", "")}`, { signal: controller.signal, timeout: 8000 });
                if (controller.signal.aborted) return;
                resolved.current(response.data);
                setStatus("CEP consultado. Revise o endereço e informe número e complemento antes de salvar.");
            } catch (error) {
                if (controller.signal.aborted) return;
                setStatus(axios.isAxiosError(error) && error.response?.data?.detail
                    ? String(error.response.data.detail)
                    : "Consulta de CEP indisponível. Preencha manualmente ou tente novamente.");
            }
        }, 500);
        return () => { clearTimeout(timer); controller.abort(); };
    }, [value, edited, retry]);

    return <>
        <input id={id} required={required} inputMode="numeric" maxLength={9} pattern="[0-9]{5}-?[0-9]{3}"
            placeholder="00000-000"
            value={maskPostalCode(value)} onChange={event => {
                setEdited(true);
                setStatus("Aguardando consulta de CEP…");
                onChange(maskPostalCode(event.target.value));
            }} />
        <small role="status" aria-live="polite">{status || "Informe o CEP para consultar. Todos os campos podem ser corrigidos manualmente."}</small>
        <button type="button" className="comandos-secondary-button" onClick={() => { setEdited(true); setRetry(current => current + 1); }}>Consultar novamente</button>
    </>;
}
