import { apiRequest } from '../../api/http'

export interface DashboardReport {
  from: string
  to: string
  summary: {
    salesCount: number
    totalSales: number
    netSalesExcludingVat: number
    costOfGoodsSold: number
    grossProfit: number
    inventoryValue: number
  }
  payments: { method: 'CASH' | 'PROMPTPAY'; transactionCount: number; amount: number }[]
  lowStock: { productId: string; sku: string; name: string; onHand: number; reserved: number; available: number; lowStockThreshold: number }[]
  movements: {
    id: string
    productId: string
    sku: string
    productName: string
    movementType: 'OPENING' | 'RECEIVE' | 'SALE' | 'ADJUSTMENT_IN' | 'ADJUSTMENT_OUT'
    quantityDelta: number
    onHandAfter: number
    reservedAfter: number
    referenceType: string
    referenceId: string
    reason: string | null
    actorUserId: string
    occurredAt: string
  }[]
}

export function getDashboard(from: string, to: string) {
  const query = new URLSearchParams({ from, to })
  return apiRequest<DashboardReport>(`/reports/dashboard?${query}`)
}
