# Armamento 009 - identificação patrimonial

## Objetivo
Implementar número de série, asset code e identificação patrimonial do armamento.

## Contexto e dependências
Esta etapa é a base da rastreabilidade individual e do cadastro em lote. O vínculo entre asset code e serial number deve ser inequívoco em todos os endpoints.

## Escopo
Garantir vínculo único entre asset code, número de série e individual asset, incluindo cadastro em lote quando aplicável.

## Regras técnicas
- Validar unicidade com normalização de caixa, espaços e formato definido.
- Informar conflitos por campo e por registro relacionado.
- Não permitir alteração que quebre histórico de movimentações.
- Derivar quantidade de individual asset a partir de seriais associados.

## Critérios de aceite
- [ ] Asset codes e seriais são únicos e vinculados corretamente.
- [ ] A quantidade do individual asset deriva dos seriais associados.
- [ ] Duplicidades geram erro claro e são testadas.

## Condição de parada
Parar após validar identificação patrimonial. Não iniciar status operacional.


## Auditoria da fila
Classificada como concluída na auditoria do estado do módulo após o commit 038.
