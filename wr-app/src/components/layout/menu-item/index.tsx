import  Link  from "next/link"

interface MenuItemProps {
    href: string;
    label: string;
}

export const MenuItem: React.FC<MenuItemProps> = ( props: MenuItemProps ) => {

    return (

        <li>
            <Link href={ props.href }>
                <span className="icon"></span>{ props.label }
            </Link>            
        </li>
           
    )

}