// Painel inicial (openapi.yaml, tag Painel; US21).
import { request } from './http'
import type { Dashboard } from './types'

export function getDashboard(): Promise<Dashboard> {
  return request<Dashboard>('GET', '/dashboard')
}
