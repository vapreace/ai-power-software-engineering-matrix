// Textos da interface para os valores técnicos da API (DA23, RNF02).
import type { Role } from './api/types'

export const roleLabels: Record<Role, string> = {
  ADMINISTRATOR: 'Administrador',
  MANAGER: 'Gestor',
  MEMBER: 'Membro',
  OBSERVER: 'Observador',
}
