<script setup lang="ts">
// Painel inicial (DA21, US21, RN33): quem entrou, os totais de tarefas por estado dos projetos
// visíveis e um cartão por projeto (sem os arquivados, pela ordem da lista de projetos).
// É pedido de novo sempre que se abre, por isso reflete as alterações sem recarregar a página.
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getDashboard } from '@/api/dashboard'
import type { Dashboard } from '@/api/types'
import { currentUser } from '@/session'
import { roleLabels } from '@/labels'
import StatusTag from '@/components/StatusTag.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import TaskCounts from '@/components/TaskCounts.vue'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'

const dashboard = ref<Dashboard | null>(null)
const state = ref<'loading' | 'ready' | 'error'>('loading')

const subtitle = computed(() => {
  const user = currentUser.value
  if (!user) {
    return ''
  }
  return [user.name, roleLabels[user.role], ...user.teams.map((team) => team.name)].join(' · ')
})

async function load() {
  state.value = 'loading'
  try {
    dashboard.value = await getDashboard()
    state.value = 'ready'
  } catch {
    state.value = 'error'
  }
}

void load()
</script>

<template>
  <main class="page">
    <div class="page-header">
      <h1>Painel</h1>
      <p class="subtitle">{{ subtitle }}</p>
    </div>

    <LoadingState v-if="state === 'loading'" text="A carregar o painel..." />
    <ErrorState v-else-if="state === 'error'" message="Não foi possível carregar o painel." @retry="load" />
    <EmptyState v-else-if="dashboard && dashboard.projects.length === 0" title="Não há projetos para mostrar" />

    <template v-else-if="dashboard">
      <section aria-labelledby="totais-titulo" class="block">
        <h2 id="totais-titulo" class="section-label rule">Totais dos projetos visíveis</h2>
        <TaskCounts :counts="dashboard.totals" label="Totais de tarefas por estado" />
      </section>

      <section aria-labelledby="projetos-titulo" class="block">
        <h2 id="projetos-titulo" class="section-label rule">Projetos</h2>
        <ul class="cards">
          <li v-for="project in dashboard.projects" :key="project.id" class="card project-card">
            <div class="card-top">
              <RouterLink :to="{ name: 'project', params: { id: project.id } }" class="item-link name">
                {{ project.name }}
              </RouterLink>
              <StatusTag :status="project.status" />
            </div>
            <dl class="facts">
              <dt>Equipa</dt>
              <dd>{{ project.team.name }}</dd>
              <dt>Gestor</dt>
              <dd>{{ project.manager?.name ?? 'Sem gestor' }}</dd>
            </dl>
            <TaskCounts :counts="project.taskCounts" :label="`Tarefas de ${project.name}`" compact />
            <div class="progress-block">
              <ProgressBar :progress="project.progress" />
            </div>
          </li>
        </ul>
      </section>
    </template>
  </main>
</template>

<style scoped>
.block {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rule {
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.cards {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 320px), 1fr));
  gap: 16px;
}

.project-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.name {
  font-size: 17px;
  line-height: 1.3;
}

.progress-block {
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

@media (max-width: 719.98px) {
  .project-card {
    padding: 16px;
  }
}
</style>
