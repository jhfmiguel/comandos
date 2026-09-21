# Armamento 019 — consulta principal

Implementada em `/queries/weapons`, acessível pelo menu de inventário.

- Consulta os ativos reais em `/api/erp/inventory/assets`, com filtros combinados por patrimônio, série, modelo, status, unidade e localização. Modelo e localização aceitam texto ou ID conforme o contrato existente; unidade aceita nome ou código.
- Filtros, página e ativo selecionado ficam na URL; aplicar filtros reinicia a página. Recarga e navegação voltar/avançar restauram a consulta.
- Contagem e paginação são executadas no servidor. O novo `filter.unit` usa a unidade da localização atual e preserva o predicado de autorização.
- O detalhe busca o ativo pelo ID e apresenta organização, unidade, localização, status e condição. Custódias, movimentações e auditoria possuem paginação independente e autorização no servidor, com indicação de permissão ausente na interface.
- `/api/erp/custodies/by-asset/{id}` seleciona somente cautelas vinculadas ao ativo e visíveis ao usuário, inclusive histórico de outras unidades quando autorizado. `assetId` na consulta de movimentos é uma comparação exata.
- Loading, resultados vazios, erros, 401/403/404 e nova tentativa são tratados; requisições obsoletas são canceladas.

## Validação

- `wr-api`: `.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-test-cache' '-Dtest=CustodyApiTests,AuthorizationApiTests' test` — 30 testes, zero falhas/erros.
- `wr-app`: `npx tsc --noEmit` — aprovado.
- ESLint nos arquivos frontend alterados e no script de validação — aprovado.
- `node scripts/validate-armament-query.mjs` — cinco cenários aprovados em Edge headless: sucesso (contrato, detalhe, recarga, paginação, voltar e reinício dos filtros), vazio, erro com nova tentativa, 403 e permissões restritas. As respostas são interceptadas; os contratos reais são exercitados pelos testes de API.

## Arquivos desta implementação

- `wr-app/src/app/queries/weapons/page.tsx`
- `wr-app/src/components/erp/armament-query/index.tsx`
- `wr-app/src/components/layout/menu/index.tsx`
- `wr-app/scripts/validate-armament-query.mjs`
- `wr-api/src/main/java/com/weaponsregistration/inventory/service/InventoryService.java`
- `wr-api/src/main/java/com/weaponsregistration/custody/controller/CustodyController.java`
- `wr-api/src/main/java/com/weaponsregistration/custody/service/CustodyService.java`
- `wr-api/src/test/java/com/weaponsregistration/custody/controller/CustodyApiTests.java`
- `wr-api/src/test/java/com/weaponsregistration/security/controller/AuthorizationApiTests.java` (adição de um teste; alterações anteriores preservadas)
- Este documento.

Sem bloqueios pendentes. Sem commit ou push; nenhuma outra tarefa iniciada.
