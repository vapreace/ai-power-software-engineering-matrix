<script setup lang="ts">
// Menu de acordo com o papel, nome e papel de quem entrou, e Sair (DA21, US01, SEG07).
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { logout } from '@/api/session'
import { clearSession, currentUser } from '@/session'
import { roleLabels } from '@/labels'

const router = useRouter()
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
    .filter((route) => typeof route.name === 'string' && menuRoutes.includes(route.name))
    .filter((route) => !route.meta.roles || route.meta.roles.includes(user.role))
    .sort((a, b) => menuRoutes.indexOf(String(a.name)) - menuRoutes.indexOf(String(b.name)))
    .map((route) => ({ name: String(route.name), title: route.meta.title ?? '' }))
})

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
  <header class="app-header">
    <span class="logo">Matriz</span>
    <button
      type="button"
      class="btn secondary menu-toggle"
      :aria-expanded="menuOpen"
      aria-controls="main-menu"
      @click="menuOpen = !menuOpen"
    >
      Menu
    </button>
    <nav id="main-menu" class="main-menu" :class="{ open: menuOpen }" aria-label="Menu principal">
      <RouterLink
        v-for="item in items"
        :key="item.name"
        :to="{ name: item.name }"
        @click="menuOpen = false"
      >
        {{ item.title }}
      </RouterLink>
    </nav>
    <div v-if="currentUser" class="user">
      <span>{{ currentUser.name }} ({{ roleLabels[currentUser.role] }})</span>
      <button type="button" class="btn secondary" :disabled="leaving" @click="leave">Sair</button>
    </div>
  </header>
</template>
