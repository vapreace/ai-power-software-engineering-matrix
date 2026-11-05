// Único sítio que conhece o endereço da API (DA34). Mesma origem: sem CORS (DA02).
const API_BASE = '/api'

const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

/** Erro no formato Problem Details (RFC 9457), com as extensões da Matriz (DA16). */
export interface Problem {
  type: string
  title: string
  status: number
  code: string
  detail: string
  errors?: { field: string; message: string }[]
}

export class ApiError extends Error {
  constructor(public readonly problem: Problem) {
    super(problem.detail)
  }
}

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((part) => part.startsWith(`${name}=`))
    ?.split('=')[1]
}

/**
 * Faz um pedido à API. Envia o cookie de sessão e, nos pedidos que alteram
 * dados, o token CSRF no cabeçalho X-XSRF-TOKEN (DA04).
 */
export async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (UNSAFE_METHODS.has(method)) {
    const token = readCookie('XSRF-TOKEN')
    if (token) {
      headers['X-XSRF-TOKEN'] = decodeURIComponent(token)
    }
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    credentials: 'same-origin',
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!response.ok) {
    throw new ApiError(await toProblem(response))
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function toProblem(response: Response): Promise<Problem> {
  try {
    return (await response.json()) as Problem
  } catch {
    return {
      type: 'about:blank',
      title: response.statusText,
      status: response.status,
      code: 'UNEXPECTED_ERROR',
      detail: 'Ocorreu um erro inesperado. Tente de novo.',
    }
  }
}
