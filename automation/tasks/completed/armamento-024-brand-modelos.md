# Armamento 024 - país de fabricação e código de fabricação

## Estado após auditoria do commit 038
**PENDENTE.** `Brand` ainda possui apenas nome e fabricante. `ItemModel` possui `manufacturerCode`, mas seu significado de negócio não está documentado.

## Objetivo
- Adicionar país de fabricação à marca usando referência padronizada.
- Confirmar e documentar o significado de `manufacturerCode` antes de alterar sua semântica.
- Implementar API, frontend, validação, auditoria e testes.
- Preservar compatibilidade com registros existentes.

## Escopo
Implementar exclusivamente o trabalho descrito no objetivo desta tarefa, respeitando a arquitetura, os padrões e as integrações existentes do ERP Comandos.
## Critérios de aceite
- [ ] Marca permite informar/consultar país de fabricação.
- [ ] Código de fabricação possui significado documentado.
- [ ] API/frontend/testes passam.
 
## Condição de parada
Encerrar esta tarefa somente após implementar o objetivo e satisfazer os critérios de aceite aplicáveis. Não iniciar outra tarefa; o worker controla a continuidade da fila.