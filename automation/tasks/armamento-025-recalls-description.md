# Armamento 025 - descrição de recalls

## Objetivo
Adicionar descrição aos cadastros de Recalls e Recall Items.

## Contexto e prioridade
A descrição precisa explicar o motivo, risco ou orientação do recall no registro principal e no item afetado, sem substituir códigos ou status existentes.

## Escopo
Criar os campos description no backend, contratos, persistência, telas, consultas, validações e testes, mantendo compatibilidade com registros existentes.

## Regras técnicas
- Definir tamanho máximo e comportamento de texto vazio.
- Migrar registros existentes sem falhar por ausência de descrição.
- Exibir descrição em cadastro, detalhe, consulta e auditoria quando aplicável.

## Critérios de aceite
- [ ] Recall possui descrição editável e consultável.
- [ ] Recall Item possui descrição editável e consultável.
- [ ] API compila, testes passam e frontend trata estados e validações.

## Condição de parada
Parar após validar os dois campos. Não alterar outros módulos.
