# Authentication and session foundation

This module adds password login and server-side sessions using the existing
SystemUser and Person records. It does not add a second account table or migrate
legacy User records. Code follows `security/config`, `security/service`, and
`security/controller`.

## Provision the first account

1. Start the API and frontend in the default setup mode. The interface displays
   that sign-in is optional in this mode.
2. Open **Institutional core**, create an active **Person**, then create a
   **System user** linked to that person. Set a login and password; leave blocked
   off. Passwords must satisfy the existing registration rules: at least 12
   characters and at most 72 UTF-8 bytes.
3. Open `/login` and verify that this account can sign in. The sidebar displays
   the person's name and account login. **Sign out** invalidates the session.
4. To require authentication, stop the API, then start it from PowerShell in
   `wr-api` with:

   ```powershell
   $env:ERP_REQUIRE_LOGIN = 'true'
   .\mvnw.cmd spring-boot:run
   ```

5. Refresh the frontend. Unauthenticated users are sent to `/login`. Business API
   routes under `/api/**`, including legacy registrations and sales, return 401
   without an authenticated session. Existing accounts/data remain intact.

Setup mode (`ERP_REQUIRE_LOGIN=false`, the default) intentionally leaves business
APIs accessible to provision accounts and preserve the current development flow.
It is not protected operation. No default password or automatic administrator is
created. Authentication endpoints still require CSRF protection in setup mode.

## Configuration

| Setting | Default | Purpose |
| --- | --- | --- |
| `ERP_REQUIRE_LOGIN` | `false` | Require authentication on business APIs |
| `ERP_ENFORCE_PERMISSIONS` | `false` | Also enforce configured permissions/scopes; requires login |
| `ERP_ALLOWED_ORIGIN` | `http://localhost:3000` | Exact frontend origin allowed by CORS |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | Browser API base URL; rebuild/restart the frontend after changes |
| `server.servlet.session.timeout` | `30m` | Session inactivity timeout |
| `server.servlet.session.cookie.http-only` | `true` | Keep session IDs inaccessible to JavaScript |
| `server.servlet.session.cookie.same-site` | `lax` | Same-site session cookie policy |

For HTTPS deployment, configure secure session cookies as well. This delivery
targets the current same-site frontend/API setup (localhost with different ports).
It does not configure cookies for deployments across unrelated domains.

## API and behavior

| Method | Path | Result |
| --- | --- | --- |
| GET | `/api/auth/session` | `{ requireLogin, user }`; user is null or contains only id, login and name |
| GET | `/api/auth/csrf` | Session CSRF header name and token |
| POST | `/api/auth/login` | URL-encoded username/password plus CSRF header; 204 on success, 401 for invalid credentials |
| POST | `/api/auth/logout` | CSRF-protected session invalidation; 204 on success |

The frontend sends credentials with API calls and obtains a CSRF token before
each mutation. It does not store passwords or session IDs in browser storage.
Login rotates the session ID. Logout invalidates it and clears the session cookie.
CORS is centralized for all APIs and permits credentials only from the configured
origin, including preflight requests used by sales and authenticated writes.

Every authenticated business request rechecks the account. Blocked accounts,
inactive people, removed accounts and changes to SystemUser.version invalidate
existing sessions. Password changes through the core service increment that
version. MFA-enabled accounts cannot bypass MFA through password-only login;
MFA enrollment/challenge is not implemented in this stage.

Spring Security implements session persistence and CSRF handling; see the official
[session persistence documentation](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/persistence.html)
and [CSRF documentation](https://www.springframework.org/spring-security/reference/servlet/exploits/csrf.html).

## Verification and remaining scope

`./mvnw.cmd test` covers existing core/inventory/sales behavior in setup mode and
authentication in a separate H2 context with enforcement enabled. Authentication
tests cover protected routes, session rotation, credential rejection, blocked and
inactive accounts, session invalidation, CSRF, logout and credentialed CORS.

The frontend is checked with TypeScript, ESLint and a production build.
The PostgreSQL/browser scenario in [Validation](validation.md) now checks login,
invalid credentials, session rotation, HttpOnly cookies, logout and invalidation
of an existing session after the account is blocked. Inactivity timeout, HTTPS
cookie deployment and production session infrastructure remain unverified.

Login-only mode permits authenticated accounts to use business APIs. The subsequent
[authorization module](authorization.md) can additionally enforce profile actions,
administrative boundaries and organization/unit scope. Provision grants before
enabling ERP_ENFORCE_PERMISSIONS. MFA, login throttling, password recovery, full
audit and distributed sessions remain separate architecture requirements.
