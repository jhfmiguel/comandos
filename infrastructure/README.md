# Infrastructure

Infraestrutura do COMANDOS separada do código de negócio.

- docker: ambiente local e serviços opcionais.
- kubernetes: manifests base para homologação/produção.
- terraform: infraestrutura como código; provedores e módulos devem ser definidos por ambiente.
- observability: coleta de métricas, logs e traces.

O desenvolvimento local não depende de Kubernetes.
