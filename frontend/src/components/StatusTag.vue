<script setup lang="ts">
// Estado do projeto ou da tarefa em português, com fundo suave (docs/ui/README.md, secção 5; DA23).
import { computed } from 'vue'
import type { ProjectStatus, TaskStatus } from '@/api/types'
import { projectStatusLabels, taskStatusLabels } from '@/labels'

const props = defineProps<{ status: ProjectStatus | TaskStatus }>()

const labels: Record<ProjectStatus | TaskStatus, string> = { ...projectStatusLabels, ...taskStatusLabels }
const tone: Record<ProjectStatus | TaskStatus, string> = {
  ACTIVE: 'blue',
  IN_PROGRESS: 'blue',
  COMPLETED: 'green',
  DONE: 'green',
  ARCHIVED: 'grey',
  TODO: 'grey',
}
const label = computed(() => labels[props.status])
</script>

<template>
  <span class="tag" :class="tone[status]"><span class="tag-dot" aria-hidden="true"></span>{{ label }}</span>
</template>
