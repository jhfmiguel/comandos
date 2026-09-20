# ARM-002 - Concluir interface de armamento

## Estado após auditoria do commit 038
**PARCIAL.** As telas principais existem, mas a homologação de todos os fluxos ainda não foi concluída.

## Trabalho restante
- Validar de ponta a ponta kits, doações, consumo, baixa/destruição e conclusão de manutenção com PostgreSQL.
- Corrigir erros de interface/contrato encontrados nessa homologação.
- Executar lint e build no estado final.
- Não duplicar telas ou fluxos já existentes.

## Critérios de aceite
- [ ] Fluxos principais do Armamento homologados no navegador.
- [ ] Estados de erro, vazio, sucesso e repetição idempotente verificados.
- [ ] Lint e build passam no estado final.
