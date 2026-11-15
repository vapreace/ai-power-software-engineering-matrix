<script setup lang="ts">
// Comentários com autor e data e hora, do mais antigo para o mais recente (US19, RN30).
// O texto é mostrado sempre como texto, nunca como HTML (SEG06).
import type { Comment } from '@/api/types'
import { formatDateTime } from '@/labels'

defineProps<{ comments: Comment[] }>()
</script>

<template>
  <ol v-if="comments.length" class="comments">
    <li v-for="comment in comments" :key="comment.id">
      <div class="meta">
        <span class="author">{{ comment.author.name }}</span>
        <time class="mono when" :datetime="comment.createdAt">{{ formatDateTime(comment.createdAt) }}</time>
      </div>
      <p class="content">{{ comment.content }}</p>
    </li>
  </ol>
  <p v-else class="muted empty">Ainda não há comentários.</p>
</template>

<style scoped>
.comments {
  list-style: none;
  margin: 0;
  padding: 0;
}

.comments li {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 0;
  border-bottom: 1px solid var(--border);
}

.meta {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.author {
  font-weight: 600;
}

.when {
  font-size: 12px;
  color: var(--text-muted);
}

.content {
  white-space: pre-line;
  overflow-wrap: anywhere;
}

.empty {
  padding: 12px 0;
}
</style>
