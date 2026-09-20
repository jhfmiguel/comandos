# Armamento 013 - transferência entre unidades

## Estado após auditoria do commit 038
**PARCIAL.** Transferência atômica entre unidades, ativos/lotes, escopo, idempotência, movimentos e auditoria já existem. O fluxo atual finaliza diretamente e não implementa o aceite/rejeição da unidade destino previsto na tarefa original.

## Objetivo
- Implementar estados de solicitação/envio/aceite/rejeição, ou documentar formalmente decisão de negócio que elimine o aceite.
- Se mantido aceite: segregar autorização de origem/destino e exigir justificativa na rejeição.
- Só atualizar responsabilidade/localização na etapa definida pelo fluxo aprovado.
- Testar idempotência e concorrência do fluxo completo.

## Critérios de aceite
- [ ] Regra de aceite/rejeição está implementada ou decisão de negócio está documentada.
- [ ] Origem, destino, itens, responsáveis e histórico permanecem íntegros.
- [ ] Testes cobrem o ciclo completo.
