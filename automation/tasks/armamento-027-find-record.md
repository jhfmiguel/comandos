# Armamento 027 - corrigir Find a record

## Objetivo
Corrigir o fluxo Find a record, que atualmente não funciona.

## Contexto e prioridade
É uma correção de usabilidade e diagnóstico. Antes de editar, reproduzir o erro com a mesma rota, entrada e perfil que o usuário utiliza.

## Escopo
Reproduzir o erro no frontend e backend, localizar a causa no contrato, busca, filtros, rota ou estado da tela e corrigir com teste de regressão.

## Regras técnicas
- Registrar requisição, parâmetros, resposta, console e comportamento esperado.
- Corrigir a causa raiz, não mascarar com filtro local ou dado mockado.
- Cobrir registro encontrado, não encontrado, entrada vazia e erro de API.

## Critérios de aceite
- [ ] Find a record encontra registros válidos pelos identificadores suportados.
- [ ] Resultado vazio e erro são exibidos corretamente.
- [ ] O caso que atualmente falha possui teste de regressão.
- [ ] API e frontend passam nas validações.

## Condição de parada
Parar após reproduzir, corrigir e validar o problema. Não fazer refatoração ampla.
