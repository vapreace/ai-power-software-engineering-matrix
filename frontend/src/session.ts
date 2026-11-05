// Utilizador autenticado, partilhado pelos ecrãs. Vem sempre da API (GET /session).
import { ref } from 'vue'
import { ApiError } from './api/http'
import { getSession } from './api/session'
import type { CurrentUser } from './api/types'

export const currentUser = ref<CurrentUser | null>(null)

let loaded = false

/** Lê a sessão da API na primeira vez (ou quando `force` é verdadeiro). */
export async function loadSession(force = false): Promise<CurrentUser | null> {
  if (loaded && !force) {
    return currentUser.value
  }
  try {
    currentUser.value = await getSession()
  } catch (error) {
    if (!(error instanceof ApiError) || error.problem.status !== 401) {
      throw error
    }
    currentUser.value = null
  }
  loaded = true
  return currentUser.value
}

export function setCurrentUser(user: CurrentUser): void {
  currentUser.value = user
  loaded = true
}

export function clearSession(): void {
  currentUser.value = null
  loaded = true
}
