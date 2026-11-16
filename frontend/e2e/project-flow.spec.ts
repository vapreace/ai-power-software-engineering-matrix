import { expect, test } from '@playwright/test'
import { apiDelete, count, createdId, login, openMenu, taskItem, today, uniqueName, waitForPost } from './support'

// Ficha PL 04, exercício 4 (CT33 de docs/test-plan.md), fluxo do slide e do prompt E2E dos anexos:
// autenticar, criar um projeto, criar uma tarefa, mudar o estado e verificar o painel
// (US04, US05, US07, US21). O Rui é Gestor da Logística Norte (dados demo).

let projectId: number | undefined

test.afterEach(async ({ page }) => {
  // Eliminar o projeto elimina também as suas tarefas (RN31).
  if (projectId !== undefined) {
    await apiDelete(page, `/projects/${projectId}`)
    projectId = undefined
  }
})

test('o gestor cria um projeto e uma tarefa, muda o estado e vê o painel atualizado', async ({ page }) => {
  const projectName = uniqueName('Inventário anual')
  const title = uniqueName('Etiquetar estantes')

  // Autenticar
  await login(page, 'rui@empresa.example')

  // Criar o projeto
  await openMenu(page, 'Projetos')
  await page.getByRole('link', { name: '+ Novo projeto' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Novo projeto' })).toBeVisible()
  await page.getByLabel('Equipa').selectOption({ label: 'Logística Norte' })
  await page.getByLabel('Nome', { exact: true }).fill(projectName)
  await page.getByLabel('Data de início').fill(today())
  const projectCreated = waitForPost(page, /\/api\/projects$/)
  await page.getByRole('button', { name: 'Criar projeto' }).click()
  projectId = await createdId(await projectCreated)
  await expect(page.getByRole('heading', { level: 1, name: projectName })).toBeVisible()

  // Criar a tarefa
  await page.getByRole('button', { name: '+ Nova tarefa' }).click()
  const form = page.getByRole('form', { name: 'Nova tarefa' })
  await form.getByLabel('Título').fill(title)
  const taskCreated = waitForPost(page, /\/api\/projects\/\d+\/tasks$/)
  await form.getByRole('button', { name: 'Criar tarefa' }).click()
  await createdId(await taskCreated)
  const row = taskItem(page, title)
  await expect(row).toContainText('Por fazer')

  // Alterar o estado da tarefa
  await row.getByRole('link', { name: title }).click()
  await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
  await page.getByLabel('Mudar estado para').selectOption({ label: 'Em curso' })
  await page.getByRole('button', { name: 'Mudar estado', exact: true }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Estado mudado para "Em curso".' })).toBeVisible()

  // Verificar o painel: a tarefa conta em "Em curso" (RN33)
  await openMenu(page, 'Painel')
  const counts = page.getByLabel(`Tarefas de ${projectName}`)
  await expect(count(counts, 'Por fazer')).toHaveText('0')
  await expect(count(counts, 'Em curso')).toHaveText('1')
  await expect(count(counts, 'Concluída')).toHaveText('0')
})
