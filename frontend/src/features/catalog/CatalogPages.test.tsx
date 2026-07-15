import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearCsrfToken } from '../../api/http'
import { ProductImportPage } from './ProductImportPage'
import { ProductPage } from './ProductPage'

describe('Catalog pages', () => {
  beforeEach(() => {
    clearCsrfToken()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('ให้แคชเชียร์ดูรายการสินค้าแต่ไม่แสดงปุ่มแก้ไข', async () => {
    vi.mocked(fetch).mockImplementation(async (input) => {
      const url = input.toString()
      if (url.includes('/products?')) {
        return jsonResponse({
          content: [{
            id: 'product-1',
            categoryId: 'category-1',
            categoryName: 'เครื่องมือ',
            sku: 'HAMMER-01',
            barcode: '885000000001',
            name: 'ค้อนหงอน',
            salePrice: 250,
            lowStockThreshold: 3,
            active: true,
          }],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        })
      }
      return jsonResponse([{ id: 'category-1', name: 'เครื่องมือ', active: true }])
    })

    renderWithQueryClient(<ProductPage role="CASHIER" />)

    expect(await screen.findByText('ค้อนหงอน')).toBeInTheDocument()
    expect(screen.getByText('HAMMER-01')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'เพิ่มสินค้า' })).not.toBeInTheDocument()
  })

  it('อัปโหลดแบบ multipart ตรวจ preview แล้วจึงยืนยันนำเข้า', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-CSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({
        importId: 'import-1',
        filename: 'products.csv',
        totalRows: 1,
        validRows: 1,
        invalidRows: 0,
        expiresAt: '2026-07-15T11:00:00Z',
        rows: [{
          rowNumber: 2,
          sku: 'HAMMER-01',
          barcode: '885000000001',
          name: 'ค้อนหงอน',
          category: 'เครื่องมือ',
          salePrice: 250,
          lowStockThreshold: 3,
          errors: [],
        }],
      }, 201))
      .mockResolvedValueOnce(jsonResponse({ importId: 'import-1', createdProducts: 1 }))
    const user = userEvent.setup()
    renderWithQueryClient(<ProductImportPage />)
    const file = new File(
      ['sku,barcode,name,category,salePrice,lowStockThreshold\nHAMMER-01,885000000001,ค้อนหงอน,เครื่องมือ,250,3'],
      'products.csv',
      { type: 'text/csv' },
    )

    await user.upload(screen.getByLabelText('เลือกไฟล์ CSV'), file)
    await user.click(screen.getByRole('button', { name: 'ตรวจไฟล์' }))

    expect(await screen.findByText('ค้อนหงอน')).toBeInTheDocument()
    const previewRequest = vi.mocked(fetch).mock.calls[1]
    expect(previewRequest[0]).toBe('/api/v1/product-imports/preview')
    expect(previewRequest[1]?.body).toBeInstanceOf(FormData)
    expect(new Headers(previewRequest[1]?.headers).has('Content-Type')).toBe(false)
    expect(new Headers(previewRequest[1]?.headers).get('X-CSRF-TOKEN')).toBe('csrf-value')

    await user.click(screen.getByRole('button', { name: 'ยืนยันนำเข้า 1 รายการ' }))

    expect(await screen.findByText('สร้างสินค้าแล้ว 1 รายการ')).toBeInTheDocument()
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(3))
  })
})

function renderWithQueryClient(ui: React.ReactNode) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
