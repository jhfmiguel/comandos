# Platform Branding

A identidade visual do produto deve ser configurável por instalação/licença sem exigir alteração de componentes.

## Configuração atual

O frontend usa `productDefinition` como fonte única para nome, descrição, logo, favicon, idioma, tema e cor padrão.

Variáveis suportadas:

- `NEXT_PUBLIC_PRODUCT_NAME`
- `NEXT_PUBLIC_PRODUCT_SHORT_NAME`
- `NEXT_PUBLIC_PRODUCT_DESCRIPTION`
- `NEXT_PUBLIC_PRODUCT_LOGO`
- `NEXT_PUBLIC_PRODUCT_FAVICON`
- `NEXT_PUBLIC_PRODUCT_ACCENT`
- `NEXT_PUBLIC_BRAND_LOGO`
- `NEXT_PUBLIC_BRAND_FAVICON`

As variáveis `NEXT_PUBLIC_BRAND_*` têm precedência sobre a identidade padrão do produto e servem para personalização do cliente/licenciado.

Exemplo:

```env
NEXT_PUBLIC_BRAND_LOGO=/branding/cliente/logo.png
NEXT_PUBLIC_BRAND_FAVICON=/branding/cliente/favicon.ico
```

Sem configuração específica, o produto mantém a identidade visual oficial do COMANDOS.

## Regra arquitetural

Componentes visuais não devem referenciar diretamente arquivos como `/comandos-logo-v4.png`.
Eles devem consumir a identidade através de `platform/product`.

## Evolução prevista

A configuração por ambiente atende instalações dedicadas e on-premise.

Quando houver necessidade de SaaS multiempresa ou troca de branding em tempo de execução, o Platform Core poderá evoluir para um `BrandingProfile` persistido por licença/tenant, mantendo o mesmo contrato visual e sem espalhar regras de branding pelos componentes.
