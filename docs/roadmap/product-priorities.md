# Prioridades de produto

## Prioridade 1 — Concluir Armamento

O módulo Armamento é a primeira vertical de negócio que deve atingir maturidade funcional completa no COMANDOS.

Antes de abrir novas verticais de segurança, concluir os fluxos de Armamento já existentes e fechar lacunas de:
- cadastro e classificação de equipamentos;
- armamento individual e por lote;
- munições e consumo/deflagração;
- estoque, saldos, lotes e movimentações;
- cautela/custódia;
- reservas;
- transferências;
- manutenção e inspeção;
- venda e devolução;
- doação;
- baixa/descarte;
- inventário físico/reconciliação;
- validade, certificações e recalls;
- rastreabilidade por processo de origem;
- auditoria;
- autorização;
- concorrência e idempotência;
- relatórios/consultas;
- frontend e experiência operacional;
- testes automatizados;
- validação de release.

A conclusão deve ser baseada em critérios objetivos de qualidade, não apenas existência de telas ou endpoints.

## Prioridade 2 — Enterprise Core

Em paralelo ao fechamento do Armamento, o próximo grande investimento arquitetural é o Enterprise Core reutilizável por:
- COMANDOS;
- TRATOR;
- TUBARÃO.

O Enterprise Core deve concentrar capacidades empresariais comuns, sem conhecer regras verticais.

Primeira sequência candidata:
1. pessoas e fundamentos de RH;
2. clientes e fornecedores;
3. catálogo genérico de itens/produtos/serviços;
4. unidades de medida e classificações;
5. compras/cotações/pedidos/recebimentos;
6. vendas/devoluções;
7. estoque-base/armazéns/localizações;
8. contratos;
9. centros de custo;
10. financeiro-base;
11. contas a pagar/receber;
12. ativos patrimoniais-base;
13. manutenção-base;
14. relatórios empresariais-base.

## Estratégia

- Não iniciar novas verticais grandes do COMANDOS antes do Armamento alcançar o marco de conclusão.
- Enterprise Core pode avançar em paralelo apenas quando não competir com arquivos/domínio do Armamento.
- Tudo que for promovido ao Enterprise Core deve atender a pelo menos dois produtos de forma concreta.
- Não duplicar a mesma capacidade empresarial de forma independente nos três produtos.
