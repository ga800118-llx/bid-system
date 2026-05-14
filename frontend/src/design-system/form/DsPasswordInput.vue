<template>
  <div class="ds-field">
    <label v-if="label" class="ds-field__label">
      <span v-if="required" class="ds-field__required">*</span>
      <span>{{ label }}</span>
    </label>
    <div class="ds-control">
      <a-input-password
        :model-value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        @update:model-value="$emit('update:modelValue', $event)"
      />
    </div>
    <div v-if="error" class="ds-field__error">{{ error }}</div>
    <div v-else-if="hint" class="ds-field__hint">{{ hint }}</div>
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

defineEmits(['update:modelValue'])
</script>
