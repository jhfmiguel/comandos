# Armamento - backend

## Objetivo

Finalizar a parte de backend do módulo de armamento e munição do ERP COMANDOS, aproveitando o estado atual do repositório.

## Escopo

Inspecionar as alterações já existentes em `wr-api` relacionadas a armamento, inventário, munição, estoque e movimentações. Completar modelos, serviços, controladores, regras de autorização e persistência necessários para o fluxo do módulo. Não alterar módulos ERP sem relação direta.

## Critérios de aceite

- [ ] O fluxo de backend do módulo está implementado de ponta a ponta.
- [ ] Regras de negócio e autorização estão cobertas.
- [ ] Testes automatizados relevantes passam.
- [ ] A API compila e os testes Maven passam.

## Condição de parada

Parar depois de concluir e validar o backend relacionado a armamento e munição. Não iniciar telas do frontend nesta etapa. Se houver uma decisão de negócio ausente, registrar o bloqueio no log e parar.
