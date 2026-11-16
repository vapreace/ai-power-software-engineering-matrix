<script setup lang="ts">
// Ecrã As minhas tarefas (US08, docs/ui/README.md secção 9): as tarefas de que a pessoa é
// responsável em projetos ativos (RN25), pela ordem de RN24 que a API devolve, com o filtro
// "Incluir concluídas". A tarefa abre o detalhe e o projeto abre o projeto.
import { ref, watch } from 'vue'
import { listMyTasks } from '@/api/tasks'
import type { MyTask } from '@/api/types'
import TaskList from '@/components/TaskList.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import ErrorState from '@/components/ErrorState.vue'

const tasks = ref<MyTask[]>([])
const includeCompleted = ref(false)
const state = ref<'loading' | 'ready' | 'error'>('loading')

async function load() {
  state.value = 'loading'
  try {
    tasks.value = (await listMyTasks(includeCompleted.value)).items
    state.value = 'ready'
  } catch {
    state.value = 'error'
  }
}

watch(includeCompleted, load, { immediate: true })
</script>

<template>
  <main class="page">
    <div class="page-header">
      <div class="page-title-row">
        <div class="page-title">
          <h1>As minhas tarefas</h1>
        </div>
      </div>
      <p class="muted">Tarefas de que é responsável nos projetos ativos, por prazo.</p>
    </div>

    <section class="card" aria-labelledby="minhas-tarefas-titulo">
      <div class="card-header">
        <h2 id="minhas-tarefas-titulo" class="section-label">Tarefas</h2>
        <label class="check">
          <input v-model="includeCompleted" type="checkbox" />
          Incluir concluídas
        </label>
      </div>
      <LoadingState v-if="state === 'loading'" text="A carregar as tarefas..." />
      <ErrorState v-else-if="state === 'error'" message="Não foi possível carregar as tarefas." @retry="load" />
      <EmptyState
        v-else-if="tasks.length === 0"
        title="Não tem tarefas atribuídas"
        :detail="includeCompleted ? undefined : 'As tarefas concluídas aparecem com Incluir concluídas.'"
      />
      <TaskList v-else :tasks="tasks" caption="As minhas tarefas" with-project />
    </section>
  </main>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  font-size: 14px;
}

.check input {
  width: 18px;
  height: 18px;
}
</style>
