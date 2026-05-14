<template>
  <div class="ds-field">
    <label v-if="label" class="ds-field__label">
      <span>{{ label }}</span>
    </label>
    <div class="ds-readonly-field">
      <span class="ds-readonly-field__value">{{ displayValue }}</span>
    </div>
    <div v-if="hint" class="ds-field__hint">{{ hint }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, default: '' },
  value: { type: [String, Number], default: '' },
  hint: { type: String, default: '' }
})

const displayValue = computed(() => props.value || '-')
</script>

<style scoped>
.ds-readonly-field {
  min-height: var(--ds-control-height);
  display: flex;
  align-items: center;
  padding: 0 12px;
  border: 1px solid var(--ds-control-border);
  border-radius: var(--ds-control-radius);
  background: var(--ds-control-bg-disabled);
  box-shadow: inset 0 1px 0 color-mix(in srgb, var(--ds-color-bg-card) 72%, transparent);
}

.ds-readonly-field__value {
  min-width: 0;
  overflow: hidden;
  color: var(--ds-color-text-primary);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
