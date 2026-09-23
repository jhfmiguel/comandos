# Política Git para agentes e worktrees

## Objetivo
Permitir trabalho paralelo sem dois agentes disputarem a mesma árvore de trabalho.

## Regras
1. `main` é integração oficial.
2. Cada tarefa concorrente usa branch própria.
3. Cada branch concorrente usa worktree própria quando executada localmente.
4. Uma tarefa deve possuir um conjunto claro de arquivos/módulos.
5. Evitar duas tarefas simultâneas sobre os mesmos arquivos.
6. Atualizar a branch com `main` antes da validação final.
7. Integrar somente após testes relevantes.

## Convenção sugerida de branches
- `agent/backend/<tema>`
- `agent/frontend/<tema>`
- `agent/qa/<tema>`
- `agent/release/<tema>`

## Exemplo PowerShell

Partindo do repositório principal:

```powershell
git fetch origin
git switch main
git pull --ff-only

git worktree add ..\comandos-backend -b agent/backend/armamento-rastreabilidade main
git worktree add ..\comandos-frontend -b agent/frontend/armamento-consulta main
git worktree add ..\comandos-qa -b agent/qa/armamento-regressao main
```

Cada janela do Codex deve apontar para a worktree correspondente.

## Encerramento

Depois da integração:

```powershell
git worktree remove ..\comandos-backend
git branch -d agent/backend/armamento-rastreabilidade
```

Só remover a branch após confirmar que o trabalho foi integrado.

## Conflitos
Se duas tarefas começarem a tocar o mesmo arquivo:
- parar uma delas;
- escolher um único owner;
- integrar a primeira mudança;
- atualizar a segunda branch;
- continuar depois da reconciliação.

Não resolver conflitos automaticamente sem entender a regra de negócio.
