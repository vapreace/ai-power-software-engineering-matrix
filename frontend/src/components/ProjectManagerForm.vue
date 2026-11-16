<script setup lang="ts">
// Mudar o gestor do projeto (US17): só o Administrador, também em projetos fechados (RN21,
// exceção de RN09). As pessoas elegíveis têm papel Gestor, conta ativa e pertencem à equipa do
// projeto. A regra que conta é a do backend (SEG02).
import { computed, onMounted, ref } from 'vue'
import { ApiError } from '@/api/http'
import { changeProjectManager } from '@/api/projects'
import { listTeamMembers } from '@/api/teams'
import type { Project, TeamMember } from '@/api/types'

const props = defineProps<{ project: Project }>()
const emit = defineEmits<{ updated: [project: Project]; reload: [] }>()

const members = ref<TeamMember[]>([])
const selected = ref<number | null>(null)
const busy = ref(false)
const message = ref<string | null>(null)
const versionConflict = ref(false)

const eligible = computed(() =>
  members.value.filter(
    (m) => m.role === 'MANAGER' && m.status === 'ACTIVE' && m.id !== props.project.manager?.id,
  ),
)

onMounted(async () => {
  try {
    members.value = (await listTeamMembers(props.project.team.id)).items
  } catch {
    message.value = 'Não foi possível carregar as pessoas da equipa.'
  }
})

async function submit() {
  if (selected.value === null || busy.value) {
    return
  }
  busy.value = true
  message.value = null
  versionConflict.value = false
  try {
    emit('updated', await changeProjectManager(props.project.id, selected.value, props.project.version))
    selected.value = null
  } catch (error) {
    if (error instanceof ApiError) {
      message.value = error.problem.detail
      versionConflict.value = error.problem.code === 'VERSION_CONFLICT'
    } else {
      message.value = 'Não foi possível mudar o gestor. Tente de novo.'
    }
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="manager-form" @submit.prevent="submit">
    <label for="novo-gestor" class="block-label">Mudar gestor</label>
    <div class="row">
      <select id="novo-gestor" v-model="selected" class="control" :disabled="eligible.length === 0">
        <option :value="null" disabled>
          {{ eligible.length ? 'Escolha um Gestor' : 'Sem outros Gestores' }}
        </option>
        <option v-for="member in eligible" :key="member.id" :value="member.id">{{ member.name }}</option>
      </select>
      <button type="submit" class="btn secondary small" :disabled="busy || selected === null">
        {{ busy ? 'A mudar...' : 'Mudar' }}
      </button>
    </div>
    <div v-if="message" class="alert" role="alert">
      <span>{{ message }}</span>
      <button v-if="versionConflict" type="button" class="btn secondary small reopen" @click="emit('reload')">
        Abrir de novo
      </button>
    </div>
  </form>
</template>

<style scoped>
.manager-form {
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
