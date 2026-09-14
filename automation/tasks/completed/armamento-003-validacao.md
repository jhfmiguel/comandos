# Armamento - validação

## Objetivo

Validar e fechar a entrega do módulo de armamento e munição do ERP COMANDOS.

## Contexto e prioridade

Esta etapa só deve começar depois que o frontend de `armamento-002` estiver concluído ou bloqueado com justificativa. Ela é a primeira barreira de qualidade antes das próximas melhorias de cadastro em lote, cautela por unidade e controle híbrido de munição.

## Escopo

Executar testes da API, lint do frontend e validações existentes de fluxo. Corrigir somente defeitos diretamente relacionados ao módulo de armamento e munição. Atualizar documentação específica do módulo quando necessário.

## Procedimento obrigatório

- Ler o `git diff`, os logs das etapas anteriores e os contratos efetivamente usados.
- Separar falhas preexistentes de regressões introduzidas pelo workstream.
- Executar primeiro testes focados e depois a validação completa disponível.
- Não marcar a etapa como concluída se houver teste ignorado, contrato quebrado ou erro de console relevante.
- Registrar comandos, resultados, arquivos corrigidos e pendências no relatório final.

## Critérios de aceite

- [ ] Testes Maven relevantes passam.
- [ ] Lint e validações relevantes do frontend passam.
- [ ] Não existem erros bloqueadores no fluxo de armamento e munição.
- [ ] A documentação do módulo está coerente com o comportamento entregue.

## Condição de parada

Parar quando as validações do módulo passarem e os resultados estiverem registrados. Não iniciar novos requisitos depois dessa etapa; o relatório final deve listar arquivos alterados, testes executados e pendências.
