<script setup lang="ts">
// Novo comentário (US19). O autor e a data e hora vêm do backend (E43). Com erro, o texto escrito
// mantém-se (docs/ui/README.md, secção 10).
import { ref } from 'vue'
import { ApiError } from '@/api/http'
import { createComment } from '@/api/tasks'
import type { Comment } from '@/api/types'
import { textLength } from '@/labels'
import FormField from './FormField.vue'

const props = defineProps<{ taskId: number }>()
const emit = defineEmits<{ created: [comment: Comment] }>()

const MESSAGE = 'O comentário é obrigatório e tem no máximo 2000 caracteres.'
const content = ref('')
const error = ref<string | undefined>()
const message = ref<string | null>(null)
const submitting = ref(false)

async function submit() {
  if (submitting.value) {
    return
  }
  const length = textLength(content.value)
  error.value = length === 0 || length > 2000 ? MESSAGE : undefined
  message.value = null
  if (error.value) {
    return
  }
  submitting.value = true
  try {
    const created = await createComment(props.taskId, content.value)
    content.value = ''
    emit('created', created)
  } catch (e) {
    if (e instanceof ApiError) {
      error.value = e.fieldError('content')
      message.value = error.value ? null : e.problem.detail
    } else {
      message.value = 'Não foi possível guardar o comentário. Tente de novo.'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="comment-form" novalidate @submit.prevent="submit">
    <p v-if="message" class="alert" role="alert">{{ message }}</p>
    <FormField
      id="novo-comentario"
      v-model="content"
      label="Novo comentário"
      multiline
      :rows="3"
      hint="Até 2000 caracteres."
      :error="error"
    />
    <div class="actions end">
      <button type="submit" class="btn" :disabled="submitting">
        <span v-if="submitting" class="spinner" aria-hidden="true"></span>
        {{ submitting ? 'A enviar...' : 'Comentar' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.comment-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.end {
  justify-content: flex-end;
}
</style>
