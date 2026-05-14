<template>
  <div class="ds-field">
    <label v-if="label" class="ds-field__label">
      <span v-if="required" class="ds-field__required">*</span>
      <span>{{ label }}</span>
    </label>
    <div class="ds-control">
      <a-textarea
        :model-value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :auto-size="autoSize"
        :max-length="maxLength"
        @update:model-value="handleUpdate"
        @blur="handleBlur"
      />
    </div>
    <div v-if="displayError" class="ds-field__error">{{ displayError }}</div>
    <div v-else-if="hint" class="ds-field__hint">{{ hint }}</div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { normalizeDsInputValue, validateDsInputValue } from './validators'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  maxLength: { type: Number, default: undefined },
  autoSize: { type: [Boolean, Object], default: () => ({ minRows: 3, maxRows: 6 }) }
})

const emit = defineEmits(['update:modelValue', 'valid-change'])

const touched = ref(false)
const localError = ref('')
const displayError = computed(() => props.error || localError.value)

const runValidation = () => {
  const message = validateDsInputValue({
    value: props.modelValue,
    label: props.label,
    type: 'text',
    required: props.required,
    maxLength: props.maxLength
  })
  localError.value = message
  emit('valid-change', !message)
  return !message
}

const handleUpdate = (value) => {
  const nextValue = normalizeDsInputValue(value, props.maxLength)
  emit('update:modelValue', nextValue)
  if (touched.value) {
    localError.value = validateDsInputValue({
      value: nextValue,
      label: props.label,
      type: 'text',
      required: props.required,
      maxLength: props.maxLength
    })
    emit('valid-change', !localError.value)
  }
}

const handleBlur = () => {
  touched.value = true
  runValidation()
}

defineExpose({ validate: runValidation })
</script>
