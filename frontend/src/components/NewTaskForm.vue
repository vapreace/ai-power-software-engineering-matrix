<script setup lang="ts">
// Formulário "Nova tarefa" dentro do cartão Tarefas (US05). A tarefa nasce "Por fazer" e
// "Sem responsável" (RN12). Prioridade "Média" por omissão (RN11). O prazo valida-se no backend,
// com a data atual da organização (RN13, RNF06).
import { reactive, ref } from 'vue'
import { ApiError } from '@/api/http'
import { createTask } from '@/api/tasks'
import type { Task, TaskPriority } from '@/api/types'
import { parseDate, priorityLabels, textLength } from '@/labels'
import { useNarrowScreen } from '@/composables/useNarrowScreen'
import FormField from './FormField.vue'
import SelectField from './SelectField.vue'

const props = defineProps<{ projectId: number }>()
const emit = defineEmits<{ created: [task: Task]; cancel: [] }>()
const narrow = useNarrowScreen()

type Field = 'title' | 'description' | 'dueDate' | 'priority'

const priorities: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH']
const priorityOptions = priorities.map((value) => ({ value, label: priorityLabels[value] }))

const form = reactive({ title: '', description: '', dueDate: '', priority: 'MEDIUM' as TaskPriority | null })
const errors = reactive<Partial<Record<Field, string>>>({})
const message = ref<string | null>(null)
const submitting = ref(false)

function validate(): string | null | undefined {
  ;(Object.keys(errors) as Field[]).forEach((key) => delete errors[key])
  message.value = null
  const length = textLength(form.title)
  if (length === 0 || length > 200) errors.title = 'O título é obrigatório e tem no máximo 200 caracteres.'
  if (textLength(form.description) > 2000) errors.description = 'A descrição tem no máximo 2000 caracteres.'
  const dueDate = parseDate(form.dueDate)
  if (dueDate === undefined) errors.dueDate = 'Indique a data no formato dd/mm/aaaa.'
  return Object.keys(errors).length === 0 ? dueDate : undefined
}

async function submit() {
  if (submitting.value) {
    return
  }
  const dueDate = validate()
  if (dueDate === undefined) {
    return
  }
  submitting.value = true
  try {
    const task = await createTask(props.projectId, {
      title: form.title,
      description: form.description.trim() ? form.description : null,
      dueDate,
      priority: form.priority ?? 'MEDIUM',
    })
    emit('created', task)
  } catch (error) {
    // Os dados escritos mantêm-se no formulário (docs/ui/README.md, secção 10).
    if (error instanceof ApiError) {
      let shown = false
      for (const field of ['title', 'description', 'dueDate', 'priority'] as Field[]) {
        const fieldMessage = error.fieldError(field)
        if (fieldMessage) {
          errors[field] = fieldMessage
          shown = true
        }
      }
      if (!shown) message.value = error.problem.detail
    } else {
      message.value = 'Não foi possível criar a tarefa. Tente de novo.'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="new-task" novalidate aria-labelledby="nova-tarefa-titulo" @submit.prevent="submit">
    <h3 id="nova-tarefa-titulo">Nova tarefa</h3>
    <p v-if="message" class="alert" role="alert">{{ message }}</p>
    <div class="row" :class="{ narrow }">
      <FormField id="nt-titulo" v-model="form.title" label="Título" hint="Até 200 caracteres." :error="errors.title" />
      <FormField
        id="nt-prazo"
        v-model="form.dueDate"
        label="Prazo"
        optional
        date
        hint="dd/mm/aaaa"
        :error="errors.dueDate"
      />
      <SelectField
        id="nt-prioridade"
        v-model="form.priority"
        label="Prioridade"
        :options="priorityOptions"
        :error="errors.priority"
      />
    </div>
    <FormField
      id="nt-descricao"
      v-model="form.description"
      label="Descrição"
      optional
      multiline
      :rows="3"
      hint="Até 2000 caracteres."
      :error="errors.description"
    />
    <div class="footer" :class="{ narrow }">
      <p class="muted note">A tarefa fica "Por fazer" e "Sem responsável".</p>
      <div class="actions" :class="{ 'stack-on-mobile': narrow }">
        <button type="button" class="btn secondary small" :disabled="submitting" @click="emit('cancel')">Cancelar</button>
        <button type="submit" class="btn small" :disabled="submitting">
          <span v-if="submitting" class="spinner" aria-hidden="true"></span>
          {{ submitting ? 'A criar...' : 'Criar tarefa' }}
        </button>
      </div>
    </div>
  </form>
</template>

<style scoped>
.new-task {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 24px 24px;
  border-top: 1px solid var(--border);
  background: var(--surface-muted);
}

h3 {
  font-size: 16px;
  font-weight: 600;
}

.row {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.row.narrow {
  grid-template-columns: minmax(0, 1fr);
}

.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.footer.narrow {
  flex-direction: column;
  align-items: stretch;
}

.note {
  font-size: 13px;
}

@media (max-width: 719.98px) {
  .new-task {
    padding: 16px;
  }
}
</style>
