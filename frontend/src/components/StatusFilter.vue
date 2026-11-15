<script setup lang="ts" generic="T extends string">
// Caixas de seleção com legenda (US12, US11). Corresponde ao parâmetro status da API.
defineProps<{ legend: string; options: { value: T; label: string }[] }>()
const model = defineModel<T[]>({ required: true })
</script>

<template>
  <fieldset class="filter">
    <legend class="section-label">{{ legend }}</legend>
    <label v-for="option in options" :key="option.value" class="option">
      <input v-model="model" type="checkbox" :value="option.value" />
      {{ option.label }}
    </label>
  </fieldset>
</template>

<style scoped>
.filter {
  margin: 0;
  padding: 0;
  border: 0;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0 18px;
}

.filter legend {
  float: left;
  padding: 0;
  margin-right: 2px;
  line-height: 44px;
}

.option {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  cursor: pointer;
  white-space: nowrap;
}

.option input {
  width: 16px;
  height: 16px;
  margin: 0;
  accent-color: var(--text);
  cursor: pointer;
}
</style>
