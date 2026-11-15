<script setup lang="ts">
// Painel inicial (DA21). O conteúdo (projetos e contagens, US21) chega no incremento I5.
import { computed } from 'vue'
import { currentUser } from '@/session'
import { roleLabels } from '@/labels'

const subtitle = computed(() => {
  const user = currentUser.value
  if (!user) {
    return ''
  }
  return [user.name, roleLabels[user.role], ...user.teams.map((team) => team.name)].join(' · ')
})
</script>

<template>
  <main class="page">
    <div class="page-header">
      <h1>Painel</h1>
      <p class="subtitle">{{ subtitle }}</p>
    </div>
    <p class="card card-body muted">Os projetos e as contagens de tarefas por estado chegam no incremento I5.</p>
  </main>
</template>
