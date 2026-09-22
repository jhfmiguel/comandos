# COMANDOS

Plataforma modular de gestão para Segurança Pública e Privada.

## Estrutura

- `app/` — frontend Next.js/React/TypeScript.
- `api/` — backend Java 25 / Spring Boot.
- `infrastructure/` — Docker, Kubernetes, IaC e observabilidade.
- `data/` — arquitetura analítica, lakehouse e pipelines.
- `docs/` — documentação funcional e técnica.

O backend evolui como monólito modular, com fronteiras verificáveis entre módulos e comunicação orientada a eventos. A extração futura de módulos para serviços distribuídos deve ocorrer apenas quando houver necessidade operacional real.

## Desenvolvimento local

Backend:

```powershell
cd api
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd app
npm.cmd run dev
```
