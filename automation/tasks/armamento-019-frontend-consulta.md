# Armamento 019 - frontend de consulta

## Estado após auditoria do commit 038
**PARCIAL.** Há consultas, paginação e buscas nos workspaces, inclusive por patrimônio/série em fluxos específicos.

## Trabalho restante
- Consolidar uma consulta principal do armamento por asset code, serial, modelo, status, unidade e localização.
- Confirmar filtros no servidor e paginação para volumes grandes.
- Preservar filtros relevantes em query parameters para recarga/compartilhamento.
- Exibir detalhe integrado com status, localização, custódia e histórico/auditoria conforme permissão.

## Critérios de aceite
- [ ] Um armamento pode ser localizado diretamente pelos identificadores principais.
- [ ] Filtros/paginação usam contratos reais da API.
- [ ] Loading, erro, vazio e autorização estão tratados.
