<script setup lang="ts">
// Prioridade em português, com fundo suave (docs/ui/README.md, secção 5; DA23).
import type { TaskPriority } from '@/api/types'
import { priorityLabels } from '@/labels'

defineProps<{ priority: TaskPriority }>()
const tone: Record<TaskPriority, string> = { HIGH: 'red', MEDIUM: 'amber', LOW: 'grey' }
</script>

<template>
  <span class="priority" :class="tone[priority]">{{ priorityLabels[priority] }}</span>
</template>

<style scoped>
.priority {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  border-radius: 4px;
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.red {
  background: var(--tag-red-bg);
  color: var(--tag-red-fg);
}

.amber {
  background: var(--tag-amber-bg);
  color: var(--tag-amber-fg);
}

.grey {
  background: var(--tag-grey-bg);
  color: var(--tag-grey-fg);
}
</style>
