# Digital Experience Platform

## Diretriz

A família de produtos deve possuir uma camada de experiência digital própria, sem depender de plataformas prontas de CMS, blog, comércio eletrônico, portal, chat ou atendimento.

Não utilizar como fundação funcional:
- WordPress / WooCommerce;
- Shopify;
- Magento / Adobe Commerce;
- Bagisto;
- Medusa;
- Strapi;
- plataformas SaaS equivalentes;
- kits de componentes que imponham licença comercial ou acoplamento ao fornecedor.

As capacidades de produto serão desenvolvidas e mantidas no código da própria família.

## Escopo

A Digital Experience Platform poderá atender, conforme o produto e o cliente:

- site institucional;
- páginas e landing pages;
- CMS e publicação;
- blog, notícias e mídia;
- catálogo público;
- busca;
- SEO técnico e editorial;
- sitemap, robots e metadados;
- dados estruturados;
- internacionalização;
- acessibilidade;
- área autenticada;
- autoatendimento;
- protocolos e solicitações;
- formulários;
- comércio eletrônico;
- carrinho;
- checkout;
- pedidos;
- promoções;
- cupons;
- preços;
- pagamentos por adaptadores;
- acompanhamento de pedidos;
- avaliações e favoritos;
- chat;
- atendimento;
- notificações;
- base de conhecimento;
- consentimento e privacidade;
- analytics;
- integrações externas por contratos e adaptadores.

## Limite de "feito do zero"

"Feito do zero" significa que as regras, fluxos, componentes de domínio, UX, contratos e experiência do produto são próprios.

Não significa reimplementar primitivas cuja segurança depende de padronização e revisão especializada. É proibido criar implementações próprias de:

- criptografia;
- TLS;
- funções de hash de senha;
- geração criptográfica aleatória;
- OAuth/OIDC;
- WebAuthn;
- parsers de protocolos de segurança;
- drivers de banco;
- mecanismos de compressão;
- codecs;
- bibliotecas HTTP de baixo nível.

Esses componentes devem usar APIs de plataforma, padrões abertos e dependências fundamentais amplamente auditadas, com licença compatível e atualização controlada.

## Arquitetura

```text
Digital Experience
        |
Vertical Domains
        |
Enterprise Core
        |
Platform Core
```

Digital Experience não acessa tabelas ou entidades administrativas diretamente. Toda operação passa por casos de uso autorizados da API.

## Segurança

- menor privilégio;
- separação entre conta pública/externa e operador administrativo;
- autorização no backend;
- proteção CSRF quando aplicável;
- CSP e headers de segurança;
- cookies seguros e HttpOnly;
- rate limiting;
- proteção contra abuso;
- validação server-side;
- auditoria;
- idempotência;
- isolamento de tenant/licença;
- upload de arquivos com validação e quarentena;
- nenhuma credencial ou segredo no frontend;
- nenhuma regra crítica apenas no cliente.

## SEO

A camada pública deve nascer preparada para:

- renderização indexável;
- metadata por rota;
- canonical URLs;
- Open Graph;
- Twitter/X cards;
- JSON-LD;
- sitemap segmentado;
- robots;
- redirects permanentes;
- paginação indexável quando apropriado;
- Core Web Vitals;
- imagens responsivas;
- cache e revalidação;
- URLs legíveis;
- conteúdo semântico;
- acessibilidade WCAG;
- monitoramento de links quebrados.

## Dependências

A preferência é por código próprio para funcionalidades de produto e interface.

Dependências externas só são aceitas quando forem infraestrutura fundamental ou quando a implementação própria aumentar risco técnico, de segurança ou de interoperabilidade.

Toda dependência deve:
1. ter licença compatível com produto comercial proprietário;
2. possuir finalidade clara;
3. ser pequena e substituível sempre que possível;
4. ter histórico de manutenção adequado;
5. passar por auditoria de vulnerabilidades e licença;
6. não impor dependência comercial ao cliente.

## Branding

A Digital Experience utiliza o mesmo contrato de branding da plataforma: identidade padrão do produto com possibilidade de sobrescrita por licença/cliente, sem alterar componentes.
