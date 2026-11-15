// Operações de tarefas (openapi.yaml, tag Tarefas; US05, US07, US10, US11, US16).
import { request } from './http'
import type { CreateTaskRequest, Items, Task, TaskFilters, TaskStatus, UpdateTaskRequest } from './types'

export function listTasks(projectId: number, filters: TaskFilters): Promise<Items<Task>> {
  const params = new URLSearchParams()
  filters.statuses.forEach((status) => params.append('status', status))
  filters.priorities.forEach((priority) => params.append('priority', priority))
  if (filters.overdue) {
    params.set('overdue', 'true')
  }
  const query = params.toString()
  return request<Items<Task>>('GET', `/projects/${projectId}/tasks${query ? `?${query}` : ''}`)
}

export function createTask(projectId: number, body: CreateTaskRequest): Promise<Task> {
  return request<Task>('POST', `/projects/${projectId}/tasks`, body)
}

export function getTask(id: number): Promise<Task> {
  return request<Task>('GET', `/tasks/${id}`)
}

export function updateTask(id: number, body: UpdateTaskRequest): Promise<Task> {
  return request<Task>('PATCH', `/tasks/${id}`, body)
}

export function changeTaskStatus(id: number, status: TaskStatus, version: number): Promise<Task> {
  return request<Task>('PUT', `/tasks/${id}/status`, { status, version })
}

export function deleteTask(id: number): Promise<void> {
  return request<void>('DELETE', `/tasks/${id}`)
}
