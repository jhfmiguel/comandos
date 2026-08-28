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
        <div className="m-0 p-0 app" style={{ minWidth: '100vw', minHeight: '100vh' }}>

            <section className="main-content columns is-gapless" style={{ minHeight: '100vh' }}>

                <Menu />

                <div className="is-11 column">

                    {/* 💡 ALTERAÇÃO AQUI: 
                        Utilizado "p-0" para zerar o padding de todos os lados (topo, baixo, esquerda e direita).
                        O card agora vai encostar perfeitamente nas bordas e no Menu lateral. */}
                    <div className="p-10">

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
