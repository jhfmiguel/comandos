# Armamento - cadastro em lote de individual assets

## Objetivo

Tornar o cadastro de Individual Assets eficiente para grandes quantidades de armamentos, permitindo cadastrar uma lista de armas de uma mesma família, como Beretta APX, sem repetir manualmente o formulário para cada unidade.

## Contexto e prioridade

Esta é a primeira melhoria de produtividade depois da validação do frontend. O caso principal é cadastrar várias Berettas APX em uma única operação, sem perder o pareamento entre cada asset code e seu serial number.

## Prioridade do usuário

Esta é a próxima melhoria prioritária após as etapas atuais do workstream de armamento. Preservar as alterações já existentes no workspace e começar pelo fluxo atual de Individual Assets.

## Escopo

Implementar no backend e no frontend um cadastro em lote de Individual Assets com os seguintes dados:

- Família, modelo ou tipo do armamento, por exemplo Beretta APX.
- Lista de pares `asset code` e `serial number`.
- Vínculo permanente entre cada asset code e seu respectivo serial number.
- Inclusão, edição e remoção de linhas antes da confirmação.
- Validação de campos obrigatórios.
- Bloqueio de asset codes duplicados.
- Bloqueio de serial numbers duplicados.
- Relatório das linhas aceitas e rejeitadas quando o lote for processado.
- Operação transacional: não criar registros parcialmente quando o lote inteiro não puder ser validado, salvo se a API já seguir um padrão explícito de processamento parcial.

Preferir uma experiência de tabela editável ou importação estruturada compatível com os padrões atuais do projeto. Não substituir o cadastro unitário se ele continuar necessário para correções pontuais.

## Regras técnicas adicionais

- A linha é a unidade mínima do lote e deve manter `assetCode` e `serialNumber` juntos.
- Validar duplicidades dentro do lote e contra registros já persistidos.
- Exibir erros por linha sem apagar as linhas válidas antes da revisão.
- Definir claramente se a confirmação é atômica; preferir transação completa.
- Auditar usuário, data, modelo/família, quantidade de linhas e resultado.
- Não aceitar quantidade livre que contradiga a quantidade de seriais cadastrados.

## Critérios de aceite

- [ ] O usuário consegue informar o modelo/família e várias linhas de asset code e serial number.
- [ ] Cada asset code permanece associado ao serial number informado na mesma linha.
- [ ] O usuário consegue revisar o lote antes de confirmar.
- [ ] Duplicidades e campos inválidos são identificados antes da persistência.
- [ ] O backend persiste os pares corretamente e mantém as regras de autorização e auditoria.
- [ ] O frontend mostra estados de carregamento, sucesso, erro e rejeições por linha.
- [ ] É possível cadastrar rapidamente um lote de Beretta APX sem abrir um formulário por unidade.
- [ ] Testes automatizados do backend e validações do frontend passam.

## Condição de parada

Parar após implementar e validar o cadastro em lote diretamente relacionado a Individual Assets. Não iniciar importação de outros tipos de estoque, transporte ou módulos ERP diferentes. Registrar no log o formato escolhido para o lote, arquivos alterados, testes executados e qualquer decisão de negócio pendente.
