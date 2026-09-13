# Armamento 022 - testes do frontend

## Objetivo
Validar os fluxos de interface do módulo de armamento.

## Contexto e prioridade
Validar os fluxos que o usuário realmente executa, incluindo formulários, filtros, lotes, cautela por unidade e movimentações. Não considerar apenas a compilação como validação suficiente.

## Escopo
Executar e completar lint, build e validações de navegador existentes para cadastro, consulta, movimentação, custódia e inventário.

## Regras técnicas
- Verificar chamadas reais e contratos, sem aceitar mocks permanentes.
- Conferir responsividade e ausência de sobreposição.
- Registrar falhas com rota, ação, resposta e reprodução.
- Separar falhas preexistentes de regressões da etapa.

## Critérios de aceite
- [ ] Lint passa.
- [ ] Build passa.
- [ ] Fluxos principais não apresentam erro de console ou contrato.
- [ ] Estados de erro, vazio e sucesso são verificados.

## Condição de parada
Parar após validar o frontend. Não corrigir módulos sem relação com armamento.
