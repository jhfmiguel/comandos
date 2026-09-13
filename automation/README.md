# Bot de desenvolvimento do COMANDOS

O `codex-erp-bot.ps1` processa tarefas Markdown em segundo plano quando o executável `codex` estiver disponível no `PATH`.

## Preparação

1. Instale e autentique o Codex CLI conforme a distribuição liberada para sua conta.
2. Verifique no PowerShell:

```powershell
codex --version
```

3. Coloque uma tarefa em `automation/tasks/`, por exemplo `001-inventario.md`.

## Execução contínua

```powershell
powershell -ExecutionPolicy Bypass -File .\automation\codex-erp-bot.ps1
```

O worker aguarda o Codex ficar disponível, busca tarefas por ordem alfabética e processa uma por vez. Para executar sem manter o terminal aberto:

```powershell
Start-Process powershell.exe -WindowStyle Hidden -ArgumentList '-ExecutionPolicy Bypass -File .\automation\codex-erp-bot.ps1'
```

Para processar somente uma tarefa:

```powershell
powershell -ExecutionPolicy Bypass -File .\automation\codex-erp-bot.ps1 -Once
```

## Publicação automática opcional

Por padrão, o bot não cria commits nem envia código ao remoto. Para habilitar isso explicitamente:

```powershell
powershell -ExecutionPolicy Bypass -File .\automation\codex-erp-bot.ps1 -AutoCommit -AutoPush
```

O remoto e a branch podem ser alterados com `-Remote` e `-Branch`.

## Estados da fila

- `automation/tasks/`: tarefas aguardando o Codex.
- `automation/tasks/working/`: tarefa atualmente processada.
- `automation/tasks/completed/`: tarefa processada e validada.
- `automation/tasks/failed/`: tarefa que precisa de intervenção.
- `automation/logs/`: saída do Codex e validações.

O worker chama `wr-api/mvnw.cmd test` e `wr-app/npm.cmd run lint` após cada tarefa. Falhas ficam registradas e não são publicadas automaticamente.
