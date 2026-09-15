"use client"

export type ComandosLocale = "pt-BR" | "en-US"

type TranslationPair = readonly [english: string, portuguese: string]

export const translationPairs: TranslationPair[] = [
    // Shell / navigation
    ["Navigation", "Navegação"],
    ["Dashboard", "Painel"],
    ["Operational Dashboard", "Painel operacional"],
    ["Command Center", "Central de Comando"],
    ["Transactions", "Transações"],
    ["Institutional core", "Núcleo institucional"],
    ["Institutional", "Institucional"],
    ["Organizations", "Organizações"],
    ["Organizational units", "Unidades organizacionais"],
    ["People", "Pessoas"],
    ["Person roles", "Papéis de pessoa"],
    ["Person role assignments", "Atribuições de papéis"],
    ["Role details", "Detalhes do papel"],
    ["Credentials", "Credenciais"],
    ["Qualifications", "Qualificações"],
    ["Access", "Acesso"],
    ["System users", "Usuários do sistema"],
    ["Access profiles", "Perfis de acesso"],
    ["Permissions", "Permissões"],
    ["User profiles", "Perfis de usuário"],
    ["Profile permissions", "Permissões de perfil"],
    ["Assets and inventory", "Bens e inventário"],
    ["Reference data", "Dados de referência"],
    ["Catalog", "Catálogo"],
    ["Specifications", "Especificações"],
    ["Controlled equipment", "Equipamentos controlados"],
    ["Compliance", "Conformidade"],
    ["Inventory", "Inventário"],
    ["Stock history", "Histórico de estoque"],
    ["Equipment Movement", "Movimentação de equipamentos"],
    ["Physical inventory", "Inventário físico"],
    ["Audit history", "Histórico de auditoria"],
    ["Settings", "Configurações"],
    ["Notifications", "Notificações"],
    ["Sign in", "Entrar"],
    ["Sign out", "Sair"],
    ["Signing in…", "Entrando…"],
    ["Signing out…", "Saindo…"],
    ["Continue setup", "Continuar configuração"],
    ["Open user menu", "Abrir menu do usuário"],
    ["Setup mode", "Modo de configuração"],
    ["Guest", "Visitante"],

    // Settings / preferences
    ["ERP settings", "Configurações do ERP"],
    ["Language", "Idioma"],
    ["Portuguese", "Português"],
    ["English", "Inglês"],
    ["Appearance", "Aparência"],
    ["Light", "Claro"],
    ["Dark", "Escuro"],
    ["Color palette", "Paleta de cores"],
    ["Green", "Verde"],
    ["Blue", "Azul"],
    ["Lilac", "Lilás"],
    ["Red", "Vermelho"],
    ["Yellow", "Amarelo"],
    ["Orange", "Laranja"],
    ["Activate light mode", "Ativar modo claro"],
    ["Activate dark mode", "Ativar modo escuro"],
    ["Comandos quick navigation", "Navegação rápida do Comandos"],
    ["Preferences are saved automatically in this browser.", "As preferências são salvas automaticamente neste navegador."],

    // Purchases / procurement
    ["Purchase", "Compra"],
    ["Purchases", "Compras"],
    ["Purchase history", "Histórico de compras"],
    ["Purchase items", "Itens da compra"],
    ["Purchase number", "Número da compra"],
    ["Purchase number *", "Número da compra *"],
    ["Purchase date", "Data da compra"],
    ["Purchase date *", "Data da compra *"],
    ["Buyer organization", "Organização compradora"],
    ["Buyer organization *", "Organização compradora *"],
    ["Supplier", "Fornecedor"],
    ["Equipment / item", "Equipamento / item"],
    ["Quantity", "Quantidade"],
    ["Unit price", "Preço unitário"],
    ["Discount", "Desconto"],
    ["Freight", "Frete"],
    ["Taxes", "Impostos"],
    ["Total", "Total"],
    ["Save purchase", "Salvar compra"],
    ["+ Purchase", "+ Compra"],
    ["+ Item", "+ Item"],
    ["Loading purchases…", "Carregando compras…"],
    ["No purchases registered for this organization.", "Nenhuma compra registrada para esta organização."],
    ["Not informed", "Não informado"],
    ["Procurement", "Contratação"],
    ["Procurement / contracting", "Licitação / contratação"],
    ["Method *", "Método *"],
    ["Not required", "Não exigida"],
    ["Bidding", "Licitação"],
    ["Direct contracting", "Contratação direta"],
    ["Process number *", "Número do processo *"],
    ["Object *", "Objeto *"],
    ["Bidding modality *", "Modalidade de licitação *"],
    ["Direct contracting *", "Contratação direta *"],
    ["Justification", "Justificativa"],
    ["Legal basis", "Fundamentação legal"],
    ["Supplier choice reason", "Razão da escolha do fornecedor"],
    ["Price justification", "Justificativa de preço"],
    ["Save procurement", "Salvar contratação"],
    ["Register equipment and material purchases. Public organizations can continue through bidding or direct contracting after the purchase is created.", "Registre compras de equipamentos e materiais. Organizações públicas podem prosseguir por licitação ou contratação direta após a criação da compra."],
    ["For private organizations choose “Not required”. For organizations subject to public procurement, select bidding or direct contracting.", "Para organizações privadas, selecione “Não exigida”. Para organizações sujeitas à contratação pública, selecione licitação ou contratação direta."],
    ["The backend currently represents the supplier as an Organization. Person/supplier roles can be generalized in the next backend evolution.", "O backend atualmente representa o fornecedor como uma Organização. Os papéis de pessoa/fornecedor serão generalizados na evolução do backend."],
    ["DRAFT", "RASCUNHO"],
    ["PLANNING", "PLANEJAMENTO"],
    ["PROCUREMENT_IN_PROGRESS", "CONTRATAÇÃO EM ANDAMENTO"],
    ["AUTHORIZED", "AUTORIZADA"],
    ["ORDERED", "PEDIDO EMITIDO"],
    ["PARTIALLY_RECEIVED", "RECEBIDA PARCIALMENTE"],
    ["RECEIVED", "RECEBIDA"],
    ["CANCELLED", "CANCELADA"],
    ["NOT_REQUIRED", "NÃO EXIGIDA"],
    ["BIDDING", "LICITAÇÃO"],
    ["DIRECT_CONTRACTING", "CONTRATAÇÃO DIRETA"],
    ["PREGAO", "PREGÃO"],
    ["CONCORRENCIA", "CONCORRÊNCIA"],
    ["CONCURSO", "CONCURSO"],
    ["LEILAO", "LEILÃO"],
    ["DIALOGO_COMPETITIVO", "DIÁLOGO COMPETITIVO"],
    ["DISPENSA", "DISPENSA"],
    ["INEXIGIBILIDADE", "INEXIGIBILIDADE"],
    // Generic actions
    ["Actions", "Ações"],
    ["Action", "Ação"],
    ["Add", "Adicionar"],
    ["Add set", "Adicionar conjunto"],
    ["Back", "Voltar"],
    ["Cancel", "Cancelar"],
    ["Close", "Fechar"],
    ["Create", "Criar"],
    ["Delete", "Excluir"],
    ["Edit", "Editar"],
    ["Finalize", "Finalizar"],
    ["New", "Novo"],
    ["New record", "Novo registro"],
    ["New requirement", "Novo requisito"],
    ["New user", "Novo usuário"],
    ["New weapon", "Nova arma"],
    ["Next", "Próximo"],
    ["Previous", "Anterior"],
    ["Remove", "Remover"],
    ["Retry", "Tentar novamente"],
    ["Save", "Salvar"],
    ["Saving…", "Salvando…"],
    ["Search", "Pesquisar"],
    ["Search records", "Pesquisar registros"],
    ["Select", "Selecionar"],
    ["Select an option", "Selecione uma opção"],
    ["Update", "Atualizar"],

    // Generic field labels
    ["ID", "ID"],
    ["Name", "Nome"],
    ["Code", "Código"],
    ["Description", "Descrição"],
    ["Status", "Situação"],
    ["Type", "Tipo"],
    ["Category", "Categoria"],
    ["Model", "Modelo"],
    ["Brand", "Marca"],
    ["Quantity", "Quantidade"],
    ["Available", "Disponível"],
    ["Available quantity", "Quantidade disponível"],
    ["Serial number", "Número de série"],
    ["Asset code", "Código patrimonial"],
    ["Creation date", "Data de criação"],
    ["Created at", "Criado em"],
    ["Updated at", "Atualizado em"],
    ["Active", "Ativo"],
    ["Inactive", "Inativo"],
    ["Notes", "Observações"],
    ["Reason", "Motivo"],
    ["Date", "Data"],
    ["Origin", "Origem"],
    ["Destination", "Destino"],
    ["Location", "Localização"],
    ["Unit", "Unidade"],
    ["Organization", "Organização"],
    ["Person", "Pessoa"],
    ["User", "Usuário"],
    ["Role", "Papel"],
    ["Profile", "Perfil"],
    ["Permission", "Permissão"],
    ["Price", "Preço"],
    ["Value", "Valor"],
    ["Condition", "Condição"],
    ["Acquisition state", "Estado de aquisição"],
    ["Acquisition origin", "Origem de aquisição"],
    ["Manufacturer", "Fabricante"],
    ["Caliber", "Calibre"],
    ["Lot", "Lote"],
    ["Lots", "Lotes"],
    ["Purpose", "Finalidade"],
    ["Result", "Resultado"],
    ["Responsible person", "Pessoa responsável"],
    ["Authorizer", "Autorizador"],
    ["Recipient", "Destinatário"],
    ["Recipient type", "Tipo de destinatário"],
    ["Receiving organizational unit", "Unidade organizacional recebedora"],
    ["Issuing unit", "Unidade emissora"],
    ["Due date and time", "Data e hora de devolução"],
    ["Return condition", "Condição de devolução"],
    ["Inspection notes", "Observações da inspeção"],
    ["Components", "Componentes"],
    ["Password", "Senha"],
    ["Login", "Login"],

    // Inventory / ballistic / optical / regulatory fields
    ["Equipment", "Equipamento"],
    ["Equipament", "Equipamento"],
    ["Stock", "Estoque"],
    ["stock", "estoque"],
    ["Material", "Material"],
    ["material", "material"],
    ["Ballistic protection model", "Modelo de prote"],
    ["Protection type", "Tipo de prote"],
    ["Protection level", "N"],
    ["Certification", "Certifica"],
    ["Optical model", "Modelo "],
    ["Optical equipment model", "Modelo de equipamento "],
    ["Optical type", "Tipo "],
    ["Magnification", "Amplia"],
    ["Objective diameter", "Di"],
    ["Reticle", "Ret"],
    ["Field of view", "Campo de vis"],
    ["Night vision", "Vis"],
    ["Thermal vision", "Vis"],
    ["Regulatory control", "Controle regulat"],
    ["Regulatory agency", ""],
    ["Regulatory requirement", "Requisito regulat"],
    ["Control type", "Tipo de controle"],
    ["License", "Licen"],
    ["Authorization", "Autoriza"],
    ["Issue date", "Data de emiss"],
    ["Expiration date", "Data de validade"],
    ["Issuing authority", "Autoridade emissora"],
    ["Jurisdiction", "Jurisdi"],
    ["Controlled product classification", "Classifica"],

    // Generic messages
    ["Loading records…", "Carregando registros…"],
    ["Loading…", "Carregando…"],
    ["No records found.", "Nenhum registro encontrado."],
    ["No resources are available for your access profile.", "Nenhum recurso está disponível para o seu perfil de acesso."],
    ["Unable to complete the request. Check the API connection and try again.", "Não foi possível concluir a solicitação. Verifique a conexão com a API e tente novamente."],
    ["Unable to complete the request. Please try again.", "Não foi possível concluir a solicitação. Tente novamente."],
    ["Unable to sign in. Check the API connection and try again.", "Não foi possível entrar. Verifique a conexão com a API e tente novamente."],
    ["Yes", "Sim"],
    ["No", "Não"],
    ["Not set", "Não informado"],
    ["Not recorded", "Não registrado"],
    ["Organization-wide", "Toda a organização"],

    // Login
    ["Use your system access account to continue.", "Use sua conta de acesso ao sistema para continuar."],

    // Dashboard
    ["COMMAND CENTER", "CENTRO DE COMANDO"],
    ["Consolidated view of assets, inventory and equipment movement.", "Visão consolidada de bens, inventário e movimentação de equipamentos."],
    ["Operational", "Operacional"],
    ["Total assets", "Total de bens"],
    ["Available stock", "Estoque disponível"],
    ["Active custodies", "Cautelas ativas"],
    ["this month", "neste mês"],
    ["updated today", "atualizados hoje"],
    ["EQUIPMENT MOVEMENT", "MOVIMENTAÇÃO DE EQUIPAMENTOS"],
    ["Movement overview", "Visão geral das movimentações"],
    ["Last 12 months", "Últimos 12 meses"],
    ["Movements", "Movimentações"],
    ["Custodies", "Cautelas"],
    ["Firearms", "Armas de fogo"],
    ["Ammunition", "Munição"],
    ["Ballistic protection", "Proteção balística"],
    ["Less lethal", "Menos letal"],
    ["Optical equipment", "Equipamento óptico"],
    ["Assets by category", "Bens por categoria"],
    ["DISTRIBUTION", "DISTRIBUIÇÃO"],
    ["Movement types", "Tipos de movimentação"],
    ["movements", "movimentações"],
    ["OPERATIONAL STATUS", "SITUAÇÃO OPERACIONAL"],
    ["Inventory readiness", "Prontidão do inventário"],
    ["In custody", "Cautelado"],
    ["Reserved", "Reservado"],
    ["Maintenance", "Manutenção"],
    ["Restricted", "Restrito"],
    ["Visual dashboard structure. Values will be connected to the ERP API.", "Estrutura visual do painel. Os valores serão conectados à API do ERP."],
    ["Sep", "Set"],
    ["Oct", "Out"],
    ["Nov", "Nov"],
    ["Dec", "Dez"],
    ["Jan", "Jan"],
    ["Feb", "Fev"],
    ["Mar", "Mar"],
    ["Apr", "Abr"],
    ["May", "Mai"],
    ["Jun", "Jun"],
    ["Jul", "Jul"],
    ["Aug", "Ago"],

    // Catalog resources
    ["Individual assets", "Bens individuais"],
    ["Stock locations", "Locais de estoque"],
    ["Stock balances", "Saldos de estoque"],
    ["Stock movements", "Movimentações de estoque"],
    ["Stock lots", "Lotes de estoque"],
    ["Equipment sets", "Conjuntos de equipamentos"],
    ["Equipment set components", "Componentes do conjunto"],
    ["Item categories", "Categorias de item"],
    ["Armament types", "Tipos de armamento"],
    ["Armament classifications", "Classificações de armamento"],
    ["Brands", "Marcas"],
    ["Item models", "Modelos de item"],
    ["Technical characteristics", "Características técnicas"],
    ["Category characteristics", "Características da categoria"],
    ["Model characteristics", "Características do modelo"],
    ["Asset characteristics", "Características do bem"],
    ["Firearm specifications", "Especificações de arma de fogo"],
    ["Ammunition specifications", "Especificações de munição"],
    ["Grenade specifications", "Especificações de granada"],
    ["Spray specifications", "Especificações de espargidor"],
    ["Ballistic protection specifications", "Especificações de proteção balística"],
    ["Electrical device specifications", "Especificações de dispositivo elétrico"],
    ["Optical specifications", "Especificações ópticas"],
    ["Regulatory controls", "Controles regulatórios"],
    ["Expiration controls", "Controles de validade"],
    ["Certifications", "Certificações"],
    ["Recalls", "Recalls"],
    ["Recall items", "Itens de recall"],
    ["Custody return condition types", "Tipos de condição de devolução de cautela"],
    ["Sale return reason types", "Tipos de motivo de devolução de venda"],
    ["Inventory count status types", "Tipos de situação de contagem de inventário"],
    ["Inventory count result types", "Tipos de resultado de contagem de inventário"],
    ["Reservation status types", "Tipos de situação de reserva"],

    // Core / shared workspace
    ["Manage organizations, people, their roles, and system account assignments.", "Gerencie organizações, pessoas, seus papéis e vínculos com contas do sistema."],
    ["Register single asset", "Cadastrar bem individual"],
    ["Receive ammunition boxes", "Receber caixas de munição"],
    ["Calculated by inventory operations.", "Calculado pelas operações de inventário."],
    ["Fixed after registration.", "Fixo após o cadastro."],
    ["At least 12 characters.", "No mínimo 12 caracteres."],
    ["Leave blank to keep the current password.", "Deixe em branco para manter a senha atual."],
    ["Use SYSTEM (all organizations), ORGANIZATION or UNIT. Other levels do not grant access.", "Use SYSTEM (todas as organizações), ORGANIZATION ou UNIT. Outros níveis não concedem acesso."],
    ["Use an exact resource code, such as core/people, inventory/assets, sales or security/access. An asterisk grants all resources.", "Use um código exato de recurso, como core/people, inventory/assets, sales ou security/access. Um asterisco concede acesso a todos os recursos."],
    ["Use READ, CREATE, UPDATE, DELETE, MANAGE (access administration), or *.", "Use READ, CREATE, UPDATE, DELETE, MANAGE (administração de acesso) ou *."],
    ["Required for UNIT profiles; leave empty for SYSTEM and ORGANIZATION profiles.", "Obrigatório para perfis UNIT; deixe vazio para perfis SYSTEM e ORGANIZATION."],
    ["Select an existing model and register one individual item. Asset code must be unique; serial number is required for serialized models and must be unique within the model. Leading and trailing spaces are removed; letter case is preserved.", "Selecione um modelo existente e cadastre um item individual. O código patrimonial deve ser único; o número de série é obrigatório para modelos serializados e deve ser único dentro do modelo. Espaços no início e no fim são removidos; maiúsculas e minúsculas são preservadas."],

    // Equipment movements
    ["Sale", "Venda"],
    ["Custody", "Cautela"],
    ["Ammunition consumption", "Consumo de munição"],
    ["Consumption", "Consumo"],
    ["Donation", "Doação"],
    ["Transfer", "Transferência"],
    ["Disposal", "Baixa"],
    ["Reservation", "Reserva"],

    // Ammunition consumption
    ["Record authorized ammunition use and deduct each lot from inventory.", "Registre o uso autorizado de munição e dê baixa de cada lote no inventário."],
    ["The result could not be confirmed. Retry the same request to avoid consuming stock twice.", "O resultado não pôde ser confirmado. Repita a mesma solicitação para evitar consumir o estoque duas vezes."],
    ["Consumption items", "Itens do consumo"],
    ["Ammunition / lot", "Munição / lote"],
    ["Available ammunition", "Munição disponível"],
    ["SKU, model, lot or location", "SKU, modelo, lote ou localização"],
    ["Loading ammunition…", "Carregando munições…"],
    ["SKU / model", "SKU / modelo"],
    ["Expires", "Validade"],
    ["No available ammunition matches your search.", "Nenhuma munição disponível corresponde à pesquisa."],
    ["Consumption history", "Histórico de consumo"],
    ["No consumption records in the selected scope.", "Nenhum registro de consumo no escopo selecionado."],
    ["Finalize consumption", "Finalizar consumo"],
    ["Retry finalize", "Tentar finalizar novamente"],
    ["Finalizing…", "Finalizando…"],
    ["Consumed", "Consumido"],
    ["Select the organization and add an available ammunition lot.", "Selecione a organização e adicione um lote de munição disponível."],
    ["Select an organization and an authorized unit when required by your profile.", "Selecione uma organização e uma unidade autorizada quando exigido pelo seu perfil."],
    ["Authorized by", "Autorizado por"],
    ["Finalized by", "Finalizado por"],

    // Custody
    ["Equipment custody", "Cautela de equipamentos"],
    ["Issue available equipment to a person or an organizational unit and record each return with stock history.", "Cautele equipamentos disponíveis para uma pessoa ou unidade organizacional e registre cada devolução com histórico de estoque."],
    ["The result could not be confirmed. Retry with the same request to avoid issuing twice.", "O resultado não pôde ser confirmado. Repita a mesma solicitação para evitar cautelar duas vezes."],
    ["Person", "Pessoa"],
    ["Organizational unit", "Unidade organizacional"],
    ["Selected equipment sets", "Conjuntos de equipamentos selecionados"],
    ["Selected equipment", "Equipamentos selecionados"],
    ["Code / name", "Código / nome"],
    ["Asset / model", "Bem / modelo"],
    ["No equipment set selected.", "Nenhum conjunto de equipamentos selecionado."],
    ["Select the organization and add available equipment.", "Selecione a organização e adicione equipamentos disponíveis."],
    ["Available equipment sets", "Conjuntos de equipamentos disponíveis"],
    ["Set code or name", "Código ou nome do conjunto"],
    ["Loading equipment sets…", "Carregando conjuntos de equipamentos…"],
    ["No complete equipment set is currently available.", "Nenhum conjunto completo de equipamentos está disponível no momento."],
    ["Available equipment", "Equipamentos disponíveis"],
    ["Asset code, serial number or model", "Código patrimonial, número de série ou modelo"],
    ["Loading equipment…", "Carregando equipamentos…"],
    ["No available equipment matches your search.", "Nenhum equipamento disponível corresponde à pesquisa."],
    ["Issue custody", "Registrar cautela"],
    ["Retry issue", "Tentar cautela novamente"],
    ["Issuing…", "Registrando cautela…"],

    // Common movement vocabulary used across sale/donation/transfer/disposal/reservation/maintenance/audit
    ["History", "Histórico"],
    ["Details", "Detalhes"],
    ["Items", "Itens"],
    ["Item", "Item"],
    ["Selected items", "Itens selecionados"],
    ["Available items", "Itens disponíveis"],
    ["Reference", "Referência"],
    ["Document", "Documento"],
    ["Document number", "Número do documento"],
    ["Protocol", "Protocolo"],
    ["Requester", "Solicitante"],
    ["Approver", "Aprovador"],
    ["Authorized by", "Autorizado por"],
    ["Requested by", "Solicitado por"],
    ["Created by", "Criado por"],
    ["Updated by", "Atualizado por"],
    ["Completed", "Concluído"],
    ["Pending", "Pendente"],
    ["Cancelled", "Cancelado"],
    ["Approved", "Aprovado"],
    ["Rejected", "Rejeitado"],
    ["Open", "Aberto"],
    ["Closed", "Fechado"],
    ["Draft", "Rascunho"],
    ["Start date", "Data inicial"],
    ["End date", "Data final"],
    ["Start", "Início"],
    ["End", "Fim"],
    ["Inspection", "Inspeção"],
    ["Maintenance plan", "Plano de manutenção"],
    ["Work order", "Ordem de serviço"],
    ["Diagnosis", "Diagnóstico"],
    ["Executed service", "Serviço executado"],
    ["Functional test", "Teste funcional"],
    ["Reservation history", "Histórico de reservas"],
    ["Transfer history", "Histórico de transferências"],
    ["Donation history", "Histórico de doações"],
    ["Disposal history", "Histórico de baixas"],
    ["Maintenance history", "Histórico de manutenção"],
    ["Audit", "Auditoria"],
    ["Audit log", "Registro de auditoria"],
    ["Event", "Evento"],
    ["Entity", "Entidade"],
    ["Entity ID", "ID da entidade"],
    ["Actor", "Responsável"],
    ["Timestamp", "Data/hora"],
    ["Before", "Antes"],
    ["After", "Depois"],

    // Command Center / task board
    ["BOT STATUS", "STATUS DO BOT"],
    ["CURRENT REQUIREMENT", "REQUISITO ATUAL"],
    ["RUNNING REQUIREMENT", "REQUISITO EM EXECUÇÃO"],
    ["DETAIL", "DETALHE"],
    ["WORKER CONTROL", "CONTROLE DO WORKER"],
    ["No requirement running", "Nenhum requisito em execução"],
    ["ERP planning", "Planejamento do ERP"],
    ["CURRENT SPRINT", "SPRINT ATUAL"],
    ["Search requirement", "Buscar requisito"],
    ["items in module", "itens no módulo"],
    ["running", "em execução"],
    ["completed", "concluídos"],
    ["Local planning; actual execution in the queue below", "Planejamento local; execução real na fila abaixo"],
    ["No items in this stage.", "Nenhum item nesta etapa."],
    ["Bot queue", "Fila do bot"],
    ["Task", "Tarefa"],
    ["File", "Arquivo"],
    ["No tasks in this queue.", "Nenhuma tarefa nesta fila."],
    ["Work until requirement", "Trabalhe até o requisito"],
    ["Pause BOT", "Pausar BOT"],
    ["Stop BOT", "Parar BOT"],
    ["Start BOT", "Iniciar BOT"],
    ["Resume BOT", "Continuar BOT"],
    ["Pause", "Pausar"],
    ["Stop", "Parar"],
    ["Resume", "Continuar"],
    ["Backlog", "Backlog"],
    ["Doing", "Fazendo"],
    ["Done", "Concluído"],
    ["Requirement", "Requisito"],
    ["Correction", "Correção"],
    ["Priority", "Prioridade"],
    ["Module", "Módulo"],
    ["Sprint", "Sprint"],
    ["Export", "Exportar"],
    ["Print", "Imprimir"],
    ["Undo", "Desfazer"],
    ["Redo", "Refazer"],
    ["Filter", "Filtrar"],
    ["Sort", "Ordenar"],
    ["Group", "Agrupar"],
    ["Compact", "Compacto"],
    ["Comfortable", "Confortável"],

    // Legacy users / weapons / sales
    ["Users", "Usuários"],
    ["Weapons", "Armas"],
    ["Sales", "Vendas"],
    ["Users registration", "Cadastro de usuários"],
    ["Weapons registration", "Cadastro de armas"],
    ["User registration", "Cadastro de usuário"],
    ["Weapon registration", "Cadastro de arma"],
    ["CPF", "CPF"],
    ["Birth", "Nascimento"],
    ["Address", "Endereço"],
    ["E-mail", "E-mail"],
    ["Email", "E-mail"],
    ["Phone", "Telefone"],
    ["SKU", "SKU"],
    ["Payment method", "Forma de pagamento"],
    ["Total", "Total"],
    ["Subtotal", "Subtotal"],
    ["Customer", "Cliente"],
    ["Supplier", "Fornecedor"],
    // UI audit - menu, main and registrations
    ["Dashboard period", "Período do painel"],
    ["Available inventory", "Inventário disponível"],
    ["Pending controls", "Controles pendentes"],
    ["Monthly movements", "Movimentações mensais"],
    ["Equipment movement volume", "Volume de movimentação de equipamentos"],
    ["this year", "neste ano"],
    ["Monthly target", "Meta mensal"],
    ["Operational readiness", "Prontidão operacional"],
    ["Inventory availability is progressing toward the operational target.", "A disponibilidade do inventário está avançando em direção à meta operacional."],
    ["Movements and custodies", "Movimentações e cautelas"],
    ["Inventory overview", "Visão geral do inventário"],
    ["Asset availability by status", "Disponibilidade de bens por situação"],
    ["Recent movements", "Movimentações recentes"],
    ["Latest inventory operations", "Últimas operações de inventário"],
    ["Destination / Unit", "Destino / Unidade"],
    ["Live data integration will replace the placeholder values shown in this dashboard.", "A integração com dados reais substituirá os valores provisórios exibidos neste painel."],
    ["Toggle sidebar", "Alternar menu lateral"],
    ["Maintenance and inspection", "Manutenção e inspeção"],
    ["Inventory sales", "Vendas de inventário"],
    ["Buyer", "Comprador"],
    ["Select a payment method", "Selecione uma forma de pagamento"],
    ["Sale items", "Itens da venda"],
    ["Item / code", "Item / código"],
    ["Unit price", "Preço unitário"],
    ["Stock type", "Tipo de estoque"],
    ["Code, SKU or model", "Código, SKU ou modelo"],
    ["Refresh stock", "Atualizar estoque"],
    ["Loading stock…", "Carregando estoque…"],
    ["Code / model", "Código / modelo"],
    ["No available stock matches your search.", "Nenhum estoque disponível corresponde à pesquisa."],
    ["Returns and cancellations", "Devoluções e cancelamentos"],
    ["Sales history", "Histórico de vendas"],
    ["Refresh history", "Atualizar histórico"],
    ["Loading sales…", "Carregando vendas…"],
    ["No sales recorded for the selected scope.", "Nenhuma venda registrada no escopo selecionado."],
    ["All sale items have been returned.", "Todos os itens da venda foram devolvidos."],
    ["Return or cancel sale", "Devolver ou cancelar venda"],
    ["Return reason", "Motivo da devolução"],
    ["Refund reference", "Referência do reembolso"],
    ["Return quantity", "Quantidade a devolver"],
    ["Custody history", "Histórico de cautelas"],
    ["Retry pending return", "Tentar devolução pendente novamente"],
    ["Loading custody history…", "Carregando histórico de cautelas…"],
    ["Equipment / model", "Equipamento / modelo"],
    ["Set / role", "Conjunto / função"],
    ["Open maintenance", "Abrir manutenção"],
    ["Return all pending", "Devolver todos os pendentes"],
    ["No custody records in the selected scope.", "Nenhum registro de cautela no escopo selecionado."],
    ["Asset disposal", "Baixa de bem"],
    ["Process number", "Número do processo"],
    ["Record physical destruction", "Registrar destruição física"],
    ["Enable when disposal includes certified destruction.", "Ative quando a baixa incluir destruição certificada."],
    ["Destruction method", "Método de destruição"],
    ["Destruction date and time", "Data e hora da destruição"],
    ["Destruction certificate", "Certificado de destruição"],
    ["Disposal items", "Itens da baixa"],
    ["Code, model or SKU", "Código, modelo ou SKU"],
    ["Loading history…", "Carregando histórico…"],
    ["No disposals in the selected scope.", "Nenhuma baixa no escopo selecionado."],
    ["Open maintenance work orders, record diagnosis and services, and release assets through a functional test.", "Abra ordens de serviço de manutenção, registre diagnóstico e serviços e libere bens por meio de teste funcional."],
    ["Open work order", "Abrir ordem de serviço"],
    ["Find asset", "Localizar bem"],
    ["Select an asset", "Selecione um bem"],
    ["No plan", "Sem plano"],
    ["Create maintenance plan", "Criar plano de manutenção"],
    ["Plan name", "Nome do plano"],
    ["Periodicity in days", "Periodicidade em dias"],
    ["Create plan", "Criar plano"],
    ["Work order history", "Histórico de ordens de serviço"],
    ["No work orders in the selected scope.", "Nenhuma ordem de serviço no escopo selecionado."],
    ["Defect", "Defeito"],
    ["Cause", "Causa"],
    ["Technical opinion", "Parecer técnico"],
    ["Service performed", "Serviço realizado"],
    ["Test notes", "Observações do teste"],
    ["Service cost", "Custo do serviço"],
    ["Functional test result", "Resultado do teste funcional"],
    ["Complete work order", "Concluir ordem de serviço"],
    ["Record saved successfully.", "Registro salvo com sucesso."],
    ["Record deleted successfully.", "Registro excluído com sucesso."],
    ["Type to find a record", "Digite para localizar um registro"],
    ["Delete record", "Excluir registro"],
    ["Select a record", "Selecione um registro"],
    ["Select an organization first.", "Selecione primeiro uma organização."],
    ["Loading models and records...", "Carregando modelos e registros..."],
    ["Units without boxes", "Unidades sem caixas"],
    ["Boxes and loose units", "Caixas e unidades avulsas"],
    ["Units per box", "Unidades por caixa"],
    ["Remove box row", "Remover linha de caixa"],
    ["Add box size", "Adicionar tamanho de caixa"],
    ["Loose units", "Unidades avulsas"],
    ["Your profile cannot view audit history.", "Seu perfil não pode visualizar o histórico de auditoria."],
    ["From (local time)", "De (hora local)"],
    ["Until (local time)", "Até (hora local)"],
    ["Clear filters", "Limpar filtros"],
    ["No audit records match these filters.", "Nenhum registro de auditoria corresponde a estes filtros."],
    ["Audit event details", "Detalhes do evento de auditoria"],
    ["Close details", "Fechar detalhes"],
    ["Search account...", "Pesquisar conta..."],
    ["Search operation...", "Pesquisar operação..."],
    ["Search resource...", "Pesquisar recurso..."],
    ["Search ID...", "Pesquisar ID..."],
    ["Custody return condition", "Condição de devolução de cautela"],
    ["Sale return reason", "Motivo de devolução de venda"],
    ["Inventory count status", "Situação da contagem de inventário"],
    ["Inventory count result", "Resultado da contagem de inventário"],
    ["Reservation status", "Situação da reserva"],
    ["Electrical device", "Dispositivo elétrico"],
    ["Equipment set", "Conjunto de equipamentos"],
    ["Paste a list from a spreadsheet", "Cole uma lista de uma planilha"],
    ["Asset code and serial number (two columns, no header)", "Código patrimonial e número de série (duas colunas, sem cabeçalho)"],
    ["Add pasted rows", "Adicionar linhas coladas"],
    ["Add row", "Adicionar linha"],
    ["Loose rounds (without a box)", "Munições avulsas (sem caixa)"],
    ["Edit batch", "Editar lote"],
    ["Stock location", "Local de estoque"],
    ["Save count", "Salvar contagem"],
    ["Approve adjustments", "Aprovar ajustes"],
    ["Inventory history", "Histórico de inventário"],
    ["Donor representative", "Representante do doador"],
    ["Donee", "Donatário"],
    ["Donation term", "Termo de doação"],
    ["Donation items", "Itens da doação"],
    ["No donations in the selected scope.", "Nenhuma doação no escopo selecionado."],
    ["Inventory transfers", "Transferências de inventário"],
    ["Source unit", "Unidade de origem"],
    ["Destination unit", "Unidade de destino"],
    ["Destination location", "Local de destino"],
    ["Transfer items", "Itens da transferência"],
    ["Source location", "Local de origem"],
    ["Available stock at source", "Estoque disponível na origem"],
    ["No transfers in the selected scope.", "Nenhuma transferência no escopo selecionado."],
    ["Inventory reservations", "Reservas de inventário"],
    ["Start date and time", "Data e hora inicial"],
    ["End date and time", "Data e hora final"],
    ["Reserved items", "Itens reservados"],
    ["Add available stock.", "Adicione estoque disponível."],
    ["New User", "Novo usuário"],
    ["Delete User", "Excluir usuário"],
    ["Edit user", "Editar usuário"],
    ["Save changes", "Salvar alterações"],
    ["Cancel editing", "Cancelar edição"],
    ["Delete user", "Excluir usuário"],
    ["User successfully updated", "Usuário atualizado com sucesso"],
    ["User successfully registered", "Usuário cadastrado com sucesso"],
    ["An error occurred while saving the user.", "Ocorreu um erro ao salvar o usuário."],
    ["Creation Date", "Data de criação"],
    ["New Weapon", "Nova arma"],
    ["Delete Weapon", "Excluir arma"],
    ["Edit weapon", "Editar arma"],
    ["Delete weapon", "Excluir arma"],
    ["An error occurred while loading the weapon.", "Ocorreu um erro ao carregar a arma."],
    ["Weapon successfully updated.", "Arma atualizada com sucesso."],
    ["Weapon successfully registered.", "Arma cadastrada com sucesso."],
    ["An error occurred while saving the weapon.", "Ocorreu um erro ao salvar a arma."],
    ["Weapons Registration", "Cadastro de armas"],
    ["Registration Date", "Data de cadastro"],
    ["Type the SKU", "Digite o SKU"],
    ["Type the price", "Digite o preço"],
    ["Type the name", "Digite o nome"],
    ["Type the description", "Digite a descrição"],
    ["Sale successfully completed!", "Venda concluída com sucesso!"],
    ["Credit Card", "Cartão de crédito"],
    ["Debit Card", "Cartão de débito"],
    ["Bank Transfer", "Transferência bancária"],
    ["Weapons in this sale", "Armas nesta venda"],
    ["Decrease quantity", "Diminuir quantidade"],
    ["Increase quantity", "Aumentar quantidade"],
    ["Delete item", "Excluir item"],
    ["No user found", "Nenhum usuário encontrado"],
    ["No weapon found", "Nenhuma arma encontrada"],
    ["Unit Price", "Preço unitário"],
    ["No weapons added to the sale.", "Nenhuma arma adicionada à venda."],
    ["Delete Item", "Excluir item"],
    ["Are you sure you want to delete this item?", "Tem certeza de que deseja excluir este item?"],
    ["Checking session…", "Verificando sessão…"],

    // Deep i18n sweep - ERP metadata, catalogs, statuses and shared UI
    ["Command center", "Centro de comando"],
    ["Nature", "Natureza"],
    ["Acronym", "Sigla"],
    ["Tax ID (CNPJ)", "CNPJ"],
    ["Tax ID (CPF / CNPJ)", "CPF / CNPJ"],
    ["Total records", "Total de registros"],
    ["Public organization", "Órgão público"],
    ["Parent unit", "Unidade superior"],
    ["Person type", "Tipo de pessoa"],
    ["Full name / legal name", "Nome completo / razão social"],
    ["Birth date", "Data de nascimento"],
    ["Person role assignment", "Atribuição de papel da pessoa"],
    ["Key", "Chave"],
    ["Blocked", "Bloqueado"],
    ["Level", "Nível"],
    ["Resource", "Recurso"],
    ["Valid until", "Válido até"],
    ["Manage equipment models, firearm and ammunition specifications, registrations, individual assets, stock lots, and technical characteristics.", "Gerencie modelos de equipamentos, especificações de armas de fogo e munições, cadastros, bens individuais, lotes de estoque e características técnicas."],
    ["Blocks availability", "Bloqueia disponibilidade"],
    ["Display order", "Ordem de exibição"],
    ["System protected", "Protegido pelo sistema"],
    ["Terminal status", "Situação terminal"],
    ["Expired", "Expirado"],
    ["Match", "Correspondente"],
    ["Shortage", "Falta"],
    ["Surplus", "Sobra"],
    ["Counted", "Contado"],
    ["Customer return", "Devolução do cliente"],
    ["order error", "erro no pedido"],
    ["Order error", "Erro no pedido"],
    ["defective item", "item defeituoso"],
    ["Defective item", "Item defeituoso"],
    ["good", "bom"],
    ["Good", "Bom"],
    ["needs inspection", "necessita inspeção"],
    ["Needs inspection", "Necessita inspeção"],
    ["damaged", "danificado"],
    ["Damaged", "Danificado"],
    ["Categories", "Categorias"],
    ["Types", "Tipos"],
    ["Classifications", "Classificações"],
    ["models", "modelos"],
    ["Models", "Modelos"],
    ["equipament family", "família do equipamento"],
    ["Equipment family", "Família do equipamento"],
    ["serialized", "serializado"],
    ["Serialized", "Serializado"],
    ["lote controlled", "controlado por lote"],
    ["Lot controlled", "Controlado por lote"],
    ["consumable", "consumível"],
    ["Consumable", "Consumível"],
    ["type", "tipo"],
    ["Armament type", "Tipo de armamento"],
    ["Armament classification", "Classificação de armamento"],
    ["Unit of measure", "Unidade de medida"],
    ["manufacturer code", "código do fabricante"],
    ["Manufacturer code", "Código do fabricante"],
    ["List price", "Preço de tabela"],
    ["Technical", "Técnico"],
    ["Data type", "Tipo de dado"],
    ["Characteristic", "Característica"],
    ["Required value", "Valor obrigatório"],
    ["Per individual item", "Por item individual"],
    ["Asset", "Bem"],
    ["Firearm", "Arma de fogo"],
    ["Grenade", "Granada"],
    ["Spray", "Espargidor"],
    ["Optical", "Óptico"],
    ["Required", "Obrigatório"],
    ["Optional", "Opcional"],
    ["Clear", "Limpar"],
    ["Filters", "Filtros"],
    ["Results", "Resultados"],
    ["Loading records...", "Carregando registros..."],
    ["Number", "Número"],

]

const englishToPortuguese = new Map<string, string>(translationPairs)
const portugueseToEnglish = new Map<string, string>(
    translationPairs.map(([english, portuguese]) => [portuguese, english])
)

const replaceDynamic = (
    text: string,
    locale: ComandosLocale
): string | null => {
    if (locale === "pt-BR") {
        let match = text.match(/^Search (.+)\.\.\.$/i)
        if (match) {
            return `Pesquisar ${translateText(match[1], locale).toLowerCase()}...`
        }

        match = text.match(/^Edit (.+)$/)
        if (match) return `Editar ${match[1]}`

        match = text.match(/^Delete (.+)$/)
        if (match) return `Excluir ${match[1]}`

        match = text.match(/^Quantity for (.+)$/)
        if (match) return `Quantidade para ${match[1]}`

        match = text.match(/^Result for (.+)$/)
        if (match) return `Resultado para ${match[1]}`

        match = text.match(/^(\d+) records · Page (\d+)$/)
        if (match) return `${match[1]} registros · Página ${match[2]}`

        match = text.match(/^Consumption #(\d+) finalized successfully\.$/)
        if (match) return `Consumo #${match[1]} finalizado com sucesso.`

        match = text.match(/^Custody #(\d+) issued successfully\.$/)
        if (match) return `Cautela #${match[1]} registrada com sucesso.`

        match = text.match(/^Consumption #(\d+) · (.+)$/)
        if (match) return `Consumo #${match[1]} · ${match[2]}`

        match = text.match(/^Lot (.+)$/)
        if (match) return `Lote ${match[1]}`

        match = text.match(/^Unit: (.+)$/)
        if (match) return `Unidade: ${match[1]}`

        match = text.match(/^Purpose: (.+)$/)
        if (match) return `Finalidade: ${match[1]}`

        match = text.match(/^Date: (.+)$/)
        if (match) return `Data: ${match[1]}`

        match = text.match(/^Expires: (.+)$/)
        if (match) return `Validade: ${translateText(match[1], locale)}`
    } else {
        let match = text.match(/^Pesquisar (.+)\.\.\.$/i)
        if (match) {
            return `Search ${translateText(match[1], locale).toLowerCase()}...`
        }

        match = text.match(/^Editar (.+)$/)
        if (match) return `Edit ${match[1]}`

        match = text.match(/^Excluir (.+)$/)
        if (match) return `Delete ${match[1]}`

        match = text.match(/^Quantidade para (.+)$/)
        if (match) return `Quantity for ${match[1]}`

        match = text.match(/^Resultado para (.+)$/)
        if (match) return `Result for ${match[1]}`

        match = text.match(/^(\d+) registros · Página (\d+)$/)
        if (match) return `${match[1]} records · Page ${match[2]}`
    }

    return null
}

export const translateText = (
    text: string,
    locale: ComandosLocale
): string => {
    if (!text) return text

    const trimmed = text.trim()
    if (!trimmed) return text

    const map =
        locale === "pt-BR"
            ? englishToPortuguese
            : portugueseToEnglish

    const exact = map.get(trimmed)

    if (exact !== undefined) {
        return text.replace(trimmed, exact)
    }

    // Translate decorated labels from the base catalog entry.

    const decorated = trimmed.match(/^(\+\s*)?(.+?)(\s*:\s*\*?|\s+\*)$/)

    if (decorated) {

        const prefix = decorated[1] ?? ""

        const base = decorated[2].trim()

        const suffix = decorated[3] ?? ""

        const translatedBase = map.get(base)


        if (translatedBase !== undefined) {

            return text.replace(trimmed, `${prefix}${translatedBase}${suffix}`)

        }

    }


    const prefixedAction = trimmed.match(/^\+\s+(.+)$/)

    if (prefixedAction) {

        const translatedAction = map.get(prefixedAction[1])

        if (translatedAction !== undefined) {

            return text.replace(trimmed, `+ ${translatedAction}`)

        }

    }


    const dynamic = replaceDynamic(trimmed, locale)

    if (dynamic !== null) {
        return text.replace(trimmed, dynamic)
    }

    // Translate compound labels separated by common UI separators.
    const parts = trimmed.split(/(\s+[·|/]\s+|:\s+)/)

    if (parts.length > 1) {
        const translated = parts
            .map((part) => {
                if (/^\s+[·|/]\s+$/.test(part) || /^:\s+$/.test(part)) {
                    return part
                }

                return map.get(part) ?? part
            })
            .join("")

        if (translated !== trimmed) {
            return text.replace(trimmed, translated)
        }
    }

    return text
}

export const hasTranslation = (text: string): boolean => {
    const trimmed = text.trim()

    return (
        englishToPortuguese.has(trimmed) ||
        portugueseToEnglish.has(trimmed) ||
        replaceDynamic(trimmed, "pt-BR") !== null ||
        replaceDynamic(trimmed, "en-US") !== null
    )
}
