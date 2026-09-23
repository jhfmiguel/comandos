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


## Modelo comercial e modularidade

A Digital Experience Platform é um módulo comercial independente e opcional.

Ela não faz parte obrigatoriamente da licença-base do ERP e pode ser contratada, implantada e precificada separadamente.

Combinações comerciais possíveis:

- ERP sem portal/site;
- site institucional;
- site + blog/CMS;
- site + portal autenticado;
- site + comércio eletrônico;
- site + comércio eletrônico + chat/atendimento;
- ERP + portal;
- ERP + site;
- ERP + comércio eletrônico;
- ERP + pacote completo de Digital Experience.

A arquitetura deve permitir habilitar ou desabilitar capacidades por licença, sem alterar o código-fonte do cliente.

Capacidades comerciais internas devem ser granularizadas, por exemplo:

- WEBSITE;
- CMS;
- BLOG;
- CUSTOMER_PORTAL;
- EMPLOYEE_PORTAL;
- ECOMMERCE;
- CHAT;
- SUPPORT;
- KNOWLEDGE_BASE;
- MARKETING;
- ANALYTICS;
- SEO_ADVANCED.

O módulo deve possuir identidade, versionamento e ciclo de implantação próprios, embora reutilize Platform Core e Enterprise Core.

A contratação do módulo Digital Experience não deve conceder acesso administrativo ao ERP. O portal consome apenas APIs e casos de uso explicitamente autorizados.


## Campaigns e marketing

A Digital Experience deve incluir uma plataforma própria de campanhas e automação de marketing, licenciável separadamente ou em conjunto com WEBSITE/ECOMMERCE.

Capacidades previstas:

- campanhas institucionais e comerciais;
- banners, vitrines, destaques e pop-ups;
- landing pages;
- segmentação por público, perfil, origem, comportamento e contexto;
- audiências salvas e dinâmicas;
- cupons e promoções;
- regras de preço promocional;
- kits, combos e cross-sell/up-sell;
- campanhas por período, canal e região;
- newsletter;
- e-mail transacional e de campanha;
- notificações web/push;
- SMS/WhatsApp por adaptadores externos;
- jornadas e automações;
- gatilhos por evento;
- abandono de carrinho;
- recuperação de cliente;
- campanhas de aniversário e relacionamento;
- remarketing por integrações;
- programas de fidelidade;
- indicação/referral;
- listas de desejos e favoritos;
- pesquisa de satisfação;
- formulários e captação de leads;
- consentimento e preferências de comunicação;
- A/B testing;
- experimentos;
- atribuição de campanha;
- UTM e tracking;
- pixels/conversões por adaptadores;
- funil;
- metas;
- conversões;
- CAC, LTV, ROAS e métricas correlatas;
- dashboards;
- agenda/calendário editorial e promocional;
- aprovação de conteúdo e campanha;
- versionamento;
- auditoria;
- orçamento de campanha;
- limites de frequência;
- exclusões e listas de bloqueio;
- personalização de conteúdo;
- recomendações;
- campanhas vinculadas a catálogo, categoria, produto, serviço ou evento.

A plataforma deve permitir canais próprios e integrações externas sem acoplamento ao fornecedor. Toda integração com e-mail, SMS, WhatsApp, meios de pagamento, redes sociais ou mídia paga deve ser implementada por adaptadores substituíveis.
