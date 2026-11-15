<script setup lang="ts">
// Lista de projetos: tabela no computador e cartões no telemóvel (DA22, 720 px).
// Mostra todos os campos de RF09; o nome abre o detalhe do projeto.
import { RouterLink } from 'vue-router'
import type { ProjectSummary } from '@/api/types'
import { formatDate } from '@/labels'
import { useNarrowScreen } from '@/composables/useNarrowScreen'
import StatusTag from './StatusTag.vue'
import ProgressBar from './ProgressBar.vue'
import OverdueBadge from './OverdueBadge.vue'

defineProps<{ projects: ProjectSummary[]; caption: string }>()
const narrow = useNarrowScreen()
</script>

<template>
  <ul v-if="narrow" class="cards" :aria-label="caption">
    <li v-for="project in projects" :key="project.id" class="card project-card">
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
        <dt>Fim previsto</dt>
        <dd class="mono">{{ project.expectedEndDate ? formatDate(project.expectedEndDate) : 'Sem data' }}</dd>
      </dl>
      <div class="card-bottom">
        <ProgressBar :progress="project.progress" />
        <div><OverdueBadge :count="project.overdueTaskCount" /></div>
      </div>
    </li>
  </ul>
  <div v-else class="card table-card">
    <table class="data-table">
      <caption class="visually-hidden">{{ caption }}</caption>
      <thead>
        <tr>
          <th scope="col">Nome</th>
          <th scope="col">Equipa</th>
          <th scope="col">Gestor</th>
          <th scope="col" class="w-status">Estado</th>
          <th scope="col" class="w-date">Fim previsto</th>
          <th scope="col" class="w-progress">Progresso</th>
          <th scope="col" class="w-overdue">Atrasos</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="project in projects" :key="project.id">
          <td>
            <RouterLink :to="{ name: 'project', params: { id: project.id } }" class="item-link">
              {{ project.name }}
            </RouterLink>
          </td>
          <td>{{ project.team.name }}</td>
          <td>{{ project.manager?.name ?? 'Sem gestor' }}</td>
          <td><StatusTag :status="project.status" /></td>
          <td class="mono nowrap">{{ project.expectedEndDate ? formatDate(project.expectedEndDate) : 'Sem data' }}</td>
          <td><ProgressBar :progress="project.progress" /></td>
          <td><OverdueBadge :count="project.overdueTaskCount" /></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-card {
  overflow: hidden;
}

.w-status {
  width: 110px;
}

.w-date {
  width: 120px;
}

.w-progress {
  width: 220px;
}

.w-overdue {
  width: 180px;
}

.nowrap {
  white-space: nowrap;
}

.cards {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.project-card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
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

.card-bottom {
  padding-top: 12px;
  border-top: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
