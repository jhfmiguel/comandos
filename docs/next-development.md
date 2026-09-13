# Próxima etapa solicitada

## Cadastro em lote de equipamentos individuais

Solicitação do usuário em 12/09/2026, para a próxima etapa de desenvolvimento:
em **Individual assets**, cadastrar de uma só vez os códigos patrimoniais
(`assetCode`) e números de série (`serialNumber`) de várias unidades do mesmo
modelo, por exemplo todas as Beretta APX.

Comportamento solicitado:

- Selecionar o modelo comum às unidades.
- Exibir uma lista/tabela editável com uma linha por unidade e as colunas
  **Asset code** e **Serial number**.
- Manter o par código patrimonial/número de série vinculado por linha.
- Calcular automaticamente a quantidade pelo número de séries cadastradas:
  20 pares código patrimonial/número de série representam 20 unidades do modelo.
  A quantidade deve ser derivada dos registros individuais, não preenchida
  separadamente para o grupo.
- Cadastrar várias unidades em uma operação, preservando um registro individual
  para cada equipamento e sua rastreabilidade nos demais fluxos.

Na implementação, verificar também a reutilização dos campos comuns, a indicação
de erros por linha, as duplicidades e as permissões existentes. Esses detalhes
complementam o pedido e devem seguir as regras atuais do cadastro.

Estado em 13/09/2026: backend implementado em `POST /api/erp/inventory/assets/batch`,
com quantidade derivada das linhas, repetição idempotente e reversão integral em
caso de erro. Editor validado no navegador com PostgreSQL: importação de pares,
quantidade automática, duplicidades e recuperação após perda de resposta.

## Cautela para unidade organizacional

Solicitação adicional do usuário em 12/09/2026: uma unidade organizacional
também pode receber armamento em cautela. O destinatário deve poder ser uma
pessoa ou uma unidade organizacional. Preservar a identificação individual
dos armamentos e os fluxos de devolução, autorização e rastreabilidade.
Estado em 13/09/2026: backend e campos de frontend implementados para destinatário
pessoa ou unidade. Há testes de emissão, devolução, exclusividade do destinatário,
escopo organizacional e atualização do esquema legado. Emissão para unidade e
devolução também passaram no navegador com PostgreSQL.

## Limpeza de dados

O usuário autorizou apagar cautelas e ativos individuais nos bancos principal
e de testes. A limpeza geral dos dados fica para a conclusão do projeto.

## Validação do backend em 13/09/2026

- `wr-api/mvnw.cmd test`: 101 testes, sem falhas, erros ou testes ignorados.
- Corrigida a detecção de séries duplicadas em lote quando `modelId` chega como
  texto numérico com zeros à esquerda. A resposta informa a linha com erro e a
  transação desfaz os ativos e movimentos anteriores do mesmo envio.
- Teste de regressão reproduziu o erro antes da correção.
- Telas de cadastro em lote, cautela, reserva, inventário e transferência
  validadas com a API e PostgreSQL em 13/09/2026; detalhes em `validation.md`.
- Próximas verificações: fluxos de kits, doações, consumo, baixa/destruição e
  conclusão de manutenção no navegador, conforme a lista de homologação.
