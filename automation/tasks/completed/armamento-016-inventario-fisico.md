# Armamento 016 - inventário físico

## Objetivo
Implementar conferência física de armamentos.

## Contexto e dependências
O inventário compara o estado físico com unidade, localização, custódia, serial number e status registrados. A reconciliação não pode eliminar evidências da contagem.

## Escopo
Planejamento, contagem por unidade/local, divergências, reconciliação, justificativa, aprovação e auditoria.

## Regras técnicas
- Congelar ou versionar o recorte contado para evitar mudança silenciosa durante a conferência.
- Identificar itens encontrados, ausentes, excedentes e não reconhecidos.
- Exigir justificativa e aprovação para ajustes.
- Permitir recontagem sem sobrescrever a primeira evidência.

## Critérios de aceite
- [ ] Inventário pode ser aberto, contado e encerrado.
- [ ] Divergências são calculadas e tratadas com justificativa.
- [ ] Reconciliação não remove histórico.

## Condição de parada
Parar após validar inventário físico. Não iniciar auditoria avançada.


## Auditoria da fila
Classificada como concluída na auditoria do estado do módulo após o commit 038.
