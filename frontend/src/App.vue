<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppNavBar from '@/components/AppNavBar.vue'
import { currentUser } from '@/session'

const route = useRoute()
const showNavBar = computed(
  () => currentUser.value !== null && !currentUser.value.mustChangePassword && !route.meta.public,
)
</script>

<template>
  <AppNavBar v-if="showNavBar" />
  <RouterView />
</template>
