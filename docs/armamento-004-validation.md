# Armamento 004 — cadastro em lote de Individual Assets

## Entrega e formato

Implementação existente no workspace preservada e concluída em 13/09/2026.
Tabela editável de 1 a 1000 pares `assetCode`/`serialNumber`, com modelo,
localização e demais campos comuns. Colagem opcional de duas colunas sem
cabeçalho, separadas por tabulação ou ponto e vírgula. Quantidade derivada
das linhas, sem quantidade livre. O cadastro de uma linha e a edição pontual
continuam disponíveis; os identificadores são campos imutáveis após criação.

`POST /api/erp/inventory/assets/batch/review` valida sem criar ativos.
`POST /api/erp/inventory/assets/batch` revalida e confirma atomicamente.
Uma rejeição impede toda a persistência de ativos e movimentos. Linhas válidas
continuam no editor para correção. Retorno por linha: VALID, REJECTED ou
ACCEPTED, com erros e pares. Confirmações podem ser recuperadas pelo mesmo
requestId sem duplicar registros. Autorização e escopo usam as regras atuais.
Auditoria registra ator, data, campos comuns/modelo, quantidade e resultado.
Duplicidades no lote e seriais já persistidos são bloqueados; a comparação
preserva maiúsculas/minúsculas e remove espaços nas extremidades.

## Arquivos alterados nesta execução

- `wr-api/src/main/java/com/weaponsregistration/inventory/service/StockIntakeService.java`:
  consultas agrupadas de códigos e seriais já registrados, substituindo duas
  consultas adicionais por linha, preservando validação e transação existentes.
- `wr-app/src/components/erp/inventory/stock-intake-editor.tsx`:
  relatório de confirmação permanece visível, com cada par marcado como
  “Accepted; saved”, até o usuário clicar em Done.
- `wr-app/scripts/validate-asset-batch.mjs`: conferência do relatório por linha,
  fechamento explícito, navegação direta compatível com o menu atual e URLs
  configuráveis para ambiente de validação.
- `wr-app/scripts/validate-stock-intake.mjs`: fechamento do relatório de ativos
  antes de prosseguir no cenário existente.
- `docs/armamento-004-validation.md`: este log.

Os endpoints, integração do editor, auditoria e testes de API já estavam
alterados no workspace ao iniciar esta execução e foram preservados.

## Validações executadas

- Em `wr-api`: `.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-repository' '-Dtest=InventoryApiTests,AuthorizationApiTests' test`.
  **41 testes, zero falhas, erros ou ignorados; BUILD SUCCESS.** Inclui
  pareamento, repetição idempotente, rejeição atômica, revisão, revalidação
  após criação concorrente, campos inválidos, duplicidades, autorização,
  escopo e auditoria do usuário autenticado.
  Log: `wr-api/target/armamento-004-tests.log`.
- Em `wr-app`: `npx.cmd --no-install tsc --noEmit`: código 0.
- `npx.cmd --no-install eslint src/components/erp/inventory/stock-intake-editor.tsx scripts/validate-asset-batch.mjs scripts/validate-stock-intake.mjs`:
  código 0, sem erros ou avisos de lint (npm emitiu avisos de configuração).
- `node scripts/validate-asset-batch.mjs`: **PASS** nos dois grupos de cenários,
  sem exceções JavaScript: pares, quantidade, duplicidades, revisão sem escrita,
  edição, confirmação, relatório de aceitação, recuperação idempotente após
  perda de resposta e recusa 403, rejeições por linha, preservação da linha
  válida, remoção e confirmação corrigida.
- Verificação de whitespace dos arquivos de código alterados: código 0.

Navegador: Playwright/Edge headless já instalado em
`wr-api/target/browser-validation/node_modules` (NODE_PATH). Frontend existente
na porta 3000; API temporária na porta 8182 com origem permitida 3000, usando
exclusivamente PostgreSQL `wr_validation_20260912`. Variáveis do script:
`ASSET_BATCH_APP_URL=http://localhost:3000` e
`ASSET_BATCH_API_URL=http://localhost:8182`.

A primeira tentativa ocorreu antes da API ficar disponível. A tentativa pela
navegação antiga também exibiu erro de parsing CSS no servidor de desenvolvimento
existente. Após usar a rota atual `?resource=assets`, a execução completa passou
sem alteração de CSS. Não foi executado build de produção nem o cenário amplo
de munições; a validação ficou restrita à tarefa.

## Condição de parada

Cadastro em lote diretamente relacionado a Individual Assets implementado e
validado. Nenhum bloqueio restante nos cenários executados e nenhuma decisão
de negócio pendente para este escopo. Sem commit ou push, sem iniciar outra
tarefa ou expandir funcionalidades de estoque, transporte ou outros módulos.
