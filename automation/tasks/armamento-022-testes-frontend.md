# Armamento 022 - testes do frontend

## Estado após auditoria do commit 038
**PARCIAL.** Existem scripts de validação em navegador e rodadas anteriores de lint/build, mas não há validação final documentada após os commits 037/038.

## Objetivo
- Executar `yarn lint` e `yarn build` no HEAD atual.
- Executar os scripts de navegador existentes contra PostgreSQL.
- Completar a homologação dos fluxos faltantes.
- Verificar responsividade, console, estados de erro/vazio/sucesso e contratos reais.

## Escopo
Implementar exclusivamente o trabalho descrito no objetivo desta tarefa, respeitando a arquitetura, os padrões e as integrações existentes do ERP Comandos.
## Critérios de aceite
- [ ] Lint e build passam.
- [ ] Fluxos principais passam sem erro de console/contrato.
- [ ] Resultado da validação fica documentado.
 
## Condição de parada
Encerrar esta tarefa somente após implementar o objetivo e satisfazer os critérios de aceite aplicáveis. Não iniciar outra tarefa; o worker controla a continuidade da fila.