# COMANDOS — Third-Party License Inventory

Este arquivo é um inventário técnico inicial para preparação de distribuição
comercial. Não substitui os textos integrais das licenças que precisarem
acompanhar uma distribuição final.

## Frontend

- React / React DOM — MIT
- Next.js — MIT
- Base UI — MIT
- Axios — MIT
- clsx — MIT
- tailwind-merge — MIT
- shadcn CLI — MIT
- SWR — MIT
- react-number-format — MIT
- @4us-dev/utils — MIT
- class-variance-authority — Apache-2.0
- Formik — Apache-2.0
- Lucide React — ISC/MIT
- Tailwind CSS — MIT
- Yup — MIT

## Backend — atenção

- Spring Boot / Spring Framework — Apache-2.0
- Spring Modulith — Apache-2.0
- Flyway Core — Apache-2.0
- Hibernate ORM — LGPL family
- Oracle JDBC — Oracle Free Use Terms and Conditions (FUTC)
- PostgreSQL JDBC — permissive
- H2 — somente testes; revisar licença na geração final do SBOM

## Proibidos sem nova aprovação

PrimeReact 11 / PrimeUI e seus pacotes auxiliares não devem ser reintroduzidos
sem decisão comercial e jurídica explícita.


## Migração Prime concluída

A remoção de PrimeReact 11, PrimeUI, PrimeIcons e PrimeUX foi concluída.
O frontend não possui esses pacotes em package.json ou yarn.lock. O CI bloqueia
qualquer reintrodução futura dessas dependências sem nova decisão comercial e
jurídica explícita.
