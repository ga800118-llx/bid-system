<template>
  <div class="ds-field">
    <label v-if="label" class="ds-field__label">
      <span v-if="required" class="ds-field__required">*</span>
      <span>{{ label }}</span>
    </label>
    <div class="ds-control">
      <a-date-picker
        :model-value="modelValue"
        :disabled="disabled"
        :placeholder="placeholder"
        :show-time="showTime"
        :format="format"
        :value-format="valueFormat"
        style="width: 100%;"
        @update:model-value="$emit('update:modelValue', $event)"
      />
    </div>
    <div v-if="error" class="ds-field__error">{{ error }}</div>
    <div v-else-if="hint" class="ds-field__hint">{{ hint }}</div>
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: [String, Array], default: '' },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '请选择日期' },
  showTime: { type: Boolean, default: false },
  format: { type: String, default: undefined },
  valueFormat: { type: String, default: undefined },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})
defineEmits(['update:modelValue'])
</script>
