<template>
  <span class="ds-status-tag" :class="`ds-status-tag--${resolvedTone}`">
    {{ resolvedLabel }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  tone: { type: String, default: '' },
  options: { type: Array, default: () => [] }
})

const matched = computed(() => props.options.find(item => String(item.value) === String(props.value)))
const resolvedLabel = computed(() => props.label || matched.value?.label || props.value || '-')
const resolvedTone = computed(() => {
  if (props.tone) return props.tone
  const color = matched.value?.color
  if (String(resolvedLabel.value).includes('未分配')) return 'warning'
  if (color === 'green') return 'success'
  if (color === 'red') return 'danger'
  if (color === 'orange') return 'warning'
  if (color === 'arcoblue' || color === 'blue') return 'primary'
  if (String(props.value) === '1' || String(props.value).toLowerCase() === 'enabled') return 'success'
  if (String(props.value) === '0' || String(props.value).toLowerCase() === 'disabled') return 'danger'
  return 'neutral'
})
</script>

<style scoped>
.ds-status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  line-height: 20px;
  white-space: nowrap;
  vertical-align: middle;
}

.ds-status-tag--primary {
  background: var(--ds-color-tag-blue-bg);
  color: var(--ds-color-tag-blue-text);
}

.ds-status-tag--success {
  background: var(--ds-color-tag-green-bg);
  color: var(--ds-color-tag-green-text);
}

.ds-status-tag--warning {
  background: color-mix(in srgb, var(--ds-color-tag-orange-bg) 72%, var(--ds-color-bg-card));
  color: color-mix(in srgb, var(--ds-color-tag-orange-text) 84%, var(--ds-color-text-regular));
}

.ds-status-tag--danger {
  background: var(--ds-color-tag-red-bg);
  color: var(--ds-color-tag-red-text);
}

.ds-status-tag--neutral {
  background: var(--ds-color-bg-soft);
  color: var(--ds-color-text-regular);
}
</style>
