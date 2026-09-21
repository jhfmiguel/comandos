# Armamento 022 — validação do frontend

Rodada de 20/09/2026, HEAD `6d8dd3a57951e69a5927ffd1161b3cb870fb8353`.
Resultado: **reprovado / homologação bloqueada**. Não há aceite dos fluxos
principais. As alterações preexistentes da tarefa 023 não fazem parte desta rodada.

## Ambiente e comandos

Windows, Node 24.20.0, Next.js 16.3.1, Playwright com Edge headless e PostgreSQL
18.6. Banco confirmado por `SELECT current_database(), version()`:
`wr_validation_20260912`. O banco habitual `weapons` não foi usado.

Em `wr-app`:

```powershell
yarn.cmd lint
$env:NODE_ENV='production'
yarn.cmd build
yarn.cmd start --port 3100
```

Em `wr-api`, a API de preparação foi iniciada com o cache Maven offline existente:

```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=target/m2-validation' spring-boot:run '-Dspring-boot.run.arguments=--server.port=8180 --spring.datasource.url=jdbc:postgresql://localhost:5432/wr_validation_20260912 --erp.allowed-origin=http://localhost:3100 --spring.jpa.show-sql=false'
```

Os scripts encaminham chamadas de `localhost:8080` para a API isolada 8180.
Foram executados individualmente, em `wr-app`, com `node scripts/<nome>.mjs`:

| Validação | Resultado desta rodada |
| --- | --- |
| `yarn lint` (repetido após adicionar o script) | Exit 0; zero erros, 10 avisos em diretórios de backup |
| `yarn build`, com `NODE_ENV=production` | Exit 0; compilação, TypeScript e geração de páginas concluídos |
| `validate-browser` | Exit 1; preparação de local retorna 500, antes do fluxo de cautela |
| `validate-asset-batch` | Exit 1; preparação de local retorna 500, antes do cadastro em lote |
| `validate-stock-intake` | Exit 1; preparação de local retorna 500, antes do recebimento |
| `validate-stock-workflows` | Exit 1; preparação de local retorna 500, antes de reserva/inventário/transferência |
| `validate-sales-access` | Exit 1; preparação de local retorna 500, antes do login e das vendas |
| `validate-armamento-layout` | Exit 1; 12 combinações aprovadas e 9 reprovadas, conforme detalhamento abaixo |

A API protegida 8181 não foi iniciada: o script de acesso falhou ainda na
preparação pela 8180. Nenhuma conclusão sobre permissões é extraída dessa tentativa.
`validate-command-center.mjs` testa o worker de automação e não integra o escopo
de armamento desta tarefa.

## Cobertura adicional implementada

`wr-app/scripts/validate-armamento-layout.mjs` verifica as telas de cadastro,
recebimento, cautela, transferência, manutenção, baixa e inventário nas larguras
390, 768 e 1440 pixels. Usa respostas reais, exige conteúdo principal e chamadas
à API, mede transbordamento horizontal do documento e coleta exceções JavaScript,
console, respostas HTTP de erro e falhas de rede. Continua após falhas para
registrar todas as combinações; retorna exit 1 se qualquer combinação falhar.

Execute `node scripts/validate-armamento-layout.mjs` com os serviços acima.
JSON e capturas ficam em `wr-api/target/browser-validation/armamento-022/`.
Este teste de abertura não comprova operações de gravação, todos os estados
vazios, sucesso, recuperação de erro nem usabilidade completa dos controles.

Resultados iguais nas três larguras:

| Tela | Abertura/contrato/console |
| --- | --- |
| Cadastro (`inventory`), cautela, manutenção, baixa | Abertura aprovada; não comprova gravação |
| Transferências, inventário físico | Falha HTTP 500 na consulta de locais e erro de console |
| Recebimentos (`receivings`) | Falha HTTP 404 em `http://localhost:3100/api/erp/receivings`, exceção JavaScript e ausência de `main` |

Não houve transbordamento horizontal nas 18 combinações em que o conteúdo
principal pôde ser medido. Recebimentos falhou antes dessa medição; sua
responsividade não foi aprovada. A chamada relativa de recebimentos usa o
servidor Next.js e não passa pelo encaminhamento de `localhost:8080` utilizado
pelos scripts existentes. Esse contrato exige investigação adicional.

## Bloqueio reproduzido

POST e GET de `/api/erp/inventory/locations` retornam HTTP 500:
`column "active" of relation "erp_stock_location" does not exist`.
A consulta a `information_schema.columns` confirmou a ausência da coluna.
O Hibernate tentou `add column active boolean not null`, mas o PostgreSQL
recusou a alteração porque existem registros anteriores sem valor para ela.

O log de inicialização também registra falhas equivalentes nos campos
`erp_custody.custody_scope`, `duration_type`, `recipient_type`,
`erp_inventory_transfer.transfer_type`, `erp_stock_lot.condition`, `status` e
`erp_work_order.maintenance_type`. Iniciar a API não significa que o esquema
foi atualizado com sucesso. Nenhum dado anterior foi apagado ou preenchido
com valores presumidos para fazer os testes passarem. Os scripts deixaram
dados fictícios parciais de preparação no banco isolado.

A correção e o ensaio da migração de dados existentes são pré-requisitos para
homologar os fluxos neste ambiente. Após isso, repetir os cinco scripts,
iniciar também a API protegida conforme `docs/validation.md` e completar os
cenários de gravação, estados vazio/erro/sucesso e permissões. Não considerar
os resultados históricos como aprovação do HEAD atual.

## Evidências locais

- `wr-app/lint-armamento-022.log`, `build-armamento-022.log` e `start-armamento-022.log`.
- `wr-app/validate-*-armamento-022.log`, um arquivo para cada script existente.
- `wr-app/layout-armamento-022.log` e o JSON/capturas do novo script.
- `wr-api/target/armamento-022-api.log`, incluindo falhas DDL e SQL.

Logs e capturas são artefatos locais ignorados pelo Git; este relatório preserva
os comandos e resultados. Lint/build e documentação foram entregues, mas o
critério de fluxos principais sem erros de console/contrato permanece pendente.
Nenhum commit, push ou início de outra tarefa foi realizado.
Os processos de API e frontend iniciados para esta rodada foram encerrados.
