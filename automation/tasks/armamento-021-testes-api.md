# Armamento 021 - testes da API

## Estado após auditoria do commit 038
**IMPLEMENTADA EM BOA PARTE, VALIDAÇÃO FINAL PENDENTE.** Há suítes específicas para inventário, autorização, cautela, transferência, manutenção, descarte, inventário físico, auditoria, consumo, vendas, doações, reservas e CEP. A última validação documentada antecede parte das mudanças dos commits 037/038.

## Objetivo
- Executar `mvnw.cmd test` no estado atual.
- Corrigir qualquer regressão.
- Completar casos faltantes de status, lifecycle/workflow/report e novas especificações.
- Garantir sucesso, validação, conflito, duplicidade, idempotência e autorização.

## Escopo
Implementar exclusivamente o trabalho descrito no objetivo desta tarefa, respeitando a arquitetura, os padrões e as integrações existentes do ERP Comandos.
## Critérios de aceite
- [ ] Suíte completa da API passa no HEAD atual.
- [ ] Regras críticas possuem cobertura de regressão.
- [ ] Testes são repetíveis e isolados.
 
## Condição de parada
Encerrar esta tarefa somente após implementar o objetivo e satisfazer os critérios de aceite aplicáveis. Não iniciar outra tarefa; o worker controla a continuidade da fila.