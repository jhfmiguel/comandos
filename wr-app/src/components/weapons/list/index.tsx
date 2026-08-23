'use client';

import { useState, useEffect } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import useSWR from 'swr'
import { AxiosResponse } from 'axios'

import { Layout, Loader } from 'components'
import { WeaponsTable } from './table'
import { Weapon } from 'api/models/weapons'
import { httpClient } from 'api/http'
import { useWeaponService } from 'api/services'
import { Alert } from 'components/common/message'


export const WeaponsList: React.FC = () => {

    const router = useRouter()

    const service = useWeaponService();

    const [ message, setMessage ] = useState<Array<Alert>>([])
    const [ list, setList ] = useState<Weapon[]>([])

    
    // --- RETURN ALL DATA FROM THE WEAPONS TABLE ---

    const weapons: Weapon[] = []
    const { data, error } = 
        useSWR<AxiosResponse<Weapon[]>>
        ( '/api/weapons', (url: string) => httpClient.get( url ) )


    // ---  ---

    useEffect( () => {

        setList( data?.data || [] )

    }, [ data ] )

    
    // --- EDIT A WEAPON ---
    
    const editWeapon = ( weapon: Weapon ) => {
        
        const url = `/registrations/weapons?id=${weapon.id}`
        router.push( url )

    }

    // --- DELETS A WEAPON ---
    
    const deleteWeapon = ( weapon: Weapon ) => {
        
        service.deleteWeapon( weapon.id ).then( response => {
            
            setMessage([
                { type: "success", text: "Weapon successfully deleted."}
            ])

            const newList: Weapon[] = list?.filter( p => p.id != weapon.id)
            setList (newList)

        })
    }
    
    
    return (

        <Layout 
            title = "Weapons" 
            message = { message } 
        >

            <Link href = "/registrations/weapons">
                <button className="button is-info">New</button>
            </Link>

            <br />
            <br />

            <Loader show = { !data } />

            <WeaponsTable
                onEditWeapon = { editWeapon }
                onDeleteWeapon = { deleteWeapon }
                weapons = { list }
            />

        </Layout>

    )

}