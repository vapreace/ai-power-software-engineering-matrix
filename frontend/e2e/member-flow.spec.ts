import { expect, test } from '@playwright/test'
import { api, login, openMenu, taskItem, uniqueName } from './support'

// Passo 2b (depois da Ficha PL 04): percurso do Membro, passos 4 e 5 do percurso principal
// (requirements.md secção 8.3, US08, US07, RN15). O Rui (Gestor) prepara pela API uma tarefa no
// Novo armazém com a Marta como responsável; a Marta encontra-a em "As minhas tarefas" e muda o
// estado pela interface. A tarefa é apagada no fim.

test('a responsável encontra a tarefa em "As minhas tarefas" e muda o estado', async ({ browser, page }) => {
  const title = uniqueName('Contar caixas')

  // A Marta entra e fica a saber-se o seu identificador
  await login(page, 'marta@empresa.example')
  const marta = (await api(page, 'GET', '/session')) as { id: number }

  // O Rui cria a tarefa e associa a Marta, numa sessão separada
  const managerContext = await browser.newContext({ baseURL: test.info().project.use.baseURL })
  const rui = await managerContext.newPage()
  await login(rui, 'rui@empresa.example')
  const projects = (await api(rui, 'GET', `/projects?q=${encodeURIComponent('Novo armazém')}`)) as {
    items: { id: number; name: string }[]
  }
  const projectId = projects.items.find((project) => project.name === 'Novo armazém')!.id
  const task = (await api(rui, 'POST', `/projects/${projectId}/tasks`, { title })) as { id: number }

  try {
    await api(rui, 'PUT', `/tasks/${task.id}/assignees/${marta.id}`)

    // Encontrar a tarefa em "As minhas tarefas", com o projeto
    await openMenu(page, 'As minhas tarefas')
    await expect(page.getByRole('heading', { level: 1, name: 'As minhas tarefas' })).toBeVisible()
    const item = taskItem(page, title)
    await expect(item).toContainText('Novo armazém')
    await expect(item).toContainText('Por fazer')

    // Mudar o estado: a responsável pode, mesmo sendo Membro (RN15)
    await item.getByRole('link', { name: title }).click()
    await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
    await page.getByLabel('Mudar estado para').selectOption({ label: 'Em curso' })
    await page.getByRole('button', { name: 'Mudar estado', exact: true }).click()
    await expect(page.getByRole('status').filter({ hasText: 'Estado mudado para "Em curso".' })).toBeVisible()

    // A lista mostra o estado novo
    await openMenu(page, 'As minhas tarefas')
    await expect(taskItem(page, title)).toContainText('Em curso')
  } finally {
    await api(rui, 'DELETE', `/tasks/${task.id}`)
    await managerContext.close()
  }
})
