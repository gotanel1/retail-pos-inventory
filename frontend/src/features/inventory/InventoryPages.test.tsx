import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearCsrfToken } from '../../api/http'
import { GoodsReceiptPage } from './GoodsReceiptPage'
import { InventoryPage } from './InventoryPage'

describe('Inventory pages', () => {
  beforeEach(() => {
    clearCsrfToken()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('แสดง onHand, reserved, available และ movement แยกความหมาย', async () => {
    vi.mocked(fetch).mockImplementation(async (input) => {
      const url = input.toString()
      if (url.includes('/inventory/balances')) {
        return jsonResponse(page([{ productId: 'p1', sku: 'HAMMER-01', productName: 'ค้อนหงอน', lowStockThreshold: 3, onHand: 10, reserved: 2, available: 8, averageCost: 100, lowStock: false }]))
      }
      return jsonResponse(page([{ id: 'm1', productId: 'p1', sku: 'HAMMER-01', productName: 'ค้อนหงอน', movementType: 'RECEIVE', quantityDelta: 10, onHandAfter: 10, reservedAfter: 0, unitCost: 100, averageCostAfter: 100, referenceType: 'GOODS_RECEIPT', referenceId: 'r1', reason: null, actorUserId: 'u1', occurredAt: '2026-07-15T10:00:00Z' }]))
    })

    renderWithQueryClient(<InventoryPage />)

    expect(await screen.findByText('ค้อนหงอน')).toBeInTheDocument()
    expect(screen.getByText('+10')).toBeInTheDocument()
    expect(screen.getByText('รับเข้า')).toBeInTheDocument()
    expect(screen.getByText('฿100.0000')).toBeInTheDocument()
  })

  it('ส่ง Goods Receipt พร้อม CSRF แล้ว refresh ยอดสต็อก', async () => {
    vi.mocked(fetch).mockImplementation(async (input, init) => {
      const url = input.toString()
      if (url.endsWith('/api/v1/suppliers')) return jsonResponse([{ id: 's1', name: 'Supplier A', phone: null, note: null, active: true }])
      if (url.includes('/api/v1/products?')) return jsonResponse(page([{ id: 'p1', categoryId: 'c1', categoryName: 'Tools', sku: 'HAMMER-01', barcode: null, name: 'ค้อนหงอน', salePrice: 250, lowStockThreshold: 3, active: true }]))
      if (url.includes('/api/v1/goods-receipts?')) return jsonResponse(page([]))
      if (url.endsWith('/api/v1/auth/csrf')) return jsonResponse({ headerName: 'X-CSRF-TOKEN', token: 'csrf-value' })
      if (url.endsWith('/api/v1/goods-receipts') && init?.method === 'POST') return jsonResponse({ id: 'r1' }, 201)
      throw new Error(`unexpected request: ${url}`)
    })
    const user = userEvent.setup()
    renderWithQueryClient(<GoodsReceiptPage />)

    await user.click(await screen.findByRole('combobox', { name: /Supplier/ }))
    await user.click(screen.getByRole('option', { name: 'Supplier A' }))
    await user.click(screen.getByRole('combobox', { name: 'สินค้ารายการ 1' }))
    await user.click(screen.getByRole('option', { name: /HAMMER-01/ }))
    await user.clear(screen.getByLabelText('จำนวนรายการ 1'))
    await user.type(screen.getByLabelText('จำนวนรายการ 1'), '10')
    await user.clear(screen.getByLabelText('ต้นทุนรายการ 1'))
    await user.type(screen.getByLabelText('ต้นทุนรายการ 1'), '100.0000')
    await user.click(screen.getByRole('button', { name: 'ยืนยันรับสินค้า' }))

    expect(await screen.findByText('บันทึกรับสินค้าและอัปเดต Stock Ledger แล้ว')).toBeInTheDocument()
    const request = vi.mocked(fetch).mock.calls.find(([url, init]) => url === '/api/v1/goods-receipts' && init?.method === 'POST')
    expect(request).toBeDefined()
    expect(new Headers(request?.[1]?.headers).get('X-CSRF-TOKEN')).toBe('csrf-value')
    expect(JSON.parse(String(request?.[1]?.body))).toEqual(expect.objectContaining({
      supplierId: 's1',
      items: [{ productId: 'p1', quantity: 10, unitCost: '100' }],
    }))
    await waitFor(() => expect(fetch).toHaveBeenCalled())
  })
})

function renderWithQueryClient(ui: React.ReactNode) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
}

function page(content: unknown[]) {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: content.length ? 1 : 0, first: true, last: true }
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}
