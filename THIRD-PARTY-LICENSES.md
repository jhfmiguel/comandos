# Third-party license inventory

This inventory covers the principal direct dependencies used by COMANDOS. It is
maintained to support commercial distribution and should be reviewed on dependency
updates. It is not a substitute for legal advice.

## Frontend

| Dependency | License | Commercial use |
| --- | --- | --- |
| React / React DOM | MIT | Permissive |
| Next.js | MIT | Permissive |
| Base UI (`@base-ui/react`) | MIT | Permissive |
| Tailwind CSS | MIT | Permissive |
| PrimeFlex 4.x | MIT | Permissive; retained only as CSS utility during migration |
| Lucide React | ISC | Permissive |
| Axios | MIT | Permissive |
| Formik | Apache-2.0 | Permissive; attribution/license notice required |
| Yup | MIT | Permissive |
| SWR | MIT | Permissive |
| react-number-format | MIT | Permissive |
| clsx | MIT | Permissive |
| class-variance-authority | Apache-2.0 | Permissive; attribution/license notice required |
| tailwind-merge | MIT | Permissive |
| tw-animate-css | MIT | Permissive |
| @4us-dev/utils | MIT | Permissive |
| shadcn CLI (development) | MIT | Development tooling |

## Backend

| Dependency | License | Commercial use |
| --- | --- | --- |
| Spring Boot / Spring Framework | Apache-2.0 | Permissive |
| Spring Modulith | Apache-2.0 | Permissive |
| Micrometer | Apache-2.0 | Permissive |
| PostgreSQL JDBC | BSD-2-Clause | Permissive |
| Flyway Community/Core | Apache-2.0 | Permissive |
| H2 (tests only) | MPL-2.0 / EPL-1.0 | Allowed, but keep license notices and avoid modifying/distributing H2 source without review |
| Apache Kafka (optional profile) | Apache-2.0 | Permissive |
| Oracle JDBC (optional profile) | Oracle Free Use Terms and Conditions | Review Oracle terms for every shipped version and deployment model |

## Removed from the commercial application

The following current-generation packages use the PrimeUI License and are not
part of the target commercial dependency set:

- `primereact` 11.x
- `@primereact/ui` 11.x
- `@primereact/core` 11.x
- `@primeuix/themes`
- `@primeicons/react` 8.x
- `primeicons` 8.x

PrimeFlex 4.x is a separate MIT-licensed package and is not subject to the
PrimeUI License. It may be replaced later for vendor independence, but it is not
a commercial-license blocker.

## Commercial distribution rule

Before each release, generate an SBOM and license report from the resolved
dependency graph. A new dependency must not be approved only because its package
name looks open-source; its exact version and license must be checked.
