# COMANDOS

Plataforma modular especializada para gestão de segurança pública e privada. O COMANDOS consome contratos genéricos publicados pelo Faria Miguel e mantém neste projeto suas regras de armamento, munições, custódia, operações e demais domínios próprios.

## Estrutura

- `app/` — frontend Next.js/React/TypeScript.
- `api/` — backend Java 25 / Spring Boot.
- `infrastructure/` — infraestrutura do produto.
- `data/` — arquitetura analítica e pipelines.
- `docs/` — documentação funcional e técnica.

O backend evolui como monólito modular, com fronteiras verificáveis entre módulos e comunicação orientada a eventos. O código genérico compartilhado pertence ao repositório canônico Faria Miguel.

## Desenvolvimento local

Primeiro instale e teste os artefatos compartilhados no Maven local:

```powershell
cd C:\workspace\faria-miguel\faria-miguel
mvn.cmd -B clean install
```

Depois inicie a API COMANDOS com o perfil isolado H2 em memória:

```powershell
cd C:\workspace\faria-miguel\comandos\api
mvn.cmd -Plocal spring-boot:run "-Dspring-boot.run.profiles=local"
```

Esse perfil não conecta ao Oracle nem a um banco persistente. O perfil `oracle` é o padrão para ambientes configurados e não deve ser usado localmente sem configuração e autorização operacional próprias.

Frontend:

```powershell
cd C:\workspace\faria-miguel\comandos\app
npm.cmd run dev
```