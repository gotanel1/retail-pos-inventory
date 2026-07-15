import { apiRequest } from '../../api/http'
import type { PageResponse } from '../inventory/inventoryApi'

export interface Customer { id: string; name: string; phone: string | null; note: string | null; marketingConsent: boolean; consentUpdatedAt: string; active: boolean; anonymizedAt: string | null }
export interface CustomerInput { name: string; phone: string; note: string; marketingConsent: boolean }

export function getCustomers(search = '') {
  const params = new URLSearchParams({ page: '0', size: '100', sort: 'name,asc' }); if (search.trim()) params.set('search', search.trim())
  return apiRequest<PageResponse<Customer>>(`/customers?${params}`)
}
export function createCustomer(input: CustomerInput) { return apiRequest<Customer>('/customers', { method: 'POST', body: JSON.stringify(input) }) }
export function anonymizeCustomer(id: string) { return apiRequest<void>(`/customers/${id}`, { method: 'DELETE' }) }
