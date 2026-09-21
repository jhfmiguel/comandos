# Armamento 020 — movimentação no frontend

## Implementação

- Doação, consumo, baixa/destruição e conclusão de manutenção exigem confirmação explícita com referências selecionadas, motivo/documento, itens, origem e resultado esperado. Cancelar não envia requisição.
- As quatro ações têm trava síncrona contra envio repetido. Após resposta incerta, os dados ficam bloqueados e a nova tentativa usa a mesma chave e o mesmo corpo, inclusive quando uma tentativa intermediária recebe 403.
- Seletores de estoque deixam de permitir inclusão durante envio/recuperação. Estoque e histórico são remontados ao mudar organização/unidade em doação, consumo e baixa.
- Conclusão de manutenção impede nova conclusão após sucesso, inclusive enquanto o histórico recarrega, e exige custo numérico não negativo.
- Devolução de cautela preserva a requisição pendente após erro de recuperação e impede envio concorrente. Isso também se aplica à devolução integral de kits.

## Validação em 20/09/2026

Executar em `wr-app`, com o frontend ativo (padrão `http://localhost:3000`, configurável por `APP_URL`):

```text
node scripts/validate-movement-confirmation.mjs
node node_modules/typescript/bin/tsc --noEmit
node node_modules/eslint/bin/eslint.js src/components/erp/shared/confirm-movement.ts src/components/erp/donations/index.tsx src/components/erp/disposals/index.tsx src/components/erp/ammunition-consumption/index.tsx src/components/erp/maintenance/index.tsx src/components/erp/custody/index.tsx scripts/validate-movement-confirmation.mjs
```

O script usa Edge headless com Playwright e intercepta todas as chamadas de API. Nenhum estoque real é alterado. Casos:

1. Doação de ativo e quantidade fracionada de lote para pessoa, com termo e unidade de origem.
2. Consumo de munição com responsável, autorizador, finalidade e resultado.
3. Baixa de ativo e lote com processo e motivo.
4. Destruição com método, data e certificado.
5. Conclusão de manutenção com teste aprovado.
6. Conclusão de manutenção com teste reprovado.
7. Entrega e devolução integral de kit para pessoa.
8. Entrega e devolução integral de kit para unidade.

Os seis primeiros verificam confirmação, cancelamento sem POST, submissões simultâneas, perda de resposta, erro 403 na recuperação, bloqueio dos dados, identidade das três tentativas e histórico final. Os dois últimos verificam o destinatário, componentes devolvidos juntos e recuperação idempotente da devolução. Nenhum erro JavaScript foi observado. TypeScript e ESLint passaram.

Kits são suportados pelo contrato de cautela (`equipmentSetIds`); os contratos de doação, baixa, consumo e manutenção operam sobre ativos/saldos, sem operação agregada de kit. Esta tarefa não cria novos contratos de domínio.

## Limite da evidência

Esta é validação de comportamento do frontend no navegador com API controlada, não homologação integrada de persistência/transações. Não foram executadas movimentações contra a API de desenvolvimento da porta 8080, nem comprovadas contagens de movimentos no banco nesta execução.

A API isolada foi iniciada na porta 8180 usando o cache Maven `automation/runtime/m2-recalls/repository` e o banco documentado `wr_validation_20260912`. A atualização automática do esquema falhou ao adicionar colunas obrigatórias sobre registros existentes (incluindo `erp_custody.custody_scope` e `erp_work_order.maintenance_type`). A consulta `GET /api/erp/maintenance/orders?organizationId=1` retornou HTTP 500: `column wo1_0.maintenance_type does not exist`. A resposta está em `automation/runtime/armamento-020-maintenance-response.json`. A instância iniciada nesta execução foi encerrada.

Resultado da tarefa: incompleta. As correções de frontend e oito cenários controlados estão presentes, mas a homologação integrada permanece bloqueada pela incompatibilidade do esquema. É necessário disponibilizar um banco de validação compatível para comprovar persistência e ausência de movimentações duplicadas. Nenhuma migração de domínio foi acrescentada a esta tarefa de frontend.
