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
- configurações de tema/paleta.

## Próximos blocos

1. toast/notificações;
2. diálogos e botões simples;
3. sales form;
4. stock intake;
5. tabelas de usuários e armas;
6. audit;
7. record-workspace / DataTable / Paginator / Tabs;
8. remoção final dos pacotes e do presente arquivo.

Quando não houver nenhum import Prime no frontend, os pacotes serão removidos do
package.json e yarn.lock e este marcador será excluído.
