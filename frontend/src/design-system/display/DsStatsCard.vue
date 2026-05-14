<template>
  <div class="ds-stats-card ds-card">
    <div class="ds-stats-card__icon" :class="`ds-stats-card__icon--${tone}`">
      <slot name="icon" />
    </div>
    <div class="ds-stats-card__content">
      <span class="ds-stats-card__label">{{ label }}</span>
      <strong class="ds-stats-card__value">{{ value }}</strong>
      <span v-if="hint" class="ds-stats-card__hint">{{ hint }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, default: '' },
  value: { type: [String, Number], default: '' },
  hint: { type: String, default: '' },
  tone: { type: String, default: 'primary' }
})
</script>

<style scoped>
.ds-stats-card {
  padding: 20px;
  min-height: 104px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  column-gap: 14px;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-border));
  box-shadow: 0 8px 24px var(--ds-color-shadow);
}

.ds-stats-card:hover {
  box-shadow: 0 14px 28px var(--ds-color-shadow);
  transform: translateY(-1px);
}

.ds-stats-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.ds-stats-card__icon :deep(svg) {
  width: 21px;
  height: 21px;
}

.ds-stats-card__icon--primary {
  background: var(--ds-color-tag-blue-bg);
  color: var(--ds-color-tag-blue-text);
}

.ds-stats-card__icon--success {
  background: var(--ds-color-tag-green-bg);
  color: var(--ds-color-tag-green-text);
}

.ds-stats-card__icon--danger {
  background: var(--ds-color-tag-red-bg);
  color: var(--ds-color-tag-red-text);
}

.ds-stats-card__icon--warning {
  background: var(--ds-color-tag-orange-bg);
  color: var(--ds-color-tag-orange-text);
}

.ds-stats-card__content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ds-stats-card__label {
  color: var(--ds-color-text-secondary);
  font-size: 13px;
  line-height: 1.4;
}

.ds-stats-card__value {
  color: var(--ds-color-text-primary);
  font-size: 26px;
  font-weight: 600;
  line-height: 1.1;
}

.ds-stats-card__hint {
  color: var(--ds-color-text-regular);
  font-size: 12px;
  line-height: 1.45;
  margin-top: 2px;
}
</style>
