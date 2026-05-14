<template>
  <div class="ds-field">
    <label v-if="label" class="ds-field__label">
      <span>{{ label }}</span>
    </label>
    <div class="ds-switch">
      <a-switch :model-value="modelValue" :disabled="disabled" @update:model-value="$emit('update:modelValue', $event)" />
    </div>
    <div v-if="hint" class="ds-field__hint">{{ hint }}</div>
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  label: { type: String, default: '' },
  hint: { type: String, default: '' },
  disabled: { type: Boolean, default: false }
})
defineEmits(['update:modelValue'])
</script>

<style scoped>
.ds-switch {
  min-height: var(--ds-control-height);
  display: flex;
  align-items: center;
}

.ds-switch :deep(.arco-switch) {
  background: color-mix(in srgb, var(--ds-color-border) 64%, var(--ds-color-bg-card));
}

.ds-switch :deep(.arco-switch.arco-switch-checked) {
  background: var(--ds-color-primary);
}

.ds-switch :deep(.arco-switch.arco-switch-disabled) {
  opacity: 0.6;
}
</style>
