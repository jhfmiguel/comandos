import { NextResponse } from "next/server";

type CountriesNowItem = {
    name?: string;
    Iso2?: string;
    Iso3?: string;
};

type CountriesNowResponse = {
    error?: boolean;
    msg?: string;
    data?: CountriesNowItem[];
};

export const revalidate = 86400;

export async function GET() {
    try {
        const response = await fetch(
            "https://countriesnow.space/api/v0.1/countries/iso",
            {
                next: { revalidate: 86400 },
                headers: {
                    Accept: "application/json"
                }
            }
        );

        if (!response.ok) {
            return NextResponse.json(
                { message: "Não foi possível carregar os países." },
                { status: 502 }
            );
        }

        const payload = (await response.json()) as CountriesNowResponse;
        const displayNames = new Intl.DisplayNames(["pt-BR"], { type: "region" });

        const countries = (payload.data ?? [])
            .map(item => {
                const iso2 = String(item.Iso2 ?? "").toUpperCase();
                if (!iso2) return null;

                const label = displayNames.of(iso2) ?? item.name ?? iso2;
                return {
                    value: label,
                    label,
                    iso2
                };
            })
            .filter((item): item is { value: string; label: string; iso2: string } => item !== null)
            .sort((left, right) => left.label.localeCompare(right.label, "pt-BR"));

        if (!countries.length) {
            return NextResponse.json(
                { message: "A consulta de países não retornou resultados." },
                { status: 502 }
            );
        }

        return NextResponse.json(countries);
    } catch {
        return NextResponse.json(
            { message: "Não foi possível carregar os países." },
            { status: 502 }
        );
    }
}
