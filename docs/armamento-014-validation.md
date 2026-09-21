# Armamento 014 — manutenção e inspeção

Homologação executada em 21/09/2026 com Edge headless (Playwright), Next.js em `http://localhost:3000` e API em `http://localhost:8114`, usando PostgreSQL 18.6, banco isolado `wr_maintenance_014_20260920`. Login e permissões estavam habilitados. O navegador encaminhou as requisições da aplicação à API isolada pelo roteamento do Playwright.

## Correções

- O histórico de inspeções dependia do texto da mensagem de sucesso. A segunda inspeção consecutiva era persistida, mas não aparecia na tela. A atualização agora depende de um contador incrementado em cada registro/aprovação. O teste de regressão falhou antes da correção e passou depois.
- A validação de reenvio comparava os pedidos antes de a segunda resposta chegar. Agora aguarda a resposta da conclusão, verifica HTTP 200, duas submissões e igualdade dos respectivos conteúdos.
- A preparação do cenário de validade vencida agora usa um ativo `BLOCKED`, respeitando a regra da API que impede cadastrar um ativo vencido como `AVAILABLE`.

## Cenários aprovados

- Criação de plano, abertura de ordens, diagnóstico, serviço, peças e teste funcional aprovado/reprovado pelo navegador; custo total, periodicidade e operador conferidos na API.
- Ativos em manutenção recusados em cautela, transferência, nova ordem, inspeção conflitante e alteração cadastral para disponível.
- Cancelamento da confirmação sem submissão; perda simulada da resposta seguida de reenvio idempotente; rejeição de conclusão com conteúdo divergente.
- Retorno aprovado para `AVAILABLE`; reprovação e validade vencida para `BLOCKED`; histórico e auditoria de abertura/conclusão preservados.
- Inspeção periódica gerando ordem corretiva; aprovação administrativa sem liberar a manutenção; conclusão da ordem liberando o ativo.
- Duas inspeções aprovadas consecutivas aparecendo imediatamente no histórico, sem gerar novas ordens.
- Upload pelo navegador e download com comparação dos bytes para ambas as ordens e a inspeção; rejeição de anexo para ordem inexistente.

## Execução

O banco isolado já estava inicializado com a conta de `wr-app/scripts/maintenance-validation-bootstrap.sql`. Para um banco novo, aplicar esse bootstrap após a criação do schema pela API; não reaplicá-lo em banco já preparado.

Em `wr-api`:

```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-test-cache' spring-boot:run '-Dspring-boot.run.arguments=--server.port=8114 --spring.datasource.url=jdbc:postgresql://localhost:5432/wr_maintenance_014_20260920 --erp.allowed-origin=http://localhost:3000 --spring.jpa.show-sql=false --erp.security.require-login=true --erp.security.enforce-permissions=true'
.\mvnw.cmd -o '-Dmaven.repo.local=target/maven-test-cache' '-Dtest=MaintenanceApiTests,LifecycleWorkflowReportApiTests' test
```

Em `wr-app`, com o servidor Next.js disponível:

```powershell
node node_modules/next/dist/bin/next dev --port 3000
node scripts/validate-maintenance.mjs
node node_modules/eslint/bin/eslint.js src/components/erp/lifecycle/index.tsx
node node_modules/typescript/bin/tsc --noEmit
```

Resultado: fluxo completo de navegador/API aprovado, 15 testes Java sem falhas ou erros, ESLint e TypeScript sem erros. Evidências locais: `wr-app/maintenance-validation.log`, `wr-api/target/armamento-014-current-tests.log` e `wr-api/target/armamento-014-current-postgres.log`. Execução final: organização de teste 9. Nenhum bloqueador pendente.

Consulta direta ao PostgreSQL confirmou as quatro ordens concluídas (16–19), cada uma com exatamente uma saída e um retorno de manutenção, inclusive após o reenvio. Estados finais: `AVAILABLE`, `BLOCKED`, `AVAILABLE`, `BLOCKED`, respectivamente. Foram persistidas três inspeções.
