// Leitura de equipas (openapi.yaml, tag Equipas). No I2 servem o formulário de projeto (US04).
import { request } from './http'
import type { Items, Reference, TeamMember } from './types'

export function listTeams(): Promise<Items<Reference>> {
  return request<Items<Reference>>('GET', '/teams')
}

export function listTeamMembers(teamId: number): Promise<Items<TeamMember>> {
  return request<Items<TeamMember>>('GET', `/teams/${teamId}/members`)
}
