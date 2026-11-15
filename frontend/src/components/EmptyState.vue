<script setup lang="ts">
defineProps<{ title: string; detail?: string }>()
</script>

<template>
  <div class="card state" role="status">
    <p class="title">{{ title }}</p>
    <p v-if="detail" class="muted">{{ detail }}</p>
    <slot />
  </div>
</template>

<style scoped>
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 40px 24px;
  text-align: center;
}

.title {
  font-weight: 600;
  font-size: 18px;
}
</style>
