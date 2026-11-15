<script setup lang="ts">
// Barra e texto do progresso (US09). O texto "60% (6 de 10 concluídas)" garante que a
// informação não depende só da barra. Sem tarefas mostra "Sem tarefas" (E1).
import { computed } from 'vue'
import type { Progress } from '@/api/types'
import { progressText } from '@/labels'

const props = defineProps<{ progress: Progress }>()
const text = computed(() => progressText(props.progress))
</script>

<template>
  <div v-if="progress.percentage !== null" class="progress">
    <div
      class="bar"
      role="progressbar"
      aria-valuemin="0"
      aria-valuemax="100"
      :aria-valuenow="progress.percentage"
      :aria-valuetext="text"
    >
      <div class="fill" :style="{ width: `${progress.percentage}%` }"></div>
    </div>
    <span class="text"
      ><span class="pct">{{ progress.percentage }}%</span> ({{ progress.completedTasks }} de
      {{ progress.totalTasks }} concluídas)</span
    >
  </div>
  <span v-else class="muted empty">Sem tarefas</span>
</template>

<style scoped>
.progress {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.bar {
  height: 4px;
  border-radius: 2px;
  background: var(--border);
  overflow: hidden;
}

.fill {
  height: 100%;
  background: var(--text);
  border-radius: 2px;
}

.text {
  font-size: 13px;
  color: var(--text-muted);
}

.pct {
  font-family: var(--font-mono);
  font-weight: 500;
  color: var(--text);
}

.empty {
  font-size: 14px;
}
</style>
