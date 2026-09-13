# Armamento - recebimento de cautela por unidade organizacional

## Objetivo

Permitir que uma cautela de armamento seja recebida por uma unidade organizacional, além do recebimento individual já existente.

## Prioridade do usuário

Esta é uma correção prioritária: atualmente não é possível selecionar uma unidade organizacional como recebedora da cautela. Implementar esta etapa depois de `armamento-004-cadastro-em-lote-assets.md`.

## Escopo

Inspecionar o fluxo atual de cautela e custódia no backend e frontend. Ajustar o domínio, contratos, serviços, endpoints e telas para aceitar um recebedor do tipo pessoa ou unidade organizacional.

- Permitir escolher o tipo de recebedor: pessoa ou unidade organizacional.
- Exibir unidades organizacionais autorizadas para o usuário e contexto da operação.
- Persistir o tipo e a referência correta do recebedor.
- Manter compatibilidade com cautelas individuais existentes.
- Validar que o recebedor pertence a uma unidade válida e ativa.
- Aplicar autorização para criar, consultar, alterar e encerrar a cautela.
- Registrar auditoria com origem, destino, usuário executor e data.
- Exibir mensagens claras para unidade inválida, inativa ou sem permissão.
- Cobrir o fluxo na interface com estados de carregamento, vazio, erro e sucesso.

Não mascarar a diferença entre pessoa responsável e unidade recebedora. Se o modelo atual precisar de uma decisão de negócio para representar responsabilidade interna da unidade, registrar o bloqueio antes de alterar a persistência.

## Critérios de aceite

- [ ] O formulário de cautela permite selecionar uma unidade organizacional como recebedora.
- [ ] O fluxo individual existente continua funcionando.
- [ ] O backend persiste e retorna corretamente o tipo e identificador do recebedor.
- [ ] Unidades inativas, inexistentes ou não autorizadas são rejeitadas.
- [ ] A operação gera auditoria completa.
- [ ] Testes automatizados cobrem recebimento individual e por unidade.
- [ ] O frontend passa no lint e trata os estados do fluxo.

## Condição de parada

Parar quando a cautela puder ser criada, consultada e encerrada com recebedor pessoa ou unidade organizacional, com autorização, auditoria e testes validados. Não iniciar transporte ou módulos ERP sem relação direta.
