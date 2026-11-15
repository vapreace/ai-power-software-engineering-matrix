<script setup lang="ts">
// Menu de acordo com o papel, nome e papel de quem entrou, e Sair (DA21, US01, SEG07).
// No telemóvel (abaixo de 720 px, DA22) o menu recolhe atrás do botão Menu.
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { logout } from '@/api/session'
import { clearSession, currentUser } from '@/session'
import { roleLabels } from '@/labels'
import { useNarrowScreen } from '@/composables/useNarrowScreen'

const router = useRouter()
const route = useRoute()
const narrow = useNarrowScreen()
const menuOpen = ref(false)
const leaving = ref(false)

const menuRoutes = ['home', 'projects', 'my-tasks', 'teams', 'accounts']

const items = computed(() => {
  const user = currentUser.value
  if (!user) {
    return []
  }
  return router
    .getRoutes()
    .filter((r) => typeof r.name === 'string' && menuRoutes.includes(r.name))
    .filter((r) => !r.meta.roles || r.meta.roles.includes(user.role))
    .sort((a, b) => menuRoutes.indexOf(String(a.name)) - menuRoutes.indexOf(String(b.name)))
    .map((r) => ({ name: String(r.name), title: r.meta.title ?? '' }))
})

// A secção do menu fica ativa também nas páginas dentro dela (por exemplo, o detalhe de um projeto).
const activeSection = computed(() => route.meta.section ?? String(route.name))
const who = computed(() =>
  currentUser.value ? `${currentUser.value.name} (${roleLabels[currentUser.value.role]})` : '',
)

watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  },
)

async function leave() {
  if (leaving.value) {
    return
  }
  leaving.value = true
  try {
    await logout()
  } catch {
    // A sessão pode já ter terminado; em qualquer caso, volta à entrada.
  } finally {
    clearSession()
    leaving.value = false
    await router.push({ name: 'login' })
  }
}
</script>

<template>
  <header class="app-header" :class="{ narrow }">
    <div class="bar">
      <RouterLink :to="{ name: 'home' }" class="logo">Matriz</RouterLink>
      <nav v-if="!narrow" class="menu" aria-label="Menu principal">
        <RouterLink
          v-for="item in items"
          :key="item.name"
          :to="{ name: item.name }"
          :aria-current="activeSection === item.name ? 'page' : undefined"
          :class="{ active: activeSection === item.name }"
        >
          {{ item.title }}
        </RouterLink>
      </nav>
      <div class="user">
        <span class="who">{{ who }}</span>
        <button v-if="!narrow" type="button" class="btn secondary header-btn" :disabled="leaving" @click="leave">
          Sair
        </button>
        <button
          v-else
          type="button"
          class="btn secondary header-btn"
          :aria-expanded="menuOpen ? 'true' : 'false'"
          aria-controls="menu-principal"
          @click="menuOpen = !menuOpen"
        >
          Menu
        </button>
      </div>
    </div>
    <nav v-if="narrow && menuOpen" id="menu-principal" class="mobile-menu" aria-label="Menu principal">
      <RouterLink
        v-for="item in items"
        :key="item.name"
        :to="{ name: item.name }"
        :aria-current="activeSection === item.name ? 'page' : undefined"
        :class="{ active: activeSection === item.name }"
      >
        <span v-if="activeSection === item.name" class="mark" aria-hidden="true"></span>
        {{ item.title }}
      </RouterLink>
      <button type="button" class="btn secondary block" :disabled="leaving" @click="leave">Sair</button>
    </nav>
  </header>
</template>

<style scoped>
.app-header {
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}

.bar {
  display: flex;
  align-items: center;
  gap: 40px;
  height: 64px;
  max-width: 1264px;
  margin: 0 auto;
  padding: 0 32px;
}

.logo {
  font-weight: 600;
  font-size: 17px;
  letter-spacing: 0.01em;
  line-height: 1.25;
  padding: 2px 9px;
  border: 1.5px solid var(--text);
  color: var(--text);
  text-decoration: none;
}

.logo:hover {
  color: var(--text);
}

.menu {
  display: flex;
  gap: 28px;
  align-self: stretch;
}

.menu a {
  display: flex;
  align-items: center;
  color: var(--text-muted);
  text-decoration: none;
  white-space: nowrap;
}

.menu a:hover {
  color: var(--text);
}

.menu a.active {
  color: var(--text);
  font-weight: 600;
  box-shadow: inset 0 -2px 0 var(--accent);
}

.user {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 20px;
}

.who {
  font-size: 14px;
  white-space: nowrap;
}

.header-btn {
  min-height: 36px;
  padding: 0 14px;
  font-size: 14px;
}

.narrow .bar {
  height: 60px;
  gap: 12px;
  padding: 0 16px;
}

.narrow .user {
  gap: 12px;
}

.narrow .header-btn {
  min-height: 44px;
  font-size: 15px;
}

.mobile-menu {
  display: flex;
  flex-direction: column;
  padding: 4px 16px 16px;
}

.mobile-menu a {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding-left: 18px;
  border-bottom: 1px solid var(--border);
  color: var(--text-2);
  text-decoration: none;
}

.mobile-menu a.active {
  padding-left: 0;
  color: var(--text);
  font-weight: 600;
}

.mark {
  width: 8px;
  height: 8px;
  background: var(--accent);
}

.mobile-menu .btn {
  margin-top: 16px;
}
</style>
