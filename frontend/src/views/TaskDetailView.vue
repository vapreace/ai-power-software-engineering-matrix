<script setup lang="ts">
// Ecrã Detalhe da tarefa (docs/ui/README.md, secção 9): dados, estado, prioridade e prazo.
// Mudar o estado: o Administrador, o gestor do projeto e os responsáveis (RN15, US07).
// Editar e apagar (com confirmação): o Administrador e o gestor do projeto (US16, RN19).
// Responsáveis: associar e remover (US06). Comentários: ler e escrever (US19, RN30).
// Num projeto concluído ou arquivado nada se altera (RN09).
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { getProject } from '@/api/projects'
import { changeTaskStatus, deleteTask, getTask, listComments } from '@/api/tasks'
import type { Comment, Project, Task, TaskStatus } from '@/api/types'
import { currentUser } from '@/session'
import { canChangeTaskStatus, canComment, canManageProject } from '@/permissions'
import { deleteTaskMessage, formatDate, taskStatusLabels } from '@/labels'
import { useNarrowScreen } from '@/composables/useNarrowScreen'
import StatusTag from '@/components/StatusTag.vue'
import PriorityTag from '@/components/PriorityTag.vue'
import OverdueBadge from '@/components/OverdueBadge.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import LoadingState from '@/components/LoadingState.vue'
import ErrorState from '@/components/ErrorState.vue'
import AssigneePicker from '@/components/AssigneePicker.vue'
import CommentList from '@/components/CommentList.vue'
import CommentForm from '@/components/CommentForm.vue'

const route = useRoute()
const router = useRouter()
const narrow = useNarrowScreen()

const task = ref<Task | null>(null)
const project = ref<Project | null>(null)
const state = ref<'loading' | 'ready' | 'error' | 'refused'>('loading')
const refusal = ref('')
const newStatus = ref<TaskStatus | null>(null)
const changing = ref(false)
const statusMessage = ref<string | null>(null)
const versionConflict = ref(false)
const statusOk = ref(false)
const confirming = ref(false)
const deleting = ref(false)
const actionError = ref<string | null>(null)
const comments = ref<Comment[]>([])
const commentsState = ref<'loading' | 'ready' | 'error'>('loading')

const open = computed(() => project.value?.status === 'ACTIVE')
const canManage = computed(() => project.value !== null && canManageProject(currentUser.value, project.value))
const canEdit = computed(() => canManage.value && open.value)
const canChangeStatus = computed(
  () =>
    open.value &&
    task.value !== null &&
    project.value !== null &&
    canChangeTaskStatus(currentUser.value, project.value, task.value),
)
const statusOptions = computed(() =>
  (['TODO', 'IN_PROGRESS', 'DONE'] as TaskStatus[]).filter((status) => status !== task.value?.status),
)
const mayComment = computed(() => open.value && project.value !== null && canComment(currentUser.value, project.value))

async function loadComments() {
  if (!task.value) {
    return
  }
  commentsState.value = 'loading'
  try {
    comments.value = (await listComments(task.value.id)).items
    commentsState.value = 'ready'
  } catch {
    commentsState.value = 'error'
  }
}

function onCommentCreated(comment: Comment) {
  // A resposta da API é o registo confirmado pelo backend (docs/ui/README.md, secção 10).
  comments.value = [...comments.value, comment]
}

function onAssigneesUpdated(updated: Task) {
  task.value = updated
  newStatus.value = statusOptions.value[0] ?? null
}

async function load() {
  state.value = 'loading'
  actionError.value = null
  statusMessage.value = null
  versionConflict.value = false
  try {
    task.value = await getTask(Number(route.params.id))
    project.value = await getProject(task.value.projectId)
    newStatus.value = statusOptions.value[0] ?? null
    state.value = 'ready'
    void loadComments()
  } catch (error) {
    if (error instanceof ApiError && [403, 404].includes(error.problem.status)) {
      refusal.value = error.problem.detail
      state.value = 'refused'
    } else {
      state.value = 'error'
    }
  }
}

async function submitStatus() {
  if (!task.value || !newStatus.value || changing.value) {
    return
  }
  changing.value = true
  statusMessage.value = null
  statusOk.value = false
  versionConflict.value = false
  try {
    task.value = await changeTaskStatus(task.value.id, newStatus.value, task.value.version)
    newStatus.value = statusOptions.value[0] ?? null
    statusMessage.value = `Estado mudado para "${taskStatusLabels[task.value.status]}".`
    statusOk.value = true
  } catch (error) {
    if (error instanceof ApiError) {
      statusMessage.value = error.problem.detail
      versionConflict.value = error.problem.code === 'VERSION_CONFLICT'
    } else {
      statusMessage.value = 'Não foi possível mudar o estado. Tente de novo.'
    }
  } finally {
    changing.value = false
  }
}

async function confirmDelete() {
  if (!task.value || deleting.value) {
    return
  }
  deleting.value = true
  const projectId = task.value.projectId
  try {
    await deleteTask(task.value.id)
    confirming.value = false
    await router.push({ name: 'project', params: { id: projectId } })
  } catch (error) {
    confirming.value = false
    actionError.value =
      error instanceof ApiError ? error.problem.detail : 'Não foi possível apagar a tarefa. Tente de novo.'
  } finally {
    deleting.value = false
  }
}

watch(() => route.params.id, load, { immediate: true })
</script>

<template>
  <main class="page">
    <LoadingState v-if="state === 'loading'" text="A carregar a tarefa..." />
    <ErrorState v-else-if="state === 'error'" message="Não foi possível carregar a tarefa." @retry="load" />
    <div v-else-if="state === 'refused'" class="card card-body refused">
      <p class="alert" role="alert">{{ refusal }}</p>
      <RouterLink :to="{ name: 'projects' }">Ir para Projetos</RouterLink>
    </div>

    <template v-else-if="task && project">
      <div class="page-header">
        <nav aria-label="Caminho" class="breadcrumb">
          <RouterLink :to="{ name: 'projects' }">Projetos</RouterLink>
          <span aria-hidden="true">/</span>
          <RouterLink :to="{ name: 'project', params: { id: project.id } }">{{ project.name }}</RouterLink>
          <span aria-hidden="true">/</span>
          <span aria-current="page">{{ task.title }}</span>
        </nav>
        <div class="page-title-row">
          <div class="page-title">
            <h1>{{ task.title }}</h1>
            <StatusTag :status="task.status" />
          </div>
          <div v-if="canEdit" class="actions" :class="{ 'mobile-actions': narrow }">
            <RouterLink :to="{ name: 'task-edit', params: { id: task.id } }" class="btn secondary small">Editar</RouterLink>
            <button type="button" class="btn danger-outline small" @click="confirming = true">Apagar tarefa</button>
          </div>
        </div>
      </div>

      <p v-if="actionError" class="alert" role="alert">{{ actionError }}</p>
      <p v-if="!open" class="alert info">O projeto está concluído ou arquivado: a tarefa só pode ser consultada.</p>

      <div class="layout" :class="{ narrow }">
        <aside class="card summary" aria-labelledby="resumo-tarefa">
          <h2 id="resumo-tarefa" class="section-label summary-title">Resumo</h2>

          <form v-if="canChangeStatus" class="status-form" @submit.prevent="submitStatus">
            <label for="novo-estado" class="block-label">Mudar estado para</label>
            <div class="status-row">
              <select id="novo-estado" v-model="newStatus" class="control">
                <option v-for="status in statusOptions" :key="status" :value="status">
                  {{ taskStatusLabels[status] }}
                </option>
              </select>
              <button type="submit" class="btn secondary small" :disabled="changing">
                {{ changing ? 'A mudar...' : 'Mudar estado' }}
              </button>
            </div>
            <div
              v-if="statusMessage"
              :class="statusOk ? 'alert info' : 'alert'"
              :role="statusOk ? 'status' : 'alert'"
            >
              <span>{{ statusMessage }}</span>
              <button v-if="versionConflict" type="button" class="btn secondary small reopen" @click="load">
                Abrir de novo
              </button>
            </div>
          </form>

          <dl class="facts summary-facts">
            <dt>Prioridade</dt>
            <dd><PriorityTag :priority="task.priority" /></dd>
            <dt>Prazo</dt>
            <dd class="due">
              <span class="mono">{{ task.dueDate ? formatDate(task.dueDate) : 'Sem prazo' }}</span>
              <OverdueBadge v-if="task.overdue" task />
            </dd>
            <dt>Projeto</dt>
            <dd>
              <RouterLink :to="{ name: 'project', params: { id: project.id } }">{{ project.name }}</RouterLink>
            </dd>
          </dl>

          <AssigneePicker
            :task="task"
            :team-id="project.team.id"
            :editable="canEdit"
            @updated="onAssigneesUpdated"
          />
        </aside>

        <div class="main-column">
          <section class="card card-body" aria-labelledby="descricao-titulo">
            <h2 id="descricao-titulo" class="section-label card-title">Descrição</h2>
            <p v-if="task.description" class="description">{{ task.description }}</p>
            <p v-else class="muted description">Sem descrição.</p>
          </section>
          <section class="card card-body" aria-labelledby="comentarios-titulo">
            <h2 id="comentarios-titulo" class="section-label card-title">Comentários</h2>
            <LoadingState v-if="commentsState === 'loading'" text="A carregar comentários..." />
            <ErrorState
              v-else-if="commentsState === 'error'"
              message="Não foi possível carregar os comentários."
              @retry="loadComments"
            />
            <CommentList v-else :comments="comments" />
            <CommentForm v-if="mayComment && commentsState === 'ready'" :task-id="task.id" @created="onCommentCreated" />
          </section>
        </div>
      </div>
    </template>

    <ConfirmDialog
      v-if="confirming && task"
      id="apagar-tarefa"
      title="Apagar tarefa"
      :message="deleteTaskMessage(task.title)"
      danger
      :busy="deleting"
      busy-label="A apagar..."
      @confirm="confirmDelete"
      @cancel="confirming = false"
    />
  </main>
</template>

<style scoped>
.refused {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 16px;
  max-width: 720px;
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  grid-template-areas: 'main summary';
  gap: 24px;
  align-items: start;
}

.layout.narrow {
  grid-template-columns: minmax(0, 1fr);
  grid-template-areas:
    'summary'
    'main';
  gap: 16px;
}

.main-column {
  grid-area: main;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary {
  grid-area: summary;
  padding: 16px 24px 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.layout.narrow .summary {
  padding: 16px;
}

.summary-title {
  min-height: 40px;
  display: flex;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.status-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-row {
  display: flex;
  gap: 8px;
}

.status-row .control {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 40px;
  padding: 6px 8px;
  font-size: 14px;
}

.reopen {
  margin-top: 8px;
}

.summary-facts {
  gap: 10px 16px;
  align-items: center;
}

.due {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.block-label {
  font-size: 13px;
  font-weight: 600;
}

.description {
  margin-top: 12px;
  white-space: pre-line;
  overflow-wrap: anywhere;
}

.mobile-actions {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.mobile-actions .btn {
  width: 100%;
}
</style>
