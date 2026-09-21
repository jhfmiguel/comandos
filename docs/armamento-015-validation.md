# Armamento 015 — baixa, descarte e inativação

Homologação realizada em 20/09/2026. Alterações anteriores do workspace foram preservadas. Nenhum commit, push ou outra tarefa foi iniciado.

## Implementação

- Finalização exige `disposals/CREATE` e `disposals/APPROVE` no escopo selecionado e de cada item, além de `confirmed: true`. A interface mantém a confirmação detalhada e exige ambas as permissões.
- Estoque em inventário `OPEN`/`COUNTED` e saldos reservados são excluídos da seleção e bloqueados na API. Cautela, manutenção e reserva de itens serializados continuam bloqueadas pelo estado.
- Baixa lógica é terminal; destruição física acrescenta método, data e certificado. Inativação temporária (`BLOCKED`) não é baixa nem comprovação de destruição.
- Fechada a reativação indireta por inspeções, ocorrências e resolução de recuperação criada antes da baixa. A resolução bloqueia o item para conferir o estado atual.

## PostgreSQL e navegador

Base nova e isolada `wr_disposal_015_20260920`, PostgreSQL 18.6, API em `localhost:8115`, frontend existente em `localhost:3000`, Edge headless via Playwright. Login e permissões habilitados. A base habitual e a base de validação antiga não foram alteradas. Os dados fictícios foram preservados na base isolada.

Inicialização da API, em `wr-api`:

```powershell
.\mvnw.cmd '-Dmaven.repo.local=C:\workspace\weapons-registration\wr-api\target\maven-test-cache' -o spring-boot:run '-Dspring-boot.run.arguments=--server.port=8115 --spring.datasource.url=jdbc:postgresql://localhost:5432/wr_disposal_015_20260920 --erp.allowed-origin=http://localhost:3000 --spring.jpa.show-sql=false --erp.security.require-login=true --erp.security.enforce-permissions=true'
```

Após criar o esquema, executar uma única vez `wr-app/scripts/disposal-validation-bootstrap.sql` via psql nessa base. O script recusa bases sem prefixo `wr_disposal_015_`; a conta administrativa e senha nele são exclusivamente fixtures de teste. Executar em `wr-app`:

```powershell
node scripts/validate-disposal.mjs
```

Resultado: todos os cenários passaram. Evidência local: `wr-app/disposal-validation.log`; inicialização: `wr-api/target/armamento-015-postgres.log`.

- Operações reais de cautela, manutenção, reserva de item e lote, inventário aberto/contado e transferência criadas pela API PostgreSQL; tentativas de baixa incompatíveis rejeitadas sem finalizar.
- Transferência não possui estado ativo/em trânsito no contrato existente. Validado o bloqueio da unidade de origem após a finalização atômica, sem ampliar o contrato de transferência.
- Operador restrito com CREATE recebeu 403; APPROVE no escopo permitiu finalizar; confirmação falsa retornou 400; outro escopo e revogação de APPROVE retornaram 403.
- Navegador executou baixa lógica e destruição física com item e lote; cancelamento da confirmação não enviou POST. Perda simulada da resposta após commit real recuperou o mesmo processo por repetição idêntica.
- Histórico exibiu operador, processo e certificado, distinguindo baixa lógica. Auditoria FINALIZE foi consultada. Lote terminou com 7,5 unidades, sem desconto duplicado.
- Item baixado sumiu da seleção; edição cadastral, nova baixa, inspeção, ocorrência e recuperação antiga não o reativaram. Nenhum erro JavaScript.

O proxy do teste encaminha as requisições ao servidor real e conserva sessão/CSRF; não substitui respostas de estoque por fixtures. Somente a perda de resposta é simulada.

## Validação automatizada

```powershell
# Em wr-api
.\mvnw.cmd '-Dmaven.repo.local=C:\workspace\weapons-registration\wr-api\target\maven-test-cache' -o '-Dtest=DisposalApiTests,AuthorizationApiTests,LifecycleWorkflowReportApiTests' test
# Em wr-app
npx.cmd eslint src/components/erp/disposals/index.tsx src/api/models/erp/disposal.ts scripts/validate-disposal.mjs
npx.cmd tsc --noEmit
```

ESLint e TypeScript concluídos sem erros. Maven: **39 testes, zero falhas, erros ou ignorados; BUILD SUCCESS**, registrado em `wr-api/target/armamento-015-tests.log`. Esses testes usam H2; a execução separada acima comprova o fluxo real em PostgreSQL.

Não é homologação de migração de uma base antiga nem criação de um workflow de dupla aprovação. A autorização elevada usa a permissão explícita APPROVE do mecanismo existente.
