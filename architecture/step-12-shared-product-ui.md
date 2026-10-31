# Etapa 12 — Componentes compartilhados no COMANDOS

O COMANDOS migrou a infraestrutura frontend genérica para a Faria Miguel.

## Consumido diretamente ou por adapters finos

- tabelas;
- paginação;
- FormField;
- FloatLabel e enhancer legado;
- modal/ConfirmDialog;
- layout administrativo;
- temas light/dark/mixed;
- notificações;
- contratos de autenticação;
- helpers de dados e hooks;
- Badge, FilterBar, UploadField e demais primitivas comuns.

## Permanece específico do COMANDOS

- máscaras e formatação de CPF, CEP, telefone, moeda e datas;
- navegação/menu do produto;
- paleta institucional;
- traduções do produto;
- overlay de sessão/roteamento próprio;
- regras de Armamento, munições e demais verticais de segurança.

Os arquivos de compatibilidade em `app/src/platform` podem continuar existindo para evitar uma reescrita simultânea de todas as telas, mas sua implementação genérica deve delegar aos pacotes `@faria-miguel/*`.
