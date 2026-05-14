<template>
  <div class="ds-empty-state">
    <EmptyStateIcon class="ds-empty-state__icon" />
    <div class="ds-empty-state__title">{{ title }}</div>
    <div v-if="description" class="ds-empty-state__description">{{ description }}</div>
  </div>
</template>

<script setup>
import EmptyStateIcon from '@/design-system/icons/EmptyStateIcon.vue'

defineProps({
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.ds-empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 32px 16px; text-align: center; }
.ds-empty-state__icon { width: 88px; height: 88px; color: var(--ds-color-text-placeholder); }
.ds-empty-state__title { margin-top: 12px; color: var(--ds-color-text-primary); font-size: 14px; font-weight: 600; }
.ds-empty-state__description { margin-top: 8px; color: var(--ds-color-text-secondary); font-size: 12px; line-height: 1.6; }
</style>
