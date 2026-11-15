// Ações que a interface mostra conforme o papel (requirements.md, secção 3.1).
// Esconder um botão é conforto: a proteção que conta é a do backend (SEG02).
import type { CurrentUser, Project } from './api/types'

export function canCreateProjects(user: CurrentUser | null): boolean {
  return user?.role === 'ADMINISTRATOR' || user?.role === 'MANAGER'
}

/** Administrador, ou o gestor do projeto (RN05). Num projeto "Sem gestor", só o Administrador (RN22). */
export function canManageProject(user: CurrentUser | null, project: Project): boolean {
  if (!user) {
    return false
  }
  if (user.role === 'ADMINISTRATOR') {
    return true
  }
  return (
    user.role === 'MANAGER' &&
    project.manager?.id === user.id &&
    user.teams.some((team) => team.id === project.team.id)
  )
}
