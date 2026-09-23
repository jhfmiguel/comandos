# Enterprise Core

O Enterprise Core é a camada de capacidades empresariais reutilizáveis entre COMANDOS, Tubarão e TRATOR.

Ele fica acima do Platform Core e abaixo dos domínios verticais.

## Direção de dependência

```text
Platform Core
    ↑
Enterprise Core
    ↑
Domínios verticais
```

A dependência nunca deve ser invertida.

## O que pertence ao Enterprise Core

Capacidades de negócio que fazem sentido em mais de um produto pessoal/comercial, sem depender de conceitos de segurança pública, agronegócio ou indústria.

Candidatos naturais:

- pessoas e fundamentos de RH;
- clientes;
- fornecedores;
- catálogo de itens/produtos/serviços;
- unidades de medida;
- categorias e classificações genéricas;
- compras;
- cotações;
- pedidos;
- recebimentos;
- vendas;
- devoluções;
- estoque-base;
- armazéns e localizações;
- lotes-base;
- movimentações de estoque-base;
- reservas-base;
- inventário físico-base;
- contratos;
- centros de custo;
- financeiro-base;
- contas a pagar;
- contas a receber;
- ativos patrimoniais-base;
- manutenção-base;
- documentos comerciais-base;
- relatórios empresariais-base;
- cadastros fiscais/tributários genéricos quando aplicáveis e legalmente apropriados.

## O que não pertence

Não entram no Enterprise Core conceitos verticais, por exemplo:

### COMANDOS
- armamento;
- munição e deflagração;
- cautela operacional;
- escolta;
- inteligência;
- operações de segurança;
- ocorrências específicas;
- controle balístico.

### TRATOR
- talhão;
- safra;
- cultura;
- rebanho;
- manejo;
- operação agronômica;
- agricultura de precisão.

### TUBARÃO
- ordem de produção industrial;
- MRP específico;
- chão de fábrica;
- centro de trabalho industrial;
- execução de produção;
- qualidade industrial especializada.

## Extensão por domínio

O Enterprise Core fornece fundamentos, não classes gigantes com todos os campos possíveis.

Exemplo:

```text
CatalogItem
    ├── SecurityEquipment
    ├── AgriculturalInput
    ├── IndustrialMaterial
    ├── FinishedProduct
    └── ServiceItem
```

A parte comum permanece compartilhável; atributos e regras específicas ficam no domínio vertical.

## Critérios para promover algo ao Enterprise Core

Uma capacidade deve ser promovida quando:

1. existe necessidade concreta em pelo menos dois produtos;
2. o contrato pode ser descrito sem vocabulário vertical;
3. a regra compartilhada é realmente equivalente;
4. a extração reduz duplicação sem criar acoplamento artificial;
5. testes conseguem validar o comportamento independentemente do produto.

## Regra de implementação

Primeiro estabilizar fronteiras lógicas dentro dos produtos.
Somente depois extrair fisicamente para biblioteca/pacote compartilhado quando houver consumo real por múltiplos repositórios.

Não antecipar uma biblioteca comum sem uso concreto.
