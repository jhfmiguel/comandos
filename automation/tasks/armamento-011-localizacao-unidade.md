# Armamento 011 - localização e unidade responsável

## Estado após auditoria do commit 038
**PARCIAL.** `StockLocation`, unidade organizacional, filtros por escopo e atualização de localização em transferências já existem.

## Objetivo
- Revisar todas as alterações de localização para garantir uma única localização vigente por ativo.
- Validar de forma uniforme unidade inexistente/inativa e escopo de acesso.
- Confirmar auditoria estruturada de origem, destino, operador e data em todos os fluxos que mudam localização.
- Adicionar testes para os casos faltantes.

## Escopo
Implementar exclusivamente o trabalho descrito no objetivo desta tarefa, respeitando a arquitetura, os padrões e as integrações existentes do ERP Comandos.
## Critérios de aceite
- [ ] Unidade responsável e localização são consistentes em todos os fluxos.
- [ ] Unidade inválida/inativa/sem permissão é rejeitada.
- [ ] Mudanças de localização são auditáveis e testadas.
 
## Condição de parada
Encerrar esta tarefa somente após implementar o objetivo e satisfazer os critérios de aceite aplicáveis. Não iniciar outra tarefa; o worker controla a continuidade da fila.