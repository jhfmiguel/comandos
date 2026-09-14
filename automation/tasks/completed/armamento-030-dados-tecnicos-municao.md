# Armamento 030 - dados técnicos da munição

## Objetivo
Adicionar peso em grains e velocidade ao cadastro de munição.

## Contexto e prioridade
São dados balísticos que podem pertencer ao modelo de munição ou ao lote, conforme o domínio existente. A granularidade deve ser confirmada antes da migração.

## Escopo
Atualizar modelo, banco, API, contratos, frontend, filtros e testes. Documentar a unidade e conversões somente se já houver padrão definido no projeto.

## Regras técnicas
- Preservar a unidade original e não converter silenciosamente.
- Validar valores positivos e limites coerentes.
- Exibir unidade em cadastro, consulta, lote e documentação.

## Critérios de aceite
- [ ] Munição aceita peso em grains.
- [ ] Munição aceita velocidade com unidade clara.
- [ ] Campos aparecem no cadastro, detalhe e consulta.
- [ ] Valores inválidos e unidades inconsistentes são rejeitados.

## Condição de parada
Parar se a unidade de velocidade ou regra de conversão não puder ser confirmada.
