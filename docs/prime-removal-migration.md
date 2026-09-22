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
- modal e botões da entrada de estoque.

## Próximos blocos

1. sales form;
2. tabelas de usuários e armas;
3. audit;
4. record-workspace / DataTable / Paginator / Tabs;
5. demais diálogos/botões residuais;
6. remoção final dos pacotes e do presente arquivo.

Quando não houver nenhum import Prime no frontend, os pacotes serão removidos do
package.json e yarn.lock e este marcador será excluído.
