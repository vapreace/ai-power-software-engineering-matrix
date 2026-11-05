// Operações de sessão (openapi.yaml, tag Sessão; US01).
import { request } from './http'
import type { CurrentUser } from './types'

export function getSession(): Promise<CurrentUser> {
  return request<CurrentUser>('GET', '/session')
}

export function login(email: string, password: string): Promise<CurrentUser> {
  return request<CurrentUser>('POST', '/session', { email, password })
}

export function logout(): Promise<void> {
  return request<void>('DELETE', '/session')
}

export function setNewPassword(newPassword: string): Promise<void> {
  return request<void>('PUT', '/session/password', { newPassword })
}
