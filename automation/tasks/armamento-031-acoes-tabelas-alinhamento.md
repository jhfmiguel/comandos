# Armamento 031 - ações das tabelas

## Objetivo
Padronizar as ações e os botões das tabelas do ERP.

## Contexto e prioridade
É uma melhoria transversal de ergonomia, mas deve permanecer limitada às tabelas do escopo atual para não causar regressões visuais no ERP inteiro.

## Escopo
Alinhar à direita as colunas de ações e seus botões em todas as tabelas relacionadas ao escopo atual, preservando acessibilidade, tooltips, responsividade e padrões visuais existentes.

## Regras técnicas
- Usar uma classe/padrão compartilhado quando já houver abstração local.
- Manter ordem, foco por teclado, nomes acessíveis e tooltips.
- Testar tabelas com uma e várias ações, texto longo e viewport móvel.

## Critérios de aceite
- [ ] Ações das tabelas ficam alinhadas à direita.
- [ ] Botões mantêm tamanho, espaçamento, foco e acessibilidade.
- [ ] Layout desktop e mobile não apresenta sobreposição.
- [ ] Lint e validação visual passam.

## Condição de parada
Parar após aplicar o padrão apenas às tabelas do escopo e validar frontend.
