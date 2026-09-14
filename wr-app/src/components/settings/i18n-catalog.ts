"use client"

export type ComandosLocale = "pt-BR" | "en-US"

type TranslationPair = readonly [english: string, portuguese: string]

export const translationPairs: TranslationPair[] = [
    // Shell / navigation
    ["Navigation", "Navegação"],
    ["Dashboard", "Painel"],
    ["Operational Dashboard", "Painel operacional"],
    ["Command Center", "Command Center"],
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
    ["Issue available equipment to a person or an organizational unit and record each return with stock history.", "Cautеле equipamentos disponíveis para uma pessoa ou unidade organizacional e registre cada devolução com histórico de estoque."],
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
    ["Supplier", "Fornecedor"]
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
