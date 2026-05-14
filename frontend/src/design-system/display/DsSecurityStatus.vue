<template>
  <DsSecurityPopover
    :status-text="popoverStatusText"
    :failed-count="failedCountNumber"
    :last-failed-text="lastFailedText"
    :last-login-text="lastLoginText"
    :abnormal="resolved.tone === 'danger'"
    :suggestion="resolved.tone === 'danger' ? '建议：重置密码或解锁账号' : ''"
  >
    <DsStatusTag :label="resolved.label" :tone="resolved.tone" />
  </DsSecurityPopover>
</template>

<script setup>
import { computed } from 'vue'
import DsSecurityPopover from './DsSecurityPopover.vue'
import DsStatusTag from './DsStatusTag.vue'

const props = defineProps({
  failedCount: { type: [Number, String], default: 0 },
  locked: { type: Boolean, default: false },
  label: { type: String, default: '' },
  lastFailedAt: { type: String, default: '' },
  lastLoginAt: { type: String, default: '' }
})

const failedCountNumber = computed(() => Number(props.failedCount || 0))

const resolved = computed(() => {
  if (props.label) {
    if (props.label.includes('锁定') || props.label.includes('失败')) {
      return { label: props.label, tone: 'danger' }
    }
    return { label: props.label, tone: 'neutral' }
  }
  if (props.locked) return { label: '已锁定', tone: 'danger' }
  const failed = failedCountNumber.value
  if (failed > 0) return { label: `失败 ${failed} 次`, tone: 'danger' }
  return { label: '无异常', tone: 'neutral' }
})

const popoverStatusText = computed(() => {
  if (props.locked || resolved.value.label.includes('锁定')) return '已锁定'
  if (failedCountNumber.value > 0) return `失败 ${failedCountNumber.value} 次`
  return '正常'
})

const lastFailedText = computed(() => props.lastFailedAt || '无失败记录')
const lastLoginText = computed(() => props.lastLoginAt || '-')
</script>
