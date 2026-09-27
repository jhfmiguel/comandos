# Digital Experience frontend

Esta pasta inicia a fronteira frontend do Faria Miguel Digital Experience.

O produto tera duas superficies separadas:

- `public`: experiencia publica (site, conteudo, catalogo, checkout e portal);
- `admin`: administracao do Digital Experience, integrada ao shell administrativo quando instalada em um ERP.

Regras:

- componentes publicos nao importam telas ou regras de dominios verticais do COMANDOS;
- o site publico nao compartilha autorizacao implicita com o backoffice;
- componentes reutilizaveis de infraestrutura visual continuam no Platform Core;
- contratos de catalogo, cliente, preco, estoque e venda pertencem ao Enterprise Core quando a instalacao estiver integrada a um ERP;
- acessibilidade, responsividade, SEO e desempenho sao requisitos de base, nao etapas posteriores.

A primeira fatia de implementacao sera o renderer publico de paginas CMS por blocos e o backoffice correspondente.
