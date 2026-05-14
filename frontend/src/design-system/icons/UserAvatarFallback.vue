<template>
  <span class="avatar-fallback" :class="toneClass" :style="{ width: `${size}px`, height: `${size}px`, fontSize: `${fontSize}px` }">
    {{ displayText }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  text: { type: String, default: '' },
  size: { type: Number, default: 32 }
})

const displayText = computed(() => {
  const value = String(props.text || '').trim()
  return value ? value.charAt(0).toUpperCase() : 'U'
})

const toneClass = computed(() => {
  const value = String(props.text || '')
  let sum = 0
  for (const char of value) sum += char.charCodeAt(0)
  return `avatar-fallback--${sum % 5}`
})

const fontSize = computed(() => Math.max(12, Math.floor(props.size * 0.42)))
</script>

<style scoped>
.avatar-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-weight: 700;
  flex: 0 0 auto;
}
.avatar-fallback--0 { background: var(--ds-color-tag-blue-bg); color: var(--ds-color-tag-blue-text); }
.avatar-fallback--1 { background: var(--ds-color-tag-green-bg); color: var(--ds-color-tag-green-text); }
.avatar-fallback--2 { background: var(--ds-color-tag-orange-bg); color: var(--ds-color-tag-orange-text); }
.avatar-fallback--3 { background: var(--ds-color-tag-purple-bg); color: var(--ds-color-tag-purple-text); }
.avatar-fallback--4 { background: var(--ds-color-tag-red-bg); color: var(--ds-color-tag-red-text); }
</style>
