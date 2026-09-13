# Armamento 008 - classificação e tipos

## Objetivo
Implementar classificação, tipos e categorias de armamentos.

## Contexto e dependências
A classificação será referência para filtros, modelos compatíveis, relatórios e validações posteriores. Usar catálogo parametrizado quando já existir; não criar listas fixas duplicadas.

## Escopo
Criar ou completar referências de tipo, classificação e regras de validação na API e no frontend, reutilizando padrões existentes.

## Regras técnicas
- Definir relacionamento entre tipo, classificação, categoria e modelo.
- Impedir inativação de valores ainda usados sem tratamento explícito.
- Registrar alteração de catálogo e manter compatibilidade com registros antigos.

## Critérios de aceite
- [ ] Tipos e classificações podem ser cadastrados, consultados e selecionados.
- [ ] Valores inválidos são rejeitados.
- [ ] Testes da API e lint do frontend passam.

## Condição de parada
Parar após validar classificação e tipos. Não iniciar identificação patrimonial.
