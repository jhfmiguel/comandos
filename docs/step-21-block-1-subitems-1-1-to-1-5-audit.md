# Etapa 21 — bloco 1, subitens 1.1 a 1.5

Data da revisão estática: 2026-10-12. Repositório: jhfmiguel/comandos, branch main.

> **Resultado:** análise estática realizada; cutover operacional NÃO comprovado. Não remover MasterDataMigrationService. Nenhuma instrução deste documento declara paridade de dados operacional ou gate PASS.

## 1.1 — MasterDataCutoverStatusService: revisado (código)

`MasterDataCutoverStatusService.status()` exige conjuntamente: readinessPassed, parityPassed, shadowWriteEnabled, productReferenceShadowEnabled, productReferencePrimaryReadEnabled, productReferencesReady, productReferenceParityPassed, legacyProductAssociationsRetired e canonicalReadEnabled. `requireReadyToRetireLegacyPersistence()` lança IllegalStateException se qualquer condição falhar. Não há evidência atual de execução bem-sucedida na base operacional.

## 1.2 — Integridade dos dados migrados: verificação estática concluída, execução pendente

`MasterDataMigrationService.readiness()` verifica CPFs/CNPJs duplicados e endereços, telefones, e-mails, unidades, credenciais e qualificações inválidas. `ProductMasterDataReferenceBackfillService.readiness()` participa do gate. Rodar no Oracle com dados reais/representativos, capturar resultados e validar contagens antes de afirmar integridade.

## 1.3 — Paridade: verificação estática concluída, execução pendente

`MasterDataParityService.verify()` cobre pessoas, organizações, unidades, contatos, papéis, documentos e qualificações. `ProductMasterDataReferenceParityService.verify()` verifica referências de produto. A existência de testes H2 e métodos verify() **não é** evidência de paridade zero no Oracle operacional. Exigir relatórios `consistent=true`, `mismatches=[]`, contagens e log de `legacy persistence retirement gate=PASS` em ambiente representativo, acompanhados da auditoria SQL read-only `scripts/oracle-step-21-retirement-audit.sql`.

## 1.4 — Consumidores: dependências diretas verificadas, inventário integral ainda pendente

- `MasterDataCutoverStatusService`: injeta `MasterDataMigrationService` e usa `readiness()`; injeta `MasterDataParityService` e usa `verify()`.
- `MasterDataMigrationRunner`: injeta `MasterDataMigrationService` para `readiness()` e `backfill()`; também usa paridade e cutover.
- `MasterDataMigrationIntegrationTests`: injeta `MasterDataMigrationService` e testa `readiness()` e `backfill()`.
- `MasterDataCutoverStatusService.CutoverStatus`: expõe `MasterDataMigrationService.ReadinessReport`.
- `.github/workflows/master-data-retirement-validation.yml`: depende da inicialização com os serviços e do sinal de gate PASS.

Antes da remoção, realizar busca de referência abrangente na árvore Java, configuração, testes, scripts, jobs e chamadas de API; o inventário acima é de consumidores confirmados, não um certificado de ausência de outros consumidores.

## 1.5 — Remoção de MasterDataMigrationService: BLOQUEADA

O arquivo `api/src/main/java/com/comandos/core/service/MasterDataMigrationService.java` permanece deliberadamente intacto. Removê-lo agora quebraria consumidores e eliminaria o backfill operacional antes da comprovação do cutover.

### Bloqueador já catalogado

`architecture/step-21-duplicate-retirement.json` registra falha do workflow na resolução dos artefatos Maven privados Faria Miguel, antes de iniciar a aplicação e atingir o gate. `architecture/step-21-master-data-retirement.json` também marca 21.12 como bloqueado. Essas evidências registradas podem estar desatualizadas; é obrigatório conferir a última execução e seus artefatos antes de repetir a validação.

### Critérios de liberação

1. Resolver publicação/acesso aos artefatos canônicos Faria Miguel e obter uma execução verde do workflow, com log `legacy persistence retirement gate=PASS`.
2. Rodar auditoria Oracle read-only e arquivar inventário de estruturas e contagens; comprovar integridade e zero divergências na base relevante.
3. Confirmar todos os consumidores e migrar `MasterDataMigrationRunner`, `MasterDataCutoverStatusService`, os testes e quaisquer usos adicionais para caminho canônico definitivo.
4. Comprovar que backfill não é mais necessário e que há estratégia de rollback e migrations compatíveis.
5. Só então eliminar `MasterDataMigrationService` em commit separado, com testes e gates verdes.

Referências: `architecture/master-data-foreign-keys.json`; `architecture/step-21-master-data-retirement.json`; `architecture/step-21-duplicate-retirement.json`; `api/src/main/resources/application.properties`; `scripts/oracle-step-21-retirement-audit.sql`; `.github/workflows/master-data-retirement-validation.yml`.
