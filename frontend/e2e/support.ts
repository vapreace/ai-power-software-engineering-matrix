import { expect, type Locator, type Page, type Response } from '@playwright/test'

/** Palavra-passe das contas de demonstração (README.md; dados fictícios, PRIV03). */
const PASSWORD = process.env.MATRIZ_E2E_PASSWORD ?? 'matriz-demo-2026'

/** Entra pela página de entrada e espera pelo Painel (US01). */
export async function login(page: Page, email: string): Promise<void> {
  await page.goto('/entrar')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Palavra-passe').fill(PASSWORD)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Painel' })).toBeVisible()
}

/** Abre um item do menu principal (DA21). */
export async function openMenu(page: Page, item: string): Promise<void> {
  await page.getByRole('navigation', { name: 'Menu principal' }).getByRole('link', { name: item, exact: true }).click()
}

/** Nome único em cada execução, para não depender de dados de execuções anteriores. */
export function uniqueName(text: string): string {
  return `${text} E2E ${Date.now()}`
}

/** Data atual da organização (Europe/Lisbon, DA24) no formato dos formulários, dd/mm/aaaa. */
export function today(): string {
  return new Intl.DateTimeFormat('pt-PT', {
    timeZone: 'Europe/Lisbon',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date())
}

/** Espera pela resposta de um POST da API cujo caminho acaba no padrão indicado. */
export function waitForPost(page: Page, path: RegExp): Promise<Response> {
  return page.waitForResponse(
    (response) => response.request().method() === 'POST' && path.test(new URL(response.url()).pathname),
  )
}

/** Identificador do recurso criado, lido do corpo da resposta 201. */
export async function createdId(response: Response): Promise<number> {
  expect(response.status(), `POST ${response.url()}`).toBe(201)
  return ((await response.json()) as { id: number }).id
}

/**
 * Limpeza no fim do teste: apaga pela API, com a sessão e o token CSRF do browser (DA04).
 * A eliminação é lógica (RN32): o registo deixa de aparecer nas listas e contagens.
 */
export async function apiDelete(page: Page, path: string): Promise<void> {
  const token = (await page.context().cookies()).find((cookie) => cookie.name === 'XSRF-TOKEN')?.value
  const response = await page.request.delete(`/api${path}`, {
    headers: token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {},
  })
  expect(response.status(), `DELETE /api${path}`).toBe(204)
}

/** Valor de uma contagem ("Por fazer", "Em curso", "Concluída") de um bloco TaskCounts (US21). */
export function count(counts: Locator, label: string): Locator {
  return counts.locator('div').filter({ has: counts.page().getByText(label, { exact: true }) }).locator('dd')
}
