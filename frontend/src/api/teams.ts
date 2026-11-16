// Equipas (openapi.yaml, tag Equipas; US03). Criar, mudar o nome e gerir pessoas: só o Administrador.
import { request } from './http'
import type { Items, Reference, TeamMember } from './types'

export function listTeams(): Promise<Items<Reference>> {
  return request<Items<Reference>>('GET', '/teams')
}

export function listTeamMembers(teamId: number): Promise<Items<TeamMember>> {
  return request<Items<TeamMember>>('GET', `/teams/${teamId}/members`)
}

export function createTeam(name: string): Promise<Reference> {
  return request<Reference>('POST', '/teams', { name })
}

export function renameTeam(teamId: number, name: string): Promise<Reference> {
  return request<Reference>('PATCH', `/teams/${teamId}`, { name })
}

export function addTeamMember(teamId: number, userId: number): Promise<void> {
  return request<void>('PUT', `/teams/${teamId}/members/${userId}`)
}

export function removeTeamMember(teamId: number, userId: number): Promise<void> {
  return request<void>('DELETE', `/teams/${teamId}/members/${userId}`)
}
