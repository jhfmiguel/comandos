# Armamento 011 - localização e unidade responsável

## Objetivo
Associar armamentos à localização e à unidade organizacional responsável.

## Escopo
Implementar seleção, persistência, consulta e validação de unidade e localização, respeitando autorização.

## Critérios de aceite
- [ ] Unidade responsável e localização podem ser informadas e consultadas.
- [ ] Unidade inexistente, inativa ou sem permissão é rejeitada.
- [ ] Alterações geram auditoria e testes.

## Condição de parada
Parar após validar localização e unidade responsável. Não iniciar cautela.
