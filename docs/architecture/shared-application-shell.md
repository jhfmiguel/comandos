# Casca visual compartilhada entre produtos

## Decisão

A casca comum pertence ao Platform Core do Faria Miguel (`frontend/packages/platform`). O pacote já é consumido pelo COMANDOS e é o local adequado para layout e navegação técnica compartilhados. Não será criado um core separado para telas.

Cada produto mantém sua identidade, definição de menu e páginas de domínio. A casca recebe esses elementos por propriedades/slots: marca, itens de navegação, cabeçalho e conteúdo principal. Componentes genéricos de formulário continuam nos pacotes de UI/design system; regras de negócio e páginas permanecem no produto dono do domínio.

## Inventário COMANDOS

| Arquivo atual | Responsabilidade | Destino |
|---|---|---|
| `app/src/components/layout/index.tsx` | Composição de sidebar, cabeçalho e área principal | Adaptador COMANDOS usando a casca do Platform |
| `app/src/components/layout/menu/index.tsx` | Itens de navegação, identidade, sessão e preferências do produto | Menu/configuração COMANDOS; fornecer conteúdo à casca |
| `app/src/components/layout/menu-item/index.tsx` | Renderização de navegação hierárquica e estado de rota | Extrair renderizador genérico configurável para Platform |
| `app/src/components/layout/top-toolbar.tsx` | Ação visual de tema | Toolbar do Platform com ações fornecidas pelo produto |
| `app/src/app/globals.css` (`.comandos-sidebar*`, `.comandos-main-*`) | Estilos da casca misturados aos estilos específicos | Mover estilos genéricos para Platform; manter tokens/branding configuráveis |
| páginas em `app/src/app/**` e componentes `app/src/components/erp/**` | Conteúdo e regras dos domínios COMANDOS | Permanecem no COMANDOS |

O `RootLayout` também importa `@faria-miguel/ui/styles.css`; componentes de formulário e tokens base já têm um pacote compartilhado e não devem ser duplicados na nova casca.

## Contrato a implementar

- O Platform fornece estrutura responsiva, áreas de sidebar/header/main, acessibilidade, estado de expansão e pontos de extensão.
- O consumidor fornece marca, navegação permitida para sua edição/instalação, ações do cabeçalho e conteúdo principal.
- A casca não conhece rotas, permissões, traduções, sessão ou conceitos de domínio do COMANDOS; o produto adapta esses serviços e filtra os itens conforme módulos contratados e autorizações.
- A remoção local só ocorre após o COMANDOS renderizar a casca compartilhada com comportamento equivalente.

## Critério de conclusão desta delimitação

Responsabilidades e arquivos estão classificados acima. A implementação e a publicação do contrato pertencem às etapas seguintes.
