import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ReportingPage } from './ReportingPage'

describe('Reporting dashboard', () => {
  beforeEach(() => vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({
    from: '2026-07-15', to: '2026-07-15',
    summary: { salesCount: 2, totalSales: 214, netSalesExcludingVat: 200, costOfGoodsSold: 120, grossProfit: 80, inventoryValue: 600 },
    payments: [{ method: 'CASH', transactionCount: 1, amount: 107 }, { method: 'PROMPTPAY', transactionCount: 1, amount: 107 }],
    lowStock: [{ productId: 'p1', sku: 'HAMMER-01', name: 'ค้อนหงอน', onHand: 1, reserved: 0, available: 1, lowStockThreshold: 1 }],
    movements: [{ id: 'm1', productId: 'p1', sku: 'HAMMER-01', productName: 'ค้อนหงอน', movementType: 'SALE', quantityDelta: -1, onHandAfter: 1, reservedAfter: 0, referenceType: 'SALE', referenceId: 's1', reason: null, actorUserId: 'u1', occurredAt: '2026-07-15T10:00:00Z' }],
  }), { headers: { 'Content-Type': 'application/json' } }))))
  afterEach(() => { cleanup(); vi.unstubAllGlobals() })

  it('แสดงยอดขาย กำไร วิธีชำระ และ movement จาก server', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><ReportingPage /></QueryClientProvider>)
    expect(await screen.findByText('฿80.00')).toBeInTheDocument()
    expect(screen.getByText('PromptPay')).toBeInTheDocument()
    expect(screen.getAllByText(/HAMMER-01/)).toHaveLength(2)
    expect(screen.getByText('ขาย')).toBeInTheDocument()
  })
})
