<script setup lang="ts">
// Ecrã Detalhe do projeto (docs/ui/README.md, secção 9): dados, progresso e atrasos no cartão
// Resumo (US09, US10). Editar e Eliminar só para o Administrador e o gestor do projeto
// (RN05, RN22, RN31). Eliminar pede confirmação com o número de tarefas (US20).
// As tarefas ficam no cartão Tarefas (I3); a mudança de estado e de gestor chega no I7.
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { deleteProject, getProject } from '@/api/projects'
import type { Project } from '@/api/types'
import { currentUser } from '@/session'
import { canManageProject } from '@/permissions'
import { deleteProjectMessage, formatDate } from '@/labels'
import { useNarrowScreen } from '@/composables/useNarrowScreen'
import StatusTag from '@/components/StatusTag.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import OverdueBadge from '@/components/OverdueBadge.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import LoadingState from '@/components/LoadingState.vue'
import ErrorState from '@/components/ErrorState.vue'
import ProjectTasks from '@/components/ProjectTasks.vue'

const route = useRoute()
const router = useRouter()
const narrow = useNarrowScreen()

const project = ref<Project | null>(null)
const state = ref<'loading' | 'ready' | 'error' | 'refused'>('loading')
const refusal = ref('')
const confirming = ref(false)
const deleting = ref(false)
const actionError = ref<string | null>(null)

const canManage = computed(() => project.value !== null && canManageProject(currentUser.value, project.value))
// Um projeto concluído ou arquivado não se edita (RN09), mas pode ser eliminado (RN31).
const canEdit = computed(() => canManage.value && project.value?.status === 'ACTIVE')

async function load() {
  state.value = 'loading'
  actionError.value = null
  try {
    project.value = await getProject(Number(route.params.id))
    state.value = 'ready'
  } catch (error) {
    if (error instanceof ApiError && [403, 404].includes(error.problem.status)) {
      // 403: "Não tem acesso a esta página."; 404: "O projeto não existe." (docs/ui/README.md, secção 10).
      refusal.value = error.problem.detail
      state.value = 'refused'
    } else {
      state.value = 'error'
    }
  }
}

/** Depois de criar uma tarefa: o Resumo (progresso e atrasos) é pedido de novo, sem recarregar. */
async function refreshProject() {
  try {
    project.value = await getProject(Number(route.params.id))
  } catch {
    // Mantém os dados que estão no ecrã; o próximo carregamento mostra o erro.
  }
}

async function confirmDelete() {
  if (!project.value || deleting.value) {
    return
  }
  deleting.value = true
  try {
    await deleteProject(project.value.id)
    confirming.value = false
    await router.push({ name: 'projects' })
  } catch (error) {
    confirming.value = false
    actionError.value =
      error instanceof ApiError ? error.problem.detail : 'Não foi possível eliminar o projeto. Tente de novo.'
  } finally {
    deleting.value = false
  }
}

watch(() => route.params.id, load, { immediate: true })
</script>

<template>
  <main class="page">
    <LoadingState v-if="state === 'loading'" text="A carregar o projeto..." />
    <ErrorState v-else-if="state === 'error'" message="Não foi possível carregar o projeto." @retry="load" />
    <div v-else-if="state === 'refused'" class="card card-body refused">
      <p class="alert" role="alert">{{ refusal }}</p>
      <RouterLink :to="{ name: 'projects' }">Ir para Projetos</RouterLink>
    </div>

    <template v-else-if="project">
      <div class="page-header">
        <nav aria-label="Caminho" class="breadcrumb">
          <RouterLink :to="{ name: 'projects' }">Projetos</RouterLink>
          <span aria-hidden="true">/</span>
          <span aria-current="page">{{ project.name }}</span>
        </nav>
        <div class="page-title-row">
          <div class="page-title">
            <h1>{{ project.name }}</h1>
            <StatusTag :status="project.status" />
          </div>
          <div v-if="canManage" class="actions" :class="{ 'mobile-actions': narrow }">
            <RouterLink
              v-if="canEdit"
              :to="{ name: 'project-edit', params: { id: project.id } }"
              class="btn secondary small"
            >
              Editar
            </RouterLink>
            <button type="button" class="btn danger-outline small" @click="confirming = true">Eliminar projeto</button>
          </div>
        </div>
      </div>

      <p v-if="actionError" class="alert" role="alert">{{ actionError }}</p>

      <div class="layout" :class="{ narrow }">
        <aside class="card summary" aria-labelledby="resumo-titulo">
          <h2 id="resumo-titulo" class="section-label summary-title">Resumo</h2>
          <dl class="facts summary-facts">
            <dt>Equipa</dt>
            <dd>{{ project.team.name }}</dd>
            <dt>Gestor</dt>
            <dd>{{ project.manager?.name ?? 'Sem gestor' }}</dd>
            <dt>Data de início</dt>
            <dd class="mono">{{ formatDate(project.startDate) }}</dd>
            <dt>Fim previsto</dt>
            <dd class="mono">{{ project.expectedEndDate ? formatDate(project.expectedEndDate) : 'Sem data' }}</dd>
          </dl>
          <p v-if="project.description" class="description">{{ project.description }}</p>
          <div class="summary-block separated">
            <span class="block-label">Progresso</span>
            <ProgressBar :progress="project.progress" />
          </div>
          <div class="summary-block">
            <span class="block-label">Atrasos</span>
            <div><OverdueBadge :count="project.overdueTaskCount" /></div>
          </div>
        </aside>

        <ProjectTasks class="tasks" :project="project" :can-create="canEdit" @changed="refreshProject" />
      </div>
    </template>

    <ConfirmDialog
      v-if="confirming && project"
      id="eliminar-projeto"
      title="Eliminar projeto"
      :message="deleteProjectMessage(project.name, project.progress.totalTasks)"
      danger
      :busy="deleting"
      busy-label="A eliminar..."
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
  grid-template-areas: 'tasks summary';
  gap: 24px;
  align-items: start;
}

.layout.narrow {
  grid-template-columns: minmax(0, 1fr);
  grid-template-areas:
    'summary'
    'tasks';
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

.tasks {
  grid-area: tasks;
}

.summary-title {
  min-height: 40px;
  display: flex;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.summary-facts {
  gap: 10px 16px;
}

.description {
  font-size: 14px;
  color: var(--text-2);
  white-space: pre-line;
  overflow-wrap: anywhere;
}

.summary-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.separated {
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.block-label {
  font-size: 13px;
  font-weight: 600;
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
