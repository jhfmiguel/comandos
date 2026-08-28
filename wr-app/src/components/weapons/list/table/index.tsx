import { useState } from 'react'
import { Weapon } from 'api/models/weapons'
import { Button } from 'components'

interface WeaponsTableProps {
    weapons: Array<Weapon>
    onEditWeapon: ( weapon: Weapon ) => void
    onDeleteWeapon: ( weapon: Weapon ) => void
}

export const WeaponsTable: React.FC<WeaponsTableProps> = ( {
    weapons,
    onEditWeapon,
    onDeleteWeapon
} ) => {
    // Estilo repetível para travar o centro vertical em todas as células
    const cellStyle = { verticalAlign: 'middle' };

    return (
        <div className="table-container">
            <table className="table is-bordered is-vcentered is-striped is-hoverable is-fullwidth" style={{ tableLayout: 'fixed' }}>
                <thead className="is-size-6">
                    {/* 💡 DISTRIBUIÇÃO ATUALIZADA COM COLSPAN:
                        Code, SKU e Price = 8% cada (total 24%)
                        Name = 28% 
                        Description = 32%
                        Célula Mesclada de Ações = 16% (Soma das duas de 8%)
                        Total Geral = 100% */}
                    <tr>
                        <th className="has-text-centered" style={{ ...cellStyle, width: '8%' }}>Code</th>
                        <th className="has-text-centered" style={{ ...cellStyle, width: '8%' }}>SKU</th>
                        <th className="has-text-centered" style={{ ...cellStyle, width: '28%' }}>Name</th>
                        <th className="has-text-centered" style={{ ...cellStyle, width: '8%' }}>Price</th>
                        <th className="has-text-centered" style={{ ...cellStyle, width: '32%' }}>Description</th>
                        {/* 💡 ALTERAÇÃO AQUI: As duas THs foram unidas em uma só com colSpan={2} e width de 16% */}
                        <th className="has-text-centered" colSpan={2} style={{ ...cellStyle, width: '16%' }}>Actions</th>
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
    onEditWeapon: ( weapon: Weapon ) => void
    onDeleteWeapon: ( weapon: Weapon ) => void
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

    // Estilo repetível aplicado diretamente em cada TD para manter o centro vertical
    const cellStyle = { verticalAlign: 'middle' };

    return (
        <tr>
            <td className="has-text-centered" style={ cellStyle }>{ weapon.id }</td>
            <td className="has-text-centered" style={ cellStyle }>{ weapon.sku }</td>
            
            <td 
                style={{ 
                    ...cellStyle, 
                    overflow: 'hidden', 
                    textOverflow: 'ellipsis', 
                    whiteSpace: 'nowrap' 
                }}
            >{ weapon.name }</td>
            <td 
                className="has-text-right" 
                style={ cellStyle }
            >{ weapon.priceFormatted || 'R$ 0,00' }</td>
            <td 
                style={{ 
                    ...cellStyle,
                    overflow: 'hidden', 
                    textOverflow: 'ellipsis', 
                    whiteSpace: 'nowrap' 
                }}
            >
                { weapon.description }
            </td>
            
            {/* Mantidas as duas TDs de dados abaixo para que os botões continuem divididos corretamente em suas respectivas colunas */}
            {!deleting ? (
                <td className="has-text-centered" style={cellStyle}>
                    <Button
                        label = 'Edit'
                        columnClasses='is-warning is-rounded m-0'
                        onClick = { e => onEditWeapon( weapon ) } 
                    />
                </td>
            ) : <></>}
            
            <td className="has-text-centered" style={cellStyle}>
                <Button
                    label = { deleting ? 'Yes' : 'Delete' }
                    columnClasses='has-text-dark-red is-rounded m-0'
                    onClick = { e => onDeleteConfirmation( weapon ) } 
                />
            </td>
            
            {deleting ? (
                <td className="has-text-centered" style={cellStyle}>
                    <Button
                        label = 'No'
                        columnClasses='is-rounded is-primary m-0'
                        onClick = { cancelDelete } 
                    />
                </td>
            ) : <></>}
        </tr>
    )
}
