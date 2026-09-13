# Bot de desenvolvimento do COMANDOS

O `codex-erp-bot.ps1` processa tarefas Markdown em segundo plano quando o executável `codex` estiver disponível no `PATH`.

## Preparação

1. Instale o Codex CLI oficial globalmente:

```powershell
npm.cmd install --global @openai/codex
```

2. Feche e abra o PowerShell novamente, depois autentique sua conta:

```powershell
codex login
```

3. Verifique no PowerShell:

```powershell
codex --version
```

Se o comando ainda não for encontrado, confirme o PATH e diagnostique a instalação:

```powershell
where.exe codex
codex doctor
```

O diretório global padrão do npm no Windows é `%APPDATA%\npm`. Ele precisa estar no PATH do usuário. Depois de alterá-lo, abra um novo terminal.

4. Coloque uma tarefa em `automation/tasks/`, por exemplo `001-inventario.md`.

## Execução contínua

```powershell
powershell -ExecutionPolicy Bypass -File .\automation\codex-erp-bot.ps1
```

O worker aguarda o Codex ficar disponível, busca tarefas por ordem alfabética e processa uma tarefa por ciclo quando nenhum escopo é informado. A tarefa precisa declarar objetivo, escopo, critérios de aceite e condição de parada. Ao terminar, o bot mostra uma notificação do Windows e grava o último resultado em `automation/logs/last-completion.txt`.

Para listar as tarefas disponíveis:

```powershell
powershell -ExecutionPolicy Bypass -File .\automation\codex-erp-bot.ps1 -ListTasks
```

Para acompanhar a fila com nome, estado, descrição, data e status do worker:

```powershell
powershell -ExecutionPolicy Bypass -File .\automation\watch-armamento.ps1
```

Para mostrar uma fotografia única da fila, sem atualização contínua:

```powershell
powershell -ExecutionPolicy Bypass -File .\automation\watch-armamento.ps1 -Once
```

Para escolher exatamente uma tarefa:

```powershell
powershell -ExecutionPolicy Bypass -File .\automation\codex-erp-bot.ps1 -TaskName 001-inventario.md -Once
```

Para concluir todas as etapas de um módulo, nomeie as tarefas com o mesmo prefixo e execute o workstream. Por exemplo, `armamento-001-modelo.md`, `armamento-002-endpoint.md` e `armamento-003-testes.md` serão executadas nessa ordem:

```powershell
powershell -ExecutionPolicy Bypass -File .\automation\codex-erp-bot.ps1 -Workstream armamento
```

Esse comando continua até não existirem mais tarefas `armamento-*.md`. Se uma etapa falhar, o bot para no ponto da falha e avisa para que ela seja corrigida antes de continuar.

Para executar sem manter o terminal aberto:

```powershell
Start-Process powershell.exe -WindowStyle Hidden -ArgumentList '-ExecutionPolicy Bypass -File .\automation\codex-erp-bot.ps1'
```

Para processar somente uma tarefa:

```powershell
powershell -ExecutionPolicy Bypass -File .\automation\codex-erp-bot.ps1 -Once
```

Sem `-Workstream`, o limite padrão é uma tarefa por execução. Para autorizar mais tarefas no mesmo ciclo, use `-MaxTasks 3`.

Use `-NoNotification` somente se não quiser o aviso visual do Windows.

O bot executa o Codex com `--approve-for-me`. Isso permite o trabalho sem solicitar `Allow once` a cada comando. A tarefa continua sendo o limite funcional: o Codex deve parar na condição de parada descrita no arquivo selecionado. Se o serviço estiver em rate limit, a etapa volta para a fila e o worker aguarda, sem marcá-la como concluída.

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
