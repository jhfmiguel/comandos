# Política de dependências e licenças

## Objetivo
Evitar que uma dependência introduza custo obrigatório, restrição de redistribuição, copyleft incompatível ou risco jurídico inesperado.

## Regras
- Não adicionar dependência comercial/proprietária sem aprovação explícita.
- Preferir dependências permissivas e amplamente mantidas.
- Verificar licença antes de adicionar uma nova biblioteca.
- Não remover avisos/licenças exigidos por dependências.
- Não copiar código de exemplos/projetos externos sem verificar a licença.
- Não usar pacote abandonado em área crítica sem justificativa.
- Fixar versões quando necessário para builds reproduzíveis.

## Revisão mínima
Para cada dependência nova registrar:
- nome;
- versão;
- finalidade;
- licença;
- origem oficial;
- necessidade real;
- alternativa considerada quando houver risco comercial.

## Licenças que exigem revisão jurídica
Qualquer licença copyleft forte, source-available, dual-license, restrição comercial, uso não comercial ou termos personalizados deve ser revisada antes da adoção.

## Automação
O CI do COMANDOS deve continuar executando:
- auditoria de dependências;
- política de licenças;
- geração de SBOM quando configurada.

Uma dependência não deve ser aprovada apenas porque compila.
