# Armamento 017 - auditoria e rastreabilidade

## Estado após auditoria do commit 038
**PARCIAL.** Existe `AuditRecord` imutável, filtros, snapshots antes/depois, ator autenticado e registros em diversos serviços. A documentação ainda declara lacunas de cobertura e de proteção/retencão.

## Objetivo
- Conferir cobertura de eventos para custódia, transferência, manutenção, descarte, inventário, consumo, doação, recebimento/incorporação e workflow.
- Garantir filtros por item, usuário, operação, unidade e período conforme aplicável.
- Revisar escopo organizacional de leitura sem expor dados de outra unidade.
- Registrar explicitamente o que ficará para infraestrutura (retenção/tamper-evidence/exportação).

## Escopo
Implementar exclusivamente o trabalho descrito no objetivo desta tarefa, respeitando a arquitetura, os padrões e as integrações existentes do ERP Comandos.
## Critérios de aceite
- [ ] Todas as operações críticas do Armamento geram evento estruturado.
- [ ] Consultas respeitam autorização e filtros necessários.
- [ ] Testes comprovam imutabilidade e rastreabilidade ponta a ponta.
 
## Condição de parada
Encerrar esta tarefa somente após implementar o objetivo e satisfazer os critérios de aceite aplicáveis. Não iniciar outra tarefa; o worker controla a continuidade da fila.