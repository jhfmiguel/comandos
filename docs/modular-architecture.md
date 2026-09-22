# Arquitetura modular do COMANDOS

O COMANDOS adota um **monólito modular**: um único deploy por enquanto, com
fronteiras de domínio explícitas e evolução preparada para extração futura de
serviços.

## Plataforma

Módulos transversais:
- core
- identity
- organization
- security
- audit
- workflow
- documents
- notifications
- messaging
- analytics

## Armamento

O domínio Armamento é composto por módulos independentes de aquisição e
recebimento, inventário, custódia, transferência, manutenção, consumo,
reconciliação, reserva, alienação/venda, doação, descarte, lifecycle e relatórios.

## Domínios reservados

- transport
- escort
- intelligence
- operations
- personnel
- procurement

## Regras

1. Um módulo não deve gravar diretamente nas tabelas pertencentes a outro módulo.
2. Integrações transversais devem preferir APIs de módulo ou eventos de domínio.
3. Eventos internos usam Spring Application Events/Spring Modulith.
4. Kafka será usado quando houver necessidade de comunicação distribuída.
5. PostgreSQL permanece como banco operacional durante a migração.
6. Oracle é o banco-alvo preparado; o corte de dados somente ocorrerá após
   migrations Oracle e validação do ciclo completo.
7. Documentos binários devem evoluir para Object Storage; o banco mantém metadados,
   hash, classificação e referência.
8. Analytics/Big Data não deve competir com o banco transacional. O alvo é
   lakehouse em object storage, com formato de tabela aberto e pipelines próprios.

## Estrutura do repositório

- app: frontend
- api: backend modular
- infrastructure: Docker, Kubernetes, IaC e observabilidade
- data: lakehouse e pipelines
- docs: arquitetura, operação e requisitos
