# Armamento 010 - status operacional

## Objetivo
Implementar o ciclo de status operacional dos armamentos.

## Contexto e dependências
Status impacta cautela, transferência, manutenção, inventário e descarte. A matriz de transições deve ser explícita e auditável.

## Escopo
Definir estados, transições permitidas, validações, autorização, auditoria e apresentação no frontend.

## Regras técnicas
- Documentar estado inicial, estados terminais e transições autorizadas.
- Bloquear uso/movimentação incompatível com o status atual.
- Exigir motivo e evidência quando a transição for sensível.
- Preservar histórico anterior e posterior.

## Critérios de aceite
- [ ] Estados e transições inválidas são bloqueados.
- [ ] O status atual aparece na consulta e no detalhe.
- [ ] Alterações são auditadas e testadas.

## Condição de parada
Parar após validar status e transições. Não iniciar localização.
