<script setup lang="ts">
// Campo de pesquisa com etiqueta visível (US22, docs/ui/README.md, secção 7).
defineProps<{ id: string; label: string; placeholder?: string; stacked?: boolean }>()
const model = defineModel<string>({ required: true })
</script>

<template>
  <div class="search" :class="{ stacked }">
    <label :for="id" class="section-label">{{ label }}</label>
    <input :id="id" v-model="model" type="search" class="control" :placeholder="placeholder" maxlength="100" />
  </div>
</template>

<style scoped>
.search {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.search .control {
  width: 260px;
  max-width: 100%;
  min-height: 40px;
  padding: 6px 12px;
  font-size: 14px;
}

.stacked {
  flex-direction: column;
  align-items: stretch;
}

.stacked .control {
  width: 100%;
}
</style>
