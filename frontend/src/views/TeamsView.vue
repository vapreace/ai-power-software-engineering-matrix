<script setup lang="ts">
// Ecrã Equipas (US03, docs/ui/README.md secção 9): cada equipa com quem lhe pertence.
// Administrador e Observador veem todas; Gestor e Membro, as suas (requirements.md secção 3.1).
// Só o Administrador cria, muda o nome, adiciona e remove pessoas. Remover pede confirmação,
// porque a pessoa sai das tarefas abertas da equipa e os projetos que gere ficam sem gestor
// (RN17, RN22). A regra que conta é a do backend (SEG02).
import { computed, onMounted, reactive, ref } from 'vue'
import { ApiError } from '@/api/http'
import { addTeamMember, createTeam, listTeamMembers, listTeams, removeTeamMember, renameTeam } from '@/api/teams'
import { listUsers } from '@/api/users'
import type { Reference, TeamMember, User } from '@/api/types'
import { currentUser } from '@/session'
import { isAdministrator } from '@/permissions'
import { roleLabels } from '@/labels'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import ErrorState from '@/components/ErrorState.vue'

interface TeamCard {
  team: Reference
  members: TeamMember[]
}

const NAME_MESSAGE = 'O nome é obrigatório e tem no máximo 100 caracteres.'

const admin = computed(() => isAdministrator(currentUser.value))
const cards = ref<TeamCard[]>([])
const eligiblePeople = ref<User[]>([])
const state = ref<'loading' | 'ready' | 'error'>('loading')

const newName = ref('')
const newNameError = ref<string | null>(null)
const creating = ref(false)

const renaming = ref<number | null>(null)
const renameValue = ref('')
const renameError = ref<string | null>(null)

const toAdd = reactive<Record<number, number | undefined>>({})
const cardMessage = reactive<Record<number, string | null>>({})
const busyTeam = ref<number | null>(null)

const removal = ref<{ team: Reference; member: TeamMember } | null>(null)
const removing = ref(false)

async function load() {
  state.value = 'loading'
  try {
    const teams = (await listTeams()).items
    const members = await Promise.all(teams.map((team) => listTeamMembers(team.id)))
    cards.value = teams.map((team, index) => ({ team, members: members[index]?.items ?? [] }))
    if (admin.value) {
      eligiblePeople.value = (await listUsers({ statuses: ['ACTIVE'], roles: ['MANAGER', 'MEMBER'] })).items
    }
    state.value = 'ready'
  } catch {
    state.value = 'error'
  }
}

function eligibleFor(card: TeamCard): User[] {
  return eligiblePeople.value.filter((person) => !card.members.some((member) => member.id === person.id))
}

function nameLength(value: string): number {
  return [...value.trim().normalize('NFC')].length
}

async function submitCreate() {
  newNameError.value = null
  const length = nameLength(newName.value)
  if (length === 0 || length > 100) {
    newNameError.value = NAME_MESSAGE
    return
  }
  creating.value = true
  try {
    await createTeam(newName.value.trim())
    newName.value = ''
    await load()
  } catch (error) {
    newNameError.value = error instanceof ApiError ? error.problem.detail : 'Não foi possível criar a equipa.'
  } finally {
    creating.value = false
  }
}

function startRename(card: TeamCard) {
  renaming.value = card.team.id
  renameValue.value = card.team.name
  renameError.value = null
}

async function submitRename(card: TeamCard) {
  renameError.value = null
  const length = nameLength(renameValue.value)
  if (length === 0 || length > 100) {
    renameError.value = NAME_MESSAGE
    return
  }
  busyTeam.value = card.team.id
  try {
    card.team = await renameTeam(card.team.id, renameValue.value.trim())
    renaming.value = null
  } catch (error) {
    renameError.value = error instanceof ApiError ? error.problem.detail : 'Não foi possível mudar o nome.'
  } finally {
    busyTeam.value = null
  }
}

async function refreshMembers(card: TeamCard) {
  card.members = (await listTeamMembers(card.team.id)).items
}

async function submitAdd(card: TeamCard) {
  const userId = toAdd[card.team.id]
  if (userId == null) {
    return
  }
  busyTeam.value = card.team.id
  cardMessage[card.team.id] = null
  try {
    await addTeamMember(card.team.id, userId)
    toAdd[card.team.id] = undefined
    await refreshMembers(card)
  } catch (error) {
    cardMessage[card.team.id] =
      error instanceof ApiError ? error.problem.detail : 'Não foi possível adicionar a pessoa.'
  } finally {
    busyTeam.value = null
  }
}

async function confirmRemoval() {
  if (!removal.value) {
    return
  }
  const { team, member } = removal.value
  const card = cards.value.find((c) => c.team.id === team.id)
  removing.value = true
  try {
    await removeTeamMember(team.id, member.id)
    if (card) {
      await refreshMembers(card)
    }
  } catch (error) {
    cardMessage[team.id] = error instanceof ApiError ? error.problem.detail : 'Não foi possível remover a pessoa.'
  } finally {
    removing.value = false
    removal.value = null
  }
}

const removalMessage = computed(() =>
  removal.value
    ? `Remover ${removal.value.member.name} da equipa ${removal.value.team.name}? Deixa de ser responsável ` +
      'pelas tarefas abertas dos projetos desta equipa e os projetos que gere ficam sem gestor.'
    : '',
)

onMounted(load)
</script>

<template>
  <main class="page">
    <div class="page-header">
      <div class="page-title-row">
        <div class="page-title">
          <h1>Equipas</h1>
        </div>
      </div>
      <p class="muted">Quem pertence a cada equipa vê os projetos dessa equipa.</p>
    </div>

    <form v-if="admin && state === 'ready'" class="card card-body create" @submit.prevent="submitCreate">
      <div class="field grow">
        <label for="nova-equipa">Nova equipa</label>
        <input
          id="nova-equipa"
          v-model="newName"
          class="control"
          :aria-invalid="newNameError ? 'true' : undefined"
          :aria-describedby="newNameError ? 'nova-equipa-erro' : undefined"
        />
        <p v-if="newNameError" id="nova-equipa-erro" class="field-error">{{ newNameError }}</p>
      </div>
      <button type="submit" class="btn" :disabled="creating">{{ creating ? 'A criar...' : 'Criar equipa' }}</button>
    </form>

    <LoadingState v-if="state === 'loading'" text="A carregar as equipas..." />
    <ErrorState v-else-if="state === 'error'" message="Não foi possível carregar as equipas." @retry="load" />
    <EmptyState v-else-if="cards.length === 0" title="Não pertence a nenhuma equipa" />

    <div v-else class="teams">
      <section v-for="card in cards" :key="card.team.id" class="card" :aria-labelledby="`equipa-${card.team.id}`">
        <div class="card-header">
          <h2 v-if="renaming !== card.team.id" :id="`equipa-${card.team.id}`" class="team-name">
            {{ card.team.name }}
          </h2>
          <form v-else class="rename" @submit.prevent="submitRename(card)">
            <label :for="`nome-${card.team.id}`" class="visually-hidden">Nome da equipa</label>
            <input
              :id="`nome-${card.team.id}`"
              v-model="renameValue"
              class="control"
              :aria-invalid="renameError ? 'true' : undefined"
            />
            <button type="submit" class="btn small" :disabled="busyTeam === card.team.id">Guardar</button>
            <button type="button" class="btn secondary small" @click="renaming = null">Cancelar</button>
          </form>
          <button
            v-if="admin && renaming !== card.team.id"
            type="button"
            class="btn secondary small"
            @click="startRename(card)"
          >
            Mudar nome
          </button>
        </div>
        <p v-if="renaming === card.team.id && renameError" class="field-error rename-error" role="alert">
          {{ renameError }}
        </p>

        <ul v-if="card.members.length" class="members" :aria-label="`Pessoas da equipa ${card.team.name}`">
          <li v-for="member in card.members" :key="member.id">
            <div class="who">
              <span>{{ member.name }}</span>
              <span class="muted small-text">
                {{ roleLabels[member.role] }}{{ member.status === 'DEACTIVATED' ? ' · Conta desativada' : '' }}
              </span>
            </div>
            <button
              v-if="admin"
              type="button"
              class="link-button"
              :aria-label="`Remover ${member.name} da equipa ${card.team.name}`"
              @click="removal = { team: card.team, member }"
            >
              Remover
            </button>
          </li>
        </ul>
        <p v-else class="card-body muted">Ainda ninguém pertence a esta equipa.</p>

        <form v-if="admin" class="card-body add" @submit.prevent="submitAdd(card)">
          <label :for="`adicionar-${card.team.id}`" class="block-label">Adicionar pessoa</label>
          <div class="row">
            <select
              :id="`adicionar-${card.team.id}`"
              v-model="toAdd[card.team.id]"
              class="control"
              :disabled="eligibleFor(card).length === 0"
            >
              <option :value="undefined" disabled>
                {{ eligibleFor(card).length ? 'Escolha uma pessoa' : 'Não há mais pessoas elegíveis' }}
              </option>
              <option v-for="person in eligibleFor(card)" :key="person.id" :value="person.id">
                {{ person.name }} ({{ roleLabels[person.role] }})
              </option>
            </select>
            <button
              type="submit"
              class="btn secondary small"
              :disabled="busyTeam === card.team.id || toAdd[card.team.id] == null"
            >
              Adicionar
            </button>
          </div>
          <p v-if="cardMessage[card.team.id]" class="alert" role="alert">{{ cardMessage[card.team.id] }}</p>
        </form>
      </section>
    </div>

    <ConfirmDialog
      v-if="removal"
      id="remover-pessoa"
      title="Remover pessoa da equipa"
      :message="removalMessage"
      confirm-label="Remover"
      danger
      :busy="removing"
      busy-label="A remover..."
      @confirm="confirmRemoval"
      @cancel="removal = null"
    />
  </main>
</template>

<style scoped>
.create {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.grow {
  flex: 1 1 240px;
  margin: 0;
}

.teams {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  align-items: start;
}

@media (max-width: 719px) {
  .teams {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
  }
}

.card-header {
  border-bottom: 1px solid var(--border);
  flex-wrap: wrap;
}

.team-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.rename {
  display: flex;
  gap: 8px;
  flex: 1 1 auto;
  flex-wrap: wrap;
}

.rename .control {
  flex: 1 1 160px;
  min-width: 0;
}

.rename-error {
  padding: 8px 24px 0;
}

.members {
  list-style: none;
  margin: 0;
  padding: 0 24px;
}

.members li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 52px;
}

.members li + li {
  border-top: 1px solid var(--border);
}

.who {
  display: flex;
  flex-direction: column;
}

.small-text {
  font-size: 13px;
}

.add {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-top: 1px solid var(--border);
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

@media (max-width: 719px) {
  .members {
    padding: 0 16px;
  }

  .rename-error {
    padding: 8px 16px 0;
  }
}
</style>
