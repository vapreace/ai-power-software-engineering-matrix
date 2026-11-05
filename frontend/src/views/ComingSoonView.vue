<script setup lang="ts">
// Página do menu que ainda não foi implementada (docs/implementation-plan.md).
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <main class="page">
    <h1>{{ route.meta.title }}</h1>
    <p class="muted">Esta página chega num próximo incremento.</p>
  </main>
</template>
