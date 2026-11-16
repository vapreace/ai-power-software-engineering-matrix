// Operações de projeto (openapi.yaml, tag Projetos; US04, US09, US10, US12, US13, US15, US17, US20, US22).
import { request } from './http'
import type {
  CreateProjectRequest,
  Items,
  Project,
  ProjectStatus,
  ProjectSummary,
  UpdateProjectRequest,
} from './types'

export function listProjects(statuses: ProjectStatus[], q: string): Promise<Items<ProjectSummary>> {
  const params = new URLSearchParams()
  statuses.forEach((status) => params.append('status', status))
  if (q.trim()) {
    params.set('q', q)
  }
  const query = params.toString()
  return request<Items<ProjectSummary>>('GET', `/projects${query ? `?${query}` : ''}`)
}

export function getProject(id: number): Promise<Project> {
  return request<Project>('GET', `/projects/${id}`)
}

export function createProject(body: CreateProjectRequest): Promise<Project> {
  return request<Project>('POST', '/projects', body)
}

export function updateProject(id: number, body: UpdateProjectRequest): Promise<Project> {
  return request<Project>('PATCH', `/projects/${id}`, body)
}

export function deleteProject(id: number): Promise<void> {
  return request<void>('DELETE', `/projects/${id}`)
}

/** Mudar o estado e arquivar (US13). A confirmação de RN10 é feita antes, no ecrã. */
export function changeProjectStatus(id: number, status: ProjectStatus, version: number): Promise<Project> {
  return request<Project>('PUT', `/projects/${id}/status`, { status, version })
}

/** Mudar o gestor (US17). Só o Administrador. */
export function changeProjectManager(id: number, managerId: number, version: number): Promise<Project> {
  return request<Project>('PUT', `/projects/${id}/manager`, { managerId, version })
}
