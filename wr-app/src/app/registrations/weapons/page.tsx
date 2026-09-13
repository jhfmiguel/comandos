import { WeaponsRegistration } from "components";
import { Suspense } from "react";

export default function WeaponsRegistrationPage() {
    return <Suspense fallback={<p role="status">Loading weapon registration…</p>}><WeaponsRegistration /></Suspense>;
}
