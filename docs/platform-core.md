# COMANDOS Platform Core

O Platform Core é a parte reutilizável do COMANDOS que não pertence a Armamento
nem a qualquer outro domínio de negócio específico.

## Objetivo

Permitir que novos sistemas aproveitem a mesma base de:

- autenticação e sessão;
- autorização e RBAC;
- organizações e unidades;
- pessoas e identidades;
- auditoria;
- workflow;
- documentos;
- notificações;
- mensageria e eventos;
- preferências, idioma, tema e paleta;
- cliente HTTP;
- tratamento visual de erros e mensagens;
- componentes de interface;
- paginação, filtros e formulários genéricos;
- observabilidade e segurança;
- infraestrutura e políticas de dependências.

## Backend

Os módulos Spring Modulith considerados plataforma são:

- `core`
- `identity`
- `organization`
- `security`
- `audit`
- `workflow`
- `documents`
- `notifications`
- `messaging`
- `analytics`

Domínios como `inventory`, `purchase`, `custody`, `maintenance`,
`consumption`, `transfer`, `sales` e similares não fazem parte do Platform
Core. Eles consomem a plataforma.

## Frontend

A API pública reutilizável começa em:

```text
app/src/platform/
  auth.ts
  http.ts
  preferences.ts
  ui.ts
  index.ts
```

Novos módulos devem preferir importar da camada `platform` quando o recurso for
genérico. Os caminhos antigos permanecem válidos durante a migração para evitar
regressões.

Exemplo:

```ts
import {
    Button,
    httpClient,
    useSession,
    useComandosPreferences
} from "platform"
```

## Regra de dependência

```text
Platform Core
    ↑
Domínios de negócio
    ↑
Aplicações / produtos
```

O Platform Core nunca deve importar Armamento, Transporte, Escolta, Inteligência
ou qualquer módulo vertical de produto.

## Estratégia de extração

1. estabilizar APIs públicas;
2. eliminar referências específicas a COMANDOS/Armamento dos componentes
   genéricos;
3. adicionar testes próprios do core;
4. separar configurações por produto;
5. gerar pacote/biblioteca reutilizável;
6. permitir uso em outros repositórios sem copiar código manualmente.

A extração física para pacote separado só deve acontecer após as dependências
internas estarem isoladas e testadas.


## Definição de produto

A identidade do produto é configurada em `platform/product.ts`. Nome, descrição,
ícone, idioma, tema, cor padrão e namespace de armazenamento podem ser
substituídos por variáveis `NEXT_PUBLIC_PRODUCT_*`.

Assim, um novo produto pode reutilizar o mesmo shell sem renomear componentes ou
alterar código do core.
