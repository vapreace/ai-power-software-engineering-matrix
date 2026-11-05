// Tipos da API, com os nomes de openapi.yaml (DA08).

export type Role = 'ADMINISTRATOR' | 'MANAGER' | 'MEMBER' | 'OBSERVER'

export interface Reference {
  id: number
  name: string
}

export interface CurrentUser {
  id: number
  name: string
  email: string
  role: Role
  teams: Reference[]
  mustChangePassword: boolean
}
