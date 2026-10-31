import { Loader as SharedLoader } from "@faria-miguel/ui/loader"

interface LoaderProps {
    show: boolean
}

export const Loader: React.FC<LoaderProps> = ({ show }) => (
    <SharedLoader show={show} label="Carregando…" className="comandos-loader" />
)
