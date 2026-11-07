import { httpClient } from "api/http";
import type { ImportExpense, ImportFile, ImportLine, ImportLineCost, ImportProcess, ImportStatusTransition, InventoryBalance, InventoryLot, InventoryMovement, InventoryReservation, InventoryTransfer, InventoryValuation, Shipment, ShipmentEvent, ShipmentItem, WarehouseBin } from "api/models/erp/logistics";

const root = "/api/erp/logistics";
const scope = (organizationId: number, unitId?: number) => ({ organizationId, ...(unitId ? { unitId } : {}) });

export const logisticsService = {
    bins: async (organizationId: number, unitId?: number, warehouseId?: string) =>
        (await httpClient.get<WarehouseBin[]>(`${root}/bins`, { params: { ...scope(organizationId, unitId), ...(warehouseId ? { warehouseId } : {}) } })).data,
    createBin: async (body: { id: string; organizationId: number; unitId?: number; warehouseId: string; parentBinId?: string; code: string; name: string; unitOfMeasure: string; capacity?: string }) =>
        (await httpClient.post<WarehouseBin>(`${root}/bins`, body)).data,
    lots: async (organizationId: number, unitId?: number, productId?: string) =>
        (await httpClient.get<InventoryLot[]>(`${root}/lots`, { params: { ...scope(organizationId, unitId), ...(productId ? { productId } : {}) } })).data,
    createLot: async (body: { id: string; organizationId: number; unitId?: number; productId: string; lotNumber: string; manufacturedOn?: string; expiresOn?: string; supplierId?: string; originReference?: string }) =>
        (await httpClient.post<InventoryLot>(`${root}/lots`, body)).data,
    updateLotStatus: async (id: string, organizationId: number, unitId: number | undefined, status: InventoryLot["status"]) =>
        (await httpClient.put<InventoryLot>(`${root}/lots/${encodeURIComponent(id)}/status`, { status }, { params: scope(organizationId, unitId) })).data,
    balances: async (binId: string, organizationId: number, unitId: number | undefined, productId: string, unitOfMeasure: string) =>
        (await httpClient.get<InventoryBalance[]>(`${root}/bins/${encodeURIComponent(binId)}/balances`, { params: { ...scope(organizationId, unitId), productId, unitOfMeasure } })).data,
    valuation: async (binId: string, organizationId: number, unitId: number | undefined, productId: string, lotId: string | null, unitOfMeasure: string, method: InventoryValuation["method"] = "WEIGHTED_AVERAGE") =>
        (await httpClient.get<InventoryValuation>(`${root}/bins/${encodeURIComponent(binId)}/valuation`, { params: { ...scope(organizationId, unitId), productId, ...(lotId ? { lotId } : {}), unitOfMeasure, method } })).data,
    movements: async (binId: string, organizationId: number, unitId?: number, productId?: string) =>
        (await httpClient.get<InventoryMovement[]>(`${root}/bins/${encodeURIComponent(binId)}/movements`, { params: { ...scope(organizationId, unitId), ...(productId ? { productId } : {}) } })).data,
    receive: async (binId: string, organizationId: number, unitId: number | undefined, body: { id: string; productId: string; lotId?: string; quantity: string; unitOfMeasure: string; referenceType?: string; referenceId?: string; unitCost?: string; currency?: string }) =>
        (await httpClient.post(`${root}/bins/${encodeURIComponent(binId)}/receipts`, body, { params: scope(organizationId, unitId) })).data,
    adjust: async (binId: string, organizationId: number, unitId: number | undefined, body: { id: string; productId: string; lotId?: string; type: "ADJUSTMENT_IN" | "ADJUSTMENT_OUT"; quantity: string; unitOfMeasure: string; referenceId?: string }) =>
        (await httpClient.post<InventoryMovement>(`${root}/bins/${encodeURIComponent(binId)}/adjustments`, body, { params: scope(organizationId, unitId) })).data,
    reserve: async (binId: string, organizationId: number, unitId: number | undefined, body: { id: string; productId: string; lotId?: string; quantity: string; unitOfMeasure: string; referenceType?: string; referenceId?: string }) =>
        (await httpClient.post<InventoryReservation>(`${root}/bins/${encodeURIComponent(binId)}/reservations`, body, { params: scope(organizationId, unitId) })).data,
    reserveFefo: async (binId: string, organizationId: number, unitId: number | undefined, body: { id: string; productId: string; quantity: string; unitOfMeasure: string; referenceType?: string; referenceId?: string }) =>
        (await httpClient.post<InventoryReservation[]>(`${root}/bins/${encodeURIComponent(binId)}/reservations/fefo`, body, { params: scope(organizationId, unitId) })).data,
    reservations: async (binId: string, organizationId: number, unitId?: number, productId?: string) =>
        (await httpClient.get<InventoryReservation[]>(`${root}/bins/${encodeURIComponent(binId)}/reservations`, { params: { ...scope(organizationId, unitId), ...(productId ? { productId } : {}) } })).data,
    releaseReservation: async (id: string, organizationId: number, unitId?: number) =>
        (await httpClient.post<InventoryReservation>(`${root}/reservations/${encodeURIComponent(id)}/release`, {}, { params: scope(organizationId, unitId) })).data,
    fulfillReservation: async (id: string, organizationId: number, unitId?: number) =>
        (await httpClient.post<InventoryReservation>(`${root}/reservations/${encodeURIComponent(id)}/fulfill`, {}, { params: scope(organizationId, unitId) })).data,
    transfer: async (body: { id: string; organizationId: number; unitId?: number; productId: string; lotId?: string; sourceLocationId: string; destinationLocationId: string; quantity: string; unitOfMeasure: string; referenceType?: string; referenceId?: string }) =>
        (await httpClient.post<InventoryTransfer>(`${root}/transfers`, body)).data,
    transfers: async (organizationId: number, unitId?: number, productId?: string) =>
        (await httpClient.get<InventoryTransfer[]>(`${root}/transfers`, { params: { ...scope(organizationId, unitId), ...(productId ? { productId } : {}) } })).data,
    shipments: async (organizationId: number, unitId?: number) => (await httpClient.get<Shipment[]>(`${root}/shipments`, { params: scope(organizationId, unitId) })).data,
    createShipment: async (body: { id: string; organizationId: number; unitId?: number; reference: string; originLocationId: string; destinationLocationId: string; carrier?: string; trackingNumber?: string; recipient?: string; notes?: string }) =>
        (await httpClient.post<Shipment>(`${root}/shipments`, body)).data,
    recordShipmentEvent: async (id: string, organizationId: number, unitId: number | undefined, body: { id: string; type: ShipmentEvent["type"]; locationId?: string; description: string; evidenceReference?: string }) =>
        (await httpClient.post<ShipmentEvent>(`${root}/shipments/${encodeURIComponent(id)}/events`, body, { params: scope(organizationId, unitId) })).data,
    shipmentItems: async (id: string, organizationId: number, unitId?: number) => (await httpClient.get<ShipmentItem[]>(`${root}/shipments/${encodeURIComponent(id)}/items`, { params: scope(organizationId, unitId) })).data,
    addShipmentItem: async (id: string, organizationId: number, unitId: number | undefined, body: { id: string; productId: string; lotId?: string; serialNumber?: string; quantity: number; unitOfMeasure: string }) =>
        (await httpClient.post<ShipmentItem>(`${root}/shipments/${encodeURIComponent(id)}/items`, body, { params: scope(organizationId, unitId) })).data,
    imports: async (organizationId: number, unitId?: number) => (await httpClient.get<ImportProcess[]>(`${root}/imports`, { params: scope(organizationId, unitId) })).data,
    importHistory: async (id: string, organizationId: number, unitId?: number) => (await httpClient.get<ImportStatusTransition[]>(`${root}/imports/${encodeURIComponent(id)}/history`, { params: scope(organizationId, unitId) })).data,
    importLines: async (id: string, organizationId: number, unitId?: number) => (await httpClient.get<ImportLine[]>(`${root}/imports/${encodeURIComponent(id)}/lines`, { params: scope(organizationId, unitId) })).data,
    addImportLine: async (id: string, organizationId: number, unitId: number | undefined, body: { id: string; productId: string; lotId?: string; quantity: number; unitOfMeasure: string; unitPrice: number; currency: string; exchangeRate: number; baseUnitPrice: number; baseCurrency: string; supplierLineReference?: string }) => (await httpClient.post<ImportLine>(`${root}/imports/${encodeURIComponent(id)}/lines`, body, { params: scope(organizationId, unitId) })).data,
    importLineCost: async (id: string, lineId: string, organizationId: number, unitId?: number) => (await httpClient.get<ImportLineCost>(`${root}/imports/${encodeURIComponent(id)}/lines/${encodeURIComponent(lineId)}/landed-cost`, { params: scope(organizationId, unitId) })).data,
    receiveImportLine: async (id: string, lineId: string, organizationId: number, unitId: number | undefined, body: { receiptId: string; quantity: number }) => (await httpClient.post<InventoryMovement>(`${root}/imports/${encodeURIComponent(id)}/lines/${encodeURIComponent(lineId)}/receipts`, body, { params: scope(organizationId, unitId) })).data,
    updateImportStatus: async (id: string, organizationId: number, unitId: number | undefined, body: { status: ImportProcess["status"]; arrivedOn?: string; clearedOn?: string; customsReference?: string; notes?: string }) =>
        (await httpClient.post<ImportProcess>(`${root}/imports/${encodeURIComponent(id)}/status`, body, { params: scope(organizationId, unitId) })).data,
    createImport: async (body: { id: string; organizationId: number; unitId?: number; reference: string; supplierId: string; supplierCountry: string; purchaseReference?: string; currency: string; exchangeRate: number; orderedOn: string; estimatedArrival?: string; destinationLocationId: string; notes?: string }) =>
        (await httpClient.post<ImportProcess>(`${root}/imports`, body)).data,
    addImportExpense: async (id: string, organizationId: number, unitId: number | undefined, body: { id: string; category: string; amount: number; currency: string; exchangeRate: number; baseCurrencyAmount: number; incurredOn: string; documentReference?: string; notes?: string; metadata?: Record<string, string> }) =>
        (await httpClient.post<ImportExpense>(`${root}/imports/${encodeURIComponent(id)}/expenses`, body, { params: scope(organizationId, unitId) })).data,
    importExpenses: async (id: string, organizationId: number, unitId?: number) => (await httpClient.get<ImportExpense[]>(`${root}/imports/${encodeURIComponent(id)}/expenses`, { params: scope(organizationId, unitId) })).data,
    addImportDocument: async (id: string, organizationId: number, unitId: number | undefined, body: { id: string; type: string; documentReference: string; fileName: string; checksum?: string }) =>
        (await httpClient.post<ImportFile>(`${root}/imports/${encodeURIComponent(id)}/documents`, body, { params: scope(organizationId, unitId) })).data,
    importDocuments: async (id: string, organizationId: number, unitId?: number) => (await httpClient.get<ImportFile[]>(`${root}/imports/${encodeURIComponent(id)}/documents`, { params: scope(organizationId, unitId) })).data
};
