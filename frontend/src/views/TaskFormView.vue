<script setup lang="ts">
// Ecrã Editar tarefa (US16): título, descrição, prazo e prioridade. Só os campos alterados vão
// no pedido, com a versão lida (DA05, DA20). Retirar o prazo é sempre permitido (RN13, E27).
// Com conflito de versão, "Abrir de novo" lê a tarefa outra vez (docs/ui/README.md, secção 10).
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { getProject } from '@/api/projects'
import { getTask, updateTask } from '@/api/tasks'
import type { Project, Task, TaskPriority, UpdateTaskRequest } from '@/api/types'
import { currentUser } from '@/session'
import { canManageProject } from '@/permissions'
import { formatDate, parseDate, priorityLabels, textLength } from '@/labels'
import { useNarrowScreen } from '@/composables/useNarrowScreen'
import FormField from '@/components/FormField.vue'
import SelectField from '@/components/SelectField.vue'
import LoadingState from '@/components/LoadingState.vue'
import ErrorState from '@/components/ErrorState.vue'

type Field = 'title' | 'description' | 'dueDate' | 'priority'

const route = useRoute()
const router = useRouter()
const narrow = useNarrowScreen()

const priorityOptions = (['LOW', 'MEDIUM', 'HIGH'] as TaskPriority[]).map((value) => ({
  value,
  label: priorityLabels[value],
}))

const task = ref<Task | null>(null)
const project = ref<Project | null>(null)
const form = reactive({ title: '', description: '', dueDate: '', priority: 'MEDIUM' as TaskPriority | null })
const errors = reactive<Partial<Record<Field, string>>>({})
const message = ref<string | null>(null)
const versionConflict = ref(false)
const submitting = ref(false)
const state = ref<'loading' | 'ready' | 'error'>('loading')
const loadError = ref('')

const canEdit = computed(() => project.value !== null && canManageProject(currentUser.value, project.value))
const closed = computed(() => project.value !== null && project.value.status !== 'ACTIVE')

function clearErrors() {
  ;(Object.keys(errors) as Field[]).forEach((key) => delete errors[key])
  message.value = null
  versionConflict.value = false
}

async function load() {
  state.value = 'loading'
  clearErrors()
  try {
    const loaded = await getTask(Number(route.params.id))
    project.value = await getProject(loaded.projectId)
    task.value = loaded
    form.title = loaded.title
    form.description = loaded.description ?? ''
    form.dueDate = formatDate(loaded.dueDate)
    form.priority = loaded.priority
    state.value = 'ready'
  } catch (error) {
    loadError.value =
      error instanceof ApiError && [403, 404].includes(error.problem.status)
        ? error.problem.detail
        : 'Não foi possível carregar a tarefa.'
    state.value = 'error'
  }
}

function validate(): string | null | undefined {
  clearErrors()
  const length = textLength(form.title)
  if (length === 0 || length > 200) errors.title = 'O título é obrigatório e tem no máximo 200 caracteres.'
  if (textLength(form.description) > 2000) errors.description = 'A descrição tem no máximo 2000 caracteres.'
  const dueDate = parseDate(form.dueDate)
  if (dueDate === undefined) errors.dueDate = 'Indique a data no formato dd/mm/aaaa.'
  return Object.keys(errors).length === 0 ? dueDate : undefined
}

/** Só os campos alterados, com a versão lida (DA05, DA20). */
function changes(t: Task, dueDate: string | null): UpdateTaskRequest {
  const body: UpdateTaskRequest = { version: t.version }
  const description = form.description.trim() ? form.description : null
  if (form.title.trim() !== t.title) body.title = form.title
  if ((description?.trim() ?? null) !== (t.description ?? null)) body.description = description
  if (dueDate !== t.dueDate) body.dueDate = dueDate
  if (form.priority && form.priority !== t.priority) body.priority = form.priority
  return body
}

async function submit() {
  if (submitting.value || !task.value) {
    return
  }
  const dueDate = validate()
  if (dueDate === undefined) {
    return
  }
  submitting.value = true
  try {
    const body = changes(task.value, dueDate)
    if (Object.keys(body).length > 1) {
      await updateTask(task.value.id, body)
    }
    await router.push({ name: 'task', params: { id: task.value.id } })
  } catch (error) {
    // Os dados escritos mantêm-se no formulário (docs/ui/README.md, secção 10).
    if (error instanceof ApiError) {
      let shown = false
      for (const field of ['title', 'description', 'dueDate', 'priority'] as Field[]) {
        const fieldMessage = error.fieldError(field)
        if (fieldMessage) {
          errors[field] = fieldMessage
          shown = true
        }
      }
      if (!shown) {
        message.value = error.problem.detail
        versionConflict.value = error.problem.code === 'VERSION_CONFLICT'
      }
    } else {
      message.value = 'Não foi possível gravar a tarefa. Tente de novo.'
    }
  } finally {
    submitting.value = false
  }
}

function cancel() {
  void router.push({ name: 'task', params: { id: Number(route.params.id) } })
}

void load()
</script>

<template>
  <main class="page">
    <div class="page-header">
      <nav aria-label="Caminho" class="breadcrumb">
        <RouterLink :to="{ name: 'projects' }">Projetos</RouterLink>
        <template v-if="task && project">
          <span aria-hidden="true">/</span>
          <RouterLink :to="{ name: 'project', params: { id: project.id } }">{{ project.name }}</RouterLink>
          <span aria-hidden="true">/</span>
          <RouterLink :to="{ name: 'task', params: { id: task.id } }">{{ task.title }}</RouterLink>
          <span aria-hidden="true">/</span>
          <span aria-current="page">Editar</span>
        </template>
      </nav>
      <h1>Editar tarefa</h1>
    </div>

    <LoadingState v-if="state === 'loading'" text="A carregar a tarefa..." />
    <ErrorState v-else-if="state === 'error'" :message="loadError" @retry="load" />

    <div v-else-if="!canEdit || closed" class="card card-body notice">
      <p class="alert" role="alert">
        {{ !canEdit ? 'Não tem permissão para esta operação.' : 'O projeto está concluído ou arquivado e não pode ser alterado.' }}
      </p>
      <RouterLink v-if="task" :to="{ name: 'task', params: { id: task.id } }">Voltar à tarefa</RouterLink>
    </div>

    <form v-else class="card task-form" novalidate @submit.prevent="submit">
      <div class="card-body form-stack">
        <h2 v-if="!narrow" class="section-label card-title">Dados da tarefa</h2>
        <div v-if="message" class="alert" :class="{ 'alert-with-action': versionConflict }" role="alert">
          <span>{{ message }}</span>
          <button v-if="versionConflict" type="button" class="btn secondary small" @click="load">Abrir de novo</button>
        </div>
        <FormField id="titulo" v-model="form.title" label="Título" hint="Até 200 caracteres." :error="errors.title" />
        <FormField
          id="descricao"
          v-model="form.description"
          label="Descrição"
          optional
          multiline
          :rows="4"
          hint="Até 2000 caracteres."
          :error="errors.description"
        />
        <div class="form-grid">
          <FormField
            id="prazo"
            v-model="form.dueDate"
            label="Prazo"
            optional
            date
            hint="Deixe vazio para retirar o prazo."
            :error="errors.dueDate"
          />
          <SelectField
            id="prioridade"
            v-model="form.priority"
            label="Prioridade"
            :options="priorityOptions"
            :error="errors.priority"
          />
        </div>
      </div>
      <div class="form-actions" :class="narrow ? 'card-body narrow' : 'card-footer'">
        <button type="submit" class="btn primary-action" :disabled="submitting">
          <span v-if="submitting" class="spinner" aria-hidden="true"></span>
          {{ submitting ? 'A guardar...' : 'Guardar' }}
        </button>
        <button type="button" class="btn secondary" :disabled="submitting" @click="cancel">Cancelar</button>
      </div>
    </form>
  </main>
</template>

<style scoped>
.task-form,
.notice {
  max-width: 720px;
}

.notice {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 16px;
}

.card-footer .primary-action {
  order: 2;
}

.form-actions.narrow {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 0;
}

.form-actions.narrow .btn {
  width: 100%;
}
</style>
