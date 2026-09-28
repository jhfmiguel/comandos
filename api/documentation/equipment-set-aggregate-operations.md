# Operações agregadas de kits/conjuntos

## Objetivo

`EquipmentSet` continua sendo a definição reutilizável de um kit, com componentes serializados (`AssetItem`) e/ou quantitativos (`StockBalance`). A cautela já trabalha com o conjunto de forma integral. Esta camada estende a mesma semântica para doação, baixa/descarte, consumo e manutenção sem remover os fluxos individuais existentes.

## Princípios

- Selecionar um kit representa uma operação agregada e transacional.
- Os serviços de domínio já existentes continuam sendo a fonte das regras de estoque, autorização, validade, estado e idempotência.
- Nenhum componente elegível pode falhar silenciosamente: uma falha aborta toda a operação agregada.
- A operação preserva snapshots dos componentes utilizados para que alterações futuras no cadastro do kit não alterem o histórico.
- O `requestId` da operação agregada é idempotente. Repetição com o mesmo conteúdo retorna a mesma operação; repetição com conteúdo diferente é conflito.

## Doação do kit

Endpoint:

`POST /api/erp/equipment-sets/operations/donation`

A doação expande todos os componentes do conjunto. Ativos serializados são enviados com quantidade `1`; componentes de saldo usam a quantidade definida no kit. A expansão é encaminhada ao fluxo oficial de doação realizada, que continua responsável por validade, disponibilidade, baixa, movimento e transferência de titularidade.

## Baixa/descarte do kit

Endpoint:

`POST /api/erp/equipment-sets/operations/disposal`

A baixa expande todos os componentes do conjunto e usa o fluxo oficial de descarte. Para ativos, o resultado permanece terminal (`DISPOSED`). Para saldos, a baixa é quantitativa. Confirmação, processo, motivo e eventual destruição física continuam sujeitos às regras do módulo de baixa/descarte.

## Consumo do kit

Endpoint:

`POST /api/erp/equipment-sets/operations/consumption`

Em um conjunto misto, somente componentes configurados como `consumable=true`, `lotControlled=true` e `serialized=false` participam do consumo. Componentes físicos não consumíveis permanecem no kit.

A quantidade entregue é a quantidade cadastrada no componente. `returnedByComponentId` pode registrar a sobra devolvida por componente; a quantidade usada é derivada como:

`usado = entregue - devolvido`

O fluxo oficial de consumo mantém a regra `entregue = usado + devolvido` e os movimentos de entrega/devolução.

## Manutenção do kit

Endpoint:

`POST /api/erp/equipment-sets/operations/maintenance`

A manutenção seleciona todos os componentes serializados do conjunto e abre uma ordem de manutenção para cada ativo. Todos os pedidos-filho têm `requestId` determinístico derivado do `requestId` agregado e do componente, permitindo retry seguro.

Se qualquer ativo não puder entrar em manutenção, toda a operação é revertida.

## Histórico agregado

`EquipmentSetOperation` registra:

- kit e snapshots de código/nome;
- organização/unidade;
- tipo da operação;
- recurso agregado gerado;
- IDs dos registros gerados;
- quantidade de componentes operados;
- operador e timestamp;
- `requestId` e fingerprint.

`EquipmentSetOperationComponent` congela para cada componente:

- ID do componente original;
- tipo `ASSET` ou `BALANCE`;
- ID do registro de estoque;
- papel dentro do kit;
- quantidade operada.

Essa estrutura evita que uma alteração posterior na composição do kit reescreva o histórico de uma doação, baixa, consumo ou manutenção já realizada.

## Compatibilidade

Os endpoints individuais de doação, baixa, consumo e manutenção permanecem válidos. A camada agregada é aditiva e reutiliza esses serviços em uma única transação.
