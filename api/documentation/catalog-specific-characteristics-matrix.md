# Matriz de características específicas por família

Esta matriz define o conjunto mínimo de características técnicas exigidas para que um modelo de catálogo do módulo Armamento seja considerado completo. O objetivo é manter alinhados entidade, editor de frontend, dados de homologação e validação de backend.

| Família | Entidade de especificação | Campos obrigatórios mínimos | Parâmetros referenciados obrigatórios |
|---|---|---|---|
| FIREARM | `FirearmSpecification` | calibre, mecanismo de funcionamento, capacidade, comprimento do cano | `CALIBER` |
| AMMUNITION | `AmmunitionSpecification` | calibre, tipo de munição, classificação de letalidade, tipo de projétil, estojo, espoleta | `CALIBER`, `AMMUNITION_TYPE`, `PROJECTILE_TYPE`, `CASE_TYPE`, `PRIMER_TYPE` |
| BALLISTIC_PROTECTION | `BallisticProtectionSpecification` | tipo de proteção, nível, material, certificação, tamanho, vida útil | `PROTECTION_TYPE`, `PROTECTION_LEVEL`, `MATERIAL`, `SIZE` |
| GRENADE | `GrenadeSpecification` | tipo, agente, composição, validade, retardo, raio de segurança | `GRENADE_TYPE`, `AGENT`, `COMPOSITION` |
| SPRAY | `SpraySpecification` | agente, composição, validade, concentração, volume, alcance | `AGENT`, `COMPOSITION` |
| ELECTRICAL_DEVICE | `ElectricalDeviceSpecification` | tensão, ciclos, tipo de cartucho | `CARTRIDGE_TYPE` |
| OPTICAL | `OpticalSpecification` | tipo óptico, ampliação mínima, ampliação máxima, retículo, visão noturna, visão térmica | `OPTICAL_TYPE` |
| HELMET | `HelmetSpecification` | nível de proteção, material, tamanho, peso, certificação | `PROTECTION_LEVEL`, `MATERIAL`, `SIZE` |
| SHIELD | `ShieldSpecification` | tipo de escudo, material, altura, largura, peso | `SHIELD_TYPE`, `MATERIAL`; `PROTECTION_LEVEL` quando balístico |
| RESTRAINT | `RestraintSpecification` | tipo de restrição, material, mecanismo de trava, indicação de trava dupla | `MATERIAL`, `LOCKING_MECHANISM` |
| TACTICAL_EQUIPMENT | `TacticalEquipmentSpecification` | tipo de equipamento, material, tamanho, peso | `MATERIAL`, `SIZE` |
| ACCESSORY_COMPONENT | `AccessoryComponentSpecification` | tipo de componente, compatibilidade, interface de montagem, indicação de componente controlado | `COMPONENT_TYPE`, `COMPATIBILITY`, `INTERFACE` |

## Regras transversais

Todo modelo especializado deve possuir categoria, tipo e classificação coerentes entre si, marca, SKU, unidade de medida e preço de lista não negativo. O registro de especificação deve pertencer ao mesmo `ItemModel` selecionado no catálogo.

Campos numéricos que representam quantidade, dimensão, massa, duração, tensão, concentração, volume, alcance ou capacidade devem ser positivos quando definidos como obrigatórios. Ampliação óptica admite valor igual a 1, desde que a ampliação máxima seja maior ou igual à mínima.

Os campos textuais legados continuam armazenados quando existentes, mas os campos parametrizados de referência são a fonte principal para seleção encadeada e integridade. Os verificadores de homologação exigem referências ativas para os parâmetros listados na matriz.

Para escudo balístico, `PROTECTION_LEVEL` é obrigatório. Para outros tipos de escudo, pode permanecer ausente conforme a classificação do modelo.

Esta matriz deve ser atualizada sempre que uma nova família de catálogo ou nova entidade `*Specification` for introduzida.
