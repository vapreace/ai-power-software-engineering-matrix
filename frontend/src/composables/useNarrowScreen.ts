// Telemóvel ou computador, com o ponto de mudança de DA22 (720 px).
import { onBeforeUnmount, onMounted, ref } from 'vue'

export const NARROW_QUERY = '(max-width: 719.98px)'

export function useNarrowScreen() {
  const query = window.matchMedia(NARROW_QUERY)
  const narrow = ref(query.matches)
  const update = (event: MediaQueryListEvent) => {
    narrow.value = event.matches
  }
  onMounted(() => query.addEventListener('change', update))
  onBeforeUnmount(() => query.removeEventListener('change', update))
  return narrow
}
