# Armamento 021 - testes da API

## Estado após auditoria do commit 038
**IMPLEMENTADA EM BOA PARTE, VALIDAÇÃO FINAL PENDENTE.** Há suítes específicas para inventário, autorização, cautela, transferência, manutenção, descarte, inventário físico, auditoria, consumo, vendas, doações, reservas e CEP. A última validação documentada antecede parte das mudanças dos commits 037/038.

## Trabalho restante
- Executar `mvnw.cmd test` no estado atual.
- Corrigir qualquer regressão.
- Completar casos faltantes de status, lifecycle/workflow/report e novas especificações.
- Garantir sucesso, validação, conflito, duplicidade, idempotência e autorização.

## Critérios de aceite
- [ ] Suíte completa da API passa no HEAD atual.
- [ ] Regras críticas possuem cobertura de regressão.
- [ ] Testes são repetíveis e isolados.
