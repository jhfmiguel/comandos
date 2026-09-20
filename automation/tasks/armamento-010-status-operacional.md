# Armamento 010 - status operacional

## Estado após auditoria do commit 038
**PARCIAL.** Existe `EquipmentStatePolicy` e os fluxos alteram status, mas a matriz não está consolidada de forma coerente em todo o módulo. Foi encontrada divergência nominal: a política usa `IN_CUSTODY`/`MAINTENANCE`, enquanto fluxos existentes usam `CUSTODIED`/`IN_MAINTENANCE`.

## Objetivo
- Unificar os códigos canônicos de status usados por política, custódia, manutenção, transferência, venda, descarte, inspeção e ocorrências.
- Centralizar e documentar transições permitidas.
- Garantir bloqueio uniforme das operações incompatíveis.
- Cobrir transições válidas e inválidas com testes e auditoria.

## Escopo
Implementar exclusivamente o trabalho descrito no objetivo desta tarefa, respeitando a arquitetura, os padrões e as integrações existentes do ERP Comandos.
## Critérios de aceite
- [ ] Não há nomes de status conflitantes entre serviços.
- [ ] Matriz de transições é explícita e testada.
- [ ] Status atual é exibido corretamente na consulta/detalhe.
 
## Condição de parada
Encerrar esta tarefa somente após implementar o objetivo e satisfazer os critérios de aceite aplicáveis. Não iniciar outra tarefa; o worker controla a continuidade da fila.