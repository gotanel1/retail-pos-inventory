import { ApiError, apiRequest, clearCsrfToken } from '../../api/http'

export type Role = 'OWNER' | 'MANAGER' | 'CASHIER' | 'INVENTORY_STAFF'

export interface CurrentUser {
  id: string
  username: string
  displayName: string
  role: Role
}

export interface LoginInput {
  username: string
  password: string
}

export async function getCurrentUser(): Promise<CurrentUser | null> {
  try {
    return await apiRequest<CurrentUser>('/auth/me')
  }
  catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null
    }
    throw error
  }
}

export async function login(input: LoginInput): Promise<CurrentUser> {
  const user = await apiRequest<CurrentUser>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(input),
  })
  clearCsrfToken()
  return user
}

export async function logout(): Promise<void> {
  await apiRequest<void>('/auth/logout', { method: 'POST' })
  clearCsrfToken()
}
