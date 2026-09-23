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
    usePlatformPreferences
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


## Eventos de plataforma

Novos módulos backend devem publicar eventos por
`com.comandos.messaging.api.PlatformEventPublisher`.

O contrato público não expõe `ApplicationEventPublisher` do Spring. Isso permite
trocar a implementação interna por outbox, Kafka ou outro barramento sem alterar
os módulos consumidores.


## Erros HTTP

`com.comandos.core.web.ApiProblems` fornece a construção padronizada de
`ProblemDetail` para validação, não encontrado, conflito de integridade e
concorrência otimista.

Novos módulos devem reutilizar esse contrato em vez de criar formatos de erro
incompatíveis.


## Dados e erros no frontend

`platform/data.ts` contém contratos genéricos para paginação, estado assíncrono
e extração de mensagens de erro HTTP. Novos módulos devem reutilizar esses
contratos em vez de repetir tipos e parsers de erro.


## Auditoria reutilizável

Novos módulos devem depender de
`com.comandos.audit.api.AuditRecorder` em vez de `AuditService`.

A auditoria obtém o operador através de
`com.comandos.security.api.CurrentActorProvider`, evitando acoplamento direto
ao Spring Security.


## Correlação e logging HTTP

`RequestCorrelationFilter` mantém um identificador por requisição no header
`X-Request-Id`, no atributo `platform.requestId` e no MDC de logging.

Um identificador recebido do cliente só é reutilizado quando possui formato
seguro. Caso contrário, o Platform Core gera um novo valor por `IdGenerator`.

Ao final da requisição, o filtro registra método, caminho, status e duração e
sempre remove o identificador do MDC, inclusive quando a cadeia HTTP termina com
exceção. Isso evita vazamento de contexto entre requisições executadas pela mesma
thread.


## Error boundary reutilizável

O frontend mantém o fallback genérico em `platform/error-boundary.tsx`.
O arquivo `app/error.tsx` apenas integra esse componente ao App Router do Next.js.

O fallback não depende de Armamento nem de branding específico do COMANDOS.
Ele apresenta uma mensagem genérica, expõe o `digest` quando disponível e oferece
uma ação de nova tentativa por meio de `reset()`.

Aplicações que reutilizarem o Platform Core podem manter a mesma estratégia sem
duplicar tratamento de erro global.


## Preferências reutilizáveis

A API pública de preferências expõe nomes neutros para novos módulos:

- `usePlatformPreferences`
- `PlatformLocale`
- `PlatformTheme`
- `PlatformPalette`

Os nomes antigos `useComandosPreferences` e tipos `Comandos*` permanecem
disponíveis temporariamente como aliases de compatibilidade para evitar regressões
durante a migração dos módulos existentes.
