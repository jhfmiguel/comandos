# Armamento 003 — validação de 13/09/2026

## Situação da entrega

Validação restrita aos contratos disponíveis de armamento e munição. A etapa
002 foi encerrada com justificativa de bloqueio, conforme
`automation/logs/20260913-134001-armamento-002-frontend.log`: a API não oferece
identificadores e saldos de caixas individualizadas. Esse requisito continua
pendente; esta rodada não declara concluída a entrega integral do módulo.

Foram inspecionados o diff existente, o relatório
`docs/armamento-001-backend-validation.md`, os logs de 001/002 e os contratos
Java/TypeScript usados por estoque, cautela, reservas e inventário físico.
As alterações anteriores de aplicação, API, testes, automação e Command Center
foram preservadas. Nenhuma funcionalidade nova foi iniciada.

## Contratos conferidos

- `POST /api/erp/inventory/assets/batch`: campos comuns e pares
  `assetCode`/`serialNumber`, com recibo e repetição pelo mesmo `requestId`.
- `POST /api/erp/inventory/lots/from-boxes`: `boxes`, `roundsPerBox` e
  `looseUnits` formam um único saldo agregado em cartuchos. `openingPackaging`
  registra a composição da entrada, sem representar caixas com saldo próprio.
  A interface comunica essa limitação explicitamente.
- Cautela aceita exatamente um recebedor: `recipientId` ou `recipientUnitId`;
  o retorno inclui `recipientType`, com identificadores opcionais correspondentes.
- Reservas e inventário físico usam os endpoints e DTOs existentes; os testes
  conferem recuperação idempotente, saldos reservados, reversão transacional,
  contagem individual zero/um e transferências de ativo e lote.

## Correções e classificação das falhas

Único arquivo de código alterado nesta rodada:
`wr-app/scripts/validate-browser.mjs`.

- O script selecionava organização, recebedor e autorizador sem pesquisar.
  Falhou quando os registros fictícios ficaram fora dos primeiros 20 resultados.
  Passou a usar as buscas reais da interface. A premissa já existe em `HEAD`;
  é defeito preexistente do teste, não regressão do contrato da API.
- O script clicava em abrir manutenção antes de carregar a opção do ativo
  vinculado. Agora espera os valores exatos de organização e ativo. A espera
  anterior também existe em `HEAD` e podia aceitar um elemento ainda ausente.
- Acrescentada captura de mensagens `console.error`, além das exceções de
  página e respostas HTTP 5xx já verificadas pelo script geral.

Não foi necessário alterar comportamento da aplicação. A primeira tentativa
de navegador recebeu `ECONNREFUSED` na porta 8180: as instâncias documentadas
estavam paradas. Playwright e Edge já estavam disponíveis; bastou configurar
`NODE_PATH` e iniciar as instâncias isoladas. Nenhum pacote foi instalado.

A captura de console também detectou HTTP 500 para um chunk JavaScript ausente.
O `BUILD_ID` foi alterado às 13:52, depois da inicialização do servidor de teste,
e o arquivo solicitado não existia mais em `.next/static/chunks`. Isso indica
substituição concorrente do build no workspace, não falha de endpoint ERP.
O build foi refeito e a instância 3100 reiniciada antes de repetir a validação.

Os seis avisos de lint em `src/components/bot/command-center.tsx` são anteriores
e fora do módulo. `git diff --check -- wr-api wr-app` também aponta uma linha
vazia final preexistente em `wr-app/src/app/globals.css:691`; foi preservada.

## Comandos e resultados

Executados primeiro os testes Maven e lint focados, depois as validações
completas. Logs desta rodada: `wr-api/target/armamento-003-*.log`.

Em `wr-api`:

```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-repository' '-Dtest=InventoryApiTests,CustodyApiTests,CustodySchemaUpgradeTests,AmmunitionConsumptionApiTests,InventoryCountApiTests,ReservationApiTests,TransferApiTests' test
.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-repository' test
```

| Verificação | Resultado |
| --- | --- |
| Maven focado | 47 testes; zero falhas, erros ou ignorados; BUILD SUCCESS |
| Maven completo | 104 testes; zero falhas, erros ou ignorados; BUILD SUCCESS |
| ESLint focado em componentes ERP, modelos ERP e serviços alterados | Código 0, sem avisos |
| `npm.cmd run lint` | Código 0; zero erros e seis avisos no Command Center |
| `NODE_ENV=production; npm.cmd run build` | Compilação, TypeScript e geração de rotas aprovados |
| `validate-stock-intake.mjs` | PASS: pares de ativos, duplicidades, quantidades inteiras, composição de caixas, recuperação e cautela/devolução para unidade; organização 24 |
| `validate-stock-workflows.mjs` | PASS: reserva/cancelamento, inventário e transferência; organização 25, reserva 4, inventário 3, transferência 6 |
| `validate-sales-access.mjs` | PASS: login, escopo, venda mista, devolução/cancelamento, recuperação, logout e bloqueio de conta; organização 27, venda 7 |
| `validate-browser.mjs` após correções e reinício | PASS: cautela, devolução danificada e manutenção vinculada; 12 telas; buscas concorrentes; sem exceções JavaScript, erros de console ou HTTP 5xx; organização 31, ativo 47 |
| `git diff --check -- wr-app/scripts/validate-browser.mjs` | Código 0; nenhuma falha de whitespace na correção |

Lint focado em `wr-app`:

```powershell
npx.cmd --no-install eslint src/components/erp src/api/models/erp src/api/services/inventory-count.service.ts src/api/services/reservation.service.ts
npx.cmd --no-install eslint scripts/validate-browser.mjs
npm.cmd run lint
$env:NODE_ENV = 'production'
npm.cmd run build
```

O lint completo foi repetido após a alteração do script. Os cenários usam
builds de produção aprovados; o build foi repetido para resolver a substituição
concorrente de seus arquivos durante o teste geral.

## Ambiente de navegador e reprodução

API com PostgreSQL exclusivamente no banco `wr_validation_20260912`, interface
de produção na porta 3100, Edge headless e Playwright temporário já instalado.
As requisições do frontend à porta 8080 são encaminhadas pelos scripts às APIs
reais de validação. O banco habitual `weapons` não foi usado.

Iniciar em terminais separados, a partir de `wr-api`:

```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-repository' spring-boot:run '-Dspring-boot.run.arguments=--server.port=8180 --spring.datasource.url=jdbc:postgresql://localhost:5432/wr_validation_20260912 --erp.allowed-origin=http://localhost:3100 --spring.jpa.show-sql=false --logging.level.root=WARN'
.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-repository' spring-boot:run '-Dspring-boot.run.arguments=--server.port=8181 --spring.datasource.url=jdbc:postgresql://localhost:5432/wr_validation_20260912 --erp.allowed-origin=http://localhost:3100 --erp.security.require-login=true --erp.security.enforce-permissions=true --spring.jpa.show-sql=false --logging.level.root=WARN'
```

Em `wr-app`, iniciar `npm.cmd run start -- --port 3100` e executar em outro
terminal:

```powershell
$env:NODE_PATH = (Resolve-Path ../wr-api/target/browser-validation/node_modules).Path
node scripts/validate-stock-intake.mjs
node scripts/validate-stock-workflows.mjs
node scripts/validate-browser.mjs
node scripts/validate-sales-access.mjs
```

Os testes de recuperação injetam perdas de resposta e recusas HTTP 403
deliberadamente; os saldos e operações são conferidos na API real. Todos os
scripts verificam ausência de exceções JavaScript. O script geral também
verifica erros de console e respostas 5xx. Maven usa H2 em modo PostgreSQL;
os cenários de navegador usam PostgreSQL e preservam seus dados fictícios.

## Pendências e condição de parada

As validações existentes passaram após as correções do teste e do ambiente.
Nenhum teste Maven foi ignorado e nenhum script de fluxo listado foi omitido.
Não foram encontradas regressões funcionais bloqueadoras nos cenários
executados. O fechamento integral permanece bloqueado pelo requisito abaixo,
sem marcar a etapa como entrega completa de todos os requisitos de 002.

- Contrato de caixas individualizadas continua ausente, conforme bloqueio de
  002. Não substituir por registros simulados nem considerar o saldo agregado
  como atendimento desse requisito.
- Abertura de tela de consumo, doações ou baixa não equivale à execução de
  todas as operações de escrita no navegador; permanece a cobertura de API
  e o limite de homologação já descrito em `docs/validation.md`.
- Avisos do Command Center e whitespace de CSS preservados, fora do escopo.

Sem commit ou push. Arquivos alterados nesta rodada: este relatório e
`wr-app/scripts/validate-browser.mjs`. Não iniciar a etapa 004 nesta execução.
