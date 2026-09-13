# Armamento 024 - país de fabricação e código de fabricação

## Objetivo
Completar os dados de marca e modelo dos itens.

## Contexto e prioridade
Marca e modelo são referências reutilizadas por firearm, ammunition, grenade e outros equipamentos. A alteração deve preparar filtros por tipo e não misturar modelos incompatíveis.

## Escopo
Adicionar país de fabricação em Brand. Investigar no domínio e na documentação o significado de `manufacture code` em Item Models; se não houver definição, registrar a decisão de negócio antes de persistir uma interpretação. Implementar campos, API, frontend, validações e testes.

## Regras técnicas
- País deve usar referência padronizada se o projeto já possuir catálogo correspondente.
- Manufacture code não pode receber significado inventado.
- Validar unicidade e compatibilidade de marca/modelo/tipo.
- Preservar registros e consultas existentes.

## Critérios de aceite
- [ ] Brand permite informar e consultar país de fabricação.
- [ ] Manufacture code possui significado documentado e validação coerente.
- [ ] API, frontend, auditoria e testes passam.

## Condição de parada
Parar se o significado de manufacture code não puder ser confirmado.
