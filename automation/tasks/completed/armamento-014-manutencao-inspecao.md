# Armamento 014 - manutenção e inspeção

## Estado após auditoria do commit 038
**IMPLEMENTADA, NÃO HOMOLOGADA POR COMPLETO.** Planos, ordens, diagnóstico, serviços, teste funcional, peças, inspeções periódicas e vínculo com devolução bloqueante existem.

## Objetivo
- Executar fluxo completo no navegador com PostgreSQL: abertura, manutenção, peças/serviços, conclusão aprovada e reprovada.
- Validar inspeções periódicas e anexos/evidências do ciclo de vida.
- Corrigir qualquer inconsistência encontrada e atualizar documentação/testes.

## Escopo
Implementar exclusivamente o trabalho descrito no objetivo desta tarefa, respeitando a arquitetura, os padrões e as integrações existentes do ERP Comandos.
## Critérios de aceite
- [ ] Fluxo completo aprovado no navegador e API.
- [ ] Ativo em manutenção não pode ser usado indevidamente.
- [ ] Retorno ao serviço/bloqueio e histórico estão corretos.
 
## Condição de parada
Encerrar esta tarefa somente após implementar o objetivo e satisfazer os critérios de aceite aplicáveis. Não iniciar outra tarefa; o worker controla a continuidade da fila.