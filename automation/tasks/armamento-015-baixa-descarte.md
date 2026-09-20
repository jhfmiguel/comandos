# Armamento 015 - baixa, descarte e inativação

## Estado após auditoria do commit 038
**IMPLEMENTADA, NÃO HOMOLOGADA POR COMPLETO.** Processo de descarte, destruição, estoque, idempotência, permissões, auditoria e interface existem.

## Objetivo
- Homologar baixa/destruição no navegador com PostgreSQL.
- Confirmar bloqueios para cautela, transferência, reserva, inventário e manutenção ativos.
- Revisar distinção de baixa lógica, inativação e destruição física.
- Confirmar autorização elevada/confirmacão para operações irreversíveis.

## Escopo
Implementar exclusivamente o trabalho descrito no objetivo desta tarefa, respeitando a arquitetura, os padrões e as integrações existentes do ERP Comandos.
## Critérios de aceite
- [ ] Operações irreversíveis exigem autorização e confirmação adequadas.
- [ ] Item baixado não retorna silenciosamente ao uso.
- [ ] Fluxo e histórico passam na homologação integrada.
 
## Condição de parada
Encerrar esta tarefa somente após implementar o objetivo e satisfazer os critérios de aceite aplicáveis. Não iniciar outra tarefa; o worker controla a continuidade da fila.