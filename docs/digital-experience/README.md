# Faria Miguel Digital Experience

## Objetivo

Digital Experience e a camada digital reutilizavel da Faria Miguel para sites institucionais, CMS/blog, commerce e portais. O produto deve poder ser habilitado nos ERPs da Faria Miguel ou licenciado separadamente.

## Principios

- produto horizontal, nao um dominio especifico do COMANDOS;
- frontend publico separado do backoffice do ERP;
- nenhum usuario do site recebe acesso implicito ao backoffice;
- integracao com Platform Core e Enterprise Core somente por contratos explicitos;
- quando conectado a um ERP, clientes, catalogo, precos, estoque, vendas e demais dados empresariais continuam tendo o Enterprise Core como fonte de verdade;
- CMS e experiencia publica pertencem ao Digital Experience;
- nenhuma dependencia comercial/proprietaria sem aprovacao explicita;
- construir a plataforma propria: nao usar WordPress, Shopify, Medusa, Strapi ou equivalentes como base do produto.

## Capacidades

### Site e CMS

- paginas compostas por blocos reutilizaveis;
- menus, cabecalho, rodape e navegacao;
- temas e design tokens;
- biblioteca de midia;
- formularios;
- dominios e redirects;
- preview, rascunho, publicacao e versionamento.

### Conteudo e blog

- posts, autores, categorias e tags;
- workflow editorial;
- publicacao programada;
- conteudo relacionado;
- RSS;
- historico e auditoria.

### Commerce

- catalogo publico;
- produtos e variantes;
- categorias, marcas e atributos;
- carrinho persistente;
- checkout;
- pedidos;
- promocoes e cupons;
- B2B e B2C;
- integracao com precos, estoque, vendas, financeiro e expedicao do Enterprise Core.

### Portal

- conta do cliente;
- enderecos;
- pedidos e orcamentos;
- documentos;
- favoritos;
- notificacoes;
- privacidade e LGPD.

### Descoberta e crescimento

- busca unificada;
- SEO tecnico;
- sitemap e robots;
- Open Graph e dados estruturados;
- analytics proprio;
- integracoes externas opcionais.

## Arquitetura

```text
Internet
  |
  v
Digital Experience Public Frontend (Next.js / React / TypeScript)
  |
  v
Digital Experience API (Java 25 / Spring Boot)
  |
  +--> Platform Core
  |      identidade, autorizacao, auditoria, workflow,
  |      documentos, notificacoes, eventos, observabilidade
  |
  +--> Enterprise Core
         clientes, catalogo, precos, estoque, vendas,
         financeiro, contratos e demais capacidades empresariais
```

O frontend publico deve ter sessao e superficie de ataque independentes do painel administrativo. O backoffice de administracao do Digital Experience pode ser exposto dentro dos ERPs, respeitando RBAC e auditoria.

## Fronteiras iniciais

1. `digital.site` - sites, dominios, navegacao e configuracao visual.
2. `digital.cms` - paginas, blocos, midia, formularios e publicacao.
3. `digital.content` - blog, autores, categorias e tags.
4. `digital.seo` - metadados, sitemap, redirects e structured data.
5. `digital.search` - indice e busca publica.
6. `digital.commerce` - experiencia de catalogo, carrinho e checkout; nao duplica o dominio empresarial.
7. `digital.portal` - experiencia autenticada do cliente.
8. `digital.analytics` - eventos e indicadores digitais.

## Primeira entrega executavel

A primeira fatia vertical sera CMS institucional:

1. tenant/site;
2. pagina;
3. slug;
4. blocos de conteudo;
5. estados rascunho/publicado;
6. API publica de leitura;
7. renderizacao SSR/SSG no Next.js;
8. SEO basico por pagina;
9. administracao protegida por permissao;
10. auditoria das alteracoes.

Depois dessa fundacao entram blog e commerce sem quebrar os contratos do CMS.
