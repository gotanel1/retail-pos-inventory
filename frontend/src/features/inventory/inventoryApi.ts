import { apiRequest } from '../../api/http'

export interface InventoryBalance {
  productId: string
  sku: string
  productName: string
  lowStockThreshold: number
  onHand: number
  reserved: number
  available: number
  averageCost: number
  lowStock: boolean
}

export interface StockMovement {
  id: string
  productId: string
  sku: string
  productName: string
  movementType: 'OPENING' | 'RECEIVE' | 'SALE' | 'ADJUSTMENT_IN' | 'ADJUSTMENT_OUT'
  quantityDelta: number
  onHandAfter: number
  reservedAfter: number
  unitCost: number | null
  averageCostAfter: number
  referenceType: string
  referenceId: string
  reason: string | null
  actorUserId: string
  occurredAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface Supplier {
  id: string
  name: string
  phone: string | null
  note: string | null
  active: boolean
}

export interface GoodsReceiptSummary {
  id: string
  supplierId: string
  supplierName: string
  referenceNumber: string | null
  status: 'POSTED'
  receivedAt: string
  receivedBy: string
}

export interface CreateGoodsReceiptInput {
  supplierId: string
  referenceNumber: string
  receivedAt: string
  note: string
  items: Array<{ productId: string; quantity: number; unitCost: string }>
}

export function getInventoryBalances(search = '', lowStock = false) {
  const params = new URLSearchParams({ page: '0', size: '100', sort: 'sku,asc', lowStock: String(lowStock) })
  if (search.trim()) params.set('search', search.trim())
  return apiRequest<PageResponse<InventoryBalance>>(`/inventory/balances?${params}`)
}

export function getStockMovements() {
  return apiRequest<PageResponse<StockMovement>>('/inventory/movements?page=0&size=20&sort=occurredAt,desc')
}

export function getSuppliers() {
  return apiRequest<Supplier[]>('/suppliers')
}

export function createSupplier(input: { name: string; phone: string; note: string }) {
  return apiRequest<Supplier>('/suppliers', { method: 'POST', body: JSON.stringify(input) })
}

export function getGoodsReceipts() {
  return apiRequest<PageResponse<GoodsReceiptSummary>>('/goods-receipts?page=0&size=20&sort=receivedAt,desc')
}

export function createGoodsReceipt(input: CreateGoodsReceiptInput) {
  return apiRequest('/goods-receipts', { method: 'POST', body: JSON.stringify(input) })
}
