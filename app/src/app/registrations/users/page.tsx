import { redirect } from "next/navigation"

export default function LegacyUsersRedirect() {
    redirect("/erp/core?section=people&resource=people")
}
