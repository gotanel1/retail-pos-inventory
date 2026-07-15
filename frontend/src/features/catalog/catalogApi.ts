import { apiRequest } from '../../api/http'

export interface Category {
  id: string
  name: string
  active: boolean
}

export interface Product {
  id: string
  categoryId: string
  categoryName: string
  sku: string
  barcode: string | null
  name: string
  salePrice: number
  lowStockThreshold: number
  active: boolean
}

export interface ProductPage {
  content: Product[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface CreateProductInput {
  categoryId: string
  sku: string
  barcode: string
  name: string
  salePrice: string
  lowStockThreshold: number
}

export interface ImportRowPreview {
  rowNumber: number
  sku: string
  barcode: string | null
  name: string
  category: string
  salePrice: number | null
  lowStockThreshold: number | null
  errors: string[]
}

export interface ImportPreview {
  importId: string
  filename: string
  totalRows: number
  validRows: number
  invalidRows: number
  expiresAt: string
  rows: ImportRowPreview[]
}

export interface ImportCommitResult {
  importId: string
  createdProducts: number
}

export function getCategories() {
  return apiRequest<Category[]>('/categories')
}

export function getProducts(search: string, page: number) {
  const params = new URLSearchParams({ page: page.toString(), size: '20', sort: 'name,asc' })
  if (search.trim()) {
    params.set('search', search.trim())
  }
  return apiRequest<ProductPage>(`/products?${params.toString()}`)
}

export function createProduct(input: CreateProductInput) {
  return apiRequest<Product>('/products', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function previewProductImport(file: File) {
  const formData = new FormData()
  formData.set('file', file)
  return apiRequest<ImportPreview>('/product-imports/preview', {
    method: 'POST',
    body: formData,
  })
}

export function commitProductImport(importId: string) {
  return apiRequest<ImportCommitResult>(`/product-imports/${importId}/commit`, {
    method: 'POST',
  })
}
