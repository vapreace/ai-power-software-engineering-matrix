<script setup lang="ts">
// Ecrã Entrada (docs/ui/README.md, secção 9; US01).
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { login } from '@/api/session'
import { setCurrentUser } from '@/session'
import FormField from '@/components/FormField.vue'

const route = useRoute()
const router = useRouter()

const form = reactive({ email: '', password: '' })
const errors = reactive<{ email?: string; password?: string }>({})
const message = ref<string | null>(
  route.query.sessao === 'terminou' ? 'A sessão terminou. Entre de novo.' : null,
)
const submitting = ref(false)
// "A sessão terminou" é informação, não erro: fica com o fundo neutro do design.
const sessionEnded = ref(route.query.sessao === 'terminou')

function validate(): boolean {
  errors.email = form.email.trim() ? undefined : 'O email é obrigatório.'
  errors.password = form.password ? undefined : 'A palavra-passe é obrigatória.'
  return !errors.email && !errors.password
}

async function submit() {
  if (submitting.value || !validate()) {
    return
  }
  submitting.value = true
  message.value = null
  sessionEnded.value = false
  try {
    const user = await login(form.email, form.password)
    setCurrentUser(user)
    await router.replace(user.mustChangePassword ? { name: 'set-password' } : { name: 'home' })
  } catch (error) {
    // Os dados escritos mantêm-se no formulário (docs/ui/README.md, secção 10).
    message.value = error instanceof ApiError ? error.problem.detail : 'Não foi possível entrar. Tente de novo.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="entry">
    <section class="card entry-card">
      <span class="entry-logo">Matriz</span>
      <h1>Entrar</h1>
      <p class="muted">Entre com o email e a palavra-passe da sua conta.</p>
      <p v-if="message" class="alert" :class="{ info: sessionEnded }" :role="sessionEnded ? 'status' : 'alert'">
        {{ message }}
      </p>
      <form class="form-stack" novalidate @submit.prevent="submit">
        <FormField
          id="email"
          v-model="form.email"
          label="Email"
          type="email"
          autocomplete="username"
          :error="errors.email"
        />
        <FormField
          id="password"
          v-model="form.password"
          label="Palavra-passe"
          type="password"
          autocomplete="current-password"
          :error="errors.password"
        />
        <button type="submit" class="btn block" :disabled="submitting">
          <span v-if="submitting" class="spinner" aria-hidden="true"></span>
          {{ submitting ? 'A entrar...' : 'Entrar' }}
        </button>
      </form>
    </section>
  </main>
</template>
