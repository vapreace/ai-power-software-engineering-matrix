// Ações que a interface mostra conforme o papel (requirements.md, secção 3.1).
// Esconder um botão é conforto: a proteção que conta é a do backend (SEG02).
import type { CurrentUser, Project, Task } from './api/types'

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

/**
 * Mudar o estado de uma tarefa: o Administrador, o gestor do projeto e qualquer responsável da
 * tarefa, qualquer que seja o papel (RN15), também num projeto "Sem gestor" (RN22).
 */
export function canChangeTaskStatus(user: CurrentUser | null, project: Project, task: Task): boolean {
  if (!user) {
    return false
  }
  return canManageProject(user, project) || task.assignees.some((assignee) => assignee.id === user.id)
}

/** Comentar: o Administrador e as pessoas que pertencem à equipa do projeto (RN30). O Observador não comenta. */
export function canComment(user: CurrentUser | null, project: Project): boolean {
  if (!user) {
    return false
  }
  if (user.role === 'ADMINISTRATOR') {
    return true
  }
  return (
    (user.role === 'MANAGER' || user.role === 'MEMBER') && user.teams.some((team) => team.id === project.team.id)
  )
}

export function isAdministrator(user: CurrentUser | null): boolean {
  return user?.role === 'ADMINISTRATOR'
}
