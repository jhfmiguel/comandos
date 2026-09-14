# Armamento 009 — identificação patrimonial

## Implementação

- `AssetIdentity.java`: regra compartilhada de identificação: Unicode NFKC,
  maiúsculas com `Locale.ROOT`, remoção de espaços Unicode (inclusive internos,
  tabulações e espaços não separáveis). Pontuação permanece significativa.
  Permanecem os limites de 1 a 255 caracteres e a obrigatoriedade de serial
  para categorias serializadas.
- `InventoryService.java`: aplica a normalização antes da validação dos campos
  e da comparação de identificadores imutáveis. Criações verificam códigos e
  seriais globalmente, independentemente do modelo, sob o lock existente do
  catálogo. Conflitos identificam cada campo e o ID do ativo relacionado.
- `StockIntakeService.java`: revisão e confirmação em lote usam a mesma regra;
  conflitos internos identificam as linhas relacionadas e conflitos com o
  cadastro identificam o ativo. O relatório de aceitação contém os pares
  normalizados efetivamente persistidos.
- `InventoryApiTests.java`: expectativas atualizadas e cobertura de
  normalização, duplicidade entre modelos, registros legados não normalizados,
  revisão/confirmação rejeitadas e imutabilidade do vínculo com movimentos.

A comparação também normaliza registros antigos em memória, sem reescrever
o banco ou alterar os IDs usados pelo histórico. A consulta de conflitos lê
os ativos existentes; para volumes elevados, uma futura migração de chaves
canônicas indexadas pode substituir essa leitura. Não foi realizada migração
nem saneamento de eventuais duplicidades preexistentes.

Quantidade permanece derivada: cada par aceito cria um ativo individual e um
movimento de quantidade 1; o recibo conta os IDs criados. Quantidade livre no
cadastro individual é rejeitada. Identificadores e modelo continuam imutáveis,
e ativos registrados não podem ser excluídos pelo cadastro.

## Validação

Executado em `wr-api`:

```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-repository' '-Dtest=InventoryApiTests,AuthorizationApiTests' test
```

Resultado: **46 testes, zero falhas, erros ou ignorados; BUILD SUCCESS**.
Log: `wr-api/target/armamento-009-tests.log`. Testes usam H2 isolado.
`git diff --check` dos arquivos rastreados alterados: sem erros de whitespace.

Nenhum bloqueio. Alterações preexistentes preservadas. Sem commit ou push.
Encerrado após identificação patrimonial; status operacional não iniciado.
