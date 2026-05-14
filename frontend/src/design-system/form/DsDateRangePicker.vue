<template>
  <div class="ds-field">
    <label v-if="label" class="ds-field__label">
      <span v-if="required" class="ds-field__required">*</span>
      <span>{{ label }}</span>
    </label>
    <div class="ds-control">
      <a-range-picker
        :model-value="modelValue"
        :disabled="disabled"
        :shortcuts="shortcuts"
        style="width: 100%;"
        @update:model-value="$emit('update:modelValue', $event)"
      />
    </div>
    <div v-if="error" class="ds-field__error">{{ error }}</div>
    <div v-else-if="hint" class="ds-field__hint">{{ hint }}</div>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  label: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

defineEmits(['update:modelValue'])

const dayRange = (startOffset, endOffset = startOffset) => {
  const now = new Date()
  const start = new Date(now)
  const end = new Date(now)
  start.setDate(now.getDate() + startOffset)
  end.setDate(now.getDate() + endOffset)
  return [start, end]
}

const monthRange = (offset) => {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth() + offset, 1)
  const end = new Date(now.getFullYear(), now.getMonth() + offset + 1, 0)
  return [start, end]
}

const shortcuts = [
  { label: '今天', value: () => dayRange(0) },
  { label: '昨天', value: () => dayRange(-1) },
  { label: '近7天', value: () => dayRange(-6, 0) },
  { label: '近30天', value: () => dayRange(-29, 0) },
  { label: '本月', value: () => monthRange(0) },
  { label: '上月', value: () => monthRange(-1) },
  {
    label: '今年',
    value: () => {
      const now = new Date()
      return [new Date(now.getFullYear(), 0, 1), new Date(now.getFullYear(), 11, 31)]
    }
  }
]
</script>
