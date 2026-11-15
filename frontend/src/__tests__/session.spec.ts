// Regressão do bug B1 (docs/verification.md): depois de a sessão terminar, recarregar a página
// levava à entrada sem a mensagem "A sessão terminou. Entre de novo." (US01, docs/ui/README.md §10).
// Cada teste importa de novo o módulo, como acontece ao recarregar a página no mesmo separador:
// a memória da aplicação perde-se, o sessionStorage do separador mantém-se.
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { CurrentUser } from '@/api/types'

const getSession = vi.fn()
vi.mock('@/api/session', () => ({ getSession: () => getSession() }))

const rui: CurrentUser = {
  id: 2,
  name: 'Rui',
  email: 'rui@empresa.example',
  role: 'MANAGER',
  teams: [{ id: 1, name: 'Logística Norte' }],
  mustChangePassword: false,
}

/** Resposta 401 da API, criada com o módulo carregado depois do último "recarregar". */
async function sessionEnded(): Promise<never> {
  const { ApiError } = await import('@/api/http')
  throw new ApiError({
    type: 'about:blank',
    title: 'Unauthorized',
    status: 401,
    code: 'UNAUTHENTICATED',
    detail: 'A sessão terminou. Entre de novo.',
  })
}

/** Simula recarregar a página: o módulo começa do zero. */
async function reloadPage() {
  vi.resetModules()
  return import('@/session')
}

describe('sessão terminada ao recarregar a página', () => {
  beforeEach(() => {
    sessionStorage.clear()
    getSession.mockReset()
  })

  it('mostra a mensagem quando o separador tinha sessão', async () => {
    getSession.mockResolvedValueOnce(rui)
    let session = await reloadPage()
    expect(await session.loadSession()).toEqual(rui)

    getSession.mockImplementationOnce(sessionEnded)
    session = await reloadPage()
    expect(await session.loadSession()).toBeNull()
    expect(session.consumeSessionExpired()).toBe(true)
    // A mensagem aparece uma só vez.
    expect(session.consumeSessionExpired()).toBe(false)
  })

  it('não mostra a mensagem a quem ainda não tinha entrado', async () => {
    getSession.mockImplementationOnce(sessionEnded)
    const session = await reloadPage()
    expect(await session.loadSession()).toBeNull()
    expect(session.consumeSessionExpired()).toBe(false)
  })

  it('não mostra a mensagem depois de sair com o botão Sair', async () => {
    let session = await reloadPage()
    session.setCurrentUser(rui)
    session.clearSession()

    getSession.mockImplementationOnce(sessionEnded)
    session = await reloadPage()
    expect(await session.loadSession()).toBeNull()
    expect(session.consumeSessionExpired()).toBe(false)
  })

  it('não guarda dados da conta no browser, só o indicador', async () => {
    const session = await reloadPage()
    session.setCurrentUser(rui)
    expect(Object.keys(sessionStorage)).toEqual(['matriz.tinhaSessao'])
    expect(sessionStorage.getItem('matriz.tinhaSessao')).toBe('1')
  })
})
