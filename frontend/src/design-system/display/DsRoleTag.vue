<template>
  <span class="ds-role-tag" :class="`ds-role-tag--${resolvedTone}`">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, default: '-' },
  color: { type: String, default: 'arcoblue' }
})

const resolvedTone = computed(() => {
  if (props.label.includes('系统管理员') || props.label.toLowerCase().includes('admin')) return 'warning'
  if (props.label.includes('普通用户') || props.label.toLowerCase() === 'user') return 'primary'
  if (props.color === 'red') return 'danger'
  if (props.color === 'green') return 'success'
  if (props.color === 'orange') return 'warning'
  if (props.color === 'purple') return 'purple'
  return 'primary'
})
</script>

<style scoped>
.ds-role-tag {
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

.ds-role-tag--primary {
  background: var(--ds-color-tag-blue-bg);
  color: var(--ds-color-tag-blue-text);
}

.ds-role-tag--success {
  background: var(--ds-color-tag-green-bg);
  color: var(--ds-color-tag-green-text);
}

.ds-role-tag--warning {
  background: var(--ds-color-tag-orange-bg);
  color: var(--ds-color-tag-orange-text);
}

.ds-role-tag--danger {
  background: var(--ds-color-tag-red-bg);
  color: var(--ds-color-tag-red-text);
}

.ds-role-tag--purple {
  background: var(--ds-color-tag-purple-bg);
  color: var(--ds-color-tag-purple-text);
}
</style>
