<script setup lang="ts">
// Cartão Tarefas do detalhe do projeto (US05, US10, US11): "+ Nova tarefa" para quem gere um
// projeto ativo (RN08, RN05), filtros por estado, prioridade e atraso, e a lista. Os vários
// filtros aplicam-se ao mesmo tempo. Depois de criar, a lista e o Resumo são pedidos de novo,
// sem recarregar a página (docs/ui/README.md, secção 10). O filtro por responsável chega no I4.
import { computed, ref, watch } from 'vue'
import { listTasks } from '@/api/tasks'
import type { Project, Task, TaskPriority, TaskStatus } from '@/api/types'
import { priorityLabels, taskStatusLabels } from '@/labels'
import { useNarrowScreen } from '@/composables/useNarrowScreen'
import StatusFilter from './StatusFilter.vue'
import TaskList from './TaskList.vue'
import NewTaskForm from './NewTaskForm.vue'
import LoadingState from './LoadingState.vue'
import ErrorState from './ErrorState.vue'

const props = defineProps<{ project: Project; canCreate: boolean }>()
const emit = defineEmits<{ changed: [] }>()
const narrow = useNarrowScreen()

const statusOptions = (['TODO', 'IN_PROGRESS', 'DONE'] as TaskStatus[]).map((value) => ({
  value,
  label: taskStatusLabels[value],
}))
const priorityOptions = (['LOW', 'MEDIUM', 'HIGH'] as TaskPriority[]).map((value) => ({
  value,
  label: priorityLabels[value],
}))

const statuses = ref<TaskStatus[]>([])
const priorities = ref<TaskPriority[]>([])
const overdue = ref(false)
const tasks = ref<Task[]>([])
const state = ref<'loading' | 'ready' | 'error'>('loading')
const creating = ref(false)
const announcement = ref('')
let requestId = 0

const filtered = computed(() => statuses.value.length > 0 || priorities.value.length > 0 || overdue.value)

async function load() {
  const id = ++requestId
  state.value = 'loading'
  try {
    const result = await listTasks(props.project.id, {
      statuses: statuses.value,
      priorities: priorities.value,
      overdue: overdue.value,
    })
    if (id === requestId) {
      tasks.value = result.items
      state.value = 'ready'
    }
  } catch {
    if (id === requestId) {
      state.value = 'error'
    }
  }
}

async function onCreated(task: Task) {
  creating.value = false
  announcement.value = `Tarefa «${task.title}» criada.`
  await load()
  emit('changed')
}

watch([statuses, priorities, overdue], load)
watch(() => props.project.id, load)
void load()
</script>

<template>
  <section class="card tasks" aria-labelledby="tarefas-titulo">
    <div class="card-header">
      <h2 id="tarefas-titulo" class="section-label">Tarefas</h2>
      <button v-if="canCreate && !creating" type="button" class="btn small" @click="creating = true">
        + Nova tarefa
      </button>
    </div>

    <NewTaskForm v-if="creating" :project-id="project.id" @created="onCreated" @cancel="creating = false" />

    <div role="search" aria-label="Filtrar tarefas" class="filters" :class="{ narrow }">
      <StatusFilter v-model="statuses" legend="Estado" :options="statusOptions" />
      <StatusFilter v-model="priorities" legend="Prioridade" :options="priorityOptions" />
      <fieldset class="overdue-filter">
        <legend class="section-label">Atraso</legend>
        <label class="option">
          <input v-model="overdue" type="checkbox" />
          Atrasadas
        </label>
      </fieldset>
    </div>

    <p class="visually-hidden" role="status" aria-live="polite">{{ announcement }}</p>

    <div v-if="state === 'loading'" class="state"><LoadingState text="A carregar tarefas..." /></div>
    <div v-else-if="state === 'error'" class="state">
      <ErrorState message="Não foi possível carregar as tarefas." @retry="load" />
    </div>
    <p v-else-if="tasks.length === 0" class="empty" role="status">
      {{ filtered ? 'Nenhuma tarefa corresponde aos filtros' : 'Este projeto ainda não tem tarefas.' }}
    </p>
    <TaskList v-else :tasks="tasks" :caption="`Tarefas do projeto ${project.name}`" />
  </section>
</template>

<style scoped>
.tasks {
  overflow: hidden;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0 24px;
  padding: 4px 24px;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  background: var(--surface-muted);
}

.filters.narrow {
  flex-direction: column;
  align-items: stretch;
  padding: 8px 16px;
}

.overdue-filter {
  margin: 0;
  padding: 0;
  border: 0;
  display: flex;
  align-items: center;
  gap: 0 18px;
}

.overdue-filter legend {
  float: left;
  padding: 0;
  margin-right: 2px;
  line-height: 44px;
}

.option {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  cursor: pointer;
}

.option input {
  width: 16px;
  height: 16px;
  margin: 0;
  accent-color: var(--text);
}

.state {
  padding: 16px;
}

.state :deep(.card) {
  border: 0;
}

.empty {
  padding: 32px 24px;
  text-align: center;
  font-weight: 600;
}
</style>
