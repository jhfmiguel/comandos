# Armamento 013 - transferência entre unidades

## Objetivo
Implementar transferências de armamentos entre unidades organizacionais.

## Contexto e dependências
Transferência altera localização, responsabilidade e possivelmente custódia. Depende de unidade válida, item movimentável e status compatível.

## Escopo
Origem, destino, autorização, estados da transferência, aceite, rejeição, auditoria e telas do fluxo.

## Regras técnicas
- Modelar origem e destino sem alterar o histórico de operações anteriores.
- Impedir aceite pelo próprio contexto quando a regra de segregação exigir.
- Não concluir transferência sem aceite ou justificativa de rejeição.
- Garantir idempotência para evitar duplicação por reenvio.

## Critérios de aceite
- [ ] Transferência possui origem, destino, itens e responsável.
- [ ] Unidade destino pode aceitar ou rejeitar com histórico.
- [ ] Saldos, custódia e localização são atualizados corretamente.

## Condição de parada
Parar após validar transferência entre unidades. Não iniciar manutenção.
