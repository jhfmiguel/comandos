# Armamento 012 - custódia e cautela

## Objetivo
Implementar o fluxo de custódia e termo de responsabilidade de armamentos.

## Contexto e dependências
O recebedor pode ser pessoa ou unidade organizacional. O fluxo deve refletir a posse institucional sem apagar a pessoa responsável quando essa informação for exigida.

## Escopo
Permitir cautela para pessoa ou unidade organizacional, com emissão, recebimento, encerramento, autorização e auditoria.

## Regras técnicas
- Controlar estados: rascunho, emitida, recebida, devolvida, encerrada e cancelada, conforme o domínio.
- Impedir duas cautelas ativas incompatíveis para o mesmo item.
- Exigir confirmação e registrar evidência nas operações críticas.
- Manter impressão/visualização do termo se o projeto já suportar esse fluxo.

## Critérios de aceite
- [ ] Recebedor pessoa ou unidade pode ser selecionado.
- [ ] Cautela, devolução e encerramento preservam o histórico.
- [ ] Testes cobrem os dois tipos de recebedor.

## Condição de parada
Parar após validar custódia e cautela. Não iniciar transferências.


## Auditoria da fila
Classificada como concluída na auditoria do estado do módulo após o commit 038.
