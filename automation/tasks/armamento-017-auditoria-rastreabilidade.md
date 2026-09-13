# Armamento 017 - auditoria e rastreabilidade

## Objetivo
Completar a rastreabilidade das operações de armamentos.

## Contexto e dependências
Auditoria é transversal e deve consolidar os eventos produzidos por cadastro, identificação, status, unidade, cautela, transferência, manutenção, baixa e inventário.

## Escopo
Registrar quem, quando, onde, antes/depois e motivo para cadastro, status, custódia, transferência, manutenção, baixa e inventário.

## Regras técnicas
- Não registrar somente texto: preservar identificador da entidade e evento estruturado.
- Não permitir edição ou exclusão comum de evidências de auditoria.
- Restringir dados sensíveis por permissão e unidade organizacional.
- Permitir filtrar por item, usuário, operação, unidade e intervalo de datas.

## Critérios de aceite
- [ ] Operações relevantes aparecem no histórico de auditoria.
- [ ] Usuários sem permissão não acessam dados restritos.
- [ ] Consultas e testes de auditoria passam.

## Condição de parada
Parar após validar auditoria e rastreabilidade. Não iniciar telas frontend dedicadas.
