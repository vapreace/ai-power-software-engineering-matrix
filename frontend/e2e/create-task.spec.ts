import { expect, test } from '@playwright/test'
import { apiDelete, createdId, login, openMenu, uniqueName, waitForPost } from './support'

// Ficha PL 04, exercício 4 (CT03 de docs/test-plan.md): autenticar, abrir um projeto, criar uma
// tarefa e verificar a tarefa na listagem (US05, US11). O Rui gere o Novo armazém (dados demo).

let taskId: number | undefined

test.afterEach(async ({ page }) => {
  if (taskId !== undefined) {
    await apiDelete(page, `/tasks/${taskId}`)
    taskId = undefined
  }
})

test('o gestor cria uma tarefa e vê-a na lista do projeto, sem recarregar', async ({ page }) => {
  const title = uniqueName('Contar paletes')

  // Autenticar
  await login(page, 'rui@empresa.example')

  // Abrir o projeto
  await openMenu(page, 'Projetos')
  await page.getByRole('link', { name: 'Novo armazém', exact: true }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Novo armazém' })).toBeVisible()

  // Criar a tarefa
  await page.getByRole('button', { name: '+ Nova tarefa' }).click()
  const form = page.getByRole('form', { name: 'Nova tarefa' })
  await form.getByLabel('Título').fill(title)
  const created = waitForPost(page, /\/api\/projects\/\d+\/tasks$/)
  await form.getByRole('button', { name: 'Criar tarefa' }).click()
  taskId = await createdId(await created)

  // Verificar a tarefa na listagem: "Por fazer" e "Sem responsável" (RN12, RN14)
  await expect(form).toBeHidden()
  const row = page
    .getByRole('table', { name: 'Tarefas do projeto Novo armazém' })
    .getByRole('row')
    .filter({ hasText: title })
  await expect(row).toHaveCount(1)
  await expect(row).toContainText('Por fazer')
  await expect(row).toContainText('Sem responsável')
  await expect(row.getByRole('link', { name: title })).toHaveAttribute('href', `/tarefas/${taskId}`)
})
