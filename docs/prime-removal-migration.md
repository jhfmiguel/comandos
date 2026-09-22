# Migração de remoção do Prime

Este arquivo existe somente enquanto a migração gradual está em andamento.

## Regra

Nenhuma nova tela ou componente pode introduzir PrimeReact, PrimeUI, PrimeIcons
ou PrimeUX. Os pacotes atuais permanecem temporariamente instalados apenas para
que áreas legadas ainda não migradas continuem compilando entre os commits.

## Concluído

- menu/sidebar;
- governança;
- Input;
- InputMoney;
- Textarea;
- configurações de tema/paleta;
- toast/notificações;
- modal e botões da entrada de estoque;
- formulário de vendas (autocomplete, select, tabela e exclusão);
- listas de usuários e armas (filtros, paginação, edição inline e exclusão);
- auditoria (filtros, paginação e detalhes before/after);
- RecordWorkspace: tabs, tabela, paginação, exclusão e editor genérico.

## Próximos blocos

1. demais diálogos/botões residuais;
2. varredura global por imports Prime;
3. remoção final dos pacotes e do presente arquivo.

Quando não houver nenhum import Prime no frontend, os pacotes serão removidos do
package.json e yarn.lock e este marcador será excluído.
