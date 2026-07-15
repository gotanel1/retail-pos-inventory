import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearCsrfToken } from './api/http'
import { App } from './App'

const owner = {
  id: '9a0ef9dd-589f-47aa-8fa7-2a84ed1d0d69',
  username: 'owner',
  displayName: 'เจ้าของร้าน',
  role: 'OWNER',
}

describe('App authentication flow', () => {
  beforeEach(() => {
    clearCsrfToken()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('แสดงหน้าเข้าสู่ระบบเมื่อยังไม่มี session', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(problemResponse(401))

    renderApp()

    expect(await screen.findByRole('heading', { name: 'เข้าสู่ระบบ' })).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith('/api/v1/auth/me', expect.objectContaining({ credentials: 'include' }))
  })

  it('ขอ CSRF token ก่อนส่งข้อมูลเข้าสู่ระบบ', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(problemResponse(401))
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-CSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse(owner))
    const user = userEvent.setup()

    renderApp()
    await user.type(await screen.findByLabelText(/ชื่อผู้ใช้/), 'owner')
    await user.type(screen.getByLabelText(/รหัสผ่าน/), 'correct-password')
    await user.click(screen.getByRole('button', { name: 'เข้าสู่ระบบ' }))

    expect(await screen.findByRole('heading', { name: 'Retail POS & Inventory' })).toBeInTheDocument()
    expect(screen.getByText(/เจ้าของร้าน · เจ้าของร้าน/)).toBeInTheDocument()
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(3))

    const loginRequest = vi.mocked(fetch).mock.calls[2]
    expect(loginRequest[0]).toBe('/api/v1/auth/login')
    expect(new Headers(loginRequest[1]?.headers).get('X-CSRF-TOKEN')).toBe('csrf-value')
    expect(loginRequest[1]).toEqual(expect.objectContaining({ credentials: 'include', method: 'POST' }))
  })

  it('แสดงหน้าหลักเมื่อ session ยังใช้งานได้', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(owner))

    renderApp()

    expect(await screen.findByText(/เจ้าของร้าน · เจ้าของร้าน/)).toBeInTheDocument()
    expect(screen.getByText('Stock Ledger')).toBeInTheDocument()
  })
})

function renderApp() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>,
  )
}

function jsonResponse(body: unknown) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

function problemResponse(status: number) {
  return new Response(JSON.stringify({ status, title: 'ต้องเข้าสู่ระบบ' }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}
