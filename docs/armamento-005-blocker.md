# Armamento 005 — bloqueio de domínio

Data: 2026-09-13. Situação: bloqueada antes de novas alterações de persistência.

## Decisão necessária

Definir quando a cautela institucional exige uma pessoa responsável interna e quais vínculos/papéis vigentes na unidade habilitam essa pessoa. O autorizador da entrega não pode ser tratado implicitamente como responsável interno. Também não se deve reutilizar o recebedor individual para essa finalidade.

A tarefa determina: “Se o modelo atual precisar de uma decisão de negócio para representar responsabilidade interna da unidade, registrar o bloqueio antes de alterar a persistência.”

## Evidências no código atual

- `wr-api/src/main/java/com/weaponsregistration/custody/model/Custody.java` possui `recipient`, `recipientUnit` e `authorizer`, sem referência separada ao responsável interno.
- `wr-api/src/main/java/com/weaponsregistration/custody/service/CustodyService.java` aceita exclusivamente pessoa ou unidade; o fluxo institucional não exige nem valida responsabilidade interna.
- `wr-api/src/main/java/com/weaponsregistration/core/model/PersonRoleAssignment.java` permite vínculos de pessoa, papel, organização e unidade, com vigência e situação. Não define qual papel responde pela cautela institucional.
- `wr-api/src/main/java/com/weaponsregistration/core/model/OrganizationalUnit.java` não possui situação ativa; a atividade da organização não demonstra atividade da unidade.
- O discriminador retornado atualmente é calculado a partir de `recipientUnit`; não é um discriminador persistido nem um campo explícito do pedido.

## Trabalho preservado e pendências

O diretório já continha alterações não commitadas para recebimento por unidade, incluindo contrato, entidade, serviço, formulário, ajuste de schema e testes. Essas alterações foram preservadas; não são apresentadas como implementação concluída nesta execução.

Após a decisão, concluir o discriminador explícito, a responsabilidade interna conforme a regra definida, a situação ativa da unidade, a seleção autorizada e as validações correspondentes. Validar criação, consulta, devolução parcial e encerramento, compatibilidade individual, autorização e conteúdo da auditoria, além dos estados da interface e lint. Não iniciar outra tarefa.

## Validação desta execução

- Inspeção do domínio, contratos, serviço, política de autorização, formulário e testes existentes.
- Tentativa de execução: `./mvnw.cmd "-Dtest=CustodyApiTests,CustodySchemaUpgradeTests" test`, em `wr-api`.
- Maven não iniciou os testes: não conseguiu criar o repositório local em `C:\.m2\repository`, fora da área gravável do ambiente.
- Nenhum teste aprovado é reivindicado. Lint não executado, pois não houve alteração de frontend.
- Único arquivo criado nesta execução: este registro. Nenhum commit ou push realizado.
