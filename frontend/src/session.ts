// Utilizador autenticado, partilhado pelos ecrãs. Vem sempre da API (GET /session).
import { ref } from 'vue'
import { ApiError } from './api/http'
import { getSession } from './api/session'
import type { CurrentUser } from './api/types'

export const currentUser = ref<CurrentUser | null>(null)

// O separador lembra que tinha sessão. Se a página for recarregada depois de a sessão terminar
// (por exemplo, 30 minutos sem pedidos, DA26), a entrada mostra "A sessão terminou. Entre de novo."
// (US01, docs/ui/README.md, secção 10). Guarda só um indicador, nunca dados da conta.
const HAD_SESSION_KEY = 'matriz.tinhaSessao'

let loaded = false
let expired = false

function rememberSession(active: boolean): void {
  try {
    if (active) {
      sessionStorage.setItem(HAD_SESSION_KEY, '1')
    } else {
      sessionStorage.removeItem(HAD_SESSION_KEY)
    }
  } catch {
    // Sem acesso ao armazenamento do browser, a mensagem só aparece sem recarregar a página.
  }
}

function hadSession(): boolean {
  try {
    return sessionStorage.getItem(HAD_SESSION_KEY) === '1'
  } catch {
    return false
  }
}

/** Lê a sessão da API na primeira vez (ou quando `force` é verdadeiro). */
export async function loadSession(force = false): Promise<CurrentUser | null> {
  if (loaded && !force) {
    return currentUser.value
  }
  try {
    currentUser.value = await getSession()
    rememberSession(true)
  } catch (error) {
    if (!(error instanceof ApiError) || error.problem.status !== 401) {
      throw error
    }
    expired = hadSession()
    rememberSession(false)
    currentUser.value = null
  }
  loaded = true
  return currentUser.value
}

/** Verdadeiro uma vez, depois de se descobrir ao arrancar que a sessão deste separador terminou. */
export function consumeSessionExpired(): boolean {
  const value = expired
  expired = false
  return value
}

export function setCurrentUser(user: CurrentUser): void {
  currentUser.value = user
  loaded = true
  rememberSession(true)
}

export function clearSession(): void {
  currentUser.value = null
  loaded = true
  rememberSession(false)
}
