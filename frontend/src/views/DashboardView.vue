<script setup lang="ts">
// Painel inicial (DA21). O conteúdo (projetos e contagens, US21) chega no incremento I5.
import { currentUser } from '@/session'
</script>

<template>
  <main class="page">
    <h1>Painel</h1>
    <p v-if="currentUser">Olá, {{ currentUser.name }}.</p>
    <p class="muted">Os projetos e as contagens de tarefas por estado chegam no incremento I5.</p>
  </main>
</template>
