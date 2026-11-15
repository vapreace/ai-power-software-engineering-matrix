<script setup lang="ts">
defineProps<{ message: string }>()
defineEmits<{ retry: [] }>()
</script>

<template>
  <div class="card state" role="alert">
    <p class="message">{{ message }}</p>
    <button type="button" class="btn secondary" @click="$emit('retry')">Tentar de novo</button>
  </div>
</template>

<style scoped>
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 40px 24px;
  text-align: center;
}

.message {
  font-weight: 600;
  font-size: 17px;
}
</style>
