# Validação da primeira versão — 12/09/2026

Esta rodada valida parte do núcleo de patrimônio e armamento. Não representa
homologação de todo o ERP nem liberação para produção.

## Resultados executados

| Verificação | Resultado |
| --- | --- |
| API: `./mvnw.cmd test` | 87 testes, zero falhas, erros ou testes ignorados; banco H2 em modo PostgreSQL |
| Interface: `npm.cmd run lint` | Zero erros e avisos |
| Interface: `npm.cmd run build` | Compilação e TypeScript aprovados; 21 páginas estáticas geradas |
| Navegador + PostgreSQL: cautela → devolução com dano → manutenção vinculada | Aprovado; estados persistidos conferidos pela API: CUSTODIED → BLOCKED → IN_MAINTENANCE |
| Abertura de 12 telas adicionais | Aprovada; sem exceções JavaScript nem respostas HTTP 5xx nas chamadas verificadas |
| Buscas concorrentes em usuários e armas | Aprovadas: resposta antiga retida pelo teste, nova busca concluída, requisição antiga cancelada e resultado mais recente preservado |

As 12 telas são núcleo institucional, estoque, vendas, consumo de munição,
doações, transferências, baixas, reservas, inventário físico, auditoria e as
consultas antigas de usuários e armas. Abertura de tela não valida todas as suas
operações de gravação.

## Correções desta rodada

- Estabilização dos serviços de usuários e armas, carregamento inicial das listas
  e cancelamento de consultas substituídas ou abandonadas ao sair da página.
- Remoção do efeito que atualizava a seleção do menu depois de cada navegação;
  a seleção agora acompanha a mudança de rota durante a renderização.
- Navegação de cautela para manutenção pelo roteador do Next.js.
- Tipagem dos eventos de entrada e erros HTTP, remoção de estado e argumentos
  sem uso e simplificação da propriedade redundante do campo monetário.
- Correção da apresentação inicial do manual, que ainda classificava como
  inexistentes diversos fluxos já implementados.

## Ambiente e reprodução do teste de navegador

Foi criado exclusivamente para esta validação o banco PostgreSQL
`wr_validation_20260912`. Os registros de teste permanecem nele para inspeção.
O banco habitual `weapons` não foi usado pelo teste. Na primeira rodada, o cenário
de cautela aprovado criou a organização 3 e o ativo 3.

O teste usa Microsoft Edge em modo headless e o Playwright instalado como
ferramenta temporária em `wr-api/target/browser-validation`. Não houve alteração
das dependências nem dos arquivos de lock da aplicação.

1. Crie um banco separado chamado `wr_validation_20260912` no PostgreSQL local,
   ou reutilize somente esse banco de validação. Configure as credenciais de
   desenvolvimento necessárias para acessá-lo.
2. Em `wr-api`, execute:

   ```powershell
   .\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.port=8180 --spring.datasource.url=jdbc:postgresql://localhost:5432/wr_validation_20260912 --erp.allowed-origin=http://localhost:3100 --spring.jpa.show-sql=false'
   ```

3. Em `wr-app`, execute `npm.cmd run build` e depois
   `npm.cmd run start -- --port 3100`.
4. Instale a ferramenta, se necessário, a partir de `wr-api`:

   ```powershell
   npm.cmd install --prefix target/browser-validation --no-package-lock --ignore-scripts playwright
   ```

5. Em `wr-app`, execute:

   ```powershell
   $env:NODE_PATH = (Resolve-Path ../wr-api/target/browser-validation/node_modules).Path
   node scripts/validate-browser.mjs
   ```

O script cria dados fictícios na API da porta 8180 e executa as operações de
cautela, devolução e abertura de manutenção pela interface da porta 3100. As
chamadas do build direcionadas à porta 8080 são encaminhadas pelo teste à 8180;
as respostas vêm da API real com PostgreSQL. O cenário usa o modo de preparação,
com login e permissões desativados. Autenticação e autorização foram verificadas
pela suíte da API em H2, não por este cenário de navegador. A segunda rodada
abaixo adiciona um cenário separado com acesso protegido.

## Segunda rodada — vendas e acesso protegido

Foi reproduzido um defeito na interface: após uma devolução gravada cuja resposta
se perdeu, clicar novamente em devolver gerava outro UUID e uma segunda devolução.
O teste observou duas devoluções quando deveria existir somente uma.

A interface agora preserva o pedido original, oferece repetição explícita e
bloqueia edição durante a incerteza. A proteção vale para devolução e cancelamento,
inclusive quando uma tentativa de recuperação recebe 4xx. A finalização de venda
também passou a preservar sua tentativa incerta após uma recuperação recusada.

O script `wr-app/scripts/validate-sales-access.mjs` passou com PostgreSQL, Edge
headless, autenticação obrigatória e permissões habilitadas:

| Cenário | Resultado |
| --- | --- |
| Acesso sem sessão e senha inválida | Acesso negado; formulário de login apresenta a rejeição |
| Login válido | Retorno à tela solicitada, rotação da sessão e cookie HttpOnly |
| Operador limitado a uma unidade | Lista somente o ativo permitido; detalhes de outra unidade/organização retornam 403 |
| Tentativa de ampliar escopo de vendas | Consultas sem a unidade obrigatória ou de outra unidade/organização retornam 403 |
| Administração de acessos e edição genérica de estoque pelo operador | Negadas |
| Venda de um ativo e 2,125 unidades de lote | Total 38,5800; saldo do lote reduzido de 10,1250 para 8,0000; ativo SOLD; operador registrado |
| Resposta de finalização perdida, tentativa seguinte recusada e nova recuperação | Uma única venda; mesmo identificador em todas as tentativas |
| Devolução maior que o saldo vendido | 409, nenhuma devolução gravada; formulário permite corrigir a quantidade |
| Devolução parcial de uma unidade de lote | Reembolso 12,3456 e saldo 9,0000 |
| Resposta da devolução perdida e recuperação intermediária recusada | Uma única devolução, campos bloqueados e mesmo identificador nas tentativas |
| Cancelamento do saldo com perda de resposta e repetição | Um único cancelamento, reembolso 26,2344; lote restaurado a 10,1250 e ativo AVAILABLE |
| Logout | Requisições posteriores sem sessão retornam 401 |
| Conta somente de consulta | Criação ausente na tela; tentativa de venda válida retorna 403 e não grava outra venda |
| Bloqueio de uma conta com sessão aberta | Próxima carga redireciona ao login; novo login recusado e API protegida retorna 401 |

O cenário final aprovado registrou organização 16, unidade 16, venda 6, operador
11 e leitor 12. As senhas de teste são geradas em memória. O leitor termina
bloqueado como parte da verificação. Compilação/TypeScript e lint também passaram
após a correção. A suíte da API não foi repetida nesta rodada; a execução anterior
de 87 testes permanece registrada acima.

### Reprodução com acesso protegido

Mantenha a API de preparação na porta 8180 e a interface na porta 3100 conforme
as instruções anteriores. Inicie outra instância da API, **no mesmo banco de
validação**, a partir de `wr-api`:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.port=8181 --spring.datasource.url=jdbc:postgresql://localhost:5432/wr_validation_20260912 --erp.allowed-origin=http://localhost:3100 --erp.security.require-login=true --erp.security.enforce-permissions=true --spring.jpa.show-sql=false --debug=false --logging.level.root=WARN --logging.level.org.springframework=WARN --logging.level.org.hibernate.SQL=OFF'
```

Em `wr-app`, com o Playwright temporário instalado:

```powershell
$env:NODE_PATH = (Resolve-Path ../wr-api/target/browser-validation/node_modules).Path
node scripts/validate-sales-access.mjs
```

A porta 8180 é usada somente para preparar os dados fictícios e conferir estados
persistidos. O navegador usa a API protegida da porta 8181, com cookies de sessão.
O script redireciona para essa porta as chamadas do build destinadas à 8080.
Perdas de resposta são simuladas depois de a API confirmar a gravação; as recusas
intermediárias são respostas 403 injetadas pelo teste. As verificações normais de
permissão usam respostas reais da API protegida. Esse ambiente local não valida
o comportamento de cookies em HTTPS ou em domínios distintos.

## Rodada de estoque e cautela — 13/09/2026

Validação executada com Edge headless, build de produção na porta 3100 e API
real na porta 8180, usando somente o PostgreSQL `wr_validation_20260912`.

| Verificação | Resultado |
| --- | --- |
| `npm.cmd run lint` | Aprovado, sem erros ou avisos |
| `npm.cmd run build` | Compilação, TypeScript e geração das páginas aprovados |
| `validate-stock-intake.mjs` | Pares patrimônio/série, quantidade automática, duplicidades, caixas de tamanhos distintos e cautela para unidade com devolução aprovados |
| `validate-stock-workflows.mjs` | Reserva e cancelamento, inventário com bloqueio de faltante e transferência de ativo e lote aprovados |

Os cenários verificaram recuperação após perda da resposta de uma gravação e
recusa intermediária simulada com HTTP 403: o pedido permaneceu idêntico e não
houve duplicação das operações. Também conferiram os saldos persistidos, a
reversão de ajuste incompatível com estoque reservado e o destino da transferência.
Nenhuma exceção JavaScript foi capturada pelas duas suítes.

Dados para inspeção: organização 22 no cadastro/cautela; organização 23,
reserva 3, inventário 2 e transferência 4 nos fluxos de estoque.

Para reproduzir, inicie as instâncias separadas conforme a seção de ambiente
e execute em `wr-app`:

```powershell
$env:NODE_PATH = (Resolve-Path ../wr-api/target/browser-validation/node_modules).Path
node scripts/validate-stock-intake.mjs
node scripts/validate-stock-workflows.mjs
```

Esta rodada usa o modo de preparação sem login; o HTTP 403 injetado testa
recuperação de falha, não substitui a validação de permissões reais.

## Próximas verificações para homologação

- Executar os demais fluxos completos no navegador com PostgreSQL: kits, doações,
  consumo, baixa/destruição e conclusão de manutenção.
- Validar expiração por inatividade, alterações de permissões durante a sessão,
  demais perfis e configuração de sessão na infraestrutura de produção.
- Validar operação simultânea de usuários, recuperação após falhas de rede e
  consistência transacional no PostgreSQL para os demais fluxos.
- Preparar configuração de produção, migrações controladas, backup/restauração,
  HTTPS e monitoramento.
- Concluir recuperação de senha, MFA e demais extensões funcionais previstas na
  arquitetura conforme o escopo da primeira entrega.

`git diff --check` também encontrou espaços finais preexistentes em
`PaymentMethod.java` e `components/sales/index.tsx`; não são falhas funcionais.
