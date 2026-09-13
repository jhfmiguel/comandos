# Armamento 019 - frontend de consulta

## Objetivo
Finalizar consulta, filtros, detalhe e histórico de armamentos.

## Contexto e prioridade
A consulta deve ser a forma principal de localizar um armamento sem navegar por telas diferentes. O resultado precisa refletir unidade, localização, status, cautela e identificadores atuais.

## Escopo
Listagens, pesquisa por asset code/serial, filtros de status/unidade, detalhe, auditoria e estados vazios.

## Regras técnicas
- Usar paginação, ordenação e filtros suportados pela API.
- Evitar filtrar somente no cliente quando o volume puder crescer.
- Preservar query parameters ao recarregar ou compartilhar a consulta.
- Restringir informações conforme autorização do usuário.

## Critérios de aceite
- [ ] Consulta encontra armamentos pelos identificadores principais.
- [ ] Filtros e paginação respeitam contratos reais.
- [ ] Loading, erro e vazio estão tratados.

## Condição de parada
Parar após validar o frontend de consulta. Não iniciar movimentação.
