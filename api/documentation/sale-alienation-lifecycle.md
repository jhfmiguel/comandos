# Venda / alienação — ciclo completo

## Objetivo

A alienação é tratada como saída definitiva de titularidade institucional, vinculada a processo, base legal e documento de referência, com rastreabilidade de estoque, devolução/cancelamento e auditoria.

## Fechamento da venda

Uma venda finalizada deve possuir obrigatoriamente:

- organização e, quando aplicável, unidade;
- comprador;
- forma de pagamento;
- número do processo;
- base legal;
- referência documental;
- data e operador de finalização;
- data e operador da retirada definitiva;
- total calculado pelos itens;
- requestId idempotente e fingerprint.

Ao finalizar, o domínio registra `withdrawalState = WITHDRAWN` e `titleTransferState = TRANSFERRED_TO_BUYER`.

## Patrimônio individual

Para ativo serializado/patrimoniado:

1. somente ativo `AVAILABLE` pode ser vendido;
2. quantidade é obrigatoriamente 1;
3. é criado movimento `SALE` negativo;
4. o ativo passa a `SOLD`;
5. `SOLD` é estado terminal para disponibilidade normal;
6. o item preserva o proprietário anterior (`ORGANIZATION`) e novo (`BUYER`).

Enquanto não houver devolução/cancelamento válido, o ativo deve permanecer `SOLD` e não pode reaparecer no estoque disponível.

## Lote / consumível

Para lote:

1. a quantidade vendida é deduzida do `StockBalance.available`;
2. a mesma quantidade é deduzida do `StockLot.availableQuantity`;
3. é criado movimento `SALE` negativo;
4. a quantidade devolvida não pode ultrapassar a quantidade originalmente vendida.

## Devolução e cancelamento

A devolução cria movimento positivo `SALE_RETURN`. O cancelamento cria movimento positivo `SALE_CANCELLATION`.

Para ativo individual, a devolução retira o ativo do estado `SOLD`, restaurando-o para `AVAILABLE` quando elegível ou `BLOCKED` quando houver impedimento de restauração automática.

Cancelamento deve abranger integralmente todos os itens ainda não devolvidos da venda.

## Documentos

Processo, base legal e documento de referência fazem parte do ato de alienação e não são dados opcionais de apresentação. O domínio impede persistir venda finalizada sem esses três elementos.

## Relatório do ciclo

`GET /api/erp/sales/{id}/lifecycle`

O relatório operacional retorna, em uma única visão:

- organização, unidade e comprador;
- processo, base legal e documento;
- finalização;
- retirada definitiva;
- transferência de titularidade;
- total da venda;
- itens vendidos;
- patrimônio/lote;
- movimento de saída;
- proprietário anterior e novo;
- estado atual do ativo;
- quantidade devolvida por item;
- devoluções e cancelamentos;
- motivo, operador e referência de estorno.

## Regressão Oracle

`SaleAlienationLifecycleDemoVerifier` valida automaticamente:

- documentação obrigatória;
- retirada definitiva;
- transferência de titularidade;
- movimento `SALE` negativo;
- exatamente um ativo ou lote por item;
- subtotal e total matematicamente consistentes;
- quantidade devolvida <= quantidade vendida;
- ativo não devolvido permanece `SOLD`;
- ativo devolvido deixa o estado `SOLD`.
