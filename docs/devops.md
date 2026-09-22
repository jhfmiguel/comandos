# DevOps / DevSecOps

Fluxo-alvo:

commit -> CI -> testes -> análise estática -> build -> imagem -> scan -> registry
-> homologação -> aprovação -> produção.

Ambientes: DEV, TEST, HML e PROD.

Controles planejados:
- SAST e dependency scanning;
- secret scanning;
- SBOM;
- container scanning;
- assinatura de artefatos;
- IaC scanning;
- políticas de deployment;
- observabilidade com métricas, logs e traces;
- rollback reproduzível.

O workflow CI atual valida backend e frontend. CD será conectado quando os
ambientes de implantação estiverem definidos.
