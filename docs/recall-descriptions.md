# Descrições de recalls

`inventory/recalls` e `inventory/recall-items` aceitam `description` opcional
no cadastro e na edição. O limite é de 255 caracteres após remover espaços
nas extremidades. Texto vazio, somente espaços ou `null` é persistido como
`null`; valores não textuais e textos acima do limite retornam HTTP 400.

Na criação, omitir o campo equivale a `null`. Na edição, omiti-lo preserva o
valor atual para compatibilidade com clientes antigos; enviar `null` ou texto
vazio limpa a descrição. A edição mantém o controle de versão existente.

As colunas `erp_recall.description` e `erp_recall_item.description` são
`varchar(255)` anuláveis. A atualização de esquema existente do Hibernate
(`spring.jpa.hibernate.ddl-auto=update`) adiciona essas colunas sem exigir
preenchimento dos registros anteriores.

O catálogo da API expõe o campo editável aos formulários compartilhados. As
descrições aparecem no detalhe e na listagem, aceitam busca global e filtro
`filter.description`, e integram os snapshots de criação e edição da auditoria.

Validação: `ComplianceApiTests` cobre contratos, cadastro, edição, persistência,
busca, filtros, auditoria, limite e compatibilidade com payloads antigos.
