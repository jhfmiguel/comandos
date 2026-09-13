# Armamento 015 - baixa, descarte e inativação

## Objetivo
Implementar baixa, descarte e inativação segura de armamentos.

## Contexto e dependências
São operações potencialmente irreversíveis e precisam de confirmação, autorização e evidência. Devem respeitar cautela, estoque, manutenção e inventário ativos.

## Escopo
Motivos, autorização, evidências, confirmação, efeitos no estoque/status e trilha de auditoria.

## Regras técnicas
- Separar baixa lógica, inativação e descarte físico se forem conceitos diferentes.
- Bloquear item em cautela ou transferência pendente sem encerramento formal.
- Impedir reativação silenciosa e manter histórico imutável.
- Validar permissões elevadas e motivo obrigatório.

## Critérios de aceite
- [ ] Operações irreversíveis exigem autorização e confirmação.
- [ ] Item baixado não pode voltar a uso sem fluxo formal.
- [ ] Histórico e testes cobrem os motivos de baixa.

## Condição de parada
Parar após validar baixa, descarte e inativação. Não iniciar inventário físico.
