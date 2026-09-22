# Política de licenças e dependências do COMANDOS

O COMANDOS é planejado como produto comercial. Dependências de runtime devem
permitir distribuição comercial e ter termos compatíveis com software
proprietário, salvo decisão jurídica explícita em contrário.

## Regra de entrada

Antes de adicionar uma biblioteca:

1. registrar a licença;
2. verificar se permite uso e redistribuição comercial;
3. verificar obrigações de NOTICE, atribuição ou disponibilização de alterações;
4. evitar componentes que exijam chave/licença comercial no runtime quando houver
   alternativa permissiva equivalente;
5. manter ferramentas de build/CLI em devDependencies;
6. revisar também dependências transitivas relevantes.

## Dependências frontend diretas

| Dependência | Licença / situação | Uso comercial |
|---|---|---|
| React / React DOM | MIT | Permitido |
| Next.js | MIT | Permitido |
| Base UI | MIT | Permitido |
| Axios | MIT | Permitido |
| clsx | MIT | Permitido |
| tailwind-merge | MIT | Permitido |
| shadcn CLI | MIT | Permitido; somente desenvolvimento |
| SWR | MIT | Permitido |
| react-number-format | MIT | Permitido |
| @4us-dev/utils | MIT | Permitido |
| class-variance-authority | Apache-2.0 | Permitido com obrigações de licença/NOTICE |
| Formik | Apache-2.0 | Permitido com obrigações de licença/NOTICE |
| Lucide React | ISC/MIT | Permitido |
| Tailwind CSS | MIT | Permitido |
| Yup | MIT | Permitido |

## Dependências que não devem voltar sem revisão explícita

- primereact
- @primereact/*
- @primeui/*
- @primeuix/*
- @primeicons/*
- primeicons

A linha PrimeReact 11 usa a PrimeUI License e componentes styled podem exigir
chave comercial. O CI verifica package.json e yarn.lock contra esses pacotes.

## Backend

### Spring

Spring Boot e a maior parte do ecossistema Spring usado pelo projeto são
Apache-2.0, compatíveis com produto comercial mediante preservação dos avisos
aplicáveis.

### Hibernate ORM

Hibernate ORM usa LGPL. A utilização da biblioteca não torna automaticamente o
COMANDOS open source, mas redistribuição e especialmente modificações da própria
biblioteca devem respeitar os termos da LGPL.

### Flyway Core

O core utilizado no projeto é Apache-2.0. Recursos comerciais adicionais da
Redgate não devem ser adicionados sem revisão específica.

### Oracle JDBC

O driver Oracle JDBC disponível no Maven Central é distribuído sob Oracle Free
Use Terms and Conditions (FUTC), não sob MIT/Apache. Ele deve permanecer
identificado como dependência de atenção jurídica antes da distribuição do
produto.

A licença do driver não substitui a licença do Oracle Database. O banco Oracle
deve ser tratado como produto separado: implantação, edição e licenciamento do
servidor de banco devem ser definidos contratualmente com cada ambiente/cliente.

## Distribuição

Antes de uma versão comercial:

- gerar SBOM;
- gerar lista final de dependências transitivas;
- incluir THIRD-PARTY-NOTICES com licenças/avisos exigidos;
- revisar dependências LGPL/MPL/EPL/Apache com obrigações de redistribuição;
- revisar qualquer dependência não-OSS ou com licença customizada;
- submeter a versão final a revisão jurídica de software.


## Estado da migração

Migração concluída em 22/09/2026. PrimeReact/PrimeUI/PrimeIcons/PrimeUX foram
removidos do frontend, package.json e yarn.lock. O CI mantém a política de
bloqueio para impedir reintrodução acidental.
