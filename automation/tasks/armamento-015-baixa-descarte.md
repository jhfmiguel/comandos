# Armamento 015 - baixa, descarte e inativação

## Objetivo
Implementar baixa, descarte e inativação segura de armamentos.

## Escopo
Motivos, autorização, evidências, confirmação, efeitos no estoque/status e trilha de auditoria.

## Critérios de aceite
- [ ] Operações irreversíveis exigem autorização e confirmação.
- [ ] Item baixado não pode voltar a uso sem fluxo formal.
- [ ] Histórico e testes cobrem os motivos de baixa.

## Condição de parada
Parar após validar baixa, descarte e inativação. Não iniciar inventário físico.
