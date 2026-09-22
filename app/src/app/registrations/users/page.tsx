import { UserRegistration } from 'components'
import { Suspense } from 'react'

export default function UserRegistrationPage() {
    return <Suspense fallback={<p role="status">Loading user registration…</p>}><UserRegistration /></Suspense>
}
