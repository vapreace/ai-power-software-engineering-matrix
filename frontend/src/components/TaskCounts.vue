<script setup lang="ts">
// Contagens "Por fazer", "Em curso" e "Concluída" (US21, RN33). Duas variantes do design:
// cartões grandes para os totais e uma linha compacta no cartão de cada projeto.
import { computed } from 'vue'
import type { TaskCounts } from '@/api/types'

const props = defineProps<{ counts: TaskCounts; label: string; compact?: boolean }>()

const items = computed(() => [
  { title: 'Por fazer', value: props.counts.todo },
  { title: 'Em curso', value: props.counts.inProgress },
  { title: 'Concluída', value: props.counts.done },
])
</script>

<template>
  <dl :aria-label="label" :class="compact ? 'compact' : 'big'">
    <div v-for="item in items" :key="item.title" class="item">
      <dt>{{ item.title }}</dt>
      <dd>{{ item.value }}</dd>
    </div>
  </dl>
</template>

<style scoped>
dl {
  margin: 0;
}

dd {
  margin: 0;
  font-family: var(--font-mono);
  font-weight: 500;
}

.big {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 200px), 1fr));
  gap: 16px;
}

.big .item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px 24px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
}

.big dt {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.big dd {
  font-size: 44px;
  line-height: 1;
  letter-spacing: -0.02em;
}

.compact {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
}

.compact .item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
}

.compact .item + .item {
  border-left: 1px solid var(--border);
}

.compact dt {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
}

.compact dd {
  font-size: 18px;
  line-height: 1.2;
}
</style>
