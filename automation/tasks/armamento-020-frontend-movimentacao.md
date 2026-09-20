# Armamento 020 - frontend de movimentação

## Estado após auditoria do commit 038
**PARCIAL.** Existem interfaces dedicadas para cautela, transferência, manutenção, descarte, inventário, vendas, doações e consumo, com proteção contra duplo envio em vários fluxos.

## Objetivo
- Homologar no navegador os fluxos ainda não validados: doações, consumo, baixa/destruição e conclusão de manutenção; incluir kits quando aplicável.
- Garantir confirmação explícita nas ações irreversíveis.
- Verificar origem, destino, recebedor, motivo e resultado antes da confirmação.
- Corrigir inconsistências de estado/erro/repetição encontradas.

## Critérios de aceite
- [ ] Todos os fluxos de movimentação relevantes podem ser iniciados e acompanhados.
- [ ] Pessoa e unidade são suportadas onde o domínio exige.
- [ ] Nenhum erro deixa operação ambígua ou duplica movimentação.
