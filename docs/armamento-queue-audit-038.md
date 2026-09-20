# Auditoria da fila do Armamento após o commit 038

Base auditada: `b24f3fe`.

## Concluídas e movidas para completed
- 009 — identificação patrimonial.
- 012 — custódia e cautela.
- 016 — inventário físico.
- 018 — frontend de cadastro.

## Parciais
- ARM-002 — interface geral: faltam homologações integradas.
- 010 — status operacional: há política, mas existem códigos de status divergentes.
- 011 — localização/unidade: estrutura existe; falta uniformizar validação/auditoria.
- 013 — transferência: fluxo existe, mas não há aceite/rejeição do destino como previa a tarefa.
- 017 — auditoria/rastreabilidade: infraestrutura existe; cobertura final precisa ser conferida.
- 019 — consulta: consultas existem; falta consolidar consulta principal e URL de filtros.
- 020 — movimentação: telas existem; faltam homologações de fluxos.
- 022 — frontend: lint/build/browser foram executados em rodadas anteriores, não no HEAD final.

## Implementadas, aguardando validação final
- 014 — manutenção e inspeção.
- 015 — baixa/descarte.
- 021 — testes de API: suíte ampla existe; precisa rodar novamente após 037/038.

## Pendentes
- 024 — país de fabricação em Brand e definição formal de manufacturerCode.
- 025 — description em Recall e RecallItem.
- 023 — validação final/documentação; deve ser executada por último.

## Regra para o bot
O bot deve executar somente o trabalho restante descrito nos arquivos da fila. Não deve recriar funcionalidades já existentes. Antes de alterar um fluxo, deve ler implementação, testes e documentação atuais. A tarefa 023 é a barreira final e, ao concluí-la, o bot deve parar para validação do usuário.
