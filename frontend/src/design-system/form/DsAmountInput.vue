<template>
  <DsNumberInput
    :model-value="modelValue"
    :label="label"
    :placeholder="placeholder"
    :hint="hint"
    :error="error"
    :required="required"
    :disabled="disabled"
    :min="min"
    :max="max"
    :precision="2"
    @update:model-value="$emit('update:modelValue', $event)"
  />
</template>

<script setup>
import DsNumberInput from './DsNumberInput.vue'

defineProps({
  modelValue: { type: Number, default: undefined },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  min: { type: Number, default: undefined },
  max: { type: Number, default: undefined }
})

defineEmits(['update:modelValue'])
</script>
