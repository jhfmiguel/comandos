# Armamento - controle híbrido de munição por caixa e unidade

## Objetivo

Implementar o controle de munições do ERP COMANDOS permitindo registrar entradas por caixas e quantidades, controlar caixas individualmente quando necessário e também movimentar munições por unidade, sem perder rastreabilidade ou saldo.

## Contexto e prioridade

Esta é uma regra central do estoque de munição. O sistema precisa servir tanto ao controle logístico agregado quanto ao controle físico detalhado, sem misturar os dois modelos de forma silenciosa.

## Regra de negócio definida pelo usuário

A entrada de estoque deve permitir informar caixas e quantidades. O sistema deve suportar os dois modos:

1. **Caixa controlada individualmente**: cada caixa possui identificação própria, capacidade/quantidade inicial e saldo atual próprio.
2. **Entrada agregada**: informar, por exemplo, 10 caixas de 50 cartuchos, gerando 500 unidades no estoque sem exigir identificação individual de cada caixa.

A mesma munição pode ser:

- cadastrada por caixa ou por unidade;
- cautelada por caixa ou por unidade;
- vendida por caixa ou por unidade;
- cedida/doada por caixa ou por unidade;
- movimentada entre unidades por caixa ou por unidade;
- registrada em caixas com quantidades distintas.

Para `Individual Asset`, a quantidade não deve ser digitada livremente: ela deve ser calculada pela quantidade de números de série associados ao ativo individual.

## Escopo

Inspecionar os modelos, serviços, endpoints, telas e testes existentes de inventário, munição, consumo, cautela, vendas, doações e transferências. Implementar somente o necessário para o controle híbrido de munição.

### Entrada de estoque

- Permitir escolher o modo de entrada: caixas controladas ou entrada agregada.
- Permitir informar calibre, tipo, lote, fabricante e demais dados já exigidos pelo domínio.
- Permitir informar quantidade de caixas e quantidade por caixa.
- Permitir caixas com quantidades diferentes no mesmo recebimento.
- No modo individual, gerar ou aceitar identificador único para cada caixa.
- No modo agregado, manter a quantidade total de unidades e a origem da entrada sem inventar identificadores de caixa.
- Calcular automaticamente o total: soma das quantidades reais das caixas ou quantidade agregada informada.

### Saldo e rastreabilidade

- Caixa individual deve possuir saldo próprio e não pode ficar negativo.
- Baixas parciais devem reduzir o saldo da caixa quando a movimentação for por caixa.
- Movimentação por unidade deve reduzir o saldo unitário/lote conforme o modo de estoque.
- Impedir mistura silenciosa de caixas identificadas com saldo agregado.
- Registrar lote, origem, usuário, data, operação e unidade responsável.

### Operações

- Cautela por caixa inteira, parte do saldo de uma caixa ou quantidade de unidades.
- Venda por caixa ou unidade.
- Cessão/doação por caixa ou unidade.
- Transferência por caixa ou unidade.
- Consumo por unidade, com origem rastreável quando existir caixa individual.
- Devolução e estorno devem recompor o saldo correto.

### Individual Assets

- Manter cada `asset code` vinculado ao seu `serial number`.
- Derivar a quantidade do Individual Asset contando os números de série associados.
- Impedir quantidade manual divergente da quantidade de seriais.
- Validar unicidade dos números de série e dos asset codes.

## Decisões de implementação

- Modelar o modo de rastreabilidade explicitamente: caixa identificada ou saldo agregado.
- Nunca gerar identificadores de caixa fictícios para uma entrada agregada.
- Reservar e baixar saldo com concorrência segura para impedir saldo negativo.
- Definir comportamento de conversão, fracionamento, devolução e estorno antes de codificar.
- Exibir ao usuário a origem do saldo e a unidade mínima de movimentação.
- Reutilizar auditoria, autorização, lote e unidade existentes.

## Critérios de aceite

- [ ] É possível registrar 10 caixas de 50 cartuchos como entrada agregada de 500 unidades.
- [ ] É possível registrar caixas identificadas individualmente, cada uma com saldo próprio.
- [ ] É possível registrar caixas com quantidades diferentes.
- [ ] A mesma munição pode ser movimentada por caixa ou unidade.
- [ ] Cautela, venda, cessão/doação, transferência, consumo, devolução e estorno preservam o saldo correto.
- [ ] Caixas não ficam com saldo negativo.
- [ ] O histórico identifica origem, lote, caixa quando aplicável, unidade, usuário e operação.
- [ ] A quantidade de Individual Asset é calculada pela quantidade de números de série.
- [ ] Testes automatizados cobrem os modos agregado, individual e unitário.
- [ ] O frontend apresenta seleção de modo, caixas, quantidades, saldos e erros de validação.
- [ ] API compila, testes Maven passam e o lint do frontend passa.

## Condição de parada

Parar após validar os dois modos de entrada e as movimentações por caixa e unidade. Não iniciar relatórios avançados, integração fiscal ou módulos ERP sem relação direta. Registrar no log o modelo de persistência escolhido, os fluxos implementados, testes executados e pendências.
