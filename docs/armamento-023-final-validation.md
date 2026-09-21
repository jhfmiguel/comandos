# Armamento 023 — relatório final de validação

Data: 20/09/2026. Base: `6d8dd3a57951e69a5927ffd1161b3cb870fb8353`.
Esta rodada altera somente documentação; nenhum commit ou push foi executado.
Não há aprovação operacional: a homologação integrada está bloqueada.

## Validações desta rodada

| Verificação | Resultado e evidência local |
| --- | --- |
| API completa: `.\mvnw.cmd -o '-Dmaven.repo.local=target/m2-validation' test` em `wr-api` | BUILD SUCCESS: 122 testes, zero falhas, erros ou ignorados; `wr-api/target/armamento-023-api-cache-final.log` |
| `npm.cmd run lint` em `wr-app` | Exit 0: zero erros, 10 avisos em diretórios `.backup-*`; `wr-app/lint-armamento-023.log` |
| `npm.cmd run build` | Falhou no prerender de `/_global-error`, com `useContext` nulo; `wr-app/build-armamento-023.log` |
| `$env:NODE_ENV='production'; npm.cmd run build` | Exit 0; compilação, TypeScript e geração de páginas concluídos; `wr-app/build-armamento-023-production.log` |
| `node scripts/validate-stock-intake.mjs` | Falhou na preparação: POST `/api/erp/inventory/locations` retornou 500, coluna `erp_stock_location.active` ausente; `wr-app/browser-armamento-023.log` |
| `node scripts/validate-browser.mjs` | Falhou na preparação dos dados; `wr-app/browser-armamento-023-general.log`; homologação não concluída |

Os logs são artefatos locais ignorados pelo Git. O teste de navegador não chegou
às operações de recebimento; não comprova cadastro, consulta ou movimentação no
frontend. Os demais cenários integrados não foram aprovados nesta rodada.

Após a primeira tentativa sem API, ela foi iniciada com o cache offline e
`spring-boot:run`, argumentos `--server.port=8180
--spring.datasource.url=jdbc:postgresql://localhost:5432/wr_validation_20260912
--erp.allowed-origin=http://localhost:3100 --spring.jpa.show-sql=false`.
O frontend foi iniciado com `npm.cmd run start -- --port 3100` e NODE_ENV production.
O PostgreSQL isolado existente recebeu a tentativa automática de atualização de
esquema e dados fictícios parciais dos scripts. A API iniciou, mas o esquema não
ficou utilizável para cadastrar locais. Evidência de inicialização e DDL:
`wr-api/target/armamento-023-browser-api.log`. Não apagar os dados existentes nem
tratar o simples início da API como comprovação de migração bem-sucedida.

A tentativa padrão de Maven falhou por acesso a `C:\.m2\repository`; o cache
`automation/runtime/m2-recalls` tentou baixar o parent e esbarrou na restrição de
rede. O cache `wr-api/target/m2-validation` permitiu executar a suíte completa
offline, sem alterar versões ou dependências. Os testes usam a configuração H2
de `src/test/resources/application.properties`.

## Contratos, permissões e estados conferidos no código

- `Brand.manufacturingCountryCode` é opcional; `ItemModel.manufacturerCode`
  pertence ao modelo. Ver [contrato de marcas](brand-manufacturing-country.md).
- `Recall.description` e `RecallItem.description` são opcionais, com 255
  caracteres. Ver [compatibilidade de atualização](recall-descriptions.md).
- `AccessPolicy` usa recursos e ações exatos, curinga completo `*` e escopos
  SYSTEM, ORGANIZATION e UNIT. Recursos de administração de acesso exigem
  `security/access` / `MANAGE`. Catálogos sem organização exigem escopo SYSTEM
  quando as permissões estão habilitadas. Não basta ocultar botões na interface.
- `application.properties` deixa login e permissões desabilitados por padrão.
  Provisionar administrador e habilitar `ERP_REQUIRE_LOGIN` e
  `ERP_ENFORCE_PERMISSIONS` antes de uso protegido, conforme o manual.
- `TransferController` oferece consulta de estoque, finalização, lista e detalhe
  em `/api/erp/transfers`. Não existe endpoint de aceite/rejeição pelo destino.
- `CustodyService` grava `CUSTODIED`; `MaintenanceService` grava
  `IN_MAINTENANCE` e conclui com `AVAILABLE` ou `BLOCKED` conforme teste/validade.
  `EquipmentStatePolicy` ainda usa `IN_CUSTODY` e `MAINTENANCE` nas operações
  INSPECTION/OCCURRENCE. Esses códigos não devem ser documentados como equivalentes
  nem corrigidos diretamente no banco; a uniformização requer trabalho próprio.
- Histórico de movimentos e auditoria existem, mas esta revisão estática não
  comprova cobertura integral de cada ação ou isolamento entre todos os perfis.

## Migrações, riscos e pendências

O ambiente usa PostgreSQL e `spring.jpa.hibernate.ddl-auto=update`. Não foi
executada migração de produção nem modificado o banco habitual. As adições recentes
incluem `erp_brand.manufacturing_country_code` nullable e `description` nullable
em `erp_recall` e `erp_recall_item`; registros antigos não exigem valores inventados.
Planejar backup, ensaio com dados existentes, conferência de constraints e
restauração antes de liberar. H2 não substitui esse ensaio em PostgreSQL.
O ensaio desta rodada no banco isolado revelou falha concreta de compatibilidade
de esquema em `erp_stock_location.active`; uma migração com tratamento dos dados
existentes precisa ser definida e validada antes de repetir a homologação.

Pendências explícitas, sem implementação adicional nesta tarefa:

- Homologar cadastro, recebimento, consulta, cautela/devolução, transferência,
  manutenção, baixa e inventário em API e banco isolados, incluindo perfis restritos.
- Resolver divergências de estados e validar os impactos em inspeções/ocorrências.
- Aceite/rejeição do destino não integra o contrato de transferência entregue.
- Confirmar cobertura de rastreabilidade, uniformidade de localização/unidade e
  persistência de filtros na URL, apontadas pela auditoria 038.
- Confirmar com responsável de negócio o significado de manufacturerCode antes
  de qualquer reinterpretação. Não inferir país de fabricação de cada exemplar.
- A presença de infraestrutura de workflow não comprova aprovação integrada de
  todos os fluxos; fotos, assinaturas e notificações exigem aceite específico.

## Barreira final

Foi gravado `automation/runtime/control.json` com `{"command":"pause"}` usando
o controle existente. O worker consulta essa solicitação antes de buscar outra
tarefa; em falha, seu tratamento também encerra a fila. Não foi enviado `resume`
nem iniciada outra tarefa. O arquivo é local e ignorado pelo Git: não constitui
um bloqueio permanente de futuras execuções manuais. A retomada depende de
autorização explícita do usuário. A tarefa 023 permanece sem aceite final enquanto
a homologação integrada não estiver comprovada.
