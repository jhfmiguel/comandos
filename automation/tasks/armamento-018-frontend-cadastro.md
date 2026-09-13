# Armamento 018 - frontend de cadastro

## Objetivo
Finalizar as telas de cadastro de armamentos.

## Contexto e prioridade
Esta etapa transforma os contratos já validados em uma experiência de cadastro utilizável. Deve preservar o cadastro unitário e incorporar o lote de Individual Assets quando o backend oferecer suporte.

## Escopo
Formulários, validação, cadastro unitário e em lote quando aplicável, integração com API e estados de carregamento/erro/sucesso.

## Regras técnicas
- Reutilizar componentes de formulário, sessão, mensagens e serviços existentes.
- Validar antes de enviar, mas manter validação definitiva no backend.
- Não perder campos preenchidos quando uma chamada falhar.
- Exibir relação asset code/serial number e erros por linha no lote.

## Critérios de aceite
- [ ] Usuário cadastra e revisa armamentos pela interface.
- [ ] Erros da API são apresentados sem perda de dados.
- [ ] Lint e testes/validações do frontend passam.

## Condição de parada
Parar após validar o frontend de cadastro. Não iniciar consulta.
