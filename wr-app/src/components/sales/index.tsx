"use client"

import { Layout } from 'components'
import { Sale } from 'api/models/sales'
import { SalesForm } from './form'

export const Sales: React.FC = () => {

    const handleSubmit = ( sale: Sale ) => {

        console.log( sale )

    }


    return (

        <Layout title = "Sale">

            <SalesForm onSubmit = { handleSubmit } />

        </Layout>

    )

}