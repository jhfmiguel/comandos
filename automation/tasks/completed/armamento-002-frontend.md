# Armamento - frontend

## Objetivo

Finalizar a interface do módulo de armamento e munição do ERP COMANDOS sobre os contratos disponíveis na API.

## Escopo

Inspecionar e completar `wr-app` para os fluxos de cadastro, consulta, estoque e movimentação diretamente relacionados a armamento e munição. Integrar serviços, modelos, páginas e componentes existentes. Não refatorar telas sem relação direta.

## Contexto do produto

Esta etapa é a segunda do workstream `armamento`. O backend inicial já foi trabalhado; o objetivo agora é tornar os fluxos utilizáveis pela interface, sem criar dados fictícios ou contratos paralelos. O frontend deve respeitar autenticação, autorização, auditoria, unidades organizacionais, individual assets, caixas de munição e saldos existentes no domínio.

## Fluxos prioritários

1. Cadastro e edição de armamentos.
2. Cadastro em lote de individual assets, mantendo cada `asset code` vinculado ao respectivo `serial number`.
3. Consulta de armamentos por modelo, tipo, asset code, serial number, status, unidade e localização.
4. Cadastro e consulta de munições por caixa ou unidade.
5. Entrada de munição agregada, como 10 caixas de 50 cartuchos, e caixas individualizadas com saldo próprio quando aplicável.
6. Consulta de saldo, lote, caixa, quantidade e origem da munição.
7. Acesso aos fluxos de cautela, inclusive recebedor pessoa ou unidade organizacional.
8. Acesso aos fluxos de transferência e movimentação somente quando os contratos da API estiverem disponíveis.
9. Campo `model` de Controlled Equipment filtrado pelo tipo selecionado: firearm, ammunition, grenade e demais tipos compatíveis.

## Áreas para inspecionar primeiro

- `wr-app/src/app/registrations/weapons/`
- `wr-app/src/app/queries/weapons/`
- `wr-app/src/app/erp/inventory/`
- `wr-app/src/app/erp/ammunition-consumption/`
- `wr-app/src/app/erp/custody/`
- `wr-app/src/components/weapons/`
- `wr-app/src/components/erp/`
- `wr-app/src/api/models/`
- `wr-app/src/api/services/`

Os caminhos podem variar conforme o estado atual do workspace. Localizar as implementações reais antes de editar. Usar os componentes e serviços existentes em vez de criar duplicatas.

## Regras de preservação

- Ler `git diff` antes de qualquer alteração.
- Preservar e completar alterações já feitas pelo usuário.
- Não apagar nem reverter código existente sem justificar no relatório.
- Não alterar backend, documentação ampla ou módulos sem relação direta nesta etapa.
- Não substituir chamadas reais da API por mocks.
- Não esconder incompatibilidades de contrato com casts inseguros.

## Estados obrigatórios da interface

Cada fluxo deve tratar carregamento, erro da API, lista vazia, sucesso, validação de formulário, duplicidade, falta de permissão e operação em andamento. Mensagens devem indicar a ação necessária e não perder dados digitados.

## Dependências e bloqueios

Se faltar endpoint, modelo ou regra de negócio para concluir um fluxo, não inventar o contrato. Registrar no log o arquivo/tela afetado, contrato ausente, impacto e proposta de próximo passo; concluir somente os fluxos que puderem ser validados com segurança.

## Prioridade do usuário

Antes de implementar qualquer alteração nova, analisar o estado atual do workspace, o `git diff`, arquivos modificados recentemente e os componentes já iniciados pelo usuário. Preservar e completar essas alterações primeiro. Não substituir, reverter ou reescrever trabalho existente sem necessidade. Se houver conflito entre esta tarefa e alterações já presentes, priorizar a intenção mais recente do usuário e registrar a decisão no log.

## Critérios de aceite

- [ ] Os fluxos de armamento e munição estão acessíveis pela interface.
- [ ] Estados de carregamento, erro, vazio e sucesso estão tratados.
- [ ] A interface usa os contratos reais da API.
- [ ] O lint do frontend passa.
- [ ] Cadastro e consulta usam filtros de tipo, modelo, asset code, serial number, status, unidade e localização quando suportados.
- [ ] Individual assets mantêm o pareamento entre asset code e serial number.
- [ ] Munição não mistura silenciosamente controle por caixa com saldo agregado.
- [ ] O campo model de Controlled Equipment não apresenta modelos de outro tipo de equipamento.
- [ ] Cautela permite selecionar pessoa ou unidade organizacional quando o contrato estiver disponível.
- [ ] Alterações do usuário foram preservadas e listadas no relatório final.
- [ ] O relatório final lista arquivos alterados, validações executadas, bloqueios e próximos passos.

## Validações obrigatórias

- [ ] Executar `npm.cmd run lint` em `wr-app`.
- [ ] Executar `npm.cmd run build` em `wr-app`, se o ambiente e os contratos permitirem.
- [ ] Executar as validações de navegador existentes relacionadas a armas, estoque e munição.
- [ ] Conferir que não há chamadas para endpoints inexistentes ou erros de console nos fluxos alterados.

## Condição de parada

Parar depois de concluir e validar o frontend relacionado a armamento e munição, ou quando um bloqueio de contrato impedir o próximo fluxo. Não iniciar documentação ampla, o Command Center do bot ou módulos ERP diferentes. Se a API não oferecer algum contrato necessário, registrar o bloqueio no log e parar sem marcar requisitos não implementados como concluídos.
