# Armamento 028 - cadastros parametrizados

## Objetivo
Criar cadastros parametrizáveis para substituir valores fixos nas telas do ERP.

## Contexto e prioridade
Esses catálogos serão usados por vários módulos. A implementação deve evitar listas hardcoded e permitir evolução administrativa sem alteração de código.

## Escopo
Implementar, conforme os padrões existentes, cadastros, API, permissões, auditoria, telas e seletores para: Grenade Type, Agent, Unit of Measure, Projectile Type, Case Type, Primer Type, Caliber, Protection Type, Protection Level, Material, Cartridge Type, Optical Type, External System, Certification Type, Reason, Types, Category, Level e Action.

## Regras técnicas
- Identificar catálogos já existentes antes de criar entidades novas.
- Preferir ativação/inativação a exclusão física quando houver uso histórico.
- Definir escopo institucional, unicidade, ordenação e permissões por catálogo.
- Atualizar seletores consumidores sem quebrar valores antigos.

Os valores devem ser administráveis e reutilizados nas telas relacionadas, sem listas hardcoded duplicadas.

## Critérios de aceite
- [ ] Cada catálogo possui cadastro, edição, ativação/inativação e consulta quando aplicável.
- [ ] Telas usam os valores parametrizados.
- [ ] Autorização, auditoria, duplicidade e testes estão cobertos.
- [ ] O fluxo não quebra registros existentes.

## Condição de parada
Parar se algum catálogo exigir decisão de negócio não definida; registrar o bloqueio.

## Bloqueio identificado em 2026-09-14

Status: bloqueada na análise, conforme a condição de parada. Nenhuma alteração funcional realizada.

A lista não define o domínio e os consumidores dos catálogos genéricos. O código existente apresenta significados distintos que não podem ser unificados apenas pelo rótulo:

- **Types**: `OrganizationalUnit.type` e `PersonCredential.type` em `CoreCatalog.java`; `StockLocation.type` e `ExpirationRecord.type` em `InventoryCatalog.java`. Também já existe o cadastro `armament-types` (`ArmamentType`). Falta definir quais desses domínios estão incluídos e se terão catálogos separados.
- **Category**: já existe `categories` (`ItemCategory`), utilizado por modelos e tipos de armamento; `PersonQualification.category` é outro campo, textual, em `CoreCatalog.java`. Falta definir qual categoria é abrangida ou se ambas devem ser administradas separadamente.
- **Action**: `Permission.action` em `CoreCatalog.java` e `RecallItem.action` em `InventoryCatalog.java` representam, respectivamente, uma ação de autorização e uma providência de recall. Falta definir os consumidores pretendidos e se o catálogo de permissões deve permitir novas ações ou apenas administrar as reconhecidas pelo sistema.
- **Reason**: `Recall.reason` e `DisposalProcess.reason` são textos; devoluções já utilizam `SaleReturnReasonType` (`sale-return-reason-types`). Falta definir quais motivos serão parametrizados e se devem compartilhar valores.
- **Level**: `AccessProfile.level` existe em `CoreCatalog.java`, enquanto `BallisticProtectionSpecification.protectionLevel` possui domínio próprio e já aparece separadamente no escopo. Confirmar o domínio pretendido para Level.

Catálogos identificados para possível reaproveitamento: `armament-types`, `categories` e `sale-return-reason-types`. `InventoryCatalog.java` também registra catálogos de condições de devolução de cautela, status de reserva e status/resultados de inventário; portanto, não se deve criar uma entidade genérica Types sem delimitar sua relação com os cadastros existentes.

Decisão necessária para retomar: fornecer o mapeamento catálogo → módulos/campos consumidores para os nomes ambíguos acima, indicando quais domínios compartilham valores e quais permanecem separados. Essa definição orientará o escopo institucional, a unicidade, as permissões e a preservação dos valores históricos.

Validação realizada: leitura da tarefa, das instruções locais, dos catálogos, dos modelos relacionados e de `docs/inventory.md`; conferência do estado Git para identificar alterações preexistentes. Testes não executados porque a condição de parada foi atingida antes de qualquer mudança funcional. Não houve commit, push ou avanço para outra tarefa.
