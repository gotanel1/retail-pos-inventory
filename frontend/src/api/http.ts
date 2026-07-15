export interface ProblemDetails {
  title?: string
  detail?: string
  status?: number
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly problem?: ProblemDetails,
  ) {
    super(problem?.detail ?? 'ไม่สามารถเชื่อมต่อกับระบบได้')
  }
}

interface CsrfResponse {
  headerName: string
  token: string
}

let csrfToken: CsrfResponse | undefined

export function clearCsrfToken() {
  csrfToken = undefined
}

async function getCsrfToken(): Promise<CsrfResponse> {
  if (csrfToken) {
    return csrfToken
  }

  const response = await fetch('/api/v1/auth/csrf', { credentials: 'include' })
  if (!response.ok) {
    throw new ApiError(response.status)
  }

  csrfToken = await response.json() as CsrfResponse
  return csrfToken
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = init.method?.toUpperCase() ?? 'GET'
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json, application/problem+json')

  if (init.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const csrf = await getCsrfToken()
    headers.set(csrf.headerName, csrf.token)
  }

  const response = await fetch(`/api/v1${path}`, {
    ...init,
    headers,
    credentials: 'include',
  })

  if (!response.ok) {
    let problem: ProblemDetails | undefined
    if (response.headers.get('content-type')?.includes('application/problem+json')) {
      problem = await response.json() as ProblemDetails
    }
    throw new ApiError(response.status, problem)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}
