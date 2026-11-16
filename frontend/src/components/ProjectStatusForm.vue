<script setup lang="ts">
// Mudar o estado do projeto (US13): só as transições permitidas (RN07). Concluir com tarefas
// abertas pede confirmação com o número delas (RN10); arquivar não pede. Quem pode: o
// Administrador e o gestor do projeto (RN05, RN22). A regra que conta é a do backend (SEG02).
import { computed, ref, watch } from 'vue'
import { ApiError } from '@/api/http'
import { changeProjectStatus } from '@/api/projects'
import type { Project, ProjectStatus } from '@/api/types'
import { completeProjectMessage, projectStatusLabels, projectTransitions } from '@/labels'
import ConfirmDialog from './ConfirmDialog.vue'

const props = defineProps<{ project: Project }>()
const emit = defineEmits<{ updated: [project: Project]; reload: [] }>()

const options = computed(() => projectTransitions[props.project.status])
const target = ref<ProjectStatus | null>(null)
const busy = ref(false)
const confirming = ref(false)
const message = ref<string | null>(null)
const versionConflict = ref(false)

const openTasks = computed(() => props.project.progress.totalTasks - props.project.progress.completedTasks)

watch(
  () => props.project.status,
  () => {
    target.value = options.value[0] ?? null
  },
  { immediate: true },
)

function submit() {
  if (target.value === null || busy.value) {
    return
  }
  if (target.value === 'COMPLETED' && props.project.status === 'ACTIVE' && openTasks.value > 0) {
    confirming.value = true
    return
  }
  void save()
}

async function save() {
  if (target.value === null) {
    return
  }
  busy.value = true
  message.value = null
  versionConflict.value = false
  try {
    emit('updated', await changeProjectStatus(props.project.id, target.value, props.project.version))
  } catch (error) {
    if (error instanceof ApiError) {
      message.value = error.problem.detail
      versionConflict.value = error.problem.code === 'VERSION_CONFLICT'
    } else {
      message.value = 'Não foi possível mudar o estado. Tente de novo.'
    }
  } finally {
    busy.value = false
    confirming.value = false
  }
}
</script>

<template>
  <form class="status-form" @submit.prevent="submit">
    <label for="estado-projeto" class="block-label">Mudar estado para</label>
    <div class="row">
      <select id="estado-projeto" v-model="target" class="control">
        <option v-for="status in options" :key="status" :value="status">{{ projectStatusLabels[status] }}</option>
      </select>
      <button type="submit" class="btn secondary small" :disabled="busy">
        {{ busy ? 'A mudar...' : 'Mudar estado' }}
      </button>
    </div>
    <div v-if="message" class="alert" role="alert">
      <span>{{ message }}</span>
      <button v-if="versionConflict" type="button" class="btn secondary small reopen" @click="emit('reload')">
        Abrir de novo
      </button>
    </div>
    <ConfirmDialog
      v-if="confirming"
      id="concluir-projeto"
      title="Concluir projeto"
      :message="completeProjectMessage(openTasks)"
      confirm-label="Concluir"
      :busy="busy"
      busy-label="A concluir..."
      @confirm="save"
      @cancel="confirming = false"
    />
  </form>
</template>

<style scoped>
.status-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.block-label {
  font-size: 13px;
  font-weight: 600;
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

.reopen {
  margin-top: 8px;
}
</style>
