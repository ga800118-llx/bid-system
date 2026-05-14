<template>
  <DsDictSelect
    :model-value="modelValue"
    type-code="file_link_category"
    :label="label"
    :placeholder="placeholder"
    :required="required"
    :allow-clear="allowClear"
    :disabled="disabled"
    @update:model-value="$emit('update:modelValue', $event)"
  />
</template>

<script setup>
import DsDictSelect from './DsDictSelect.vue'

defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '请选择文件分类' },
  required: { type: Boolean, default: false },
  allowClear: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false }
})
defineEmits(['update:modelValue'])
</script>
