# SBOM e auditoria de dependências

O COMANDOS gera Software Bill of Materials em formato CycloneDX para o frontend
e para a API. Esses arquivos permitem rastrear componentes, versões e licenças
que entram em uma distribuição comercial.

## CI

O job `software-composition`:

1. instala o frontend exclusivamente com Yarn;
2. gera `frontend-bom.json` com o gerador oficial CycloneDX para npm;
3. gera `backend-bom.json` com o plugin oficial CycloneDX Maven;
4. executa a política de licenças sobre os dois SBOMs;
5. publica ambos como artefatos do workflow.

A política bloqueia automaticamente licenças explicitamente incompatíveis com a
diretriz comercial atual, como AGPL, SSPL, Business Source License, Commons
Clause, PolyForm, Elastic License e PrimeUI.

Licenças que dependem do modo de distribuição ou exigem obrigações adicionais
são listadas para revisão, sem bloqueio automático, incluindo GPL/LGPL, MPL,
EPL, CDDL, termos Oracle/FUTC e componentes sem licença identificada.

## Execução local

Frontend:

```powershell
cd app
yarn install --frozen-lockfile
npx --yes @cyclonedx/cyclonedx-npm@6.0.1 --output-format JSON --output-file ../artifacts/frontend-bom.json
```

Backend:

```powershell
cd api
.\mvnw.cmd org.cyclonedx:cyclonedx-maven-plugin:2.9.2:makeBom "-Dcyclonedx.outputFormat=json"
Copy-Item .\target\bom.json ..\artifacts\backend-bom.json
```

Validação:

```powershell
node scripts/check-sbom-licenses.mjs artifacts/frontend-bom.json artifacts/backend-bom.json
```

O SBOM deve ser regenerado para cada release comercial. Ele complementa, mas
não substitui, a revisão jurídica dos termos aplicáveis.
