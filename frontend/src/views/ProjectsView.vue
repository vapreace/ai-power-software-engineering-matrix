<script setup lang="ts">
// Ecrã Projetos (docs/ui/README.md, secções 1 a 4 e 9): pesquisa pelo nome, filtro por estado e
// lista por nome (US12, US15, US22). "+ Novo projeto" só para o Gestor e o Administrador (RN05).
// A pesquisa e o filtro ficam no endereço, para se manterem ao voltar do detalhe.
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { listProjects } from '@/api/projects'
import type { ProjectStatus, ProjectSummary } from '@/api/types'
import { currentUser } from '@/session'
import { canCreateProjects } from '@/permissions'
import { projectStatusLabels } from '@/labels'
import { useNarrowScreen } from '@/composables/useNarrowScreen'
import SearchBox from '@/components/SearchBox.vue'
import StatusFilter from '@/components/StatusFilter.vue'
import ProjectList from '@/components/ProjectList.vue'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'

const ALL_STATUSES: ProjectStatus[] = ['ACTIVE', 'COMPLETED', 'ARCHIVED']
// Por omissão, os arquivados ficam escondidos (RN25).
const DEFAULT_STATUSES: ProjectStatus[] = ['ACTIVE', 'COMPLETED']

const route = useRoute()
const router = useRouter()
const narrow = useNarrowScreen()

const statusOptions = ALL_STATUSES.map((value) => ({ value, label: projectStatusLabels[value] }))

function statusesFromQuery(): ProjectStatus[] {
  const raw = route.query.estado
  if (typeof raw !== 'string') {
    return [...DEFAULT_STATUSES]
  }
  return ALL_STATUSES.filter((status) => raw.split(',').includes(status))
}

const search = ref(typeof route.query.q === 'string' ? route.query.q : '')
const statuses = ref<ProjectStatus[]>(statusesFromQuery())
const projects = ref<ProjectSummary[]>([])
const state = ref<'loading' | 'ready' | 'error'>('loading')
const searched = ref('')
let requestId = 0
let debounce: ReturnType<typeof setTimeout> | undefined

const canCreate = computed(() => canCreateProjects(currentUser.value))
const emptyTitle = computed(() =>
  searched.value.trim() ? 'Nenhum projeto corresponde à pesquisa' : 'Não há projetos para mostrar',
)

async function load() {
  const id = ++requestId
  const q = search.value
  const selected = [...statuses.value]
  state.value = 'loading'
  // Sem nenhum estado escolhido não há projetos a mostrar (sem estado, a API usaria o filtro por omissão).
  if (selected.length === 0) {
    projects.value = []
    searched.value = q
    state.value = 'ready'
    return
  }
  try {
    const result = await listProjects(selected, q)
    if (id === requestId) {
      projects.value = result.items
      searched.value = q
      state.value = 'ready'
    }
  } catch {
    if (id === requestId) {
      state.value = 'error'
    }
  }
}

function syncQuery() {
  const query: Record<string, string> = {}
  if (search.value.trim()) {
    query.q = search.value
  }
  const sameAsDefault =
    statuses.value.length === DEFAULT_STATUSES.length &&
    DEFAULT_STATUSES.every((status) => statuses.value.includes(status))
  if (!sameAsDefault) {
    query.estado = statuses.value.join(',')
  }
  void router.replace({ query })
}

watch(statuses, () => {
  syncQuery()
  void load()
})

watch(search, () => {
  clearTimeout(debounce)
  debounce = setTimeout(() => {
    syncQuery()
    void load()
  }, 300)
})

onBeforeUnmount(() => clearTimeout(debounce))

void load()
</script>

<template>
  <main class="page">
    <div class="page-title-row">
      <h1>Projetos</h1>
      <RouterLink v-if="canCreate" :to="{ name: 'project-new' }" class="btn">+ Novo projeto</RouterLink>
    </div>

    <div role="search" class="card filters" :class="{ narrow }">
      <SearchBox id="pesquisa" v-model="search" label="Pesquisar" placeholder="Nome do projeto" :stacked="narrow" />
      <StatusFilter v-model="statuses" legend="Estado" :options="statusOptions" />
    </div>

    <LoadingState v-if="state === 'loading'" text="A carregar projetos..." />
    <ErrorState v-else-if="state === 'error'" message="Não foi possível carregar os projetos." @retry="load" />
    <EmptyState v-else-if="projects.length === 0" :title="emptyTitle" />
    <ProjectList v-else :projects="projects" caption="Projetos" />
  </main>
</template>

<style scoped>
.filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 32px;
  padding: 8px 24px;
}

.filters.narrow {
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
  padding: 16px;
}
</style>
