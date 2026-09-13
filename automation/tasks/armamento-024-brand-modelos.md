# Armamento 024 - país de fabricação e código de fabricação

## Objetivo
Completar os dados de marca e modelo dos itens.

## Escopo
Adicionar país de fabricação em Brand. Investigar no domínio e na documentação o significado de `manufacture code` em Item Models; se não houver definição, registrar a decisão de negócio antes de persistir uma interpretação. Implementar campos, API, frontend, validações e testes.

## Critérios de aceite
- [ ] Brand permite informar e consultar país de fabricação.
- [ ] Manufacture code possui significado documentado e validação coerente.
- [ ] API, frontend, auditoria e testes passam.

## Condição de parada
Parar se o significado de manufacture code não puder ser confirmado.
