import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearCsrfToken } from '../../api/http'
import { StockCountPage } from '../inventory/StockCountPage'
import { CustomerPage } from './CustomerPage'

describe('Stock count and customer pages', () => {
  beforeEach(() => { clearCsrfToken(); vi.stubGlobal('fetch', vi.fn()) })
  afterEach(() => { cleanup(); vi.unstubAllGlobals() })

  it('ซ่อน anonymize จาก Cashier แต่ยังให้เพิ่มลูกค้าได้', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(page([{ id: 'c1', name: 'สมชาย', phone: '0812345678', note: null, marketingConsent: false, consentUpdatedAt: '2026-07-15T10:00:00Z', active: true, anonymizedAt: null }])))
    renderPage(<CustomerPage role="CASHIER" />)
    expect(await screen.findByText('สมชาย')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'เพิ่มลูกค้า' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Anonymize' })).not.toBeInTheDocument()
  })

  it('ให้ Manager อนุมัติ Count ที่รอพิจารณา', async () => {
    vi.mocked(fetch).mockImplementation(async (input, init) => {
      const url = input.toString()
      if (url.includes('/inventory/balances')) return jsonResponse(page([]))
      if (url.includes('/inventory/counts?')) return jsonResponse(page([{ id: 'count-1', status: 'SUBMITTED', reason: 'นับประจำเดือน', countedAt: '2026-07-15T10:00:00Z', createdBy: 'u1', approvedBy: null, items: [{ productId: 'p1', sku: 'SKU-1', productName: 'สินค้า', expectedOnHand: 2, countedQuantity: 3, difference: 1 }] }]))
      if (url.endsWith('/auth/csrf')) return jsonResponse({ headerName: 'X-CSRF-TOKEN', token: 'csrf' })
      if (url.endsWith('/inventory/counts/count-1/approve') && init?.method === 'POST') return jsonResponse({ id: 'count-1', status: 'APPROVED' })
      throw new Error(`unexpected ${url}`)
    })
    const user = userEvent.setup(); renderPage(<StockCountPage role="MANAGER" />)
    await user.click(await screen.findByRole('button', { name: 'อนุมัติ' }))
    expect(vi.mocked(fetch).mock.calls.some(([url]) => url === '/api/v1/inventory/counts/count-1/approve')).toBe(true)
  })
})

function renderPage(ui: React.ReactNode) { const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } }); return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>) }
function page(content: unknown[]) { return { content, page: 0, size: 20, totalElements: content.length, totalPages: content.length ? 1 : 0, first: true, last: true } }
function jsonResponse(body: unknown) { return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } }) }
