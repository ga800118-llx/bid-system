<template>
  <div class="ds-field">
    <label v-if="label" class="ds-field__label">
      <span v-if="required" class="ds-field__required">*</span>
      <span>{{ label }}</span>
    </label>
    <div class="ds-control">
      <a-input
        :model-value="modelValue"
        :placeholder="placeholder"
        :allow-clear="allowClear"
        :disabled="disabled"
        :max-length="maxLength"
        :input-attrs="inputAttrs"
        @update:model-value="handleUpdate"
        @blur="handleBlur"
        @press-enter="$emit('press-enter', $event)"
      />
    </div>
    <div v-if="displayError" class="ds-field__error">{{ displayError }}</div>
    <div v-else-if="hint" class="ds-field__hint">{{ hint }}</div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { DS_INPUT_TYPES, normalizeDsInputValue, validateDsInputValue } from './validators'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  type: { type: String, default: 'text', validator: value => DS_INPUT_TYPES.includes(value) },
  placeholder: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  allowClear: { type: Boolean, default: true },
  maxLength: { type: Number, default: undefined }
})

const emit = defineEmits(['update:modelValue', 'press-enter', 'valid-change'])

const touched = ref(false)
const localError = ref('')

const displayError = computed(() => props.error || localError.value)

const inputAttrs = computed(() => {
  if (props.type === 'phone') return { inputmode: 'numeric' }
  if (props.type === 'idCard') return { inputmode: 'text' }
  return {}
})

const runValidation = () => {
  const message = validateDsInputValue({
    value: props.modelValue,
    label: props.label,
    type: props.type,
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
      type: props.type,
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

watch(() => [props.type, props.required, props.maxLength], () => {
  if (touched.value) runValidation()
})

defineExpose({ validate: runValidation })
</script>
