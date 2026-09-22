# Terraform / OpenTofu

Esta pasta é a fronteira de Infrastructure as Code do COMANDOS.

A implementação será dividida em módulos independentes para:
- rede;
- banco gerenciado;
- Kubernetes;
- object storage;
- secrets/KMS;
- observabilidade;
- registry de containers.

Nenhum estado Terraform deve ser versionado no Git. Em produção, use backend
remoto com locking e criptografia. O provedor (OCI, AWS, Azure, datacenter
privado ou híbrido) deve ficar em overlays por ambiente.
