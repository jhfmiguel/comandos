import { ReactNode } from 'react'

import { Menu } from './menu'
import { Message } from 'components'
import { Alert } from 'components/common/message'

interface LayoutProps {
    title?: string
    children?: ReactNode
    message?: Array<Alert>
}

export const Layout: React.FC<LayoutProps> = ( props: LayoutProps ) => {

    return (

        <div className="app">

            <section className="main-content columns is-fullheight">

                <Menu />

                <div className="container column is-10">

                    <div className="section">

                        <div className="card">

                            <div className="card-header">

                                <p className="card-header-title">
                                    { props.title }
                                </p>

                            </div>

                            <div className="card-content">

                                <div className="content">
                                
                                { props.message &&
                                    props.message.map((msg, index) => (
                                        <Message key={index} {...msg} />
                                    ))
                                }
                    
                                    { props.children }

                                </div>

                            </div>

                        </div>

                    </div>

                </div>

            </section>

        </div>

    )

}