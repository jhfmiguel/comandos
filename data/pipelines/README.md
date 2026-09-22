# Data pipelines

Pipelines analíticos devem ser independentes da API transacional.

Regras:
- ingestão idempotente;
- schema versionado;
- lineage;
- data quality checks;
- dead-letter/quarentena;
- replay seguro;
- segregação de dados sensíveis;
- métricas de atraso e falha.

Kafka será a principal fronteira de streaming quando a escala justificar
mensageria externa. Enquanto isso, eventos internos permanecem no Spring
Modulith.
