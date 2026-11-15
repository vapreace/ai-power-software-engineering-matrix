<script setup lang="ts">
// Responsáveis de uma tarefa (US06): lista com "Remover" e "Associar responsável" com as pessoas
// elegíveis (RN14): pertencem à equipa do projeto, têm conta ativa e papel Gestor ou Membro, e
// ainda não são responsáveis. Só quem gere o projeto altera, com o projeto ativo (RN05, RN09, RN22).
// Sem permissão, mostra só a lista. A regra que conta é a do backend (SEG02).
import { computed, ref, watch } from 'vue'
import { ApiError } from '@/api/http'
import { addAssignee, removeAssignee } from '@/api/tasks'
import { listTeamMembers } from '@/api/teams'
import type { Task, TeamMember } from '@/api/types'

const props = defineProps<{ task: Task; teamId: number; editable: boolean }>()
const emit = defineEmits<{ updated: [task: Task] }>()

const members = ref<TeamMember[]>([])
const selected = ref<number | null>(null)
const busy = ref(false)
const message = ref<string | null>(null)

const eligible = computed(() =>
  members.value.filter(
    (m) =>
      m.status === 'ACTIVE' &&
      (m.role === 'MANAGER' || m.role === 'MEMBER') &&
      !props.task.assignees.some((a) => a.id === m.id),
  ),
)

async function loadMembers() {
  if (!props.editable) {
    return
  }
  try {
    members.value = (await listTeamMembers(props.teamId)).items
  } catch {
    message.value = 'Não foi possível carregar as pessoas da equipa.'
  }
}

async function run(action: () => Promise<Task>) {
  if (busy.value) {
    return
  }
  busy.value = true
  message.value = null
  try {
    emit('updated', await action())
    selected.value = null
  } catch (error) {
    message.value = error instanceof ApiError ? error.problem.detail : 'Não foi possível alterar os responsáveis.'
  } finally {
    busy.value = false
  }
}

function associate() {
  if (selected.value !== null) {
    const userId = selected.value
    void run(() => addAssignee(props.task.id, userId))
  }
}

function remove(userId: number) {
  void run(() => removeAssignee(props.task.id, userId))
}

watch(() => [props.editable, props.teamId], loadMembers, { immediate: true })
</script>

<template>
  <div class="assignees">
    <span id="responsaveis-titulo" class="block-label">Responsáveis</span>
    <ul v-if="task.assignees.length" class="list" aria-labelledby="responsaveis-titulo">
      <li v-for="assignee in task.assignees" :key="assignee.id">
        <span>{{ assignee.name }}</span>
        <button
          v-if="editable"
          type="button"
          class="link-button"
          :disabled="busy"
          :aria-label="`Remover ${assignee.name}`"
          @click="remove(assignee.id)"
        >
          Remover
        </button>
      </li>
    </ul>
    <p v-else class="muted">Sem responsável</p>

    <form v-if="editable" class="associate" @submit.prevent="associate">
      <label for="associar-responsavel" class="block-label">Associar responsável</label>
      <div class="row">
        <select id="associar-responsavel" v-model="selected" class="control" :disabled="eligible.length === 0">
          <option :value="null" disabled>
            {{ eligible.length ? 'Escolha uma pessoa' : 'Não há mais pessoas elegíveis' }}
          </option>
          <option v-for="member in eligible" :key="member.id" :value="member.id">{{ member.name }}</option>
        </select>
        <button type="submit" class="btn secondary small" :disabled="busy || selected === null">Associar</button>
      </div>
    </form>
    <p v-if="message" class="alert" role="alert">{{ message }}</p>
  </div>
</template>

<style scoped>
.assignees {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.block-label {
  font-size: 13px;
  font-weight: 600;
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
}

.list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 0 12px;
}

.list li + li {
  border-top: 1px solid var(--border);
}

.link-button {
  min-height: 44px;
  padding: 0;
  border: 0;
  background: none;
  color: var(--text-muted);
  font-size: 13px;
  text-decoration: underline;
  text-underline-offset: 3px;
  cursor: pointer;
}

.link-button:hover {
  color: var(--danger);
}

.associate {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.row {
  display: flex;
  gap: 8px;
}

.row .control {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 40px;
  padding: 6px 8px;
  font-size: 14px;
}
</style>
