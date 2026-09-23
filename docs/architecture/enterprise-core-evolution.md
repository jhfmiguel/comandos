# Evolução do Enterprise Core

## Objetivo

Construir capacidades empresariais comuns uma única vez em termos de modelo, contratos e regras, evitando três implementações divergentes.

## Fases

### Fase 1 — Contrato comum
Para cada capacidade:
- definir linguagem e entidades genéricas;
- separar regras universais das extensões verticais;
- documentar APIs públicas;
- criar testes de contrato;
- evitar nomes específicos de COMANDOS, TRATOR ou TUBARÃO.

### Fase 2 — Primeira implementação
A primeira implementação pode nascer dentro do produto que tiver a necessidade concreta mais madura, desde que respeite a fronteira Enterprise Core.

Ela deve ser escrita como código reutilizável, sem dependência da vertical.

### Fase 3 — Segundo consumidor
Quando um segundo produto precisar da mesma capacidade:
- comparar requisitos reais;
- corrigir generalizações prematuras;
- confirmar quais regras são verdadeiramente comuns;
- extrair ou sincronizar o módulo de forma controlada.

### Fase 4 — Extração física
Quando existir consumo real por múltiplos produtos, avaliar extração para biblioteca/repositório compartilhado da família pessoal/comercial.

A extração física só deve ocorrer quando:
- contratos estiverem estáveis;
- testes cobrirem o comportamento;
- dependências forem neutras;
- versionamento e compatibilidade estiverem definidos;
- nenhum domínio vertical estiver vazando para o compartilhado.

## Regra principal

Nunca copiar e evoluir independentemente o mesmo módulo empresarial em três repositórios por longo período.

Se uma capacidade é realmente comum, ela deve convergir para uma única fonte compartilhada.


## Primeiro consumidor concreto — COMANDOS / Armamento

O catálogo de Armamento passa a funcionar como primeiro consumidor concreto das classes de catálogo do Enterprise Core.

A separação adotada é:

```text
Enterprise Core
  CatalogItemBase
  CatalogCategoryBase
  BrandBase
        ↑
COMANDOS / Armamento
  ItemModel
  ItemCategory
  Brand
  + classificações e especificações verticais
```

Os campos genéricos de produto, categoria e marca ficam definidos na camada empresarial. Relações e regras próprias de Armamento permanecem no domínio vertical.

Nesta fase a reutilização é lógica dentro do COMANDOS. A extração física para biblioteca compartilhada continua condicionada ao segundo consumidor real (TRATOR ou TUBARÃO).


## Catálogo empresarial — identidade, unidade e rastreabilidade

A camada de catálogo comum agora também define fundamentos que não dependem de nenhuma vertical:

- normalização de identidade de catálogo;
- código de unidade de medida;
- modo de rastreabilidade;
- política de rastreabilidade.

As regras específicas continuam fora do Enterprise Core. O COMANDOS apenas consome a política comum e acrescenta suas exigências de Armamento.

Exemplos:

```text
CatalogTrackingPolicy
  serialized=true  -> SERIAL
  lotControlled=true -> LOT
  nenhum controle -> NONE
```

Combinações contraditórias são rejeitadas na camada comum, enquanto regras como calibre, proteção balística, registro regulatório e situação operacional continuam exclusivas do domínio Armamento.
