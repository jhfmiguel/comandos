# Armamento - frontend

## Objetivo

Finalizar a interface do módulo de armamento e munição do ERP COMANDOS sobre os contratos disponíveis na API.

## Escopo

Inspecionar e completar `wr-app` para os fluxos de cadastro, consulta, estoque e movimentação diretamente relacionados a armamento e munição. Integrar serviços, modelos, páginas e componentes existentes. Não refatorar telas sem relação direta.

## Prioridade do usuário

Antes de implementar qualquer alteração nova, analisar o estado atual do workspace, o `git diff`, arquivos modificados recentemente e os componentes já iniciados pelo usuário. Preservar e completar essas alterações primeiro. Não substituir, reverter ou reescrever trabalho existente sem necessidade. Se houver conflito entre esta tarefa e alterações já presentes, priorizar a intenção mais recente do usuário e registrar a decisão no log.

## Critérios de aceite

- [ ] Os fluxos de armamento e munição estão acessíveis pela interface.
- [ ] Estados de carregamento, erro, vazio e sucesso estão tratados.
- [ ] A interface usa os contratos reais da API.
- [ ] O lint do frontend passa.

## Condição de parada

Parar depois de concluir e validar o frontend relacionado a armamento e munição. Não iniciar documentação ampla ou módulos ERP diferentes. Se a API não oferecer algum contrato necessário, registrar o bloqueio no log e parar.
