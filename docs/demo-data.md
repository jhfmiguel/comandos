# Dados fictícios locais

O COMANDOS possui uma carga de demonstração controlada pela propriedade `comandos.demo.seed`.

## Segurança

A carga fica desativada por padrão:

```properties
comandos.demo.seed=${COMANDOS_DEMO_SEED:false}
```

Ela deve ser usada somente em desenvolvimento/testes.

## Iniciar a API local com dados fictícios

No PowerShell:

```powershell
cd C:\workspace\faria-miguel\comandos\api
.\scripts\start-with-demo-data.ps1
```

O script ativa `COMANDOS_DEMO_SEED=true` apenas durante a execução e restaura o valor anterior ao encerrar.

## O que a carga principal fornece

A carga curada cria ou complementa, sem apagar registros existentes:

- organizações de demonstração;
- naturezas e atividades econômicas;
- tipos e unidades organizacionais;
- pessoas físicas e jurídicas;
- endereços, telefones e e-mails;
- papéis de pessoa e vínculos;
- locais de estoque;
- parâmetros técnicos de armamento;
- categorias, marcas e modelos;
- especificações de arma de fogo, munição e proteção balística;
- bens individuais em locais e situações diferentes;
- lotes de munição;
- saldos por local.

Há também cargas complementares para:

- dados de referência;
- manutenção;
- cobertura automática de entidades ainda vazias.

## Reexecução

Os dados curados usam chaves estáveis de demonstração e a carga é idempotente: reiniciar com a opção demo ativa complementa o banco sem recriar os mesmos registros principais.

## Exemplos de catálogo

A carga usa exemplos reconhecíveis para deixar clara a hierarquia do catálogo:

- Arma de fogo → Pistola → Semiautomática → Beretta APX A1 Full Size;
- Arma de fogo → Pistola → Semiautomática → Glock G17 Gen5;
- Arma de fogo → Pistola → Semiautomática → Taurus G3 Tactical;
- Arma de fogo → Fuzil → Seletivo → Fuzil de Assalto IMBEL 5,56 IA2;
- Arma de fogo → Espingarda → Pump action → CBC Pump Military 3.0 Cal. 12;
- Munições → Munição para pistola → FMJ → CBC 9 mm Luger FMJ;
- Proteção balística → Colete balístico → Nível III-A → Point Blank Alpha Elite Black.

## Identificação

Os registros curados usam nomes/códigos fáceis de reconhecer, por exemplo:

- `SSP-DEMO`;
- `ARM-CENTRAL`;
- `UOP-01`;
- `PAT-DEMO-0001`;
- `CBC-DEMO-2026-001`.

Assim é possível distinguir dados fictícios dos registros inseridos manualmente.
