import { apiRequest } from '../../api/http'
import type { PageResponse } from '../inventory/inventoryApi'

export type SaleStatus = 'DRAFT' | 'AWAITING_PAYMENT' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED'
export type DiscountType = 'AMOUNT' | 'PERCENT'

export interface SaleItem {
  productId: string
  sku: string
  name: string
  quantity: number
  unitPrice: number
  unitCostSnapshot: number | null
  lineTotal: number
}

export interface Sale {
  id: string
  receiptNumber: string | null
  status: SaleStatus
  customerId: string | null
  subtotal: number
  discountType: DiscountType | null
  discountValue: number
  discountAmount: number
  vatEnabled: boolean
  vatRate: number
  vatAmount: number
  total: number
  cashReceived: number | null
  changeAmount: number | null
  discountApprovedBy: string | null
  completedAt: string | null
  items: SaleItem[]
}

export interface StoreSettings {
  storeName: string
  vatEnabled: boolean
  vatRate: number
  receiptFooter: string | null
}

export interface PromptPayPayment {
  paymentId: string
  saleId: string
  amount: number
  paymentIntentId: string
  qrCodeImageUrl: string
  expiresAt: string
  status: 'PENDING'
}

export function getSales() {
  return apiRequest<PageResponse<Sale>>('/sales?page=0&size=20&sort=createdAt,desc')
}

export function getSale(saleId: string) {
  return apiRequest<Sale>(`/sales/${saleId}`)
}

export function createSale(input: { customerId: string | null; items: { productId: string; quantity: number }[] }) {
  return apiRequest<Sale>('/sales', { method: 'POST', body: JSON.stringify(input) })
}

export function applyDiscount(saleId: string, input: { type: DiscountType; value: string; managerUsername: string; managerPin: string }) {
  return apiRequest<Sale>(`/sales/${saleId}/discount`, { method: 'POST', body: JSON.stringify(input) })
}

export function checkoutCash(saleId: string, cashReceived: string, idempotencyKey: string) {
  return apiRequest<Sale>(`/sales/${saleId}/checkout/cash`, {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({ cashReceived }),
  })
}

export function checkoutPromptPay(saleId: string, idempotencyKey: string) {
  return apiRequest<PromptPayPayment>(`/sales/${saleId}/checkout/promptpay`, {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
  })
}

export function getStoreSettings() {
  return apiRequest<StoreSettings>('/store-settings')
}

export function updateStoreSettings(input: { storeName: string; vatEnabled: boolean; vatRate: string; receiptFooter: string }) {
  return apiRequest<StoreSettings>('/store-settings', { method: 'PUT', body: JSON.stringify(input) })
}

export function configureManagerPin(input: { currentPassword: string; pin: string }) {
  return apiRequest<void>('/auth/manager-pin', { method: 'POST', body: JSON.stringify(input) })
}
