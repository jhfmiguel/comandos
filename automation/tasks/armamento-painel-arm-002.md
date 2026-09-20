# ARM-002 - Concluir interface de armamento

## Estado após auditoria do commit 038
**PARCIAL.** As telas principais existem, mas a homologação de todos os fluxos ainda não foi concluída.

## Objetivo
- Validar de ponta a ponta kits, doações, consumo, baixa/destruição e conclusão de manutenção com PostgreSQL.
- Corrigir erros de interface/contrato encontrados nessa homologação.
- Executar lint e build no estado final.
- Não duplicar telas ou fluxos já existentes.

## Escopo
Implementar exclusivamente o trabalho descrito no objetivo desta tarefa, respeitando a arquitetura, os padrões e as integrações existentes do ERP Comandos.
## Critérios de aceite
- [ ] Fluxos principais do Armamento homologados no navegador.
- [ ] Estados de erro, vazio, sucesso e repetição idempotente verificados.
- [ ] Lint e build passam no estado final.
 
## Condição de parada
Encerrar esta tarefa somente após implementar o objetivo e satisfazer os critérios de aceite aplicáveis. Não iniciar outra tarefa; o worker controla a continuidade da fila.