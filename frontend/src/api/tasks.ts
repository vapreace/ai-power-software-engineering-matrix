// Operações de tarefas, responsáveis e comentários (openapi.yaml; US05 a US07, US10, US11, US16, US19).
import { request } from './http'
import type { Comment, CreateTaskRequest, Items, Task, TaskFilters, TaskStatus, UpdateTaskRequest } from './types'

export function listTasks(projectId: number, filters: TaskFilters): Promise<Items<Task>> {
  const params = new URLSearchParams()
  filters.statuses.forEach((status) => params.append('status', status))
  filters.priorities.forEach((priority) => params.append('priority', priority))
  if (filters.overdue) {
    params.set('overdue', 'true')
  }
  if (filters.assignee === 'none') {
    params.set('unassigned', 'true')
  } else if (filters.assignee !== null) {
    params.set('assigneeId', String(filters.assignee))
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

export function addAssignee(taskId: number, userId: number): Promise<Task> {
  return request<Task>('PUT', `/tasks/${taskId}/assignees/${userId}`)
}

export function removeAssignee(taskId: number, userId: number): Promise<Task> {
  return request<Task>('DELETE', `/tasks/${taskId}/assignees/${userId}`)
}

export function listComments(taskId: number): Promise<Items<Comment>> {
  return request<Items<Comment>>('GET', `/tasks/${taskId}/comments`)
}

export function createComment(taskId: number, content: string): Promise<Comment> {
  return request<Comment>('POST', `/tasks/${taskId}/comments`, { content })
}
