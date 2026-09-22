import { AxiosResponse } from "axios";
import { useMemo } from "react";

import { httpClient } from "api/http";
import { Page } from "api/models/common/page";
import { Weapon } from "api/models/weapons";

const resourceURL = "api/weapons";

export interface WeaponSearchFilters {
    sku: string;
    name: string;
    price: string;
    description: string;
}

export const useWeaponService = () => {
    return useMemo(() => {
    const saveWeapon = async (
        weapon: Weapon
    ): Promise<Weapon> => {
        const response: AxiosResponse<Weapon> =
            await httpClient.post<Weapon>(
                resourceURL,
                weapon
            );

        return response.data;
    };

    const updateWeapon = async (
        weapon: Weapon
    ): Promise<void> => {
        const url = `${resourceURL}/${weapon.id}`;

        await httpClient.put<Weapon>(
            url,
            weapon
        );
    };

    const loadWeapon = async (
        id: string | number
    ): Promise<Weapon> => {
        const url = `${resourceURL}/${id}`;

        const response: AxiosResponse<Weapon> =
            await httpClient.get<Weapon>(url);

        return response.data;
    };

    const deleteWeapon = async (
        id: string | number
    ): Promise<void> => {
        const url = `${resourceURL}/${id}`;

        await httpClient.delete(url);
    };

    const findWeapon = async (
        filters: WeaponSearchFilters,
        page = 0,
        size = 10,
        signal?: AbortSignal
    ): Promise<Page<Weapon>> => {
        const params = new URLSearchParams({
            sku: filters.sku,
            name: filters.name,
            price: filters.price,
            description: filters.description,
            page: page.toString(),
            size: size.toString()
        });

        const response: AxiosResponse<Page<Weapon>> =
            await httpClient.get(
                `${resourceURL}?${params.toString()}`,
                { signal }
            );

        return response.data;
    };

    return {
        saveWeapon,
        updateWeapon,
        loadWeapon,
        deleteWeapon,
        findWeapon
    };
    }, []);
};
