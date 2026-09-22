# COMANDOS Lakehouse

O banco transacional não é o repositório de Big Data.

Arquitetura-alvo:

Oracle/PostgreSQL -> CDC/eventos -> Kafka -> Object Storage -> Apache Iceberg

Camadas sugeridas:
- bronze: eventos e dados brutos imutáveis;
- silver: dados normalizados e enriquecidos;
- gold: visões analíticas e indicadores.

Motores previstos conforme necessidade: Spark/Flink para processamento e Trino
para consulta federada. Dados sensíveis devem manter classificação, lineage,
retenção e controle de acesso por domínio.
