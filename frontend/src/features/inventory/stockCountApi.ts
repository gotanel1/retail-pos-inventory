import { apiRequest } from '../../api/http'
import type { PageResponse } from './inventoryApi'

export interface CountItem { productId: string; sku: string; productName: string; expectedOnHand: number; countedQuantity: number; difference: number }
export interface InventoryCount { id: string; status: 'SUBMITTED' | 'APPROVED' | 'REJECTED'; reason: string; countedAt: string; createdBy: string; approvedBy: string | null; items: CountItem[] }
export function getInventoryCounts() { return apiRequest<PageResponse<InventoryCount>>('/inventory/counts?page=0&size=50&sort=createdAt,desc') }
export function submitInventoryCount(input: { countedAt: string; reason: string; items: Array<{ productId: string; countedQuantity: number }> }) { return apiRequest<InventoryCount>('/inventory/counts', { method: 'POST', body: JSON.stringify(input) }) }
export function approveInventoryCount(id: string) { return apiRequest<InventoryCount>(`/inventory/counts/${id}/approve`, { method: 'POST' }) }
export function rejectInventoryCount(id: string) { return apiRequest<InventoryCount>(`/inventory/counts/${id}/reject`, { method: 'POST' }) }
