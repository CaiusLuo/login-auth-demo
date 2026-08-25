export interface User {
  id: number
  username: string
  role: 'USER' | 'ADMIN'
  enabled: boolean
  createdAt: string
}

interface ApiError { message?: string }

let csrfToken = ''

async function ensureCsrf(): Promise<string> {
  if (csrfToken) return csrfToken
  const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
  if (!response.ok) throw new Error('无法初始化安全令牌')
  csrfToken = (await response.json()).token
  return csrfToken
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    headers.set('X-XSRF-TOKEN', await ensureCsrf())
  }
  const response = await fetch(path, { ...init, headers, credentials: 'same-origin' })
  if (!response.ok) {
    const error = await response.json().catch(() => ({} as ApiError))
    throw new Error(error.message ?? `请求失败 (${response.status})`)
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

export async function login(username: string, password: string): Promise<void> {
  const body = new URLSearchParams({ username, password })
  await api('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  csrfToken = ''
}

export async function logout(): Promise<void> {
  await api('/api/auth/logout', { method: 'POST' })
  csrfToken = ''
}
