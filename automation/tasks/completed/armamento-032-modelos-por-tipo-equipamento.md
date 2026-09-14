# Armamento 032 - modelos filtrados por tipo de equipamento

## Objetivo
Garantir que o campo `model` em Controlled Equipment mostre somente os modelos pertencentes ao tipo de equipamento controlado selecionado.

## Contexto e prioridade
Esta regra evita que o usuário associe modelo de ammunition a firearm ou modelo de firearm a grenade. A filtragem visual é necessária, mas a proteção definitiva deve existir no backend.

## Regra de negócio

Os modelos devem ser filtrados pelo tipo de equipamento:

- `Firearm`: somente modelos de firearms.
- `Ammunition`: somente modelos de ammunition.
- `Grenade`: somente modelos de grenades.
- Outros tipos: somente modelos compatíveis com o respectivo tipo.

Um modelo de ammunition não pode aparecer em firearm, e um modelo de firearm não pode aparecer em ammunition.

## Escopo

Inspecionar o fluxo atual de Controlled Equipment, tipos de equipamento, Item Models, Firearm, Ammunition, Grenade e demais equipamentos controlados. Implementar:

- Filtro dependente no frontend quando o tipo for selecionado ou alterado.
- Limpeza automática do modelo quando o tipo mudar e o modelo atual ficar incompatível.
- Endpoint ou consulta filtrada no backend, evitando carregar modelos incompatíveis.
- Validação no backend para rejeitar tipo/modelo incompatíveis mesmo que a requisição seja manipulada.
- Tratamento de carregamento, lista vazia, erro e tipo sem modelos cadastrados.
- Compatibilidade com registros existentes e filtros de consulta.
- Testes automatizados para firearm, ammunition, grenade e pelo menos um tipo adicional.

Não resolver somente ocultando opções no frontend: a regra deve existir também no backend.

## Regras técnicas
- Definir a relação persistida entre modelo e tipo de equipamento.
- Filtrar no servidor e no cliente, evitando carregar catálogo incompatível sem necessidade.
- Limpar seleção incompatível quando o tipo mudar.
- Cobrir criação, edição, consulta e dados históricos.

## Critérios de aceite

- [ ] Ao selecionar firearm, o campo model lista apenas modelos de firearm.
- [ ] Ao selecionar ammunition, o campo model lista apenas modelos de ammunition.
- [ ] Ao selecionar grenade, o campo model lista apenas modelos de grenade.
- [ ] Alterar o tipo remove ou exige confirmação para um modelo incompatível já selecionado.
- [ ] A API rejeita combinações incompatíveis entre tipo e modelo.
- [ ] Registros existentes continuam sendo exibidos corretamente.
- [ ] Estados de carregamento, vazio e erro estão tratados.
- [ ] Testes da API e lint do frontend passam.

## Condição de parada

Parar após validar o filtro de modelos por tipo no cadastro, edição e consulta de Controlled Equipment. Não iniciar novas regras de cadastro de modelos ou outros módulos ERP.
