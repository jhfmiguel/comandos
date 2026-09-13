# Armamento 007 - cadastro básico

## Objetivo
Implementar o cadastro básico de armamentos no ERP COMANDOS.

## Contexto e dependências
Começar pelos campos já existentes no domínio e nos formulários atuais. Este cadastro será usado pelas etapas de classificação, identificação, status, localização e frontend; não criar campos duplicados.

## Escopo
Modelos, API, persistência, autorização, auditoria e tela para cadastrar os dados essenciais do armamento, preservando alterações existentes.

## Regras técnicas
- Definir campos obrigatórios, unicidade e normalização antes da persistência.
- Separar dados de modelo, item individual e estoque.
- Manter cadastro unitário compatível com cadastro em lote posterior.
- Cobrir criação, edição, consulta, validação e autorização.

## Critérios de aceite
- [ ] Cadastro, consulta, edição e validação funcionam.
- [ ] API, frontend e testes usam contratos coerentes.
- [ ] Auditoria e autorização estão cobertas.

## Condição de parada
Parar após validar o cadastro básico. Não iniciar classificação, custódia ou movimentações.
