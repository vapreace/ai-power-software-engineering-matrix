<script setup lang="ts">
// Ecrã Definir palavra-passe nova (docs/ui/README.md, secção 9; US01, DA25, DA26).
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { logout, setNewPassword } from '@/api/session'
import { clearSession, loadSession } from '@/session'
import FormField from '@/components/FormField.vue'

const router = useRouter()

const form = reactive({ newPassword: '', confirmation: '' })
const errors = reactive<{ newPassword?: string; confirmation?: string }>({})
const message = ref<string | null>(null)
const submitting = ref(false)

const LENGTH_MESSAGE = 'A palavra-passe tem de ter entre 12 e 128 caracteres.'

function validate(): boolean {
  // Conta caracteres como o backend: cada código Unicode conta um (DA26).
  const length = [...form.newPassword].length
  errors.newPassword = length >= 12 && length <= 128 ? undefined : LENGTH_MESSAGE
  errors.confirmation =
    form.confirmation === form.newPassword ? undefined : 'As palavras-passe não coincidem.'
  return !errors.newPassword && !errors.confirmation
}

async function submit() {
  if (submitting.value || !validate()) {
    return
  }
  submitting.value = true
  message.value = null
  try {
    await setNewPassword(form.newPassword)
    await loadSession(true)
    await router.replace({ name: 'home' })
  } catch (error) {
    if (error instanceof ApiError) {
      errors.newPassword = error.fieldError('newPassword')
      message.value = errors.newPassword ? null : error.problem.detail
    } else {
      message.value = 'Não foi possível guardar a palavra-passe. Tente de novo.'
    }
  } finally {
    submitting.value = false
  }
}

async function leave() {
  try {
    await logout()
  } catch {
    // A sessão pode já ter terminado.
  }
  clearSession()
  await router.replace({ name: 'login' })
}
</script>

<template>
  <main class="narrow">
    <section class="card">
      <h1>Definir palavra-passe nova</h1>
      <p class="alert info">Defina uma palavra-passe nova para continuar.</p>
      <p v-if="message" class="alert" role="alert">{{ message }}</p>
      <form novalidate @submit.prevent="submit">
        <FormField
          id="new-password"
          v-model="form.newPassword"
          label="Palavra-passe nova (12 a 128 caracteres)"
          type="password"
          autocomplete="new-password"
          :error="errors.newPassword"
        />
        <FormField
          id="confirmation"
          v-model="form.confirmation"
          label="Confirmar a palavra-passe nova"
          type="password"
          autocomplete="new-password"
          :error="errors.confirmation"
        />
        <div class="actions">
          <button type="submit" class="btn" :disabled="submitting">
            {{ submitting ? 'A guardar...' : 'Guardar' }}
          </button>
          <button type="button" class="btn secondary" @click="leave">Sair</button>
        </div>
      </form>
    </section>
  </main>
</template>
