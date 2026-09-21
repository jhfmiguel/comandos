# Armamento 017 — auditoria e rastreabilidade

Implementação em 20/09/2026. Escopo restrito à tarefa 017; sem commit/push.

## Resultado funcional

- Cobertura conferida para custódia, transferência, manutenção, descarte,
  inventário, consumo, doação, incorporação e workflow. Matriz em [audit.md](audit.md).
- Referências históricas tipadas para patrimônio/lote, organização e unidades;
  filtros combináveis com operação, usuário, período e registro da operação.
- Preservação do estado anterior nas transições de inventário, workflow e
  conclusão de manutenção. Eventos e referências participam da transação de negócio.
- Imutabilidade JPA e rejeição de remoção de eventos/referências; API sem escrita.
- Leitura integral exige audit READ SYSTEM. Grants UNIT/ORGANIZATION não permitem
  ler snapshots cruzados. O relatório consolidado filtra seu histórico pela
  organização/unidade solicitada, inclusive para o leitor SYSTEM.

## Arquivos desta implementação

Sob `wr-api/src/main/java/com/weaponsregistration/`:

- `audit/model/AuditReference.java` (novo)
- `audit/model/AuditRecord.java`
- `audit/service/AuditService.java`
- `audit/controller/AuditController.java`
- `maintenance/service/MaintenanceService.java`
- `reconciliation/service/InventoryCountService.java`
- `workflow/service/WorkflowService.java`
- `report/service/ArmamentReportService.java`

Sob `wr-api/src/test/java/com/weaponsregistration/`:

- `audit/controller/AuditTraceAssertions.java` (novo)
- `audit/controller/AuditApiTests.java`
- `consumption/controller/AmmunitionConsumptionApiTests.java`
- `custody/controller/CustodyApiTests.java`
- `disposal/controller/DisposalApiTests.java`
- `donation/controller/DonationApiTests.java`
- `inventory/controller/InventoryApiTests.java`
- `maintenance/controller/MaintenanceApiTests.java`
- `reconciliation/controller/InventoryCountApiTests.java`
- `transfer/controller/TransferApiTests.java`
- `security/controller/AuthorizationApiTests.java`
- `lifecycle/controller/LifecycleWorkflowReportApiTests.java`

Documentação: `docs/audit.md` e este arquivo. Alterações anteriores no workspace
foram preservadas, inclusive nos arquivos compartilhados com esta tarefa.

## Validação

Executado a partir de `wr-api`, usando o cache Maven existente, sem rede:

```powershell
.\mvnw.cmd '-Dmaven.repo.local=C:\workspace\weapons-registration\wr-api\target\maven-test-cache' -o '-Dtest=AuditApiTests,TransferApiTests,MaintenanceApiTests,InventoryCountApiTests,DonationApiTests,DisposalApiTests,AmmunitionConsumptionApiTests,CustodyApiTests,LifecycleWorkflowReportApiTests,AuthorizationApiTests,InventoryApiTests' test
```

97 testes, zero falhas/erros/ignorados, BUILD SUCCESS. Log:
`wr-api/target/armamento-017-tests.log`.

Após acrescentar o filtro do relatório consolidado e as verificações de
incorporação em lote, repetidas apenas as suítes afetadas:

```powershell
.\mvnw.cmd '-Dmaven.repo.local=C:\workspace\weapons-registration\wr-api\target\maven-test-cache' -o '-Dtest=InventoryApiTests,LifecycleWorkflowReportApiTests,AuthorizationApiTests' test
```

61 testes, zero falhas/erros/ignorados, BUILD SUCCESS. Log:
`wr-api/target/armamento-017-final-tests.log`.

Após fazer a indexação consumir exatamente os snapshots já sanitizados,
reexecutado o mesmo comando com `-Dtest=AuditApiTests`: 6 testes, zero
falhas/erros/ignorados, BUILD SUCCESS, em
`wr-api/target/armamento-017-audit-final.log`. `git diff --check` também passou.

Os testes exercitam HTTP → serviço → banco → consulta/detalhe de auditoria,
preservação da unidade original após transferência, todos os itens de uma
incorporação em lote, snapshots, identidade do ator, filtros combinados,
idempotência, rollback, proteção contra alteração/remoção e negação de acesso.

## Limites operacionais

Sem bloqueadores funcionais. Retenção, evidência contra adulteração, arquivo e
exportação controlada são responsabilidades de infraestrutura explicitadas em
`audit.md`; não são certificadas por H2. PostgreSQL de produção não foi executado.
Os novos vínculos históricos são gravados a partir da implantação; eventos
anteriores continuam consultáveis pelos filtros originais, sem reconstruir
unidade histórica a partir da localização atual. O novo índice não concede
acesso delegado a snapshots para perfis locais.
