// Contas (openapi.yaml, tag Contas; US02, US14, US18). Só o Administrador.
import { request } from './http'
import type { AccountStatus, CreateUserRequest, Items, Role, UpdateUserRequest, User } from './types'

export function listUsers(filters: { statuses?: AccountStatus[]; roles?: Role[] } = {}): Promise<Items<User>> {
  const params = new URLSearchParams()
  filters.statuses?.forEach((status) => params.append('status', status))
  filters.roles?.forEach((role) => params.append('role', role))
  const query = params.toString()
  return request<Items<User>>('GET', `/users${query ? `?${query}` : ''}`)
}

export function createUser(body: CreateUserRequest): Promise<User> {
  return request<User>('POST', '/users', body)
}

export function updateUser(id: number, body: UpdateUserRequest): Promise<User> {
  return request<User>('PATCH', `/users/${id}`, body)
}

export function changeUserStatus(id: number, status: AccountStatus): Promise<User> {
  return request<User>('PUT', `/users/${id}/status`, { status })
}

export function resetUserPassword(id: number, temporaryPassword: string): Promise<void> {
  return request<void>('PUT', `/users/${id}/password`, { temporaryPassword })
}
