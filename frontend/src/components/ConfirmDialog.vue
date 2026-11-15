<script setup lang="ts">
// Pergunta de confirmação com Confirmar e Cancelar (RN10, RN19, RN31; docs/ui/README.md, secção 7):
// recebe o foco (começa em Cancelar), o foco não sai do diálogo, Esc fecha e a página por trás
// fica inerte. Ao fechar, o foco volta ao botão que o abriu.
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps<{
  id: string
  title: string
  message: string
  confirmLabel?: string
  busyLabel?: string
  danger?: boolean
  busy?: boolean
}>()

const emit = defineEmits<{ confirm: []; cancel: [] }>()

const dialog = ref<HTMLElement | null>(null)
const cancelButton = ref<HTMLButtonElement | null>(null)
let previousFocus: HTMLElement | null = null

function appRoot(): HTMLElement | null {
  return document.getElementById('app')
}

onMounted(async () => {
  previousFocus = document.activeElement as HTMLElement | null
  appRoot()?.setAttribute('inert', '')
  await nextTick()
  cancelButton.value?.focus()
})

onBeforeUnmount(() => {
  appRoot()?.removeAttribute('inert')
  previousFocus?.focus()
})

function cancel() {
  if (!props.busy) {
    emit('cancel')
  }
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    cancel()
    return
  }
  if (event.key !== 'Tab' || !dialog.value) {
    return
  }
  const buttons = [...dialog.value.querySelectorAll<HTMLButtonElement>('button:not(:disabled)')]
  if (buttons.length === 0) {
    return
  }
  const first = buttons[0]!
  const last = buttons[buttons.length - 1]!
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="backdrop" @keydown="onKeydown">
      <div
        ref="dialog"
        class="dialog"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="`${id}-titulo`"
        :aria-describedby="`${id}-texto`"
      >
        <div class="content">
          <h2 :id="`${id}-titulo`">{{ title }}</h2>
          <p :id="`${id}-texto`">{{ message }}</p>
        </div>
        <div class="buttons">
          <button ref="cancelButton" type="button" class="btn secondary" :disabled="busy" @click="cancel">
            Cancelar
          </button>
          <button
            type="button"
            class="btn"
            :class="{ danger }"
            :disabled="busy"
            @click="emit('confirm')"
          >
            <span v-if="busy" class="spinner" aria-hidden="true"></span>
            {{ busy ? (busyLabel ?? 'A confirmar...') : (confirmLabel ?? 'Confirmar') }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(24, 24, 27, 0.45);
}

.dialog {
  width: 100%;
  max-width: 480px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  box-shadow: 0 16px 40px rgba(24, 24, 27, 0.18);
  overflow: hidden;
}

.content {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

h2 {
  font-size: 20px;
  font-weight: 600;
  line-height: 1.3;
}

p {
  color: var(--text-2);
}

.buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
  background: var(--surface-muted);
}

.buttons .btn {
  min-width: 112px;
}
</style>
