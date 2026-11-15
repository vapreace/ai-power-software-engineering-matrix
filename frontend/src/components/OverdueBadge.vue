<script setup lang="ts">
// Contagem de atrasos de um projeto, ou "Atrasada" numa tarefa (US10). Com atrasos, o texto
// fica destacado com contorno, não só com cor.
import { computed } from 'vue'
import { overdueText } from '@/labels'

const props = defineProps<{ count?: number; task?: boolean }>()
const text = computed(() => (props.task ? 'Atrasada' : overdueText(props.count ?? 0)))
const show = computed(() => props.task || (props.count ?? 0) > 0)
</script>

<template>
  <span v-if="show" class="overdue"><span class="overdue-mark" aria-hidden="true"></span>{{ text }}</span>
  <span v-else class="muted none">{{ text }}</span>
</template>

<style scoped>
.none {
  white-space: nowrap;
}
</style>
