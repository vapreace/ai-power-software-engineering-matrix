// Textos da interface para os valores técnicos da API e formatos de data (DA23, RNF02).
import type { Progress, ProjectStatus, Role } from './api/types'

export const roleLabels: Record<Role, string> = {
  ADMINISTRATOR: 'Administrador',
  MANAGER: 'Gestor',
  MEMBER: 'Membro',
  OBSERVER: 'Observador',
}

export const projectStatusLabels: Record<ProjectStatus, string> = {
  ACTIVE: 'Ativo',
  COMPLETED: 'Concluído',
  ARCHIVED: 'Arquivado',
}

/** Data ISO (aaaa-mm-dd) no formato dd/mm/aaaa (DA23). */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) {
    return ''
  }
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

/**
 * Data escrita como dd/mm/aaaa, em ISO. Devolve null quando está vazia e
 * undefined quando não é uma data válida.
 */
export function parseDate(text: string): string | null | undefined {
  const value = text.trim()
  if (!value) {
    return null
  }
  const match = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(value)
  if (!match) {
    return undefined
  }
  const [, d, m, y] = match
  const day = Number(d)
  const month = Number(m)
  const year = Number(y)
  const date = new Date(Date.UTC(year, month - 1, day))
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) {
    return undefined
  }
  return `${y}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

/** "Sem atrasos", "1 tarefa atrasada" ou "N tarefas atrasadas" (US10). */
export function overdueText(count: number): string {
  if (count === 0) {
    return 'Sem atrasos'
  }
  return count === 1 ? '1 tarefa atrasada' : `${count} tarefas atrasadas`
}

/** "60% (6 de 10 concluídas)" ou "Sem tarefas" (US09, E1). */
export function progressText(progress: Progress): string {
  if (progress.percentage === null) {
    return 'Sem tarefas'
  }
  return `${progress.percentage}% (${progress.completedTasks} de ${progress.totalTasks} concluídas)`
}

/** Confirmação de RN31 / US20, com o número de tarefas no singular ou no plural. */
export function deleteProjectMessage(name: string, totalTasks: number): string {
  const tasks =
    totalTasks === 0 ? '' : totalTasks === 1 ? ' e a sua tarefa' : ` e as suas ${totalTasks} tarefas`
  return `Eliminar o projeto «${name}»${tasks}? Esta ação não pode ser desfeita.`
}

/** Contagem de caracteres como no backend: cada código Unicode conta um (DA11). */
export function textLength(text: string): number {
  return [...text.normalize('NFC').trim()].length
}
