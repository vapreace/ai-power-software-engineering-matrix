// Operações de projeto (openapi.yaml, tag Projetos; US04, US09, US10, US12, US15, US17, US20, US22).
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
