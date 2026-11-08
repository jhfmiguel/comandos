# Configurações versionadas do Faria Miguel no COMANDOS

O consumidor já depende de faria-miguel-platform, tenancy e infrastructure. Os serviços/API/autorização vêm das bibliotecas, sem copiar classes genéricas. Novo perfil opt-in platform-configuration seleciona runtime versionado, product=comandos, ambiente configurável, aprovação independente e autenticação. Não altera dados, schema, contas ou permissões. Não foi ativado em execução nesta entrega.

O host precisa receber bibliotecas Maven atualizadas. Para desenvolvimento local no workspace do proprietário: na raiz do Faria Miguel, mvn -DskipTests install; depois, no COMANDOS/api, mvn -DskipTests package. Não há publicação Maven remota neste lote.

Para uso posterior, combine platform-configuration com os perfis de ambiente existentes. Não iniciar local/demo em banco que se deseja preservar: esses perfis já têm opções de criação/seed próprias, que esta entrega não executou. No perfil production/prod, ServiceManagementDirectory exige o armazenamento JDBC existente; a tabela precisa existir conforme a instalação anterior. O perfil novo não cria estrutura.

A interface compartilhada fica no frontend Faria Miguel, rota /configuration. Inicie esse frontend com NEXT_PUBLIC_API_URL apontando para a API do COMANDOS e origem permitida correspondente (por padrão localhost:3000). O login é feito na API consumidora; cookies, CSRF e permissões pertencem a ela. Não é necessário duplicar o console no frontend COMANDOS.

Para integração futura dentro do shell COMANDOS, foi criado pacote proprietário @faria-miguel/configuration-console no monorepo Faria Miguel, com apiUrl explícita e showLogin=false para reaproveitar sessão externa. O pacote ainda não foi publicado em registry; não se alteraram dependências npm instaladas no COMANDOS nem seus pacotes de auth/session/UI. O frontend Faria Miguel já consome o pacote por dependência local file do próprio monorepo.

Requisitos: conta autenticada, TenantMembershipDirectory válido e grants PLATFORM_CONFIGURATION_READ, PROPOSE, APPROVE, MANAGE e/ou ROLLBACK conforme função. O adapter exige tenant e empresa/filial/unidade correspondentes ao contexto e USER do próprio ator. Aprovação exige autor e revisor diferentes; nenhum grant é concedido automaticamente. A API filtra escopos independentemente da interface.

Compile não comprova startup/HTTP. Testes e adoção operacional permanecem para lote posterior conforme orientação do usuário. Nenhum servidor foi iniciado e nenhum banco foi acessado por essa integração.