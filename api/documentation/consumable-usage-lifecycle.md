# Consumo / deflagração — entrega, uso e sobra devolvida

## Objetivo

O consumo de itens lote-controlados não deve ser interpretado apenas como uma baixa líquida. Quando o material é entregue para uma atividade e pode haver sobra, o ciclo físico é:

1. **entrega** do material ao responsável;
2. **uso/consumo efetivo** durante a atividade;
3. **devolução da sobra**, quando existir;
4. **reconciliação**, em que `entregue = usado + devolvido`.

A baixa definitiva do estoque corresponde somente à quantidade usada.

## Famílias abrangidas

O fluxo genérico não depende do nome da família. É elegível qualquer categoria com:

- `consumable = true`;
- `lotControlled = true`;
- `serialized = false`.

Isso mantém munição elegível e permite aplicar a mesma semântica a granadas consumíveis, espargidores e futuras famílias equivalentes.

O endpoint legado de munição permanece disponível para compatibilidade. O novo contrato genérico é exposto em:

`/api/erp/consumable-usages`

## Movimentos

Na entrega é criado movimento:

- natureza `CONSUMABLE_DELIVERY`;
- quantidade negativa igual a toda a quantidade entregue.

Quando existe sobra, é criado movimento:

- natureza `CONSUMABLE_RETURN`;
- quantidade positiva igual à quantidade devolvida.

Assim:

`movimento líquido = -entregue + devolvido = -usado`

A quantidade usada fica registrada explicitamente no item do ciclo e corresponde à baixa definitiva.

## Invariantes

Para cada item:

- `deliveredQuantity > 0`;
- `usedQuantity >= 0`;
- `returnedQuantity >= 0`;
- `deliveredQuantity = usedQuantity + returnedQuantity`;
- a entrega deve possuir movimento negativo equivalente;
- devolução maior que zero exige movimento positivo equivalente;
- devolução zero não pode gerar movimento de retorno;
- lote vencido não pode ser entregue;
- entrega não pode superar o saldo disponível;
- o lote deve permanecer vinculado à organização/unidade escolhida.

## Idempotência

O fechamento usa `requestId` canônico e `requestFingerprint`. Repetir a mesma requisição retorna o ciclo já persistido. Reutilizar o mesmo `requestId` com conteúdo diferente é conflito.

## Autorização

A primeira versão do contrato genérico reutiliza a autorização já consolidada em `ammunition-consumptions`, preservando o mesmo controle de escopo enquanto o recurso é generalizado no catálogo de permissões.

## Auditoria

O fechamento registra:

- organização e unidade;
- responsável;
- autorizador;
- finalidade;
- atividade/operação/treinamento;
- família do consumível;
- lote e localização;
- entregue, usado e devolvido;
- saldo anterior e saldo final;
- operador do fechamento.

## Regressão Oracle

`ConsumableUsageLifecycleDemoVerifier` valida:

- existência de munição e de ao menos uma família consumível não-munição no catálogo;
- elegibilidade por flags de categoria;
- `entregue = usado + devolvido`;
- movimento negativo de entrega;
- movimento positivo de devolução quando houver sobra;
- saldo líquido dos movimentos igual ao consumo efetivo;
- proveniência de lote, saldo e localização;
- timestamps e encerramento do ciclo.
