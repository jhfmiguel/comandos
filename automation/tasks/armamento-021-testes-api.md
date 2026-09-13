# Armamento 021 - testes da API

## Objetivo
Completar a cobertura automatizada da API do módulo de armamento.

## Contexto e prioridade
Esta tarefa é uma barreira antes da validação final. Os testes devem proteger regras de domínio, autorização, saldos e rastreabilidade, não apenas códigos HTTP.

## Escopo
Testar cadastro, classificação, identificação, status, unidade, custódia, transferência, manutenção, baixa, inventário, auditoria e regras de autorização.

## Regras técnicas
- Cobrir sucesso, validação, duplicidade, ausência, conflito e acesso negado.
- Isolar banco e dados entre testes.
- Testar transações e idempotência nas operações de movimentação.
- Não relaxar autorização ou asserções apenas para fazer a suíte passar.

## Critérios de aceite
- [ ] Casos de sucesso, validação e autorização estão cobertos.
- [ ] Testes isolam dados e são repetíveis.
- [ ] Maven test passa.

## Condição de parada
Parar após a suíte relevante da API passar. Não iniciar testes do frontend.
