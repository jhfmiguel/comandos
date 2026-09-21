# Manual de utilização do Weapons Registration ERP

## 1. Finalidade e situação atual

Situação da entrega de Armamento em 20/09/2026: consulte o
[relatório final da tarefa 023](armamento-023-final-validation.md). A homologação
integrada permanece pendente; resultados anteriores não certificam esta revisão.

O sistema reúne cadastro institucional, usuários e permissões, patrimônio e
estoque, especificações por família de equipamento, vendas e devoluções, cautela
de equipamentos e kits, consumo de munição, transferências, doações, reservas,
inventário físico, manutenção, baixa/destruição e auditoria.
As telas antigas de usuários, armas e vendas continuam disponíveis como recursos
de compatibilidade e não movimentam o estoque do novo ERP.

Cada movimentação possui sua própria tela e histórico no menu do ERP. Use o
fluxo correspondente à operação; não use uma venda ou alteração manual de
cadastro para simular consumo, transferência, doação, cautela ou baixa.

A devolução de cautela registra a condição de inspeção. Quando a condição bloqueia
um equipamento individual, o histórico permite abrir uma ordem de manutenção
vinculada à inspeção. Fotos, assinaturas, notificações e cadeias de aprovação
continuam pendentes. A existência dos fluxos não substitui a homologação antes
do uso operacional.

## 2. Iniciar o sistema

Abra dois terminais. No primeiro, inicie a API:

```powershell
cd C:\workspace\weapons-registration\wr-api
.\mvnw.cmd spring-boot:run
```

No segundo, inicie a interface:

```powershell
cd C:\workspace\weapons-registration\wr-app
npm.cmd run dev
```

Acesse `http://localhost:3000`. Por padrão, a API utiliza
`http://localhost:8080`. O desenvolvimento usa atualização automática de esquema
pelo Hibernate. Isso não garante uma migração de produção: faça backup e ensaie
a atualização e a restauração em uma cópia isolada antes de atualizar dados reais.

Se a interface exibir **Unable to reach the API** ou **Unable to complete the
request**, confirme se a API está em execução, se o frontend usa a URL correta em
`NEXT_PUBLIC_API_URL` e se `ERP_ALLOWED_ORIGIN` corresponde exatamente ao endereço
do navegador, normalmente `http://localhost:3000`.

## 3. Primeiro acesso e proteção

Na configuração inicial, login e permissões ficam desativados. Uma faixa informa
que o sistema está em modo de preparação. Cadastre primeiro uma organização,
uma pessoa e uma conta de acesso.

Para criar o administrador inicial em **Institutional core**:

1. Cadastre uma organização ativa em **Organizations**.
2. Cadastre uma pessoa ativa em **People**.
3. Cadastre a conta em **System users**, vinculando-a à pessoa. A senha deve ter
   pelo menos 12 caracteres.
4. Cadastre um perfil em **Access profiles**, com nível `SYSTEM`.
5. Cadastre uma permissão em **Permissions**, usando recurso `*` e ação `*`.
6. Ligue permissão e perfil em **Profile permissions**.
7. Em **User profiles**, atribua o perfil ao usuário e à organização, deixando a
   unidade vazia.
8. Pare a API, habilite login e permissões e inicie-a novamente:

```powershell
$env:ERP_REQUIRE_LOGIN = 'true'
$env:ERP_ENFORCE_PERMISSIONS = 'true'
.\mvnw.cmd spring-boot:run
```

Use `/login` para entrar. Alterar ou bloquear a conta invalida as sessões abertas.
Contas com MFA marcado não entram enquanto o fluxo de MFA não for implementado.

## 4. Organizações, unidades e pessoas

Em **Institutional core**, use a ordem abaixo para evitar referências ausentes:

1. **Organizations**: informe nome, natureza, sigla e identificação fiscal quando
   aplicável. Somente organizações ativas participam dos acessos.
2. **Organizational units**: escolha a organização, código, nome e tipo. A unidade
   superior é opcional e deve pertencer à mesma organização.
3. **People**: registre pessoa física ou jurídica, identificação e contato.
4. **Person roles** e **Person role assignments**: definem papéis funcionais,
   período, organização, unidade e situação.
5. **Credentials** e **Qualifications**: registram documentos e habilitações com
   suas validades e situações.

Use **New**, preencha os campos obrigatórios marcados com `*` e selecione **Save**.
Para editar, abra o registro pela lista. Se outra alteração tiver sido salva desde
a abertura, recarregue quando o sistema informar conflito de versão. Registros
referenciados por outros cadastros não podem ser excluídos.

## 5. Perfis e permissões

| Nível | Alcance |
| --- | --- |
| `SYSTEM` | Todas as organizações; unidade vazia |
| `ORGANIZATION` | Uma organização inteira; unidade vazia |
| `UNIT` | Somente a unidade selecionada |

Recursos e ações diferenciam maiúsculas de minúsculas. As ações normais são
`READ`, `CREATE`, `UPDATE` e `DELETE`. `security/access` com `MANAGE` administra
contas, perfis e permissões. O valor completo `*` é curinga; valores parciais como
`inventory/*` não são reconhecidos.

Um operador de vendas por organização normalmente precisa de:

- `sales`: READ e CREATE no nível ORGANIZATION.
- `core/organizations`: READ no nível ORGANIZATION.
- `core/people`: READ no nível SYSTEM para escolher compradores.
- `core/units`: READ no nível ORGANIZATION caso escolha unidades nas vendas.

Para limitar vendas a uma unidade, atribua `sales` READ/CREATE e `core/units` READ
no nível UNIT. O operador deverá selecionar explicitamente essa unidade.

## 6. Catálogo, armas e estoque

Em **Assets and inventory**, cadastre nesta ordem:

1. **Item categories**: para armas, selecione família `FIREARM`, marque
   **Serialized** e não marque consumível.
2. **Brands**: registre marca e fabricante.
3. **Item models**: escolha categoria e marca, informe nome, unidade de medida,
   SKU e preço de lista.
4. **Firearm specifications**: escolha o modelo de arma e informe calibre,
   mecanismo, capacidade e comprimento do cano em milímetros.
5. **Stock locations**: vincule o depósito à organização e, opcionalmente, unidade.
6. **Individual assets**: escolha modelo e local, informe código patrimonial,
   número de série, condição, disponibilidade e valor atual.

Cada modelo `FIREARM` aceita uma especificação. Capacidade deve ser um inteiro
positivo e comprimento do cano deve ser maior que zero. A especificação não pode
ser vinculada a uma categoria de outra família.

Use **Technical characteristics**, **Category characteristics**, **Model
characteristics** e **Asset characteristics** para requisitos adicionais. Uma
característica obrigatória de modelo precisa ser preenchida antes de abrir o
estoque. Quando uma característica individual for obrigatória, crie o ativo como
`DRAFT`, preencha o valor e só então altere-o para `AVAILABLE`.

Consumíveis e materiais controlados por quantidade usam **Stock lots**. Ao abrir
um lote, o sistema cria automaticamente o saldo e o movimento `OPENING`. Ativos
individuais também recebem um movimento `OPENING` de quantidade um. **Stock
balances** e **Stock movements** são históricos somente para leitura.

## 7. Realizar uma venda de estoque

Abra **Inventory sales**:

1. Selecione a organização.
2. Se a venda pertencer a uma unidade, selecione-a. Trocar organização ou unidade
   limpa o carrinho para evitar mistura de escopos.
3. Selecione o comprador e o método de pagamento.
4. Em **Available stock**, escolha ativo individual ou lote, procure pelo código,
   SKU ou modelo e clique em **Add**.
5. Para lotes, ajuste a quantidade sem ultrapassar o disponível. Ativos usam
   quantidade um.
6. Confira os subtotais e clique em **Finalize**.

A API confirma preço, validade, organização, unidade e disponibilidade. Uma venda
concluída marca o ativo como `SOLD` ou reduz saldo e lote, cria movimento `SALE`
negativo e grava tudo na mesma transação. Se qualquer item falhar, nenhuma baixa
é mantida.

Quando houver dúvida por falha de rede, use **Retry finalization**. O mesmo
identificador recupera o resultado sem duplicar a baixa. Depois do sucesso, use
**+ Sale** para limpar o formulário e iniciar outra venda.

### 7.1 Devolver itens ou cancelar o saldo da venda

1. Em **Sales history**, abra o comprovante.
2. Em **Return reason**, escolha o motivo cadastrado.
3. Preencha **Notes** com a justificativa e as condições da devolução.
4. Informe **Refund reference** quando houver número de estorno, transação ou lançamento contábil.
5. Para devolução parcial, informe **Return quantity** e clique em **Return selected items**.
6. Para desfazer tudo que ainda resta na venda, clique em **Cancel remaining sale**.

| Campo | Significado |
| --- | --- |
| Return reason | Motivo selecionado no cadastro de tipos de motivo |
| Notes | Justificativa detalhada e condição dos itens recebidos |
| Refund reference | Identificador do estorno financeiro realizado externamente |
| Remaining | Quantidade vendida que ainda não foi devolvida |
| Return quantity | Quantidade desta operação de devolução |
| Refund | Valor calculado pelo preço unitário original da venda |

O comprovante original permanece `FINALIZED` e passa a exibir o histórico de devoluções e cancelamentos. Um equipamento válido retorna a `AVAILABLE`; impedimentos de validade, recall ou ausência em inventário mantêm `BLOCKED`. O sistema registra o valor do reembolso, mas não executa transações bancárias.

Se a resposta da devolução ou do cancelamento se perder, use **Retry return** ou
**Retry cancellation**. A tela mantém a operação original e bloqueia alterações
nos campos até confirmar o resultado, evitando uma segunda entrada no estoque.
Uma tentativa de recuperação recusada não libera a criação de outra operação.
Se você sair da página ou recarregá-la, consulte o histórico antes de registrar
uma nova devolução: a tentativa pendente é mantida somente enquanto a tela está
aberta. Uma quantidade recusada na primeira tentativa pode ser corrigida nos
campos e enviada novamente.

Os motivos são cadastrados em **Assets and inventory → Reference data → Sale return reason types**.

Vendas sem unidade são da organização inteira. Vendas com unidade aceitam apenas
estoque daquela unidade. O histórico filtrado pela organização mostra todas as
suas vendas; o filtro de unidade mostra somente vendas registradas explicitamente
naquela unidade.

## 8. Consultar auditoria

Abra **Audit history**. É necessária a permissão `audit` READ no nível SYSTEM.
Filtre por recurso, identificador do registro, operação, login ou período. Abra
**View** para comparar os estados anterior e posterior.

A auditoria registra alterações bem-sucedidas nos cadastros do core e inventário,
além da finalização de vendas e seus efeitos no estoque. Senhas, hashes e tokens
não são armazenados nos snapshots. Operações que falham não geram evento.

## 9. Mensagens frequentes

| Mensagem ou situação | Ação recomendada |
| --- | --- |
| Sem permissão para a operação ou escopo | Confira perfil, recurso, ação, organização e unidade do usuário |
| Registro alterado; recarregue antes de salvar | Reabra o registro e refaça a alteração sobre a versão atual |
| Preço alterado | Remova o item da venda, atualize o estoque e adicione novamente |
| Estoque insuficiente ou indisponível | Atualize a busca; outro processo pode ter consumido o saldo |
| Unidade não pertence à organização | Corrija a unidade ou a organização selecionada |
| Modelo não pertence à família FIREARM | Corrija a categoria antes de criar a especificação |
| API inacessível | Inicie o backend e confira URL e origem permitida |

## 10. Encerramento seguro

Use **Sign out** no menu. Não compartilhe contas entre operadores: o usuário da
sessão aparece no comprovante da venda e na auditoria. Para alterações de acesso,
prefira perfis específicos por função e conceda apenas os recursos e ações usados
na rotina daquele operador.

## 11. Procedimento padrão para todos os cadastros do ERP

As páginas **Institutional core** e **Assets and inventory** usam o mesmo fluxo:

1. Abra o módulo pelo menu lateral.
2. No seletor de recurso, escolha o cadastro desejado.
3. Use **Search** para localizar registros existentes antes de criar outro.
4. Clique em **New**. Se o botão não aparecer, a conta não possui `CREATE`.
5. Preencha todos os campos marcados com `*`. Nos campos de relacionamento,
   digite parte do nome e escolha um resultado da lista.
6. Clique em **Save** e confira a mensagem de sucesso e a nova linha na lista.
7. Para alterar, use **Edit**, modifique os campos liberados e clique em **Save**.
8. Para excluir, use **Delete** e confirme. A exclusão falha quando outro registro
   ainda depende daquele cadastro ou quando uma regra exige um fluxo específico.

Datas usam o formato apresentado pelo navegador. Campos decimais aceitam até
quatro casas. Campos de relacionamento terminados em `Id` na API aparecem na tela
como seletores pelo nome do registro.

## 12. Passo a passo de cada cadastro institucional

### 12.1 Organizations — organizações

Dependências: nenhuma. Abra **Institutional core**, escolha **Organizations**,
clique em **New**, preencha os campos e salve.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Nature | Sim | Natureza jurídica ou administrativa, como empresa privada, órgão público ou associação |
| Name | Sim | Nome oficial da organização |
| Acronym | Não | Sigla usada para identificação curta |
| Tax ID (CNPJ) | Não | CNPJ ou identificador fiscal da organização |
| Public organization | Sim | Indica se a organização pertence ao setor público |
| Active | Sim | Permite que a organização seja usada em novos vínculos e concessões de acesso |

### 12.2 Organizational units — unidades organizacionais

Dependência: uma organização. Escolha **Organizational units**, clique em **New**,
selecione **Organization**, preencha código, nome e tipo e salve.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Organization | Sim | Organização proprietária da unidade |
| Parent unit | Não | Unidade imediatamente superior na hierarquia; deve pertencer à mesma organização |
| Code | Sim | Código único da unidade dentro da organização |
| Name | Sim | Nome completo da unidade |
| Type | Sim | Classificação livre, como departamento, filial, batalhão ou depósito |

Uma unidade não pode ser sua própria superior nem formar ciclos. A organização
não pode ser alterada quando os vínculos existentes ficariam inconsistentes.

### 12.3 People — pessoas

Dependências: nenhuma. Escolha **People**, clique em **New**, defina o tipo da
pessoa, seus dados e salve.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Person type | Sim | `INDIVIDUAL` para pessoa física ou `LEGAL_ENTITY` para pessoa jurídica |
| Full name / legal name | Sim | Nome completo ou razão social |
| Tax ID (CPF / CNPJ) | Não | CPF da pessoa física ou CNPJ da pessoa jurídica |
| Birth date | Não | Data de nascimento; normalmente usada para pessoa física |
| Address | Não | Endereço de contato |
| Phone | Não | Telefone de contato |
| Email | Não | Endereço eletrônico em formato válido |
| Active | Sim | Define se a pessoa pode ser usada em operações novas, como uma venda |

### 12.4 Person roles — tipos de papel

Dependências: nenhuma. Escolha **Person roles**, clique em **New**, informe código
e nome e salve. Exemplos: comprador, armeiro, autorizador e servidor.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Code | Sim | Código estável do papel, como `ARMORER` |
| Name | Sim | Nome legível do papel, como `Armorer` |

### 12.5 Person role assignments — atribuições de papéis

Dependências: pessoa, tipo de papel e organização; unidade é opcional. Este
cadastro informa onde e por quanto tempo uma pessoa exerce determinado papel.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Person | Sim | Pessoa que recebe o papel |
| Role | Sim | Tipo de papel atribuído |
| Organization | Sim | Organização na qual o papel é exercido |
| Unit | Não | Unidade específica do vínculo; deve pertencer à organização escolhida |
| Start date | Sim | Primeiro dia de vigência |
| End date | Não | Último dia de vigência; deve ser igual ou posterior ao início |
| Status | Sim | `ACTIVE`, `SUSPENDED` ou `ENDED` |

### 12.6 Role details — detalhes do papel

Dependência: uma atribuição de papel. Escolha **Role details** e registre pares de
chave e valor quando um papel precisar de informações adicionais.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Person role assignment | Sim | Vínculo de pessoa/papel ao qual o detalhe pertence |
| Key | Sim | Nome técnico da informação, como `badgeNumber` |
| Value | Sim | Conteúdo correspondente à chave |

### 12.7 Credentials — credenciais da pessoa

Dependência: uma pessoa. Escolha **Credentials**, clique em **New**, selecione a
pessoa, informe o documento e sua validade e salve.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Person | Sim | Titular da credencial |
| Type | Sim | Tipo do documento ou credencial |
| Number | Sim | Número identificador da credencial |
| Valid until | Sim | Data final de validade |

### 12.8 Qualifications — habilitações da pessoa

Dependência: uma pessoa. Use para registrar habilitações, treinamentos ou
autorizações pessoais relevantes à operação.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Person | Sim | Pessoa habilitada |
| Category | Sim | Categoria ou nome da habilitação |
| Valid until | Sim | Data final da validade |
| Status | Sim | `ACTIVE`, `SUSPENDED` ou `REVOKED` |

### 12.9 System users — usuários de acesso

Dependência: uma pessoa. Este cadastro cria a conta que entra no sistema; ele não
é o mesmo cadastro legado de usuários.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Person | Sim | Pessoa real associada à conta e exibida na sessão |
| Login | Sim | Nome único usado na autenticação; é normalizado para letras minúsculas |
| Password | Sim na criação | Senha de acesso, com no mínimo 12 caracteres e no máximo 72 bytes; nunca é exibida novamente |
| Blocked | Sim | Impede imediatamente a autenticação e invalida sessões existentes |

Ao editar, deixe **Password** vazia para manter a senha atual. Administrar este
cadastro exige `security/access` com ação `MANAGE` no nível SYSTEM.

### 12.10 Access profiles — perfis de acesso

Dependências: nenhuma. Crie um perfil para reunir permissões que serão atribuídas
a usuários.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Name | Sim | Nome funcional do perfil, como `Unit sales operator` |
| Level | Sim | Escopo reconhecido: `SYSTEM`, `ORGANIZATION` ou `UNIT` |

### 12.11 Permissions — permissões

Dependências: nenhuma. Cada registro representa uma combinação exata de recurso
e ação. Depois, vincule-o a um perfil.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Resource | Sim | Código protegido, como `core/people`, `inventory/assets`, `inventory/firearm-specifications`, `sales`, `audit` ou `security/access` |
| Action | Sim | `READ`, `CREATE`, `UPDATE`, `DELETE`, `MANAGE` ou `*` |

Use `*` como valor completo para acesso global. Não use padrões parciais.

### 12.12 Profile permissions — permissões do perfil

Dependências: perfil e permissão. Repita o procedimento para cada permissão que o
perfil deve possuir.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Profile | Sim | Perfil que receberá a capacidade |
| Permission | Sim | Combinação de recurso e ação concedida ao perfil |

### 12.13 User profiles — perfis do usuário

Dependências: usuário, perfil e organização; unidade depende do nível. Este é o
passo que efetivamente concede o perfil à conta.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| User | Sim | Conta que receberá o perfil |
| Profile | Sim | Perfil de acesso concedido |
| Organization | Sim | Organização da atribuição; também deve estar ativa em perfis SYSTEM |
| Unit | Para nível UNIT | Unidade exata do acesso; deixe vazia em SYSTEM e ORGANIZATION |

## 13. Passo a passo de cada cadastro de inventário e armamento

### 13.1 Item categories — categorias de itens

Dependências: apenas uma categoria superior opcional. Escolha **Item categories**,
clique em **New**, configure família e forma de controle e salve.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Parent category | Não | Categoria superior na hierarquia |
| Name | Sim | Nome da categoria |
| Equipment family | Sim | Família: `GENERAL`, `FIREARM`, `AMMUNITION`, `GRENADE`, `SPRAY`, `BALLISTIC_PROTECTION`, `ELECTRICAL_DEVICE` ou `OPTICAL` |
| Serialized | Sim | Controla cada unidade separadamente por ativo e número de série |
| Lot controlled | Sim | Controla entrada e saldo por lote |
| Consumable | Sim | Indica material consumido por quantidade |

Para armas, normalmente use `FIREARM`, **Serialized** ativo, **Consumable**
desativado. Para munição, use `AMMUNITION`, controle por lote e consumível. As
regras de rastreamento e família não mudam depois que modelos usam a categoria.

### 13.2 Brands — marcas

Dependências: nenhuma. Escolha **Brands**, clique em **New**, informe nome e
fabricante e, opcionalmente, o país de fabricação declarado, e salve.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Name | Sim | Nome comercial da marca |
| Manufacturer | Sim | Empresa fabricante |
| Manufacturing country | Não | País de fabricação declarado da marca, código ISO de duas letras; não determina a origem de cada exemplar |

### 13.3 Item models — modelos de item

Dependências: categoria e marca. Este cadastro descreve o produto; cada exemplar
físico será criado posteriormente como ativo ou lote.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Category | Sim | Categoria que define família e forma de rastreamento |
| Brand | Sim | Marca do modelo |
| Name | Sim | Nome comercial do modelo |
| Unit of measure | Sim | Unidade usada nas quantidades, como `EA` ou `ROUND` |
| Manufacturer code | Não | Referência de catálogo/peça do modelo fornecida pelo fabricante; diferente do SKU interno e do número de série |
| SKU | Sim | Código único de catálogo usado nas buscas |
| Description | Não | Descrição complementar |
| List price | Sim | Preço unitário usado pelo backend nas vendas |

### 13.4 Firearm specifications — especificações de arma de fogo

Dependência: um modelo cuja categoria seja `FIREARM`. Escolha **Firearm
specifications**, clique em **New**, selecione o modelo e preencha:

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Firearm model | Sim | Modelo de arma descrito; fica fixo depois do cadastro |
| Caliber | Sim | Designação do calibre, como `9x19 mm` ou `.40 S&W` |
| Operating mechanism | Sim | Mecanismo: tiro único, ferrolho, alavanca, bomba, semiautomático, automático ou revólver |
| Capacity | Sim | Quantidade inteira positiva suportada pelo armamento |
| Barrel length (mm) | Sim | Comprimento positivo do cano em milímetros, com até quatro casas decimais |

Cada modelo aceita apenas uma especificação. Para corrigir o modelo vinculado,
exclua a especificação antes que outros vínculos impeçam a operação e cadastre-a
novamente no modelo correto.

### 13.5 Ammunition specifications — especificações de munição

Use este cadastro depois de criar um modelo cuja categoria tenha **Equipment family** igual a `AMMUNITION`.

1. Abra **Assets and inventory**.
2. Selecione **Ammunition specifications**.
3. Clique em **New**.
4. Preencha os campos e salve.

- **Ammunition model**: modelo de munição ao qual a ficha técnica pertence. Não pode ser trocado depois do cadastro.
- **Caliber**: calibre nominal, por exemplo `9x19 mm`.
- **Lethality classification**: selecione `LETHAL` ou `LESS_LETHAL`. O calibre não determina sozinho essa classificação; modelos diferentes podem usar `12 GA` com classificações distintas.
- **Projectile type**: construção ou finalidade do projétil, por exemplo `Full metal jacket`.
- **Case type**: material ou tipo do estojo, por exemplo `Brass`.
- **Primer type**: sistema de espoleta, por exemplo `Boxer`.

Cada modelo aceita uma única ficha de munição. Para corrigir outro campo, edite a ficha existente.

### 13.6 Grenade specifications — especificações de granada

Use este cadastro para um modelo cuja categoria tenha **Equipment family** igual a `GRENADE`.

1. Abra **Assets and inventory** e selecione **Grenade specifications**.
2. Clique em **New**, selecione o modelo e preencha a ficha.
3. Clique em **Save**.

- **Grenade model**: modelo ao qual a ficha pertence; fica imutável depois do cadastro.
- **Grenade type**: classificação técnica ou operacional da granada.
- **Agent**: agente ou carga empregada, inclusive `INERT` quando aplicável.
- **Delay time (seconds)**: retardo positivo, em segundos.
- **Safety radius (m)**: raio de segurança positivo, em metros.

### 13.7 Spray specifications — especificações de espargidor

Use este cadastro para um modelo cuja categoria tenha **Equipment family** igual a `SPRAY`.

1. Abra **Assets and inventory** e selecione **Spray specifications**.
2. Clique em **New**, selecione o modelo e informe os dados técnicos.
3. Clique em **Save**.

- **Spray model**: modelo do espargidor; fica imutável depois do cadastro.
- **Agent**: agente empregado, como `OC` ou `CS`.
- **Concentration (%)**: concentração maior que zero e limitada a 100%.
- **Volume (mL)**: volume positivo do recipiente em mililitros.
- **Range (m)**: alcance positivo informado pelo fabricante em metros.

### 13.8 Ballistic protection specifications — especificações de proteção balística

Use este cadastro para um modelo cuja categoria tenha **Equipment family** igual a `BALLISTIC_PROTECTION`.

1. Abra **Assets and inventory** e selecione **Ballistic protection specifications**.
2. Clique em **New**, selecione o modelo e preencha todos os campos.
3. Clique em **Save**.

- **Ballistic protection model**: modelo de colete, placa, capacete ou outra proteção; fica imutável depois do cadastro.
- **Protection type**: formato ou aplicação do equipamento, como `VEST`, `PLATE` ou `HELMET`.
- **Protection level**: nível declarado na documentação aplicável ao produto.
- **Material**: material predominante, como aramida, polietileno ou cerâmica.
- **Certification**: identificação do certificado ou relatório que sustenta o nível informado.

### 13.9 Electrical device specifications — especificações de dispositivo elétrico

Use este cadastro para um modelo cuja categoria tenha **Equipment family** igual a `ELECTRICAL_DEVICE`.

1. Abra **Assets and inventory** e selecione **Electrical device specifications**.
2. Clique em **New**, selecione o modelo e preencha a ficha.
3. Clique em **Save**.

- **Electrical device model**: modelo do dispositivo; fica imutável depois do cadastro.
- **Voltage (V)**: tensão nominal positiva informada para o modelo.
- **Cycles**: quantidade inteira e positiva de ciclos suportados.
- **Cartridge type**: tipo de cartucho compatível com o dispositivo.

### 13.10 Optical specifications — especificações de equipamento óptico

Use este cadastro para um modelo cuja categoria tenha **Equipment family** igual a `OPTICAL`.

1. Abra **Assets and inventory** e selecione **Optical specifications**.
2. Clique em **New**, selecione o modelo e preencha a ficha.
3. Clique em **Save**.

- **Optical model**: modelo do equipamento óptico; fica imutável depois do cadastro.
- **Optical type**: tipo do equipamento, como mira, luneta ou monóculo.
- **Maximum magnification**: ampliação máxima positiva, por exemplo `8` para 8×.
- **Night vision**: informa se o modelo possui visão noturna.
- **Thermal vision**: informa se o modelo possui visão termal.

### 13.11 Regulatory controls — registros regulatórios

Dependência: um ativo individual cuja categoria seja `FIREARM`. Escolha
**Regulatory controls**, clique em **New**, selecione a arma e preencha os dados
conforme o comprovante emitido pelo órgão ou sistema externo.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Firearm asset | Sim | Arma individual à qual o registro pertence; fica fixa depois da criação |
| External system | Sim | Nome ou código do sistema/órgão que mantém o registro; fica fixo |
| Registration number | Sim | Número oficial da arma naquele sistema |
| Registration status | Sim | `PENDING`, `ACTIVE`, `SUSPENDED`, `CANCELLED` ou `EXPIRED` |
| Valid until | Não | Último dia de validade, quando o registro possuir vencimento |

Cada arma aceita um registro por sistema externo. O mesmo sistema não aceita o
mesmo número para duas armas. Um registro vencido não pode permanecer `ACTIVE`, e
uma arma vendida não aceita criação ou alteração de registro. O acesso acompanha
a organização e a unidade do local atual do ativo.

### 13.12 Technical characteristics — características técnicas

Dependências: nenhuma. Use para criar atributos adicionais que não possuem campo
dedicado, como peso, acabamento ou tipo de mira.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Name | Sim | Nome da característica |
| Data type | Sim | `TEXT`, `DECIMAL`, `INTEGER`, `BOOLEAN` ou `DATE` |
| Unit of measure | Não | Unidade do valor, como `kg`, `mm` ou `J` |

### 13.13 Category characteristics — características exigidas pela categoria

Dependências: categoria e característica técnica. Este cadastro define onde o
valor será informado e se será obrigatório.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Category | Sim | Categoria à qual a regra se aplica; fica fixa após a criação |
| Characteristic | Sim | Característica utilizada; fica fixa após a criação |
| Required value | Sim | Obriga o preenchimento antes de disponibilizar estoque |
| Per individual item | Sim | Ativo: valor diferente em cada ativo; desativado: valor comum ao modelo |

Valores por item exigem categoria serializada. A configuração é aplicada à
categoria selecionada diretamente; não é herdada automaticamente pelas filhas.

### 13.14 Model characteristics — valores do modelo

Dependências: modelo e uma característica configurada para sua categoria com
**Per individual item** desativado.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Model | Sim | Modelo que recebe o valor; fica fixo após a criação |
| Characteristic | Sim | Característica preenchida; fica fixa após a criação |
| Value | Sim | Valor no tipo definido pela característica |

Exemplos válidos: `850` para INTEGER, `3.7500` para DECIMAL, `true` para BOOLEAN
e `2027-12-31` para DATE.

### 13.15 Stock locations — locais de estoque

Dependências: organização e unidade opcional. Cadastre o local antes de registrar
um ativo ou abrir um lote.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Organization | Sim | Proprietária do local; não pode ser alterada posteriormente |
| Organizational unit | Não | Unidade responsável; deve pertencer à organização |
| Name | Sim | Nome do depósito, sala ou cofre |
| Type | Sim | Tipo livre do local, como `Warehouse` ou `Armory` |
| Controlled location | Sim | Indica que o local possui controle especial |

### 13.16 Individual assets — itens patrimoniais individuais

Dependências: modelo individual e local de estoque. Para armas, o número de série
é obrigatório porque a categoria deve ser serializada.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Model | Sim | Modelo do exemplar; fica fixo depois da criação |
| Location | Sim | Local inicial do exemplar; fica fixo nesta etapa |
| Asset code | Sim | Código patrimonial global e único; fica fixo |
| Serial number | Para categoria serializada | Número de série, único dentro do modelo; fica fixo |
| Condition | Sim | `NEW`, `GOOD`, `NEEDS_INSPECTION` ou `DAMAGED` |
| Availability | Sim | `DRAFT`, `AVAILABLE` ou `BLOCKED`; `SOLD` é definido somente pela venda |
| Valid until | Não | Data de validade do item, quando aplicável |
| Current value | Sim | Valor patrimonial atual, distinto do preço de lista |

Ao salvar, é criado um movimento `OPENING` de quantidade um. Um ativo vencido não
pode ficar `AVAILABLE`. Ativos vendidos não podem ser reativados pelo cadastro.

### 13.16.1 Equipment sets — conjuntos e kits de equipamentos

Use este cadastro para reunir armas, ópticos, proteções, acessórios e materiais
controlados por quantidade sob um único código.

1. Abra **Assets and inventory** e selecione **Equipment sets**.
2. Clique em **New**.
3. Preencha os campos e mantenha **Active** desativado.
4. Salve o conjunto.
5. Selecione **Equipment set components**, clique em **New** e cadastre cada componente.
6. Volte a **Equipment sets**, edite o conjunto e ative **Active**.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Organization | Sim | Organização proprietária do conjunto; fica fixa após a criação |
| Organizational unit | Não | Unidade responsável; fica fixa e deve pertencer à organização |
| Set code | Sim | Código único do conjunto dentro da organização; fica fixo |
| Name | Sim | Nome pelo qual o conjunto será identificado |
| Description | Não | Finalidade ou observações sobre a composição |
| Active | Sim | Indica que a composição está concluída e disponível para operações futuras |

Um conjunto somente pode ser ativado depois de receber pelo menos um componente.
Para alterar a composição, primeiro desative o conjunto.

### 13.16.2 Equipment set components — componentes do conjunto

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Equipment set | Sim | Conjunto ao qual o componente pertence; fica fixo |
| Individual asset | Condicional | Equipamento individual identificado pelo código patrimonial |
| Stock balance | Condicional | Lote e local dos quais a quantidade faz parte |
| Component role | Sim | Função no conjunto, como `Primary weapon`, `Optical sight` ou `Ammunition` |
| Quantity | Sim | `1` para ativo individual ou quantidade positiva do saldo selecionado |

Escolha exatamente um dos campos **Individual asset** e **Stock balance**. O item
deve pertencer à organização e, quando informada, à unidade do conjunto. Um ativo
individual pode integrar somente um conjunto ativo. A composição do kit não
reserva nem movimenta estoque.

Depois de ativado, um conjunto completo e disponível aparece em **Equipment
custody → Available equipment sets**. Clique em **Add set** e finalize a cautela.
O sistema entrega todos os componentes em uma única operação. Ativos individuais
passam para `CUSTODIED`; quantidades de lote são retiradas do saldo indicado no
componente. Na devolução, use **Return set**: todos os componentes ainda pendentes
do conjunto são devolvidos juntos e os saldos são restaurados.

Antes de devolver, selecione **Return condition** e, se necessário, preencha
**Inspection notes**. **Good** libera equipamentos válidos e devolve quantidades
ao saldo disponível. **Needs inspection** e **Damaged** colocam ativos e
quantidades no saldo bloqueado. O histórico mostra o resultado e as observações.

As opções ficam em **Assets and inventory → Reference data → Custody return
condition types**:

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Code | Sim | Código estável em letras maiúsculas, números e sublinhado |
| Name | Sim | Nome apresentado durante a devolução |
| Description | Não | Orientação para o operador sobre quando usar o resultado |
| Active | Sim | Permite selecionar a opção em novas devoluções |
| Blocks availability | Sim | Envia o item devolvido para `BLOCKED` ou saldo `blocked` |
| Display order | Sim | Ordem da opção na pesquisa |
| System protected | Calculado | Impede excluir os resultados essenciais do sistema |

### 13.17 Asset characteristics — valores por ativo

Dependências: ativo e característica configurada com **Per individual item**
ativado para a categoria daquele ativo.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Asset | Sim | Exemplar que recebe o valor; fica fixo |
| Characteristic | Sim | Característica preenchida; fica fixa |
| Value | Sim | Valor conforme o tipo configurado |

Se o valor for obrigatório, mantenha o ativo em `DRAFT`, cadastre a característica
e depois altere o ativo para `AVAILABLE`.

### 13.18 Stock lots — lotes de estoque

Dependências: modelo controlado por lote e local de abertura. Use para munições e
outros materiais por quantidade; não use para uma arma individual serializada.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Model | Sim | Modelo controlado por lote; fica fixo |
| Opening location | Sim | Local no qual o lote entra; fica fixo |
| Lot number | Sim | Identificador do lote naquele modelo/local; fica fixo |
| Opening quantity | Sim | Quantidade inicial positiva, com até quatro casas; fica fixa |
| Available quantity | Calculado | Total ainda disponível no lote em todos os locais |
| Valid until | Não | Validade do lote |

Salvar cria automaticamente o saldo no local e o movimento `OPENING`. Lotes e sua
quantidade inicial não são excluídos ou corrigidos pelo CRUD comum.

### 13.19 Stock balances — saldos de estoque

Este recurso é somente leitura e não possui botão **New**.

| Campo | Significado |
| --- | --- |
| Lot | Lote ao qual o saldo pertence |
| Location | Local onde a quantidade está registrada |
| Available | Quantidade livre para operação |
| Reserved | Quantidade separada por futuras reservas |
| Blocked | Quantidade impedida de uso |

O saldo nasce na abertura do lote e é reduzido automaticamente nas vendas.

### 13.20 Stock movements — movimentações de estoque

Este recurso também é somente leitura. Ele constitui o histórico quantitativo.

| Campo | Significado |
| --- | --- |
| Asset | Ativo individual movimentado, quando aplicável |
| Lot | Lote movimentado, quando aplicável |
| Location | Local afetado pela movimentação |
| Nature | Origem da mudança, como `OPENING`, `SALE`, `CUSTODY_ISSUE`, `CUSTODY_RETURN`, `AMMUNITION_CONSUMPTION`, `DONATION`, `TRANSFER_OUT` ou `TRANSFER_IN` |
| Quantity | Entrada positiva ou saída negativa |
| Recorded at | Data e hora registradas pelo servidor |

## 14. Operações que não usam o botão New

### 14.1 Inventory sales

Siga o procedimento completo da seção 7. A venda é finalizada pelo botão
**Finalize**, pois estoque, movimento, comprovante e auditoria precisam ser
gravados juntos. Ela não pode ser editada ou excluída pelo cadastro comum.

### 14.2 Audit history

Siga a seção 8. Auditoria é somente leitura; eventos são criados automaticamente
pelas operações bem-sucedidas.

### 14.3 Firearm custody — cautela de armamentos

Antes de começar, cadastre organização, unidade, recebedor, autorizador, modelo
`FIREARM`, local e armas individuais `AVAILABLE`. Abra **Firearm custody**:

1. Selecione **Organization**, que será responsável pela cautela e pelo estoque.
2. Se aplicável, selecione **Unit**. Trocar organização ou unidade limpa as armas.
3. Em **Recipient**, escolha quem ficará responsável pelo armamento.
4. Em **Authorizer**, escolha a pessoa que autorizou a entrega.
5. Em **Purpose**, descreva a finalidade da cautela, em até 255 caracteres.
6. Em **Due date and time**, informe a devolução prevista, se houver. A data deve
   estar no futuro.
7. Em **Available firearms**, procure por código, série ou modelo e clique **Add**.
8. Revise **Selected firearms** e clique em **Issue custody**.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Organization | Sim | Organização proprietária do estoque e responsável pela cautela |
| Unit | Não | Unidade exata da cautela e das armas; necessária para perfil UNIT |
| Recipient | Sim | Pessoa que recebe e passa a responder pelas armas |
| Authorizer | Sim | Pessoa que autorizou a entrega |
| Purpose | Sim | Motivo ou emprego autorizado |
| Due date and time | Não | Prazo previsto para devolução |
| Selected firearms | De 1 a 100 | Armas individuais entregues juntas |

Após a entrega, a arma passa de `AVAILABLE` para `CUSTODIED` e recebe movimento
`CUSTODY_ISSUE` negativo. Em **Custody history**, clique em **Return** para uma
arma ou **Return all pending** para todas. A devolução parcial gera
`PARTIALLY_RETURNED`; a última gera `RETURNED`. Cada retorno cria movimento
`CUSTODY_RETURN` positivo. Arma válida volta a `AVAILABLE`; arma vencida volta
como `BLOCKED`.

Os botões exigem `custodies` READ, CREATE e RETURN no escopo adequado. A seleção
de pessoas exige `core/people` READ. Armas vendidas, bloqueadas, vencidas ou já
cauteladas não aparecem e também são rejeitadas pela API.

## 15. Cadastros legados

As opções antigas continuam disponíveis para compatibilidade:

- **User registration**: informe nome, CPF, nascimento, endereço, e-mail e
  telefone e salve. Esse usuário é um cliente legado; ele não cria login no ERP.
- **Weapon registration**: o código é gerado pelo backend. Informe SKU, preço,
  nome e descrição e salve. Esse registro não cria ItemModel, FirearmSpecification
  ou AssetItem no novo inventário.
- **New sale**: procure o usuário, informe o código da arma, confirme o nome,
  defina uma quantidade inteira, adicione os itens, selecione o método de pagamento
  e finalize. Essa venda não reduz saldos nem cria movimentos no ERP.

Para todas as novas rotinas, prefira **Institutional core**, **Assets and
inventory** e **Inventory sales**, pois são os módulos que aplicam organização,
unidade, permissões, estoque e auditoria.
# Cadastro de doação

Antes da doação, cadastre o representante do doador e o donatário em **Institutional core → People**. O donatário pode ser uma pessoa física ou jurídica. Os ativos ou lotes precisam estar disponíveis no estoque da organização.

1. Abra **Donations** no menu.
2. Selecione **Organization**, proprietária atual dos bens.
3. Selecione **Unit** quando a doação sair de uma unidade específica.
4. Selecione **Donor representative**, pessoa responsável pela entrega em nome da organização.
5. Selecione **Donee**, pessoa física ou jurídica que receberá os bens.
6. Informe **Donation term**, número ou identificação do termo formal.
7. Em **Available stock**, escolha **Individual assets** ou **Stock lots**, pesquise e clique em **Add**.
8. Para lotes, informe a **Quantity**. Ativos individuais usam sempre quantidade 1.
9. Revise os itens e clique em **Finalize donation**.

Após o sucesso, o ativo individual passa para `DONATED`; as quantidades por lote saem do saldo. **Donation history** apresenta o termo, as partes, o operador e todos os itens. Clique em **+ Donation** para limpar os campos. Se a resposta da rede for incerta, use **Retry finalize** para recuperar a mesma operação sem nova baixa.

Significado dos campos:

- **Organization**: organização proprietária do estoque antes da doação.
- **Unit**: unidade de origem dos itens.
- **Donor representative**: pessoa que responde pela entrega.
- **Donee**: destinatário da doação.
- **Donation term**: identificação do instrumento que formaliza a transferência.
- **Stock type**: alterna entre ativos serializados e saldos por lote.
- **Quantity**: quantidade transferida; deve ser positiva e não pode superar o saldo.

# Ammunition consumption

Open **Ammunition consumption** in the main menu to register ammunition used during training, qualification, testing or another authorized activity.

1. Select **Organization**, the owner of the consumed ammunition.
2. Select **Unit** when the operation belongs to a specific organizational unit. Unit-level users must select their authorized unit.
3. Select **Responsible person**, the person accountable for the use of the ammunition.
4. Select **Authorizer**, the person who approved the activity.
5. Enter **Purpose**, describing why the ammunition will be used.
6. In **Available ammunition**, search by SKU, model, lot or location and click **Add**.
7. For every selected lot, enter **Quantity**, the exact amount consumed, and **Result**, the outcome of that ammunition use, such as `Consumed during qualification` or `Misfire recorded`.
8. Review the lot, expiry date, available quantity and storage location, then click **Finalize consumption**.

After successful finalization, the system deducts stock and shows the record in **Consumption history**. Click **+ Consumption** to clear the form and start another operation. A failed connection can show **Retry finalize**; use it to safely recover the same request without deducting stock twice.

Field meanings:

- **Organization**: legal or administrative organization that owns the stock.
- **Unit**: operational subdivision associated with the stock location and activity.
- **Responsible person**: person who assumes responsibility for the ammunition use.
- **Authorizer**: person who approved the consumption.
- **Purpose**: reason or activity that justified the ammunition issue and use.
- **SKU / model**: catalog identifier and commercial or technical ammunition model.
- **Lot**: manufacturing or acquisition batch used for traceability and expiry control.
- **Location**: inventory location from which the quantity is deducted.
- **Available**: current quantity that can still be selected at that location.
- **Quantity**: amount actually consumed, with up to four decimal places according to the unit of measure.
- **Result**: observed outcome of the use or deflagration for that lot.

# Inventory transfers

Use **Inventory transfers** to move armament, ammunition or any other registered inventory item between units of the same organization.

## Como registrar uma transferência

1. Abra **Inventory transfers** no menu principal.
2. Em **Organization**, selecione a organização proprietária dos itens.
3. Em **Source unit**, selecione a unidade onde os itens estão armazenados.
4. Em **Destination unit**, selecione uma unidade diferente dentro da mesma organização.
5. Em **Destination location**, selecione o local de estoque pertencente à unidade de destino.
6. Em **Purpose**, informe o motivo operacional ou administrativo da transferência.
7. Em **Stock type**, escolha **Individual assets** para itens individualizados ou **Stock lots** para itens controlados por quantidade.
8. Pesquise pelo código, modelo ou SKU e clique em **Add** para cada item.
9. Para lotes, informe a **Quantity**. Ativos individualizados sempre usam quantidade 1.
10. Confira a origem, o destino e os itens e clique em **Finalize transfer**.

Após o sucesso, **Transfer history** mostra a operação para as unidades de origem e destino. Clique em **+ Transfer** para limpar os campos e iniciar outra transferência. Se a resposta da rede for incerta, clique em **Retry finalize**; o sistema reapresenta a mesma operação sem duplicar movimentos.

## Significado dos campos

- **Organization**: organização proprietária do estoque e das duas unidades.
- **Source unit**: unidade que entrega os itens.
- **Destination unit**: unidade que recebe os itens; deve ser diferente da origem.
- **Destination location**: local físico ou lógico que receberá os itens na unidade de destino.
- **Purpose**: justificativa da redistribuição interna.
- **Stock type**: forma de controle do item, individual por patrimônio ou quantitativa por lote.
- **Code, model or SKU**: pesquisa usada para localizar o estoque disponível na origem.
- **Available**: quantidade que pode ser movimentada naquele momento.
- **Quantity**: quantidade transferida; deve ser positiva e não pode superar o saldo disponível.
- **Status**: situação da operação; uma transferência concluída recebe `FINALIZED`.
- **Operator**: usuário que finalizou a operação, gravado automaticamente.

Ativos indisponíveis ou vencidos não podem ser transferidos. Para lotes, a transferência reduz o saldo do local de origem e aumenta o saldo do local de destino, sem alterar o total disponível do lote na organização.

# Asset disposal

Use **Asset disposal** para registrar a baixa definitiva de armamentos, munições ou outros itens de estoque. Cadastre a baixa somente quando existir um processo administrativo que autorize a retirada permanente.

## Como cadastrar uma baixa

1. Abra **Asset disposal** no menu principal.
2. Selecione **Organization**, proprietária dos itens.
3. Selecione **Unit** quando a baixa estiver vinculada a uma unidade específica.
4. Informe **Process number**, identificação única do processo dentro da organização.
5. Informe **Reason**, justificando a baixa.
6. Marque **Record physical destruction** quando os itens forem fisicamente destruídos.
7. Se marcou a destruição, preencha **Destruction method**, **Destruction date and time** e **Destruction certificate**.
8. Em **Available stock**, escolha **Individual assets** ou **Stock lots**, pesquise e clique em **Add**.
9. Para lotes, informe **Quantity**. Ativos individualizados sempre usam quantidade 1.
10. Revise o processo e clique em **Finalize disposal**.

Após o sucesso, o ativo recebe `DISPOSED` e deixa de aparecer nas operações de estoque disponível. A quantidade de lote é retirada do saldo e do total disponível. Clique em **+ Disposal** para limpar os campos. **Disposal history** conserva o processo, motivo, operador, itens e certificado.

## Significado dos campos

- **Organization**: organização proprietária dos itens baixados.
- **Unit**: unidade responsável pelo estoque e pelo processo.
- **Process number**: número ou código formal que autoriza a baixa; não pode se repetir na organização.
- **Reason**: motivo administrativo, técnico ou legal para retirar os itens.
- **Record physical destruction**: indica que a baixa inclui destruição física certificada.
- **Destruction method**: método utilizado, como desmontagem ou inutilização controlada.
- **Destruction date and time**: momento em que a destruição ocorreu.
- **Destruction certificate**: número ou identificação do certificado comprobatório.
- **Stock type**: seleciona ativos individualizados ou quantidades por lote.
- **Available**: quantidade atualmente disponível para baixa.
- **Quantity**: quantidade definitivamente retirada.
- **Status**: situação do processo; uma baixa concluída recebe `FINALIZED`.
- **Operator**: usuário que finalizou a baixa, preenchido automaticamente.

Os três campos de destruição devem ser preenchidos em conjunto. Uma falha em qualquer item cancela toda a operação, sem baixa parcial.

# Maintenance and inspection

## Como criar um plano de manutenção

1. Abra **Maintenance and inspection**.
2. Selecione **Organization** e, quando aplicável, **Unit**.
3. Abra **Create maintenance plan**.
4. Informe **Plan name**.
5. Escolha **Type**: PREVENTIVE, CORRECTIVE ou INSPECTION.
6. Informe **Periodicity in days**.
7. Clique em **Create plan**.

## Como abrir e concluir uma ordem de serviço

1. Pesquise o patrimônio em **Find asset**.
2. Selecione o armamento em **Asset**.
3. Selecione um **Maintenance plan**, quando a ordem decorrer de um plano.
4. Informe **Reason** e clique em **Open work order**.
5. Em **Work order history**, abra a ordem.
6. Informe **Defect**, **Cause** e **Technical opinion**.
7. Informe **Service performed** e **Service cost**.
8. Escolha APPROVED ou REJECTED em **Functional test result**.
9. Registre **Test notes** e clique em **Complete work order**.

## Como abrir manutenÃ§Ã£o a partir de uma devoluÃ§Ã£o

1. Em **Equipment custody**, devolva o equipamento individual com uma **Return condition** que bloqueie a disponibilidade.
2. Abra novamente a cautela em **Custody history**.
3. Na linha devolvida, clique em **Open maintenance**.
4. Confira **Organization**, **Unit**, **Asset** e **Reason**, preenchidos com os dados da inspeÃ§Ã£o.
5. Selecione **Maintenance plan** quando houver um plano aplicÃ¡vel.
6. Clique em **Open work order**.
7. Volte ao histÃ³rico da cautela para consultar o nÃºmero exibido em **Work order**.

**Custody return inspection** Ã© a origem rastreÃ¡vel da ordem. Ela identifica exatamente a linha devolvida, sua condiÃ§Ã£o e as notas de inspeÃ§Ã£o. Uma inspeÃ§Ã£o admite somente uma ordem vinculada. Componentes controlados por quantidade permanecem no saldo bloqueado e nÃ£o usam esse fluxo de manutenÃ§Ã£o de patrimÃ´nio individual.

## Significado dos campos

- **Plan name**: nome que identifica a rotina de manutenção.
- **Type**: manutenção preventiva, corretiva ou inspeção.
- **Periodicity in days**: intervalo previsto entre execuções do plano.
- **Asset**: patrimônio individual submetido à manutenção.
- **Reason**: motivo da abertura da ordem.
- **Defect**: falha ou condição encontrada.
- **Cause**: origem técnica provável ou confirmada do defeito.
- **Technical opinion**: conclusão do responsável técnico.
- **Service performed**: intervenção executada no item.
- **Service cost**: custo da intervenção, igual ou maior que zero.
- **Functional test result**: decisão técnica após o serviço.
- **Test notes**: evidências e observações do teste.

Durante a ordem, o ativo permanece IN_MAINTENANCE. Resultado aprovado devolve um ativo válido para AVAILABLE; resultado reprovado ou validade vencida produz BLOCKED.

# Physical inventory

Abra **Physical inventory** depois de cadastrar organização, unidade, localização e estoque:

1. Selecione **Organization**, **Unit** e **Stock location**.
2. Em **Purpose**, descreva o motivo da conferência.
3. Clique em **Open inventory count** para fotografar o saldo atual do local.
4. Em **Counted**, informe a quantidade fisicamente encontrada em cada linha. Ativos individuais aceitam 0 ou 1.
5. Use **Notes** para registrar avaria, ausência, embalagem divergente ou outra evidência.
6. Clique em **Save count** para calcular as diferenças.
7. Revise **Difference** e **Result**.
8. Clique em **Approve adjustments** para atualizar o estoque, ou **Cancel** para encerrar sem ajustes.

| Campo | Significado |
| --- | --- |
| Organization | Organização proprietária do estoque |
| Unit | Unidade responsável pelo local |
| Stock location | Local físico integralmente contado |
| Purpose | Motivo, ciclo ou ordem que determinou o inventário |
| System | Quantidade fotografada na abertura |
| Counted | Quantidade encontrada fisicamente |
| Difference | Contado menos sistema |
| Result | Cadastro que classifica igualdade, falta ou sobra |
| Notes | Evidência ou explicação da contagem |

Uma falta de ativo individual o deixa `BLOCKED`. Diferenças de lote alteram o saldo somente depois da aprovação e geram movimento `INVENTORY_ADJUSTMENT`. Uma sobra individual sem cadastro precisa ser registrada antes de aprovar. Não é possível manter duas contagens inacabadas para o mesmo local.

Os cadastros **Inventory count status types** e **Inventory count result types** ficam em **Assets and inventory → Reference data**.

# Inventory reservations

Antes de reservar, cadastre organização, unidade, localização e os ativos individuais ou lotes. Abra **Inventory reservations**:

1. Selecione **Organization** e, quando aplicável, **Unit**.
2. Em **Purpose**, informe a finalidade da reserva.
3. Informe **Starts at** e **Ends at**; o término deve ser posterior ao início.
4. Em **Stock type**, escolha **Individual assets** ou **Stock lots**.
5. Pesquise por código, modelo, série ou lote e clique em **Add**.
6. Para lotes, informe **Quantity**; para ativos individuais a quantidade é sempre 1.
7. Revise **Selected items** e clique em **Create reservation**.
8. Use **Cancel** para desistir ou **Expire** após o término.

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| Organization | Sim | Organização proprietária do estoque reservado |
| Unit | Não | Unidade exata do estoque e da reserva |
| Purpose | Sim | Finalidade que justifica a separação |
| Starts at | Sim | Início planejado da reserva |
| Ends at | Sim | Limite de validade da reserva |
| Stock type | Sim | Pesquisa patrimônio individual ou saldo de lote |
| Quantity | Para lote | Quantidade transferida do saldo disponível para o reservado |
| Status | Automático | Situação baseada no cadastro de status de reserva |

Uma reserva ativa coloca o ativo individual em `BLOCKED`. Em lotes, reduz **available** e aumenta **reserved**, sem alterar o total físico. Cancelamento ou expiração libera o saldo. Cadastre opções adicionais em **Assets and inventory → Reference data → Reservation status types**.

# Correção da abrangência da cautela

O fluxo **Equipment custody** aceita qualquer equipamento individual `AVAILABLE` e dentro da validade, independentemente da família da categoria. Onde a seção 14.3 menciona arma ou `FIREARM`, leia equipamento individual; lotes continuam fora da cautela.

# Expiration, certification and recall

Abra **Assets and inventory** e use o grupo **Compliance**.

## Expiration controls

1. Abra **Expiration controls** e clique em **New**.
2. Selecione **Organization** e, quando necessário, **Organizational unit**.
3. Selecione somente **Individual asset** ou somente **Stock lot**.
4. Informe **Expiration type**, **Expiration date** e **Status**.
5. Clique em **Save**.

**Expiration type** identifica o prazo controlado; **Expiration date** é a data limite; **Status** informa se está pendente, válido, próximo do vencimento, vencido ou renovado. Um ativo marcado EXPIRED é bloqueado.

## Certifications

1. Abra **Certifications** e clique em **New**.
2. Selecione organização, unidade e exatamente um ativo ou lote.
3. Informe **Certification type**, **Certificate number**, **Valid until** e **Status**.
4. Clique em **Save**.

O número identifica o documento e deve ser único por tipo na organização. Certificação vencida não pode permanecer ACTIVE.

## Recalls

Recall e item de recall também aceitam **Description** opcional, com até 255
caracteres após remover espaços nas extremidades. A descrição complementa
**Reason** e **Required action**, sem substituí-los, e aparece em consultas,
filtros e auditoria. Apagar seu conteúdo limpa o valor.

1. Abra **Recalls**, clique em **New** e informe organização, unidade, **Recall number**, **Reason** e **Status**.
2. Salve o recall como OPEN ou IN_PROGRESS.
3. Abra **Recall items** e clique em **New**.
4. Selecione o recall e somente um **Individual asset** ou **Stock lot**.
5. Informe **Required action**, descrevendo inspeção, substituição, devolução ou baixa.
6. Clique em **Save**.

Um ativo disponível incluído no recall passa para BLOCKED. Registros de conformidade não podem ser excluídos; alterações posteriores permanecem registradas pela auditoria.
