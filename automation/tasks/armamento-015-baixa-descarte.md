# Armamento 015 - baixa, descarte e inativação

## Estado após auditoria do commit 038
**IMPLEMENTADA, NÃO HOMOLOGADA POR COMPLETO.** Processo de descarte, destruição, estoque, idempotência, permissões, auditoria e interface existem.

## Trabalho restante
- Homologar baixa/destruição no navegador com PostgreSQL.
- Confirmar bloqueios para cautela, transferência, reserva, inventário e manutenção ativos.
- Revisar distinção de baixa lógica, inativação e destruição física.
- Confirmar autorização elevada/confirmacão para operações irreversíveis.

## Critérios de aceite
- [ ] Operações irreversíveis exigem autorização e confirmação adequadas.
- [ ] Item baixado não retorna silenciosamente ao uso.
- [ ] Fluxo e histórico passam na homologação integrada.
