"use client";

import React from "react";

import { Layout } from "components";
import { Sale } from "api/models/sales";
import { SalesForm } from "./form";
import { useSaleService } from "api/services";
import { Alert } from "components/common/message";

export const Sales: React.FC = () => {


    const finalizeSale = useSaleService();

    const [ message, setMessage ] = React.useState<Alert[]>( [] );
    const [ saleCompleted, setSaleCompleted ] = React.useState<boolean>( false );
    const [saleFormKey, setSaleFormKey] = React.useState(0);

    const handleSubmit = (sale: Sale) => {
        
        setMessage([]);

        return finalizeSale(sale)
            .then(() => {
                setSaleCompleted(true);
                setMessage([
                    {
                        type: "success",
                        text: "Sale successfully completed!",
                    },
                ]);
            })
            .catch((error) => {
                setMessage([
                    {
                        type: "danger",
                        text: error?.message || "Error finalizing the sale.",
                    },
                ]);
            });
    };


    return (

        <Layout title = "Sale" message = { message }>

            <SalesForm
                key={saleFormKey}
                onSubmit={handleSubmit}
                saleCompleted={saleCompleted}
                onNewSale={() => {
                    setSaleFormKey(current => current + 1);
                    setSaleCompleted(false);
                    setMessage([]);
                }}
            />

        </Layout>

    )

}
