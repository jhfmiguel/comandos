# Armamento 021 — validação da API

Validação em 20/09/2026 sobre a base `6d8dd3a` e as alterações desta tarefa.

## Implementação

- Adicionados 10 testes HTTP de lifecycle, workflow, relatórios e cadastros de status, com banco H2 próprio, porta aleatória, operadores autenticados e dados exclusivos por teste.
- Cobertos transições válidas e inválidas, cancelamento terminal, auditoria, aprovação e resolução idempotentes, anexos, validação, duplicidade, conflito de versão e proteção dos status padrão.
- Cobertos bloqueio, perda, recuperação, geração de manutenção, emissão/devolução de cautela, filtros por organização/unidade e divergências de inventário aprovado. Contagens sem divergência não geram alerta.
- Adicionado teste com permissões habilitadas para leitura, criação, atualização, revogação e escopo de lifecycle/workflow/relatórios.
- Corrigido o dashboard para usar `CUSTODIED`, `IN_MAINTENANCE`, `status.code = APPROVED` e `result.code <> MATCH`. A consulta anterior de inventário falhava ao comparar entidades com strings.
- Corrigida a criação de workflow para exigir permissão no escopo da organização/unidade solicitada.
- Mantida a cobertura existente das especificações de equipamentos, operações de estoque, duplicidade e idempotência.

## Execução

O comando inicial `mvnw.cmd test` não conseguiu criar `C:\.m2\repository` no ambiente restrito. O cache Maven existente foi copiado para `wr-api/target/maven-test-cache`, sem alterar a configuração do projeto. As execuções seguintes usaram esse cache em modo offline.

Em `wr-api`:

```powershell
.\mvnw.cmd '-Dmaven.repo.local=C:\workspace\weapons-registration\wr-api\target\maven-test-cache' -o test
```

- Base anterior às alterações: 122 testes, zero falhas, erros ou testes ignorados.
- Execução intermediária com as correções e novos testes: 132 testes, zero falhas, erros ou testes ignorados.
- Execução final, incluindo a regressão de cautela e ordem aleatória de métodos com semente fixa: **133 testes, zero falhas, erros ou testes ignorados; BUILD SUCCESS**.

```powershell
.\mvnw.cmd '-Dmaven.repo.local=C:\workspace\weapons-registration\wr-api\target\maven-test-cache' -o '-Djunit.jupiter.testmethod.order.default=org.junit.jupiter.api.MethodOrderer$Random' '-Djunit.jupiter.execution.order.random.seed=21' test
```

Evidências locais: `wr-api/target/armamento-021-final.log` e relatórios XML em `wr-api/target/surefire-reports`. O PowerShell sinalizou saída nativa em stderr por avisos da JVM/Mockito; o resultado Maven e os relatórios Surefire confirmam o sucesso dos testes.

`git diff --check -- wr-api`: sem erros. Validação usa H2 em modo PostgreSQL, conforme o padrão da suíte; não constitui execução contra um servidor PostgreSQL externo. Nenhum bloqueio pendente, commit ou push realizado.
