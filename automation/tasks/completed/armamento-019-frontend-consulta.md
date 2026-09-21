# Armamento 019 - frontend de consulta

## Estado após auditoria do commit 038
**PARCIAL.** Há consultas, paginação e buscas nos workspaces, inclusive por patrimônio/série em fluxos específicos.

## Objetivo
- Consolidar uma consulta principal do armamento por asset code, serial, modelo, status, unidade e localização.
- Confirmar filtros no servidor e paginação para volumes grandes.
- Preservar filtros relevantes em query parameters para recarga/compartilhamento.
- Exibir detalhe integrado com status, localização, custódia e histórico/auditoria conforme permissão.

## Escopo
Implementar exclusivamente o trabalho descrito no objetivo desta tarefa, respeitando a arquitetura, os padrões e as integrações existentes do ERP Comandos.
## Critérios de aceite
- [ ] Um armamento pode ser localizado diretamente pelos identificadores principais.
- [ ] Filtros/paginação usam contratos reais da API.
- [ ] Loading, erro, vazio e autorização estão tratados.
 
## Condição de parada
Encerrar esta tarefa somente após implementar o objetivo e satisfazer os critérios de aceite aplicáveis. Não iniciar outra tarefa; o worker controla a continuidade da fila.