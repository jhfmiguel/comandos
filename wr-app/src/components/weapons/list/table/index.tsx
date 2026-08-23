import { useState } from 'react'

import { Weapon } from 'api/models/weapons'
import { Button } from 'components'


interface WeaponsTableProps {

    weapons: Array<Weapon>
    onEditWeapon: ( weapon: any ) => void
    onDeleteWeapon: ( weapon: any ) => void

}

export const WeaponsTable: React.FC<WeaponsTableProps> = ( {

    weapons,
    onEditWeapon,
    onDeleteWeapon

} ) => {

    return (

        <div className="table-container">

            <table className="table is-striped is-hoverable is-fullwidth">

                <thead className="has-text-centered is-size-6">
                    <tr>
                        <th>Code</th>
                        <th>SKU</th>
                        <th>Name</th>
                        <th>Price</th>
                        <th>Description</th>
                        <th></th>
                        <th></th>
                    </tr>
                </thead>

                <tbody className="is-size-6">

                {
                    weapons.map( weapon => 
                        <WeaponRow
                            key={weapon.id} 
                            weapon = { weapon }
                            onEditWeapon = { onEditWeapon }
                            onDeleteWeapon = { onDeleteWeapon } 
                        /> 
                    )
                }

                </tbody>

            </table>

        </div>


    )

}

interface WeaponRowProps {

    weapon: Weapon
    onEditWeapon: ( weapon: any ) => void
    onDeleteWeapon: ( weapon: any ) => void

}

const WeaponRow: React.FC<WeaponRowProps> = ( {

    weapon,
    onDeleteWeapon,
    onEditWeapon

}) => {

    const [ deleting, setDeleting ] = useState<boolean>(false)

    const onDeleteConfirmation = ( weapon: Weapon ) => {

        if( deleting ) {

            onDeleteWeapon( weapon )
            setDeleting( false )

        } else {

            setDeleting( true )

        }

    }

    const cancelDelete = () => setDeleting( false )

    return (

        <tr>
            <td>{ weapon.id }</td>
            <td>{ weapon.sku }</td>
            <td>{ weapon.name }</td>
            <td>{ weapon.price }</td>
            <td>{ weapon.description }</td>
            
            { !deleting &&
            
            <td>

                <Button
                    label = 'Edit'
                    columnClasses='is-warning is-rounded is-small'
                    onClick = { e => onEditWeapon( weapon ) } 
                />

            </td>

            }
                
            <td>
                <Button
                    label = { deleting ? 'Yes' : 'Delete' }
                    columnClasses='has-text-dark-red is-rounded is-small'
                    onClick = { e => onDeleteConfirmation( weapon ) } 
                />
            </td>
           
            { deleting &&
            
            <td>
                <Button
                    label = 'No'
                    columnClasses=' is-rounded is-small'
                    onClick = { cancelDelete } 
                />
            </td>

            }

        </tr>

    )

}