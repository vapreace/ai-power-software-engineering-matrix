<script setup lang="ts">
// Estado traduzido para português, com fundo suave (docs/ui/README.md, secção 5; DA23).
import { computed } from 'vue'
import type { ProjectStatus } from '@/api/types'
import { projectStatusLabels } from '@/labels'

const props = defineProps<{ status: ProjectStatus }>()

const tone: Record<ProjectStatus, string> = { ACTIVE: 'blue', COMPLETED: 'green', ARCHIVED: 'grey' }
const label = computed(() => projectStatusLabels[props.status])
</script>

<template>
  <span class="tag" :class="tone[status]"><span class="tag-dot" aria-hidden="true"></span>{{ label }}</span>
</template>
