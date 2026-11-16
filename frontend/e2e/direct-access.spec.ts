import { expect, test } from '@playwright/test'
import { login, openMenu } from './support'

// Ficha PL 04, exercício 4 (CT20 de docs/test-plan.md): um Membro abre pelo endereço um projeto
// de outra equipa e não vê os seus dados (US12, SEG02). Dois utilizadores de demonstração:
// o Carlos (Observador, vê todos os projetos) dá o endereço de Fornecedores, da equipa Compras;
// o Pedro (Membro da Logística Norte) tenta abri-lo.

test('um membro não abre pelo endereço um projeto de outra equipa', async ({ browser, page }) => {
  // Endereço do projeto, visto pelo Observador, numa sessão separada
  const observerContext = await browser.newContext({ baseURL: test.info().project.use.baseURL })
  const observer = await observerContext.newPage()
  await login(observer, 'carlos@empresa.example')
  await openMenu(observer, 'Projetos')
  const address = await observer.getByRole('link', { name: 'Fornecedores', exact: true }).getAttribute('href')
  await observerContext.close()
  expect(address).toMatch(/^\/projetos\/\d+$/)

  // O Pedro não vê o projeto na lista
  await login(page, 'pedro@empresa.example')
  await openMenu(page, 'Projetos')
  await expect(page.getByRole('link', { name: 'Novo armazém', exact: true })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Fornecedores', exact: true })).toHaveCount(0)

  // Abrir o endereço diretamente: acesso recusado e nenhum dado do projeto
  await page.goto(address!)
  await expect(page.getByRole('alert')).toHaveText('Não tem acesso a esta página.')
  await expect(page.getByRole('heading', { name: 'Fornecedores' })).toHaveCount(0)
  await expect(page.getByRole('table')).toHaveCount(0)
})
