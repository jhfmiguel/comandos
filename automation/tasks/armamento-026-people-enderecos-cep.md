# Armamento 026 - pessoas e múltiplos endereços

## Objetivo
Completar o cadastro de People com endereço consultado pela API de CEP.

## Escopo
Integrar busca de CEP, preencher endereço automaticamente e acrescentar número e complemento. Permitir mais de um endereço por pessoa, com tipo, principalidade, edição e remoção, respeitando os contratos e autorização existentes.

## Critérios de aceite
- [ ] CEP consulta e preenche os campos disponíveis.
- [ ] Número e complemento são armazenados.
- [ ] Uma pessoa pode possuir múltiplos endereços.
- [ ] Erros de CEP, duplicidade e endereço principal são tratados.
- [ ] Testes da API e lint do frontend passam.

## Condição de parada
Parar após validar busca de CEP e múltiplos endereços.
