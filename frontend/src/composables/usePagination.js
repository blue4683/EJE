import { computed, ref } from 'vue'

const DEFAULT_SIZE = 20

export function usePagination(loader) {
  const page = ref(0)
  const size = ref(DEFAULT_SIZE)
  const totalPages = ref(0)
  const totalElements = ref(0)
  const items = ref([])
  const state = ref('loading')
  const error = ref(null)

  const canPrev = computed(() => page.value > 0)
  const canNext = computed(() => page.value + 1 < totalPages.value)

  async function load(next = page.value) {
    const safePage = Math.max(0, Number.isFinite(Number(next)) ? Number(next) : 0)
    const safeSize = Math.min(100, Math.max(1, Number(size.value) || DEFAULT_SIZE))
    state.value = 'loading'
    error.value = null
    try {
      const data = await loader(safePage, safeSize)
      page.value = safePage
      size.value = safeSize
      items.value = data.content
      totalPages.value = data.totalPages
      totalElements.value = data.totalElements
      state.value = data.content.length ? 'ready' : 'empty'
    } catch (caught) {
      error.value = caught
      state.value = 'error'
    }
  }

  return {
    page,
    size,
    totalPages,
    totalElements,
    items,
    state,
    error,
    canPrev,
    canNext,
    load,
  }
}
