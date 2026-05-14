<template>
  <div class="ds-field">
    <label v-if="label" class="ds-field__label">
      <span v-if="required" class="ds-field__required">*</span>
      <span>{{ label }}</span>
    </label>
    <a-checkbox-group :model-value="modelValue" :disabled="disabled" @update:model-value="$emit('update:modelValue', $event)">
      <a-space wrap>
        <a-checkbox v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</a-checkbox>
      </a-space>
    </a-checkbox-group>
    <div v-if="error" class="ds-field__error">{{ error }}</div>
    <div v-else-if="hint" class="ds-field__hint">{{ hint }}</div>
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: Array, default: () => [] },
  label: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  options: { type: Array, default: () => [] }
})
defineEmits(['update:modelValue'])
</script>
