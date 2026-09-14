# Armamento 001 — validação do backend

Revisão de 13/09/2026, restrita ao `wr-api`. O backend já continha os fluxos
funcionais; esta rodada preservou as alterações existentes e completou a
cobertura automatizada de consumo de munição e autorização.

## Estado inspecionado

- Cadastro de modelos e especificações, ativos individuais em lote, entrada de
  munição por caixas/unidades, saldos e movimentos de abertura.
- Cautela para pessoa ou unidade, devoluções e atualização do esquema legado.
- Reserva, movimentação entre unidades, inventário físico, manutenção, consumo
  e baixa, com persistência transacional e auditoria.
- Permissões por ação, organização e unidade; restrições de alterações genéricas
  sobre estoque e histórico; repetição idempotente das operações.

Não foi identificada decisão de negócio pendente para este escopo.

## Alterações desta rodada

- `AmmunitionConsumptionApiTests.java`: o teste de saldo insuficiente agora usa
  dois lotes da mesma unidade e verifica HTTP 409 e reversão do primeiro débito.
  Antes, usava outra organização e verificava apenas a rejeição de escopo.
- Acrescentados testes de UUID reutilizado com conteúdo diferente, exclusão e
  rejeição de lote vencido e duas requisições simultâneas com o mesmo UUID,
  verificando um único consumo/movimento e saldos exatos.
- A verificação de auditoria passou a filtrar a operação, evitando dependência
  da ordem de execução dos testes.
- `AuthorizationApiTests.java`: acrescentada cobertura de consumo com login e
  permissões reais, incluindo ausência de CREATE, consulta sem unidade,
  organização não autorizada, conclusão permitida e revogação de CREATE antes
  de repetir a requisição.

## Reprodução

O cache padrão tentou usar `C:\.m2\repository`, fora da área gravável. Foi
copiado o cache já instalado em `C:\Users\Jorge Miguel\.m2\repository` para
`wr-api/target/maven-repository`; nenhuma dependência foi baixada.

Executar em `wr-api`:

```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-repository' '-Dtest=AmmunitionConsumptionApiTests,AuthorizationApiTests' test
.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-repository' test
```

A suíte focada passou: 22 testes, zero falhas, erros ou ignorados.
A suíte Maven completa passou: **104 testes, zero falhas, erros ou ignorados**,
com `BUILD SUCCESS` e código de saída 0. A fase de compilação da API também
passou. `git diff --check -- wr-api` não apontou erros de whitespace.
Os testes usam H2 em modo PostgreSQL; esta rodada não homologa concorrência
em PostgreSQL de produção. Os relatórios individuais ficam em
`wr-api/target/surefire-reports`.

Nenhuma alteração de frontend, commit ou push foi realizada nesta rodada.
