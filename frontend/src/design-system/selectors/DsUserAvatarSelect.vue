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
        :allow-search="true"
        :multiple="multiple"
        :disabled="disabled"
        :loading="loading"
        style="width: 100%;"
        @search="handleSearch"
        @update:model-value="$emit('update:modelValue', $event)"
      >
        <a-option v-for="item in filteredOptions" :key="item.value" :value="item.value" :disabled="item.disabled">
          <div class="user-option">
            <DsUserAvatar :name="item.label" :sub-text="item.subText" :size="28" />
          </div>
        </a-option>
      </a-select>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { userApi } from '@/api'
import DsUserAvatar from '@/design-system/display/DsUserAvatar.vue'

const props = defineProps({
  modelValue: { type: [String, Number, Array, null], default: '' },
  options: { type: Array, default: () => [] },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '请选择人员' },
  required: { type: Boolean, default: false },
  allowClear: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  remote: { type: Boolean, default: true },
  multiple: { type: Boolean, default: false }
})

defineEmits(['update:modelValue'])

const keyword = ref('')
const options = ref([])

const resolvedOptions = computed(() => {
  if (!props.remote || props.options?.length) return props.options
  return options.value
})

const filteredOptions = computed(() => {
  if (!keyword.value) return resolvedOptions.value
  const value = keyword.value.toLowerCase()
  return resolvedOptions.value.filter(item => `${item.label}${item.subText || ''}`.toLowerCase().includes(value))
})

const handleSearch = (value) => {
  keyword.value = value || ''
}

onMounted(async () => {
  if (!props.remote || props.options?.length) return
  try {
    const res = await userApi.systemList({ page: 1, size: 200 })
    const records = res.code === 200 ? (res.data?.records || []) : []
    options.value = records.map(item => ({
      value: item.id,
      label: item.realName || item.username || '未命名用户',
      subText: item.username || item.deptName || '',
      disabled: Number(item.status) === 0
    }))
  } catch {
    options.value = []
  }
})
</script>

<style scoped>
.user-option { display: flex; align-items: center; min-width: 0; }
</style>
