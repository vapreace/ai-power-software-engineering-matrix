<script setup lang="ts">
// Lista de escolha com etiqueta, ajuda e erro junto do campo (docs/ui/README.md, secção 7).
import { computed } from 'vue'

const props = defineProps<{
  id: string
  label: string
  options: { value: number; label: string }[]
  placeholder: string
  error?: string
  hint?: string
  disabled?: boolean
}>()

const model = defineModel<number | null>({ required: true })

const describedBy = computed(
  () =>
    [props.hint ? `${props.id}-ajuda` : null, props.error ? `${props.id}-erro` : null]
      .filter(Boolean)
      .join(' ') || undefined,
)
</script>

<template>
  <div class="field">
    <label :for="id">{{ label }}</label>
    <select
      :id="id"
      v-model="model"
      class="control"
      :disabled="disabled"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
    >
      <option :value="null" disabled>{{ placeholder }}</option>
      <option v-for="option in options" :key="option.value" :value="option.value">{{ option.label }}</option>
    </select>
    <p v-if="hint" :id="`${id}-ajuda`" class="field-hint">{{ hint }}</p>
    <p v-if="error" :id="`${id}-erro`" class="field-error" role="alert">{{ error }}</p>
  </div>
</template>
