<script setup lang="ts">
// Etiqueta, campo e mensagem de erro junto do campo (docs/ui/README.md, secções 5 e 7).
// As datas escrevem-se como dd/mm/aaaa (DA23), em letra monoespaçada.
import { computed } from 'vue'

const props = defineProps<{
  id: string
  label: string
  type?: string
  error?: string
  hint?: string
  optional?: boolean
  multiline?: boolean
  rows?: number
  date?: boolean
  autocomplete?: string
  placeholder?: string
  disabled?: boolean
}>()

const model = defineModel<string>({ required: true })

const describedBy = computed(
  () =>
    [props.hint ? `${props.id}-ajuda` : null, props.error ? `${props.id}-erro` : null]
      .filter(Boolean)
      .join(' ') || undefined,
)
</script>

<template>
  <div class="field">
    <label :for="id">
      {{ label }}<span v-if="optional" class="optional"> (opcional)</span>
    </label>
    <textarea
      v-if="multiline"
      :id="id"
      v-model="model"
      class="control"
      :rows="rows ?? 4"
      :placeholder="placeholder"
      :disabled="disabled"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
    ></textarea>
    <input
      v-else
      :id="id"
      v-model="model"
      class="control"
      :class="{ mono: date }"
      :type="date ? 'text' : (type ?? 'text')"
      :inputmode="date ? 'numeric' : undefined"
      :placeholder="date ? (placeholder ?? 'dd/mm/aaaa') : placeholder"
      :autocomplete="autocomplete"
      :disabled="disabled"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
    />
    <p v-if="hint" :id="`${id}-ajuda`" class="field-hint">{{ hint }}</p>
    <p v-if="error" :id="`${id}-erro`" class="field-error" role="alert">{{ error }}</p>
  </div>
</template>
