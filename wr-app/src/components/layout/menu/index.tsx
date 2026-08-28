import Link from 'next/link'

import { MenuItem } from "../menu-item";

export const Menu: React.FC = () => {

    return (
        /* Suas configurações originais foram mantidas 100% intactas */
        <aside className="column is-1 is-hidden-mobile has-background-dark">
            
            {/* 💡 Ajustado para height: calc(100vh - 40px) para fazer a div colar no fim do navegador */}
            <div className="pt-10 pl-5 pr-5" style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 40px)', whiteSpace: 'nowrap' }}>
                
                <div>
                    <Link href="/" style={{ color: '#ff9900', textDecoration: 'none' }}>
                        <p className="menu-label has-text-weight-bold" style={{ cursor: 'pointer' }}>
                            Weapons
                        </p>
                    </Link>

                    <ul className="menu-list">
                        <MenuItem href="/" label="Dashboard"/>
                        <MenuItem href="/queries/users" label="User"/>
                        <MenuItem href="/queries/weapons" label="Weapon"/>
                    </ul>
                </div>

                {/* Item Log Out empurrado de forma definitiva para a base da janela */}
                <ul className="menu-list custom-logout-list" style={{ marginTop: 'auto', marginBottom: '2rem' }}>
                    <MenuItem href="/" label="Log Out"/>
                </ul>

            </div>

        </aside>
    )
}