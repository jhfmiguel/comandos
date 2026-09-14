# Armamento 007 — cadastro básico

## Entrega

Cadastro básico concluído sobre o domínio e a tela existentes, preservando as
alterações presentes no workspace. Não foi criado um segundo cadastro de armas.
`ItemModel` mantém marca, categoria e dados de modelo; `AssetItem` representa
um item individual; `StockLot` e saldos continuam separados.

Contrato existente utilizado pela tela `/erp/inventory?resource=assets`:

- `POST /api/erp/inventory/assets`: criação unitária.
- `GET /api/erp/inventory/assets`: consulta paginada e busca.
- `GET /api/erp/inventory/assets/{id}`: consulta individual.
- `PUT /api/erp/inventory/assets/{id}`: edição com versão obrigatória.
- Modelo, localização, código, condição, disponibilidade e valor são obrigatórios.
  Série é obrigatória para categorias serializadas; validade é opcional.
- Textos são aparados antes da persistência; espaços vazios tornam-se nulos,
  preservando maiúsculas/minúsculas. Código é único globalmente e série é única
  dentro do modelo. As restrições existentes no banco permanecem.
- Modelo, localização, código e série permanecem imutáveis após o cadastro.
  Condição, disponibilidade, validade e valor seguem as validações existentes.
  Edição não gera outra entrada de estoque. Não há campo de quantidade no item.

A criação agora verifica duplicidades antes da persistência e retorna HTTP 409
com mensagem específica para código ou série. O lote usa a mesma gravação,
preservando seu relatório próprio de revisão por campo. Autorizações de ação,
escopo e referências, controle de versão e auditoria transacional foram mantidos.
A tela unitária informa as regras de identificação e normalização.

## Arquivos alterados nesta tarefa

- `wr-api/src/main/java/com/weaponsregistration/inventory/service/InventoryService.java`
- `wr-api/src/test/java/com/weaponsregistration/inventory/controller/InventoryApiTests.java`
- `wr-api/src/test/java/com/weaponsregistration/security/controller/AuthorizationApiTests.java`
- `wr-app/src/components/erp/shared/record-workspace.tsx`
- `docs/armamento-007-validation.md`

## Validação

- API: em `wr-api`, `.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-repository' '-Dtest=InventoryApiTests,AuthorizationApiTests' test`.
  Resultado Maven: **BUILD SUCCESS, 43 testes, zero falhas, erros ou ignorados**.
  Confirmado nos XMLs Surefire (25 de inventário e 18 de autorização).
  Log: `wr-api/target/armamento-007-tests.log`. O processo de shell com
  redirecionamento reportou código 1 apesar do sucesso registrado pelo Maven;
  os resultados acima foram conferidos diretamente nos relatórios.
- Novos cenários: cadastro de arma, normalização, busca e consulta, duplicidades,
  edição, conflito de versão, dados obrigatórios/inválidos, ausência de gravações
  indevidas, auditoria antes/depois e usuário autenticado, edição sem permissão
  e fora da organização. Regressão do lote também passou.
- Frontend: `npx.cmd --no-install tsc --noEmit` e
  `npx.cmd --no-install eslint src/components/erp/shared/record-workspace.tsx`:
  código 0. Apenas avisos de configuração do npm.
- `git diff --check` apontou espaços finais em alterações preexistentes do
  componente compartilhado; essas linhas foram preservadas. As linhas inseridas
  nesta tarefa não acrescentam espaços finais.
- Não foi executado navegador nem build de produção nesta tarefa.

A primeira rodada revelou duplicação de mensagem na revisão em lote, corrigida
antes da rodada final. A asserção de consulta foi ajustada para comparar campos
do contrato sem exigir igualdade textual da precisão temporal/decimal do banco.

## Parada

Sem bloqueador funcional identificado nas validações executadas. Encerrado no
cadastro básico; sem iniciar classificação, custódia, movimentações ou outra
tarefa. Sem commit ou push.
