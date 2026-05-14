<template>
  <div class="ds-field">
    <label v-if="label" class="ds-field__label">
      <span v-if="required" class="ds-field__required">*</span>
      <span>{{ label }}</span>
    </label>
    <div class="ds-control">
      <a-select
        :model-value="modelValue"
        :placeholder="placeholder"
        :allow-clear="allowClear"
        :disabled="disabled"
        style="width: 100%;"
        @update:model-value="$emit('update:modelValue', $event)"
      >
        <a-option v-if="includeAll" value="">
          <div class="status-option status-option--plain">
            <span>全部状态</span>
          </div>
        </a-option>
        <a-option v-for="item in options" :key="item.value" :value="item.value">
          <div class="status-option">
            <span class="status-option__dot" :class="`status-option__dot--${resolveTone(item)}`"></span>
            <span>{{ item.label }}</span>
          </div>
        </a-option>
      </a-select>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { loadCommonStatusOptions } from '@/utils/dict'

defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '请选择状态' },
  required: { type: Boolean, default: false },
  allowClear: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
  includeAll: { type: Boolean, default: true }
})

defineEmits(['update:modelValue'])

const options = ref([])
onMounted(async () => {
  options.value = await loadCommonStatusOptions()
})

const resolveTone = (item) => {
  if (item.color === 'green' || String(item.value) === '1') return 'success'
  if (item.color === 'red' || String(item.value) === '0') return 'danger'
  return 'neutral'
}
</script>

<style scoped>
.ds-field { width: 100%; }

.ds-field :deep(.arco-select-view) {
  height: 36px;
  border-radius: 8px;
}

.status-option { display: flex; align-items: center; gap: 8px; }
.status-option--plain { justify-content: flex-start; }
.status-option__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--ds-color-text-secondary);
  flex: 0 0 auto;
}
.status-option__dot--success { background: var(--ds-color-success); }
.status-option__dot--danger { background: var(--ds-color-danger); }
.status-option__dot--neutral { background: var(--ds-color-text-secondary); }
</style>
