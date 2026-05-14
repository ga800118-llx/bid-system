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
        :allow-search="allowSearch"
        :disabled="disabled"
        style="width: 100%;"
        @update:model-value="$emit('update:modelValue', $event)"
      >
        <a-option v-for="item in resolvedOptions" :key="item.value" :value="item.value">
          <div class="dict-option">
            <span>{{ item.label }}</span>
            <span v-if="item.color" class="dict-option__preview" :class="`dict-option__preview--${item.color}`" />
          </div>
        </a-option>
      </a-select>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { dictApi } from '@/api'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  typeCode: { type: String, default: '' },
  options: { type: Array, default: () => [] },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '请选择' },
  required: { type: Boolean, default: false },
  allowClear: { type: Boolean, default: true },
  allowSearch: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false }
})

defineEmits(['update:modelValue'])

const options = ref([])
const resolvedOptions = computed(() => props.options?.length ? props.options : options.value)

const loadOptions = async () => {
  if (!props.typeCode || props.options?.length) {
    options.value = []
    return
  }
  try {
    const res = await dictApi.publicItems(props.typeCode)
    options.value = res.code === 200 ? (res.data || []).map(item => ({
      value: item.itemValue,
      label: item.itemLabel,
      color: item.tagColor || ''
    })) : []
  } catch {
    options.value = []
  }
}

watch(() => [props.typeCode, props.options], loadOptions, { immediate: true, deep: true })
onMounted(loadOptions)
</script>

<style scoped>
.dict-option { display: flex; align-items: center; justify-content: space-between; gap: 8px; min-width: 0; }
.dict-option__preview {
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
  border-radius: 999px;
  border: 1px solid var(--ds-color-border);
  background: var(--ds-color-bg-soft);
}
.dict-option__preview--arcoblue,
.dict-option__preview--blue { background: var(--ds-color-tag-blue-bg); border-color: color-mix(in srgb, var(--ds-color-primary) 28%, var(--ds-color-border)); }
.dict-option__preview--green { background: var(--ds-color-tag-green-bg); border-color: color-mix(in srgb, var(--ds-color-success) 28%, var(--ds-color-border)); }
.dict-option__preview--red,
.dict-option__preview--orangered { background: var(--ds-color-tag-red-bg); border-color: color-mix(in srgb, var(--ds-color-danger) 24%, var(--ds-color-border)); }
.dict-option__preview--gold,
.dict-option__preview--orange { background: var(--ds-color-tag-orange-bg); border-color: color-mix(in srgb, var(--ds-color-warning) 28%, var(--ds-color-border)); }
.dict-option__preview--purple { background: color-mix(in srgb, var(--ds-color-primary) 12%, var(--ds-color-bg-card)); border-color: color-mix(in srgb, var(--ds-color-primary) 24%, var(--ds-color-border)); }
.dict-option__preview--gray { background: var(--ds-color-bg-soft); border-color: var(--ds-color-border); }
</style>
