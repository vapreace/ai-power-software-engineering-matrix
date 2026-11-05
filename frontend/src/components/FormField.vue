<script setup lang="ts">
// Etiqueta, campo e mensagem de erro junto do campo (docs/ui/README.md, secções 5 e 7).
defineProps<{
  id: string
  label: string
  type?: string
  error?: string
  autocomplete?: string
}>()

const model = defineModel<string>({ required: true })
</script>

<template>
  <div class="field">
    <label :for="id">{{ label }}</label>
    <input
      :id="id"
      v-model="model"
      :type="type ?? 'text'"
      :autocomplete="autocomplete"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="error ? `${id}-error` : undefined"
    />
    <p v-if="error" :id="`${id}-error`" class="field-error" role="alert">{{ error }}</p>
  </div>
</template>
