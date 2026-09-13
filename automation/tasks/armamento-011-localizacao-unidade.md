# Armamento 011 - localização e unidade responsável

## Objetivo
Associar armamentos à localização e à unidade organizacional responsável.

## Contexto e dependências
Localização e unidade são dimensões usadas por consulta, cautela, transferência e inventário físico. Não confundir unidade responsável com recebedor temporário.

## Escopo
Implementar seleção, persistência, consulta e validação de unidade e localização, respeitando autorização.

## Regras técnicas
- Validar hierarquia, atividade e escopo de acesso da unidade.
- Evitar duas localizações vigentes para o mesmo item sem regra de vigência.
- Auditar origem, destino, operador e data de cada alteração.

## Critérios de aceite
- [ ] Unidade responsável e localização podem ser informadas e consultadas.
- [ ] Unidade inexistente, inativa ou sem permissão é rejeitada.
- [ ] Alterações geram auditoria e testes.

## Condição de parada
Parar após validar localização e unidade responsável. Não iniciar cautela.
