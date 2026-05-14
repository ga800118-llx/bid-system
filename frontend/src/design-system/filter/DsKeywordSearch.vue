<template>
  <div class="ds-field ds-keyword-search">
    <label v-if="label" class="ds-field__label">{{ label }}</label>
    <div class="ds-control">
      <a-input-search
        :model-value="modelValue"
        :placeholder="placeholder"
        :allow-clear="allowClear"
        :disabled="disabled"
        @update:model-value="$emit('update:modelValue', $event)"
        @search="$emit('search', $event)"
        @press-enter="$emit('search', modelValue)"
      />
    </div>
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '请输入关键词' },
  allowClear: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false }
})
defineEmits(['update:modelValue', 'search'])
</script>

<style scoped>
.ds-keyword-search {
  width: min(100%, 280px);
}

.ds-keyword-search :deep(.arco-input-wrapper),
.ds-keyword-search :deep(.arco-input-search-btn) {
  height: 36px;
  border-radius: 8px;
}
</style>
