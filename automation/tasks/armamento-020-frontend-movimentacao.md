# Armamento 020 - frontend de movimentação

## Objetivo
Finalizar as telas de custódia, cautela, transferência, manutenção e baixa.

## Contexto e prioridade
Movimentações alteram responsabilidade e saldo. A interface deve tornar o estado da operação explícito e impedir duplo envio ou confirmação ambígua.

## Escopo
Fluxos de movimentação do armamento com confirmação, autorização, validação, histórico e feedback visual.

## Regras técnicas
- Desabilitar ações enquanto uma operação estiver em andamento.
- Exigir confirmação para ações irreversíveis.
- Mostrar origem, destino, recebedor, motivo e resultado antes da confirmação.
- Atualizar a consulta somente após resposta de sucesso da API.

## Critérios de aceite
- [ ] Movimentações podem ser iniciadas e acompanhadas.
- [ ] Recebedor pessoa ou unidade é suportado.
- [ ] Estados e erros não deixam operações ambíguas.

## Condição de parada
Parar após validar o frontend de movimentação. Não iniciar testes abrangentes da API.
