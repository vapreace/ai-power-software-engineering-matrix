<script setup lang="ts">
defineProps<{ text: string }>()
</script>

<template>
  <div class="card state" role="status" aria-live="polite" aria-busy="true">
    <p>{{ text }}</p>
    <div class="lines" aria-hidden="true">
      <div></div>
      <div></div>
      <div></div>
    </div>
  </div>
</template>

<style scoped>
.state {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
  color: var(--text-2);
}

.lines {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.lines div {
  height: 12px;
  border-radius: 6px;
  background: var(--skeleton);
}

.lines div:nth-child(2) {
  width: 80%;
}

.lines div:nth-child(3) {
  width: 60%;
}
</style>
