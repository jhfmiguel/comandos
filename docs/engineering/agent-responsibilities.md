# Responsabilidades dos agentes

## Coordenador / Arquiteto
Responsável por:
- decompor trabalho em tarefas independentes;
- definir fronteiras e contratos;
- decidir quando algo pertence ao Platform Core;
- evitar duplicação entre módulos;
- revisar impacto cruzado;
- definir ordem de integração.

Não deve usar o papel para fazer alterações concorrentes nos mesmos arquivos já pertencentes a outro agente.

## Backend
Responsável por:
- Java/Spring Boot;
- APIs e contratos;
- regras de domínio;
- persistência e migrations;
- autenticação/autorização no servidor;
- auditoria;
- integrações/eventos;
- testes backend.

## Frontend
Responsável por:
- Next.js/React/TypeScript;
- experiência do usuário;
- acessibilidade;
- integração com APIs;
- estados de loading/erro/permissão;
- testes e validações frontend.

## QA
Responsável por revisar, não por ampliar escopo.
Deve verificar:
- regressões;
- fluxos críticos;
- autorização;
- auditoria;
- concorrência/idempotência;
- estados inválidos;
- contratos backend/frontend;
- testes faltantes;
- segurança e exposição de dados.

## Release
Responsável por:
- CI/CD;
- build;
- SBOM;
- dependências e licenças;
- preparação de release;
- configurações de produção;
- smoke tests;
- rollback/backup.

## Regra de ownership
Cada tarefa concorrente deve declarar quais arquivos ou módulos possui.
Dois agentes não devem editar simultaneamente o mesmo conjunto de arquivos sem coordenação explícita.
