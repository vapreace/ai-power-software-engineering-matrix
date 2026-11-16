<script setup lang="ts">
// Ecrã Contas (US02, US14, US18, docs/ui/README.md secção 9). Só o Administrador: criar com
// palavra-passe temporária (DA25), editar o nome, o email e o papel, repor o acesso, desativar
// e reativar. Desativar pede confirmação, porque a pessoa sai das tarefas abertas e os projetos
// que gere ficam sem gestor (RN16, RN22). A regra que conta é a do backend (SEG02).
import { computed, onMounted, reactive, ref } from 'vue'
import { ApiError } from '@/api/http'
import { changeUserStatus, createUser, listUsers, resetUserPassword, updateUser } from '@/api/users'
import type { Role, User } from '@/api/types'
import { accountStatusLabels, roleLabels } from '@/labels'
import { useNarrowScreen } from '@/composables/useNarrowScreen'
import FormField from '@/components/FormField.vue'
import SelectField from '@/components/SelectField.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import LoadingState from '@/components/LoadingState.vue'
import ErrorState from '@/components/ErrorState.vue'

type Field = 'name' | 'email' | 'role' | 'temporaryPassword'

const MESSAGES = {
  name: 'O nome é obrigatório e tem no máximo 100 caracteres.',
  email: 'O email não é válido.',
  role: 'O papel é obrigatório.',
  password: 'A palavra-passe tem de ter entre 12 e 128 caracteres.',
}
const EMAIL = /^[^@\s]+@[^@\s.]+(\.[^@\s.]+)+$/

const narrow = useNarrowScreen()
const users = ref<User[]>([])
const state = ref<'loading' | 'ready' | 'error'>('loading')
const notice = ref<string | null>(null)

/** Painel aberto: criar, editar ou repor o acesso de uma conta. */
const panel = ref<{ mode: 'create' | 'edit' | 'reset'; user: User | null } | null>(null)
const form = reactive({ name: '', email: '', role: null as Role | null, temporaryPassword: '' })
const errors = reactive<Partial<Record<Field, string>>>({})
const formError = ref<string | null>(null)
const saving = ref(false)

const deactivating = ref<User | null>(null)
const alertMessage = ref<string | null>(null)
const changingStatus = ref(false)

const roleOptions = (Object.keys(roleLabels) as Role[]).map((role) => ({ value: role, label: roleLabels[role] }))

const roleHint = computed(() => {
  const user = panel.value?.user
  if (panel.value?.mode !== 'edit' || !user || form.role === null || form.role === user.role) {
    return undefined
  }
  const notes: string[] = []
  if (user.role === 'MANAGER') {
    notes.push('Os projetos que gere ficam sem gestor.')
  }
  if ((form.role === 'OBSERVER' || form.role === 'ADMINISTRATOR') && (user.role === 'MANAGER' || user.role === 'MEMBER')) {
    notes.push('Sai de todas as equipas e das tarefas abertas.')
  }
  return notes.join(' ') || undefined
})

async function load() {
  state.value = 'loading'
  try {
    users.value = (await listUsers()).items
    state.value = 'ready'
  } catch {
    state.value = 'error'
  }
}

function clearErrors() {
  ;(Object.keys(errors) as Field[]).forEach((key) => delete errors[key])
  formError.value = null
}

function open(mode: 'create' | 'edit' | 'reset', user: User | null = null) {
  clearErrors()
  notice.value = null
  alertMessage.value = null
  form.name = user?.name ?? ''
  form.email = user?.email ?? ''
  form.role = user?.role ?? null
  form.temporaryPassword = ''
  panel.value = { mode, user }
}

function length(value: string): number {
  return [...value.trim().normalize('NFC')].length
}

function validate(): boolean {
  clearErrors()
  const mode = panel.value?.mode
  if (mode === 'create' || mode === 'edit') {
    if (length(form.name) === 0 || length(form.name) > 100) errors.name = MESSAGES.name
    if (!EMAIL.test(form.email.trim()) || length(form.email) > 254) errors.email = MESSAGES.email
    if (form.role === null) errors.role = MESSAGES.role
  }
  if (mode === 'create' || mode === 'reset') {
    const size = [...form.temporaryPassword].length
    if (size < 12 || size > 128) errors.temporaryPassword = MESSAGES.password
  }
  return Object.keys(errors).length === 0
}

function applyApiError(error: unknown, fallback: string) {
  if (error instanceof ApiError) {
    let mapped = false
    ;(['name', 'email', 'role', 'temporaryPassword'] as Field[]).forEach((field) => {
      const message = error.fieldError(field)
      if (message) {
        errors[field] = message
        mapped = true
      }
    })
    if (error.problem.code === 'DUPLICATE_EMAIL') {
      errors.email = error.problem.detail
      mapped = true
    }
    if (!mapped) {
      formError.value = error.problem.detail
    }
  } else {
    formError.value = fallback
  }
}

async function submit() {
  if (!panel.value || saving.value || !validate()) {
    return
  }
  const { mode, user } = panel.value
  saving.value = true
  try {
    if (mode === 'create') {
      const created = await createUser({
        name: form.name.trim(),
        email: form.email.trim(),
        role: form.role,
        temporaryPassword: form.temporaryPassword,
      })
      notice.value = `Conta de ${created.name} criada. Na primeira entrada, define uma palavra-passe nova.`
    } else if (mode === 'edit' && user) {
      const body: { name?: string; email?: string; role?: Role } = {}
      if (form.name.trim() !== user.name) body.name = form.name.trim()
      if (form.email.trim().toLowerCase() !== user.email) body.email = form.email.trim()
      if (form.role !== null && form.role !== user.role) body.role = form.role
      if (Object.keys(body).length > 0) {
        await updateUser(user.id, body)
      }
      notice.value = `Conta de ${form.name.trim()} guardada.`
    } else if (mode === 'reset' && user) {
      await resetUserPassword(user.id, form.temporaryPassword)
      notice.value = `Acesso de ${user.name} reposto. Na próxima entrada, define uma palavra-passe nova.`
    }
    panel.value = null
    await load()
  } catch (error) {
    applyApiError(error, 'Não foi possível guardar. Tente de novo.')
  } finally {
    saving.value = false
  }
}

async function setStatus(user: User, status: 'ACTIVE' | 'DEACTIVATED') {
  changingStatus.value = true
  notice.value = null
  alertMessage.value = null
  try {
    await changeUserStatus(user.id, status)
    notice.value = status === 'ACTIVE' ? `Conta de ${user.name} reativada.` : `Conta de ${user.name} desativada.`
    await load()
  } catch (error) {
    alertMessage.value = error instanceof ApiError ? error.problem.detail : 'Não foi possível mudar o estado da conta.'
  } finally {
    changingStatus.value = false
    deactivating.value = null
  }
}

const deactivateMessage = computed(() =>
  deactivating.value
    ? `Desativar a conta de ${deactivating.value.name}? A pessoa deixa de conseguir entrar, deixa de ser ` +
      'responsável pelas tarefas abertas e os projetos que gere ficam sem gestor.'
    : '',
)

const panelTitle = computed(() => {
  if (!panel.value) return ''
  if (panel.value.mode === 'create') return 'Nova conta'
  if (panel.value.mode === 'edit') return `Editar conta de ${panel.value.user?.name ?? ''}`
  return `Repor o acesso de ${panel.value.user?.name ?? ''}`
})

onMounted(load)
</script>

<template>
  <main class="page">
    <div class="page-header">
      <div class="page-title-row">
        <div class="page-title">
          <h1>Contas</h1>
        </div>
        <button v-if="state === 'ready' && !panel" type="button" class="btn" @click="open('create')">+ Nova conta</button>
      </div>
    </div>

    <p v-if="notice" class="alert info" role="status">{{ notice }}</p>
    <p v-if="alertMessage" class="alert" role="alert">{{ alertMessage }}</p>

    <form v-if="panel" class="card panel" novalidate @submit.prevent="submit">
      <div class="card-header">
        <h2 class="card-title">{{ panelTitle }}</h2>
      </div>
      <div class="card-body fields">
        <template v-if="panel.mode !== 'reset'">
          <FormField id="conta-nome" v-model="form.name" label="Nome" :error="errors.name" autocomplete="off" />
          <FormField
            id="conta-email"
            v-model="form.email"
            label="Email"
            type="email"
            :error="errors.email"
            autocomplete="off"
          />
          <SelectField
            id="conta-papel"
            v-model="form.role"
            label="Papel"
            :options="roleOptions"
            placeholder="Escolha o papel"
            :error="errors.role"
            :hint="roleHint"
          />
        </template>
        <FormField
          v-if="panel.mode !== 'edit'"
          id="conta-palavra-passe"
          v-model="form.temporaryPassword"
          label="Palavra-passe temporária"
          type="password"
          autocomplete="new-password"
          :error="errors.temporaryPassword"
          hint="12 a 128 caracteres. Na entrada seguinte, a pessoa define uma palavra-passe nova."
        />
        <p v-if="formError" class="alert" role="alert">{{ formError }}</p>
      </div>
      <div class="card-footer">
        <button type="button" class="btn secondary" :disabled="saving" @click="panel = null">Cancelar</button>
        <button type="submit" class="btn" :disabled="saving">
          {{ saving ? 'A guardar...' : panel.mode === 'create' ? 'Criar conta' : panel.mode === 'edit' ? 'Guardar' : 'Repor acesso' }}
        </button>
      </div>
    </form>

    <LoadingState v-if="state === 'loading'" text="A carregar as contas..." />
    <ErrorState v-else-if="state === 'error'" message="Não foi possível carregar as contas." @retry="load" />

    <section v-else class="card" aria-labelledby="contas-titulo">
      <h2 id="contas-titulo" class="visually-hidden">Lista de contas</h2>
      <ul v-if="narrow" class="cards" aria-label="Contas">
        <li v-for="user in users" :key="user.id" class="account-card">
          <div class="top">
            <strong>{{ user.name }}</strong>
            <span class="tag" :class="user.status === 'ACTIVE' ? 'green' : 'grey'">
              {{ accountStatusLabels[user.status] }}
            </span>
          </div>
          <span class="muted">{{ user.email }}</span>
          <span>{{ roleLabels[user.role] }}{{ user.mustChangePassword ? ' · Palavra-passe por definir' : '' }}</span>
          <div class="row-actions">
            <button type="button" class="btn secondary small" @click="open('edit', user)">Editar</button>
            <button type="button" class="btn secondary small" @click="open('reset', user)">Repor acesso</button>
            <button
              v-if="user.status === 'ACTIVE'"
              type="button"
              class="btn danger-outline small"
              @click="deactivating = user"
            >
              Desativar
            </button>
            <button
              v-else
              type="button"
              class="btn secondary small"
              :disabled="changingStatus"
              @click="setStatus(user, 'ACTIVE')"
            >
              Reativar
            </button>
          </div>
        </li>
      </ul>
      <table v-else class="data-table">
        <caption class="visually-hidden">Contas</caption>
        <thead>
          <tr>
            <th scope="col">Nome</th>
            <th scope="col">Email</th>
            <th scope="col">Papel</th>
            <th scope="col">Estado</th>
            <th scope="col"><span class="visually-hidden">Ações</span></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>
              {{ user.name }}
              <span v-if="user.mustChangePassword" class="muted note">Palavra-passe por definir</span>
            </td>
            <td class="email">{{ user.email }}</td>
            <td>{{ roleLabels[user.role] }}</td>
            <td>
              <span class="tag" :class="user.status === 'ACTIVE' ? 'green' : 'grey'">
                {{ accountStatusLabels[user.status] }}
              </span>
            </td>
            <td>
              <div class="row-actions end">
                <button type="button" class="btn secondary small" :aria-label="`Editar ${user.name}`" @click="open('edit', user)">
                  Editar
                </button>
                <button
                  type="button"
                  class="btn secondary small"
                  :aria-label="`Repor o acesso de ${user.name}`"
                  @click="open('reset', user)"
                >
                  Repor acesso
                </button>
                <button
                  v-if="user.status === 'ACTIVE'"
                  type="button"
                  class="btn danger-outline small"
                  :aria-label="`Desativar ${user.name}`"
                  @click="deactivating = user"
                >
                  Desativar
                </button>
                <button
                  v-else
                  type="button"
                  class="btn secondary small"
                  :aria-label="`Reativar ${user.name}`"
                  :disabled="changingStatus"
                  @click="setStatus(user, 'ACTIVE')"
                >
                  Reativar
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <ConfirmDialog
      v-if="deactivating"
      id="desativar-conta"
      title="Desativar conta"
      :message="deactivateMessage"
      confirm-label="Desativar"
      danger
      :busy="changingStatus"
      busy-label="A desativar..."
      @confirm="setStatus(deactivating, 'DEACTIVATED')"
      @cancel="deactivating = null"
    />
  </main>
</template>

<style scoped>
.panel {
  margin-bottom: 24px;
  max-width: 720px;
}

.card-header {
  border-bottom: 1px solid var(--border);
}

.fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.note {
  display: block;
  font-size: 12px;
}

.email {
  overflow-wrap: anywhere;
}

.row-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.row-actions.end {
  justify-content: flex-end;
}

.cards {
  list-style: none;
  margin: 0;
  padding: 0;
}

.account-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px;
  font-size: 14px;
  overflow-wrap: anywhere;
}

.account-card + .account-card {
  border-top: 1px solid var(--border);
}

.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.row-actions .btn {
  flex: 1 1 auto;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.tag.green {
  background: var(--tag-green-bg);
  color: var(--tag-green-fg);
}

.tag.grey {
  background: var(--tag-grey-bg);
  color: var(--tag-grey-fg);
}
</style>
