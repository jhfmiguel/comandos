# Armamento 008 - classificação e tipos

Implementado no catálogo dinâmico de inventário:

- Categoria existente -> tipo de armamento -> classificação.
- Modelo mantém sua categoria e pode selecionar tipo e classificação compatíveis.
- `armament-types` e `armament-classifications` usam os endpoints genéricos
  GET/POST/PUT/DELETE de `/api/erp/inventory`, com busca, permissões,
  versão otimista e auditoria transacional antes/depois.
- Códigos únicos, imutáveis e em maiúsculas; referências obrigatórias nos
  catálogos; referências opcionais no modelo para compatibilidade.
- Tipo usado por classificação ou modelo não pode ser inativado, excluído
  ou movido de categoria. Classificação usada por modelo não pode ser
  inativada, excluída ou movida de tipo. É necessário reassociar os
  registros dependentes explicitamente antes dessas operações.
- Valores inativos e combinações incompatíveis são rejeitados pela API.
- Novas tabelas e colunas de referência nullable seguem o
  `spring.jpa.hibernate.ddl-auto=update` existente. Não há recodificação
  dos registros antigos. PUT de clientes antigos que omitem os novos
  campos preserva as referências; null explícito permite removê-las.
- Menu inclui os dois catálogos; formulário dinâmico filtra as seleções
  pelo vínculo e estado ativo e limpa dependências ao trocar categoria/tipo.
  A busca permite refinar os resultados paginados.

Arquivos desta tarefa:

- `wr-api/src/main/java/com/weaponsregistration/inventory/model/ArmamentType.java`
- `wr-api/src/main/java/com/weaponsregistration/inventory/model/ArmamentClassification.java`
- `wr-api/src/main/java/com/weaponsregistration/inventory/model/ItemModel.java`
- `wr-api/src/main/java/com/weaponsregistration/inventory/service/InventoryCatalog.java`
- `wr-api/src/main/java/com/weaponsregistration/inventory/service/InventoryRules.java`
- `wr-api/src/main/java/com/weaponsregistration/inventory/service/InventoryService.java`
- `wr-api/src/test/java/com/weaponsregistration/inventory/controller/InventoryApiTests.java`
- `wr-app/src/components/erp/shared/record-workspace.tsx`
- `wr-app/src/components/layout/menu/index.tsx`
- Este documento.

Validação final:

- API: `.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-repository' '-Dtest=InventoryApiTests,AuthorizationApiTests' test` em `wr-api`:
  **BUILD SUCCESS**, 45 testes, zero falhas/erros/ignorados, confirmado nos XMLs
  Surefire (27 inventário, 18 autorização). Log: `wr-api/target/armamento-008-tests.log`.
  O shell com redirecionamento reportou código 1 apesar do sucesso Maven,
  como na validação anterior; resultado conferido no log e XMLs.
- Frontend: `node.exe node_modules/eslint/bin/eslint.js`: código 0,
  zero erros e seis avisos preexistentes em backups, bot e import do menu.
- Frontend: `node.exe node_modules/typescript/bin/tsc --noEmit`: código 0.
- Primeira rodada exigiu atualizar a contagem esperada do catálogo de 31
  para 33. Rodada final passou. Sem validação de navegador ou banco de produção.
- Nenhum bloqueador identificado nas validações executadas.

Escopo encerrado em classificação e tipos, sem identificação patrimonial,
sem outra tarefa, sem commit ou push. Alterações preexistentes preservadas.
