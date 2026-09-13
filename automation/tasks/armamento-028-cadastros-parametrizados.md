# Armamento 028 - cadastros parametrizados

## Objetivo
Criar cadastros parametrizáveis para substituir valores fixos nas telas do ERP.

## Contexto e prioridade
Esses catálogos serão usados por vários módulos. A implementação deve evitar listas hardcoded e permitir evolução administrativa sem alteração de código.

## Escopo
Implementar, conforme os padrões existentes, cadastros, API, permissões, auditoria, telas e seletores para: Grenade Type, Agent, Unit of Measure, Projectile Type, Case Type, Primer Type, Caliber, Protection Type, Protection Level, Material, Cartridge Type, Optical Type, External System, Certification Type, Reason, Types, Category, Level e Action.

## Regras técnicas
- Identificar catálogos já existentes antes de criar entidades novas.
- Preferir ativação/inativação a exclusão física quando houver uso histórico.
- Definir escopo institucional, unicidade, ordenação e permissões por catálogo.
- Atualizar seletores consumidores sem quebrar valores antigos.

Os valores devem ser administráveis e reutilizados nas telas relacionadas, sem listas hardcoded duplicadas.

## Critérios de aceite
- [ ] Cada catálogo possui cadastro, edição, ativação/inativação e consulta quando aplicável.
- [ ] Telas usam os valores parametrizados.
- [ ] Autorização, auditoria, duplicidade e testes estão cobertos.
- [ ] O fluxo não quebra registros existentes.

## Condição de parada
Parar se algum catálogo exigir decisão de negócio não definida; registrar o bloqueio.
