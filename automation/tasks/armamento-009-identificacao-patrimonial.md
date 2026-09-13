# Armamento 009 - identificação patrimonial

## Objetivo
Implementar número de série, asset code e identificação patrimonial do armamento.

## Escopo
Garantir vínculo único entre asset code, número de série e individual asset, incluindo cadastro em lote quando aplicável.

## Critérios de aceite
- [ ] Asset codes e seriais são únicos e vinculados corretamente.
- [ ] A quantidade do individual asset deriva dos seriais associados.
- [ ] Duplicidades geram erro claro e são testadas.

## Condição de parada
Parar após validar identificação patrimonial. Não iniciar status operacional.
