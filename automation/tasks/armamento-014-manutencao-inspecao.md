# Armamento 014 - manutenção e inspeção

## Objetivo
Implementar manutenção, inspeção e indisponibilidade operacional de armamentos.

## Contexto e dependências
Manutenção e inspeção podem alterar o status operacional, mas não devem apagar custódia, localização ou auditoria anteriores.

## Escopo
Ordens, registros de inspeção, responsáveis, datas, resultado, anexos se suportados e impacto no status operacional.

## Regras técnicas
- Diferenciar manutenção preventiva, corretiva e inspeção quando o domínio suportar.
- Validar datas, responsável, resultado e item relacionado.
- Definir retorno ao serviço e tratamento de reprovação.
- Registrar relação com o status alterado.

## Critérios de aceite
- [ ] Manutenção e inspeção podem ser registradas e consultadas.
- [ ] Armamento em manutenção respeita as restrições de uso.
- [ ] Histórico e testes estão disponíveis.

## Condição de parada
Parar após validar manutenção e inspeção. Não iniciar baixa ou descarte.
