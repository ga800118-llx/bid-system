<template>
  <div class="ds-security-summary-card ds-card">
    <div class="ds-security-summary-card__header">
      <div class="ds-security-summary-card__title-wrap">
        <span class="ds-security-summary-card__icon">
          <slot name="icon" />
        </span>
        <span class="ds-security-summary-card__label">{{ label }}</span>
      </div>
      <span v-if="hint" class="ds-security-summary-card__hint">{{ hint }}</span>
    </div>
    <div class="ds-security-summary-card__list">
      <div v-for="item in items" :key="item.label" class="ds-security-summary-card__item">
        <span class="ds-security-summary-card__item-label">{{ item.label }}</span>
        <strong
          class="ds-security-summary-card__item-value"
          :class="{ 'ds-security-summary-card__item-value--muted': isZeroValue(item.value) }"
        >
          {{ item.value }}
        </strong>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, default: '安全提醒' },
  hint: { type: String, default: '' },
  items: {
    type: Array,
    default: () => []
  }
})

const isZeroValue = (value) => {
  if (value === null || value === undefined) return true
  const normalized = String(value).replace(/[^\d.-]/g, '')
  if (!normalized) return false
  return Number(normalized) === 0
}
</script>

<style scoped>
.ds-security-summary-card {
  padding: 20px;
  min-height: 104px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  transition: box-shadow 0.2s ease;
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-border));
  box-shadow: 0 8px 24px var(--ds-color-shadow);
}

.ds-security-summary-card:hover {
  box-shadow: 0 14px 28px var(--ds-color-shadow);
}

.ds-security-summary-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.ds-security-summary-card__title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.ds-security-summary-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-color-tag-purple-bg);
  color: var(--ds-color-tag-purple-text);
  flex: 0 0 auto;
}

.ds-security-summary-card__icon :deep(svg) {
  width: 21px;
  height: 21px;
}

.ds-security-summary-card__label {
  color: var(--ds-color-text-secondary);
  font-size: 13px;
  line-height: 1.4;
}

.ds-security-summary-card__hint {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
  text-align: right;
}

.ds-security-summary-card__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ds-security-summary-card__item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  column-gap: 12px;
}

.ds-security-summary-card__item-label {
  color: var(--ds-color-text-regular);
  font-size: 12px;
  line-height: 1.5;
}

.ds-security-summary-card__item-value {
  color: var(--ds-color-danger);
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.ds-security-summary-card__item-value--muted {
  color: var(--ds-color-text-secondary);
  font-weight: 600;
}
</style>
