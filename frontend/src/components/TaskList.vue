<script setup lang="ts">
// Tarefas de um projeto: tabela no computador e cartões no telemóvel (DA22, 720 px), pela ordem
// que a API devolve (DA33). "Atrasada" fica junto ao prazo (US10). O título abre o detalhe.
import { RouterLink } from 'vue-router'
import type { Task } from '@/api/types'
import { formatDate } from '@/labels'
import { useNarrowScreen } from '@/composables/useNarrowScreen'
import StatusTag from './StatusTag.vue'
import PriorityTag from './PriorityTag.vue'
import OverdueBadge from './OverdueBadge.vue'

defineProps<{ tasks: Task[]; caption: string }>()
const narrow = useNarrowScreen()

function assigneesText(task: Task): string {
  return task.assignees.length ? task.assignees.map((a) => a.name).join(', ') : 'Sem responsável'
}
</script>

<template>
  <ul v-if="narrow" class="cards" :aria-label="caption">
    <li v-for="task in tasks" :key="task.id" class="task-card">
      <RouterLink :to="{ name: 'task', params: { id: task.id } }" class="item-link title">{{ task.title }}</RouterLink>
      <div class="tags">
        <StatusTag :status="task.status" />
        <PriorityTag :priority="task.priority" />
        <OverdueBadge v-if="task.overdue" task />
      </div>
      <div class="meta">
        <span>{{ assigneesText(task) }}</span>
        <span class="mono">{{ task.dueDate ? formatDate(task.dueDate) : 'Sem prazo' }}</span>
      </div>
    </li>
  </ul>
  <table v-else class="data-table">
    <caption class="visually-hidden">{{ caption }}</caption>
    <thead>
      <tr>
        <th scope="col">Título</th>
        <th scope="col">Responsáveis</th>
        <th scope="col" class="w-status">Estado</th>
        <th scope="col" class="w-priority">Prioridade</th>
        <th scope="col" class="w-due">Prazo</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="task in tasks" :key="task.id">
        <td>
          <RouterLink :to="{ name: 'task', params: { id: task.id } }" class="item-link task-link">
            {{ task.title }}
          </RouterLink>
        </td>
        <td class="assignees">{{ assigneesText(task) }}</td>
        <td><StatusTag :status="task.status" /></td>
        <td><PriorityTag :priority="task.priority" /></td>
        <td>
          <div class="due">
            <span class="mono">{{ task.dueDate ? formatDate(task.dueDate) : 'Sem prazo' }}</span>
            <OverdueBadge v-if="task.overdue" task />
          </div>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>
.data-table td {
  padding-top: 12px;
  padding-bottom: 12px;
}

.task-link {
  font-weight: 500;
  overflow-wrap: anywhere;
}

.assignees {
  color: var(--text-2);
}

.w-status {
  width: 120px;
}

.w-priority {
  width: 96px;
}

.w-due {
  width: 196px;
}

.due {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.cards {
  list-style: none;
  margin: 0;
  padding: 0;
}

.task-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  border-top: 1px solid var(--border);
}

.title {
  font-weight: 600;
  overflow-wrap: anywhere;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: var(--text-2);
}
</style>
