# Armamento 025 - descrição de recalls

## Estado após auditoria do commit 038
**PENDENTE.** `Recall` possui reason/status e `RecallItem` possui action, mas nenhum dos dois possui o campo `description` solicitado.

## Objetivo
- Adicionar description em Recall e RecallItem.
- Definir tamanho máximo e comportamento para vazio/nulo.
- Expor em contratos, persistência, cadastro, detalhe, consulta e auditoria.
- Manter compatibilidade com registros existentes e criar testes.

## Critérios de aceite
- [ ] Recall possui descrição editável/consultável.
- [ ] Recall Item possui descrição editável/consultável.
- [ ] API, frontend e testes passam.
