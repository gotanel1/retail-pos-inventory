import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearCsrfToken } from '../../api/http'
import { PosPage } from './PosPage'

const item = { productId: 'p1', sku: 'HAMMER-01', name: 'ค้อนหงอน', quantity: 1, unitPrice: 107, unitCostSnapshot: null, lineTotal: 107 }
const draft = { id: 'sale-1', receiptNumber: null, status: 'DRAFT', customerId: null, subtotal: 107, discountType: null, discountValue: 0, discountAmount: 0, vatEnabled: true, vatRate: 7, vatAmount: 7, total: 107, cashReceived: null, changeAmount: null, discountApprovedBy: null, completedAt: null, items: [item] }

describe('POS cash flow', () => {
  beforeEach(() => { clearCsrfToken(); vi.stubGlobal('fetch', vi.fn()) })
  afterEach(() => { cleanup(); vi.unstubAllGlobals() })

  it('สร้างบิล ชำระเงินสดแบบ idempotent และแสดงใบเสร็จ', async () => {
    vi.mocked(fetch).mockImplementation(async (input, init) => {
      const url = input.toString()
      if (url.includes('/api/v1/products?')) return json(page([{ id: 'p1', categoryId: 'c1', categoryName: 'Tools', sku: 'HAMMER-01', barcode: null, name: 'ค้อนหงอน', salePrice: 107, lowStockThreshold: 1, active: true }]))
      if (url.includes('/api/v1/customers?')) return json(page([]))
      if (url.endsWith('/api/v1/store-settings')) return json({ storeName: 'ร้านทดสอบ', vatEnabled: true, vatRate: 7, receiptFooter: 'ขอบคุณค่ะ' })
      if (url.endsWith('/api/v1/auth/csrf')) return json({ headerName: 'X-CSRF-TOKEN', token: 'csrf-value' })
      if (url.endsWith('/api/v1/sales') && init?.method === 'POST') return json(draft, 201)
      if (url.endsWith('/api/v1/sales/sale-1/checkout/cash')) return json({ ...draft, status: 'COMPLETED', receiptNumber: 'R-000001', cashReceived: 120, changeAmount: 13, completedAt: '2026-07-15T10:00:00Z', items: [{ ...item, unitCostSnapshot: 60 }] })
      throw new Error(`unexpected request: ${url}`)
    })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('combobox', { name: 'สินค้า' }))
    await user.click(screen.getByRole('option', { name: /HAMMER-01/ }))
    await user.click(screen.getByRole('button', { name: 'เพิ่ม' }))
    await user.click(screen.getByRole('button', { name: 'สร้างบิลและยืนยันราคา' }))
    expect(await screen.findByText('VAT 7% (รวมในราคา)')).toBeInTheDocument()
    await user.type(screen.getByLabelText('รับเงินสด'), '120')
    await user.click(screen.getByRole('button', { name: 'ชำระเงินสด' }))

    expect(await screen.findByText((_, element) => element?.tagName === 'P' && element.textContent === 'เลขที่: R-000001')).toBeInTheDocument()
    expect(screen.getByText('ไม่ใช่ใบกำกับภาษี')).toBeInTheDocument()
    expect(screen.getByText('ขอบคุณค่ะ')).toBeInTheDocument()
    const checkout = vi.mocked(fetch).mock.calls.find(([url]) => url === '/api/v1/sales/sale-1/checkout/cash')
    expect(new Headers(checkout?.[1]?.headers).get('Idempotency-Key')).toBeTruthy()
    expect(new Headers(checkout?.[1]?.headers).get('X-CSRF-TOKEN')).toBe('csrf-value')
    await waitFor(() => expect(fetch).toHaveBeenCalled())
  })
})

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={client}><PosPage /></QueryClientProvider>)
}

function page(content: unknown[]) { return { content, page: 0, size: 200, totalElements: content.length, totalPages: 1, first: true, last: true } }
function json(body: unknown, status = 200) { return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }) }
