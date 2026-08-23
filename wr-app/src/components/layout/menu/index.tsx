import { MenuItem } from "../menu-item";

export const Menu: React.FC = () => {

    return (

        <aside className="column is-2 is-narrow-mobile is-fullheight section is-hidden-mobile">
            
            <p className="menu-label is-hidden-touch">
                Weapons
            </p>

             <ul className="menu-list">

                <MenuItem href="/" label="Home"/>
                <MenuItem href="/queries/weapons" label="Registration"/>
                <MenuItem href="/" label="Config"/>
                <MenuItem href="/" label="Log Out"/>

             </ul>

        </aside>

    )

}