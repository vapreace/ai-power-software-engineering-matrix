<script setup lang="ts">
// Formulários Novo projeto (US04) e Editar projeto (US17) (docs/ui/README.md, secção 9).
// O Gestor cria nas suas equipas e fica gestor: não vê o campo Gestor (RN05, RN27).
// O Administrador escolhe a equipa e um Gestor com conta ativa dessa equipa (RN21, RN27).
// Ao editar, a equipa não muda (RN21) e a versão lida vai no pedido (DA05).
// A validação do frontend ajuda; a que conta é a do backend (SEG06).
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { createProject, getProject, updateProject } from '@/api/projects'
import { listTeamMembers, listTeams } from '@/api/teams'
import type { Project, Reference, UpdateProjectRequest } from '@/api/types'
import { currentUser } from '@/session'
import { canManageProject } from '@/permissions'
import { formatDate, parseDate, textLength } from '@/labels'
import { useNarrowScreen } from '@/composables/useNarrowScreen'
import FormField from '@/components/FormField.vue'
import SelectField from '@/components/SelectField.vue'
import LoadingState from '@/components/LoadingState.vue'
import ErrorState from '@/components/ErrorState.vue'

type Field = 'teamId' | 'managerId' | 'name' | 'description' | 'startDate' | 'expectedEndDate'

const MESSAGES = {
  team: 'A equipa é obrigatória.',
  manager: 'O gestor é obrigatório.',
  name: 'O nome é obrigatório e tem no máximo 100 caracteres.',
  description: 'A descrição tem no máximo 1000 caracteres.',
  startRequired: 'A data de início é obrigatória.',
  dateFormat: 'Indique a data no formato dd/mm/aaaa.',
  dates: 'A data de fim prevista não pode ser anterior à data de início.',
  noManagers: 'A equipa não tem nenhum Gestor',
}

const route = useRoute()
const router = useRouter()
const narrow = useNarrowScreen()

const projectId = computed(() => (route.params.id ? Number(route.params.id) : null))
const editing = computed(() => projectId.value !== null)
const isAdministrator = computed(() => currentUser.value?.role === 'ADMINISTRATOR')

const form = reactive({
  teamId: null as number | null,
  managerId: null as number | null,
  name: '',
  description: '',
  startDate: '',
  expectedEndDate: '',
})
const errors = reactive<Partial<Record<Field, string>>>({})
const message = ref<string | null>(null)
const versionConflict = ref(false)
const submitting = ref(false)

const project = ref<Project | null>(null)
const teams = ref<Reference[]>([])
const managers = ref<Reference[]>([])
const managersLoaded = ref(false)
const state = ref<'loading' | 'ready' | 'error'>('loading')
const loadError = ref('')

const teamOptions = computed(() => teams.value.map((team) => ({ value: team.id, label: team.name })))
const managerOptions = computed(() => managers.value.map((m) => ({ value: m.id, label: m.name })))
const canEdit = computed(() => !project.value || canManageProject(currentUser.value, project.value))
const closed = computed(() => project.value !== null && project.value.status !== 'ACTIVE')
const title = computed(() => (editing.value ? 'Editar projeto' : 'Novo projeto'))

function fillFromProject(p: Project) {
  project.value = p
  form.name = p.name
  form.description = p.description ?? ''
  form.startDate = formatDate(p.startDate)
  form.expectedEndDate = formatDate(p.expectedEndDate)
}

async function load() {
  state.value = 'loading'
  try {
    if (editing.value) {
      fillFromProject(await getProject(projectId.value!))
    } else if (isAdministrator.value) {
      teams.value = (await listTeams()).items
    } else {
      // O Gestor só cria nas equipas a que pertence (RN05).
      teams.value = currentUser.value?.teams ?? []
      if (teams.value.length === 1) {
        form.teamId = teams.value[0]!.id
      }
    }
    state.value = 'ready'
  } catch (error) {
    loadError.value =
      error instanceof ApiError && [403, 404].includes(error.problem.status)
        ? error.problem.detail
        : editing.value
          ? 'Não foi possível carregar o projeto.'
          : 'Não foi possível carregar as equipas.'
    state.value = 'error'
  }
}

// Administrador: os gestores elegíveis são os Gestores com conta ativa da equipa (RN21).
watch(
  () => form.teamId,
  async (teamId) => {
    form.managerId = null
    managers.value = []
    managersLoaded.value = false
    if (!isAdministrator.value || editing.value || teamId === null) {
      return
    }
    try {
      const members = (await listTeamMembers(teamId)).items
      if (form.teamId === teamId) {
        managers.value = members.filter((m) => m.role === 'MANAGER' && m.status === 'ACTIVE')
        managersLoaded.value = true
      }
    } catch {
      managersLoaded.value = false
    }
  },
)

const managerHint = computed(() =>
  managersLoaded.value && managers.value.length === 0 ? undefined : 'Só Gestores com conta ativa da equipa.',
)
const managerWarning = computed(() =>
  managersLoaded.value && managers.value.length === 0 ? MESSAGES.noManagers : undefined,
)

function clearErrors() {
  ;(Object.keys(errors) as Field[]).forEach((key) => delete errors[key])
  message.value = null
  versionConflict.value = false
}

function validate(): { start: string | null; end: string | null } | null {
  clearErrors()
  if (!editing.value) {
    if (form.teamId === null) errors.teamId = MESSAGES.team
    if (isAdministrator.value && form.managerId === null) errors.managerId = MESSAGES.manager
  }
  const nameLength = textLength(form.name)
  if (nameLength === 0 || nameLength > 100) errors.name = MESSAGES.name
  if (textLength(form.description) > 1000) errors.description = MESSAGES.description
  const start = parseDate(form.startDate)
  const end = parseDate(form.expectedEndDate)
  if (start === null) errors.startDate = MESSAGES.startRequired
  if (start === undefined) errors.startDate = MESSAGES.dateFormat
  if (end === undefined) errors.expectedEndDate = MESSAGES.dateFormat
  if (start && end && end < start) errors.expectedEndDate = MESSAGES.dates
  if (Object.keys(errors).length > 0) {
    return null
  }
  return { start: start ?? null, end: end ?? null }
}

function showApiError(error: unknown) {
  if (!(error instanceof ApiError)) {
    message.value = 'Não foi possível gravar o projeto. Tente de novo.'
    return
  }
  const fields: Field[] = ['teamId', 'managerId', 'name', 'description', 'startDate', 'expectedEndDate']
  let shown = false
  for (const field of fields) {
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
}

async function submit() {
  if (submitting.value) {
    return
  }
  const dates = validate()
  if (!dates) {
    return
  }
  submitting.value = true
  try {
    if (editing.value && project.value) {
      const body = changes(project.value, dates.start!, dates.end)
      if (Object.keys(body).length === 1) {
        await router.push({ name: 'project', params: { id: project.value.id } })
        return
      }
      const saved = await updateProject(project.value.id, body)
      await router.push({ name: 'project', params: { id: saved.id } })
    } else {
      const created = await createProject({
        teamId: form.teamId,
        name: form.name,
        description: form.description.trim() ? form.description : null,
        startDate: dates.start,
        expectedEndDate: dates.end,
        ...(isAdministrator.value ? { managerId: form.managerId } : {}),
      })
      await router.push({ name: 'project', params: { id: created.id } })
    }
  } catch (error) {
    // Os dados escritos mantêm-se no formulário (docs/ui/README.md, secção 10).
    showApiError(error)
  } finally {
    submitting.value = false
  }
}

/** Só os campos alterados, com a versão lida (DA05, DA20). */
function changes(p: Project, start: string, end: string | null): UpdateProjectRequest {
  const body: UpdateProjectRequest = { version: p.version }
  const description = form.description.trim() ? form.description : null
  if (form.name.trim() !== p.name) body.name = form.name
  if ((description?.trim() ?? null) !== (p.description ?? null)) body.description = description
  if (start !== p.startDate) body.startDate = start
  if (end !== p.expectedEndDate) body.expectedEndDate = end
  return body
}

/** Depois de um conflito de versão: lê de novo o projeto e descarta o que foi escrito (DA05). */
async function reopen() {
  clearErrors()
  await load()
}

function cancel() {
  if (editing.value && projectId.value !== null) {
    void router.push({ name: 'project', params: { id: projectId.value } })
  } else {
    void router.push({ name: 'projects' })
  }
}

void load()
</script>

<template>
  <main class="page">
    <div class="page-header">
      <nav aria-label="Caminho" class="breadcrumb">
        <RouterLink :to="{ name: 'projects' }">Projetos</RouterLink>
        <template v-if="editing && project">
          <span aria-hidden="true">/</span>
          <RouterLink :to="{ name: 'project', params: { id: project.id } }">{{ project.name }}</RouterLink>
          <span aria-hidden="true">/</span>
          <span aria-current="page">Editar</span>
        </template>
        <template v-else-if="!editing">
          <span aria-hidden="true">/</span>
          <span aria-current="page">Novo projeto</span>
        </template>
      </nav>
      <h1>{{ title }}</h1>
    </div>

    <LoadingState v-if="state === 'loading'" :text="editing ? 'A carregar o projeto...' : 'A carregar...'" />
    <ErrorState v-else-if="state === 'error'" :message="loadError" @retry="load" />

    <div v-else-if="!canEdit" class="card card-body notice">
      <p class="alert" role="alert">Não tem permissão para esta operação.</p>
      <RouterLink v-if="project" :to="{ name: 'project', params: { id: project.id } }">Voltar ao projeto</RouterLink>
    </div>

    <div v-else-if="closed" class="card card-body notice">
      <p class="alert" role="alert">O projeto está concluído ou arquivado e não pode ser alterado.</p>
      <RouterLink v-if="project" :to="{ name: 'project', params: { id: project.id } }">Voltar ao projeto</RouterLink>
    </div>

    <form v-else class="card project-form" novalidate @submit.prevent="submit">
      <div class="card-body form-stack">
        <h2 v-if="!narrow" class="section-label card-title">Dados do projeto</h2>

        <div v-if="message" class="alert" :class="{ 'alert-with-action': versionConflict }" role="alert">
          <span>{{ message }}</span>
          <button v-if="versionConflict" type="button" class="btn secondary small" @click="reopen">
            Abrir de novo
          </button>
        </div>

        <template v-if="editing && project">
          <div class="field">
            <span class="field-label" id="equipa-rotulo">Equipa</span>
            <p class="field-static" aria-labelledby="equipa-rotulo" aria-describedby="equipa-ajuda">
              {{ project.team.name }}
            </p>
            <p id="equipa-ajuda" class="field-hint">A equipa não muda depois de criar o projeto.</p>
          </div>
        </template>
        <div v-else :class="isAdministrator ? 'form-grid' : ''">
          <SelectField
            id="equipa"
            v-model="form.teamId"
            label="Equipa"
            placeholder="Escolha uma equipa"
            :options="teamOptions"
            :error="errors.teamId"
          />
          <SelectField
            v-if="isAdministrator"
            id="gestor"
            v-model="form.managerId"
            label="Gestor"
            placeholder="Escolha o gestor"
            :options="managerOptions"
            :disabled="form.teamId === null"
            :hint="managerHint"
            :error="errors.managerId ?? managerWarning"
          />
        </div>

        <FormField id="nome" v-model="form.name" label="Nome" hint="Até 100 caracteres." :error="errors.name" />
        <FormField
          id="descricao"
          v-model="form.description"
          label="Descrição"
          optional
          multiline
          :rows="3"
          hint="Até 1000 caracteres."
          :error="errors.description"
        />
        <div class="form-grid">
          <FormField
            id="inicio"
            v-model="form.startDate"
            label="Data de início"
            date
            hint="dd/mm/aaaa"
            :error="errors.startDate"
          />
          <FormField
            id="fim"
            v-model="form.expectedEndDate"
            label="Data de fim prevista"
            optional
            date
            :hint="editing ? 'Deixe vazio para retirar a data.' : 'dd/mm/aaaa'"
            :error="errors.expectedEndDate"
          />
        </div>
      </div>
      <div class="form-actions" :class="narrow ? 'card-body narrow' : 'card-footer'">
        <button type="submit" class="btn primary-action" :disabled="submitting">
          <span v-if="submitting" class="spinner" aria-hidden="true"></span>
          {{ submitting ? 'A guardar...' : editing ? 'Guardar' : 'Criar projeto' }}
        </button>
        <button type="button" class="btn secondary" :disabled="submitting" @click="cancel">Cancelar</button>
      </div>
    </form>
  </main>
</template>

<style scoped>
.project-form,
.notice {
  max-width: 720px;
}

.notice {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 16px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
}

/* No computador, Cancelar fica à esquerda do botão principal; no telemóvel, por baixo. */
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
