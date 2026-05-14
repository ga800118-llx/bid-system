<template>
  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true" :class="`file-type-icon file-type-icon--${normalizedType}`">
    <path d="M7 3.5h7l4 4V20a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 6 20V5A1.5 1.5 0 0 1 7.5 3.5z" stroke="currentColor" stroke-width="1.8" />
    <path d="M14 3.8V8h4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
    <path v-if="normalizedType === 'pdf'" d="M8.2 16h2.6c1.6 0 2.4-.8 2.4-2s-.8-2-2.4-2H8.2v4zm5.7 0h2.1m-2.1 0v-4m0 4 2.4-4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
    <path v-else-if="normalizedType === 'image'" d="M8.2 16.2l2.8-3 2.2 2.2 1.6-1.5 2 2.3M10.3 10.4h.1" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
    <path v-else-if="normalizedType === 'excel'" d="M8.5 11.3l3 5m0-5-3 5M14.5 11.8h2.8M14.5 14.7h2.8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    <path v-else-if="normalizedType === 'word'" d="M8.3 11.5l1.2 4.3 1.5-3.2 1.5 3.2 1.2-4.3M15.4 11.5h2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
    <path v-else d="M8.6 16.2h6.8M8.6 13.5h6.8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, default: 'other' }
})

const normalizedType = computed(() => {
  const value = String(props.type || '').toLowerCase()
  if (['pdf'].includes(value)) return 'pdf'
  if (['image', 'jpg', 'jpeg', 'png', 'gif', 'webp'].includes(value)) return 'image'
  if (['excel', 'xls', 'xlsx', 'csv'].includes(value)) return 'excel'
  if (['word', 'doc', 'docx'].includes(value)) return 'word'
  return 'other'
})
</script>

<style scoped>
.file-type-icon--pdf { color: var(--ds-color-danger); }
.file-type-icon--image { color: var(--ds-color-info); }
.file-type-icon--excel { color: var(--ds-color-success); }
.file-type-icon--word { color: var(--ds-color-primary); }
.file-type-icon--other { color: var(--ds-color-text-secondary); }
</style>
