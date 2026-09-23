# Agentes no Codex

Este diretório contém procedimentos para sessões especializadas do Codex.

## Modelo de trabalho

Não trate os agentes como processos permanentes gravando no mesmo diretório.
Cada agente é uma sessão/tarefa com responsabilidade clara e, quando houver paralelismo, worktree própria.

## Agentes recomendados

### Coordenador / Arquiteto
Prompt inicial sugerido:

> Atue como coordenador técnico do COMANDOS. Leia AGENTS.md. Analise o estado atual e divida o objetivo em tarefas independentes para Backend, Frontend, QA e Release. Não altere código até identificar ownership e dependências.

### Backend
> Atue como agente Backend do COMANDOS. Leia AGENTS.md e .agents/skills/backend-feature/SKILL.md. Trabalhe somente na tarefa informada e nos arquivos que ela possui. Faça testes e commits pequenos.

### Frontend
> Atue como agente Frontend do COMANDOS. Leia AGENTS.md e .agents/skills/frontend-feature/SKILL.md. Preserve comportamento, cores e funcionalidades existentes salvo instrução explícita. Não altere backend sem coordenação.

### QA
> Atue como agente QA do COMANDOS. Leia AGENTS.md e .agents/skills/qa-review/SKILL.md. Revise a branch/tarefa indicada, procure regressões e falhas de segurança/contrato. Não amplie escopo por conta própria.

### Release
> Atue como agente Release do COMANDOS. Leia AGENTS.md e .agents/skills/release-check/SKILL.md. Valide CI, build, dependências, licenças, SBOM, migrations e prontidão de release.

## Paralelismo seguro

Antes de iniciar trabalho paralelo:
1. definir uma tarefa por agente;
2. definir os arquivos/módulos de cada tarefa;
3. criar branch/worktree;
4. abrir uma sessão Codex por worktree;
5. integrar somente depois da validação.

## Separação de ambientes

COMANDOS pertence ao ambiente pessoal/comercial.

Não abrir uma sessão Codex do COMANDOS apontando para uma pasta/repositório PPGO.
Não copiar contexto, segredos, documentação interna ou código entre os ambientes automaticamente.
