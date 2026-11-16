// Tipos da API, com os nomes de openapi.yaml (DA08).

export type Role = 'ADMINISTRATOR' | 'MANAGER' | 'MEMBER' | 'OBSERVER'
export type AccountStatus = 'ACTIVE' | 'DEACTIVATED'
export type ProjectStatus = 'ACTIVE' | 'COMPLETED' | 'ARCHIVED'

export interface Reference {
  id: number
  name: string
}

export interface Items<T> {
  items: T[]
}

export interface CurrentUser {
  id: number
  name: string
  email: string
  role: Role
  teams: Reference[]
  mustChangePassword: boolean
}

export interface TeamMember {
  id: number
  name: string
  role: Role
  status: AccountStatus
}

/** `percentage` é null quando o projeto não tem tarefas ("Sem tarefas"). */
export interface Progress {
  completedTasks: number
  totalTasks: number
  percentage: number | null
}

export interface ProjectSummary {
  id: number
  name: string
  team: Reference
  /** null: "Sem gestor" (RN22). */
  manager: Reference | null
  status: ProjectStatus
  expectedEndDate: string | null
  progress: Progress
  overdueTaskCount: number
}

export interface Project extends ProjectSummary {
  description: string | null
  startDate: string
  version: number
}

export interface CreateProjectRequest {
  teamId: number | null
  name: string
  description: string | null
  startDate: string | null
  expectedEndDate: string | null
  managerId?: number | null
}

/** Só os campos enviados mudam; a versão é obrigatória (DA05, DA20). */
export interface UpdateProjectRequest {
  name?: string
  description?: string | null
  startDate?: string
  expectedEndDate?: string | null
  version: number
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'

/** `assignees` vazio: "Sem responsável" (RN14). `overdue` vem calculado pelo backend (RF28). */
export interface Task {
  id: number
  projectId: number
  title: string
  description: string | null
  dueDate: string | null
  priority: TaskPriority
  status: TaskStatus
  assignees: Reference[]
  overdue: boolean
  version: number
}

export interface CreateTaskRequest {
  title: string
  description: string | null
  dueDate: string | null
  priority: TaskPriority
}

/** Só os campos enviados mudam; a versão é obrigatória (DA05, DA20). */
export interface UpdateTaskRequest {
  title?: string
  description?: string | null
  dueDate?: string | null
  priority?: TaskPriority
  version: number
}

export interface TaskFilters {
  statuses: TaskStatus[]
  priorities: TaskPriority[]
  overdue: boolean
  /** Identificador de um responsável, 'none' para "Sem responsável", ou null para todos. */
  assignee: number | 'none' | null
}

/** O autor e a data e hora vêm sempre do backend (RN30, E43). */
export interface Comment {
  id: number
  author: Reference
  content: string
  createdAt: string
}

/** Número de tarefas por estado (RN33). */
export interface TaskCounts {
  todo: number
  inProgress: number
  done: number
}

export interface DashboardProject {
  id: number
  name: string
  team: Reference
  manager: Reference | null
  status: ProjectStatus
  progress: Progress
  taskCounts: TaskCounts
}

export interface Dashboard {
  projects: DashboardProject[]
  totals: TaskCounts
}

/** Conta (User no contrato; US02, US14, US18). */
export interface User {
  id: number
  name: string
  email: string
  role: Role
  status: AccountStatus
  mustChangePassword: boolean
}

export interface CreateUserRequest {
  name: string
  email: string
  role: Role | null
  temporaryPassword: string
}

/** Só os campos enviados mudam. */
export interface UpdateUserRequest {
  name?: string
  email?: string
  role?: Role
}

/** Tarefa de "As minhas tarefas" (US08): a tarefa e o projeto a que pertence. */
export interface MyTask extends Task {
  project: Reference
}
