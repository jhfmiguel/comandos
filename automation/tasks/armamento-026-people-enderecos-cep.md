# Armamento 026 - pessoas e múltiplos endereços

## Objetivo
Completar o cadastro de People com endereço consultado pela API de CEP.

## Contexto e prioridade
O endereço deve reduzir digitação sem transformar a API de CEP em fonte única de verdade. O usuário precisa revisar e complementar os dados antes de salvar.

## Escopo
Integrar busca de CEP, preencher endereço automaticamente e acrescentar número e complemento. Permitir mais de um endereço por pessoa, com tipo, principalidade, edição e remoção, respeitando os contratos e autorização existentes.

## Regras técnicas
- Debounce e estado de carregamento na consulta de CEP.
- Permitir correção manual após o preenchimento automático.
- Garantir no máximo um endereço principal por pessoa.
- Não apagar histórico ou endereço usado por operações anteriores.
- Tratar indisponibilidade, formato inválido e CEP inexistente.

## Critérios de aceite
- [ ] CEP consulta e preenche os campos disponíveis.
- [ ] Número e complemento são armazenados.
- [ ] Uma pessoa pode possuir múltiplos endereços.
- [ ] Erros de CEP, duplicidade e endereço principal são tratados.
- [ ] Testes da API e lint do frontend passam.

## Condição de parada
Parar após validar busca de CEP e múltiplos endereços.
