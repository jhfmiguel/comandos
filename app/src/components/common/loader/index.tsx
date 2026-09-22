interface LoaderProps {
    show: boolean;
}

export const Loader: React.FC<LoaderProps> = ( { show } ) => {

    if( !show ) {
        return <></>
    }

    return (

        <div
            id = "loader"
            style = {{
                width: '100%',
                height: '100%',
                zIndex: 99999,
                position: 'absolute',
                left: '20%',
                top: '30%'
            }}   
        >

            <div style = {{
                position: 'absolute',
                left: '20%',
                top: '30%'
            }}>
                
                <div className="lds-spinner">
                    <div></div>
                    <div></div>
                    <div></div>
                    <div></div>
                    <div></div>
                    <div></div>
                    <div></div>
                    <div></div>
                    <div></div>
                    <div></div>
                    <div></div>
                    <div></div>
                </div>

            </div>

        </div>

    )

}