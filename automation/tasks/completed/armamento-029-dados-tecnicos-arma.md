# Armamento 029 - dados técnicos da arma

## Objetivo
Adicionar dados técnicos de peso e dimensões ao cadastro de armas.

## Contexto e prioridade
Peso carregada, peso descarregada e dimensões são propriedades técnicas do modelo ou do item, conforme o domínio atual. A tarefa deve confirmar essa granularidade antes de persistir.

## Escopo
Incluir peso carregada, peso descarregada e dimensões conforme unidade e padrão de medida definidos no domínio. Atualizar API, persistência, frontend, validações, consulta e testes.

## Regras técnicas
- Não misturar massa e dimensão sem unidades explícitas.
- Validar valores positivos, limites razoáveis e coerência entre pesos.
- Exibir unidades na entrada, consulta, exportação e detalhe.

## Critérios de aceite
- [ ] Peso carregada e descarregada são armazenados com unidade clara.
- [ ] Dimensões são armazenadas com unidade e validação.
- [ ] Campos aparecem no cadastro, detalhe e consulta.
- [ ] Dados inválidos são rejeitados e testes passam.

## Condição de parada
Parar se a unidade padrão de peso ou dimensão não estiver definida.
