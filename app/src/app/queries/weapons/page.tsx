import { redirect } from "next/navigation"

export default function LegacyWeaponsRedirect() {
    redirect("/erp/inventory?section=catalog&resource=item-models")
}
