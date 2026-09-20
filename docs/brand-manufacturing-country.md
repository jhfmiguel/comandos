# Marca: país de fabricação e código do fabricante

`Brand.manufacturingCountryCode` informa o país de fabricação declarado no cadastro
da marca. A referência padronizada é ISO 3166-1 alpha-2 (por exemplo, BR, US, DE),
com opções fornecidas por `Locale.getISOCountries()` do Java. Não é texto livre nem
país da sede do fabricante. Não determina a origem de cada exemplar individual.

O campo opcional é exposto no catálogo, formulário, consulta, filtros e API
`/api/erp/inventory/brands`. A API rejeita códigos fora da lista, inclusive códigos
em minúsculas. Ausência em criação e null/string vazia significam não informado;
ausência em atualização preserva o valor, permitindo clientes antigos. Null explícito
limpa o campo. CREATE/UPDATE/DELETE usam os snapshots da auditoria existente,
incluindo o país antes/depois. O mecanismo existente `ddl-auto=update` adiciona
`erp_brand.manufacturing_country_code varchar(2)` nullable, sem preencher países
por suposição e sem alterar a unicidade nome/fabricante.

## Significado de manufacturerCode

A definição técnica foi conferida no contrato atual: `ItemModel` armazena
`manufacturerCode` junto a nome, marca e SKU; `AssetItem.serialNumber` identifica
o exemplar. Neste cadastro, manufacturerCode é o código de catálogo/referência de
peça atribuído pelo fabricante ao modelo. O SKU é a referência interna do ERP.
Não representa país, lote, data de fabricação ou número de série de um exemplar.
O campo homônimo de `ReceivingSerial` pertence ao contrato de recebimento e não
é alterado nem reinterpretado por esta tarefa.

Esta documentação explicita o uso no nível de modelo, sem migração ou mudança
semântica: continua texto opcional de até 255 caracteres, sem unicidade ou
normalização nova. Valores existentes são preservados. Não houve confirmação
externa com responsável de negócio; qualquer futura reinterpretação exige essa
confirmação antes de alterar dados ou regras.
