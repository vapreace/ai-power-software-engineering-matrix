<script setup lang="ts">
// Contagem de atrasos (US10). Com atrasos, o texto fica destacado com contorno, não só com cor.
import { computed } from 'vue'
import { overdueText } from '@/labels'

const props = defineProps<{ count: number }>()
const text = computed(() => overdueText(props.count))
</script>

<template>
  <span v-if="count > 0" class="overdue"><span class="overdue-mark" aria-hidden="true"></span>{{ text }}</span>
  <span v-else class="muted none">{{ text }}</span>
</template>

<style scoped>
.none {
  white-space: nowrap;
}
</style>
