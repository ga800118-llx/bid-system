<template>
  <div ref="fieldRef" class="ds-field">
    <label v-if="label" class="ds-field__label">
      <span v-if="required" class="ds-field__required">*</span>
      <span>{{ label }}</span>
    </label>
    <div class="ds-control">
      <a-select
        :model-value="modelValue"
        :placeholder="placeholder"
        :multiple="multiple"
        :allow-search="allowSearch"
        :allow-clear="allowClear"
        :disabled="disabled"
        :filter-option="filterRoleOption"
        :max-tag-count="multiple ? effectiveMaxTagCount : 0"
        style="width: 100%;"
        @update:model-value="$emit('update:modelValue', $event)"
      >
        <template #label="{ data }">
          <a-popover
            v-if="data?.value === '__arco__more'"
            position="top"
            trigger="hover"
            popup-container="body"
          >
            <template #content>
              <div class="role-overflow-popover">
                <div class="role-overflow-popover__title">已选角色</div>
                <div class="role-overflow-popover__list">
                  <div v-for="item in selectedRoleOptions" :key="item.value" class="role-overflow-popover__item">
                    <span class="role-overflow-popover__name">{{ item.label }}</span>
                    <span class="role-overflow-popover__desc">{{ item.description || item.code || '暂无角色说明' }}</span>
                  </div>
                </div>
              </div>
            </template>
            <span class="role-selected-tag role-selected-tag--more">+{{ overflowCount }}</span>
          </a-popover>
          <span v-else-if="!multiple && (data?.value === '' || data?.value === undefined)" class="role-selected-text">全部角色</span>
          <span v-else-if="!multiple" class="role-selected-text">{{ selectedSingleLabel(data?.value) }}</span>
          <span v-else class="role-selected-tag">{{ selectedTagLabel(data?.value) }}</span>
        </template>
        <a-option v-if="!multiple" value="">
          <div class="role-option role-option--single">
            <div class="role-option__meta">
              <span>全部角色</span>
            </div>
          </div>
        </a-option>
        <a-option v-for="item in resolvedOptions" :key="item.value" :value="item.value">
          <div class="role-option" :class="{ 'role-option--selected': isSelected(item.value) }">
            <div class="role-option__meta">
              <span class="role-option__name">{{ item.label }}</span>
              <span class="role-option__desc">{{ item.description || item.code || '暂无角色说明' }}</span>
            </div>
            <span v-if="isSelected(item.value)" class="role-option__check">✓</span>
          </div>
        </a-option>
      </a-select>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { roleApi } from '@/api'

const props = defineProps({
  modelValue: { type: [Array, String, Number], default: () => [] },
  options: { type: Array, default: () => [] },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '请选择角色' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  multiple: { type: Boolean, default: true },
  allowClear: { type: Boolean, default: true },
  allowSearch: { type: Boolean, default: true },
  maxVisibleTags: { type: Number, default: 99 }
})

const emit = defineEmits(['update:modelValue'])
const remoteOptions = ref([])
const fieldRef = ref(null)
const fieldWidth = ref(0)
let resizeObserver = null

const resolvedOptions = computed(() => {
  if (props.options?.length) {
    return props.options.map(item => ({
      value: item.value,
      label: item.label,
      description: item.description || ''
    }))
  }
  return remoteOptions.value
})

const normalizedValues = computed(() => {
  if (Array.isArray(props.modelValue)) return props.modelValue.map(item => String(item))
  if (props.modelValue === '' || props.modelValue === null || props.modelValue === undefined) return []
  return [String(props.modelValue)]
})

const isSelected = (value) => normalizedValues.value.includes(String(value))

const selectedRoleOptions = computed(() => normalizedValues.value.map(value => {
  const item = resolvedOptions.value.find(option => String(option.value) === String(value))
  return item || { value, label: String(value), description: '' }
}))

const estimateTagWidth = (label) => {
  const text = String(label || '')
  let width = 28
  for (const char of text) {
    width += /[\u4e00-\u9fa5]/.test(char) ? 13 : 7
  }
  return Math.min(220, Math.max(56, width))
}

const measuredMaxTagCount = computed(() => {
  if (!props.multiple) return 0
  const total = normalizedValues.value.length
  if (total <= 1) return 0
  const width = fieldWidth.value || 0
  if (!width) return props.maxVisibleTags

  const available = Math.max(80, width - 118)
  const overflowWidth = 56
  let used = 0
  let visible = 0

  for (const item of selectedRoleOptions.value) {
    const nextWidth = estimateTagWidth(item.description ? `${item.label} · ${item.description}` : item.label) + 4
    const hasMoreAfterThis = visible + 1 < total
    const reserved = hasMoreAfterThis ? overflowWidth : 0
    if (used + nextWidth + reserved > available) break
    used += nextWidth
    visible += 1
  }

  if (visible >= total) return 0
  return Math.max(1, visible)
})

const effectiveMaxTagCount = computed(() => Math.min(props.maxVisibleTags, measuredMaxTagCount.value || props.maxVisibleTags))
const overflowStartIndex = computed(() => measuredMaxTagCount.value > 0 ? effectiveMaxTagCount.value : normalizedValues.value.length)
const overflowValues = computed(() => normalizedValues.value.slice(overflowStartIndex.value))
const overflowCount = computed(() => overflowValues.value.length)

const selectedTagLabel = (value) => {
  const item = resolvedOptions.value.find(option => String(option.value) === String(value))
  if (!item) return String(value || '')
  return item.description ? `${item.label} · ${item.description}` : item.label
}

const selectedSingleLabel = (value) => {
  const item = resolvedOptions.value.find(option => String(option.value) === String(value))
  return item?.label || String(value || '')
}

const filterRoleOption = (input, option) => {
  const value = option?.value
  const item = resolvedOptions.value.find(role => String(role.value) === String(value))
  if (!item) return true
  const keyword = String(input || '').trim().toLowerCase()
  if (!keyword) return true
  return [item.label, item.description, item.code]
    .filter(Boolean)
    .some(text => String(text).toLowerCase().includes(keyword))
}

const loadRemoteOptions = async () => {
  if (props.options?.length) return
  try {
    const res = await roleApi.list()
    const roles = res.code === 200 ? (res.data || []) : []
    remoteOptions.value = roles.filter(role => role.status !== 0).map(role => ({
      value: role.id,
      label: role.roleName,
      description: role.description || role.roleCode || '',
      code: role.roleCode || ''
    }))
  } catch {
    remoteOptions.value = []
  }
}

watch(() => props.options, loadRemoteOptions, { immediate: true })
watch([normalizedValues, resolvedOptions], () => nextTick(updateFieldWidth))

const updateFieldWidth = () => {
  fieldWidth.value = fieldRef.value?.getBoundingClientRect?.().width || 0
}

onMounted(() => {
  loadRemoteOptions()
  nextTick(updateFieldWidth)
  if (typeof ResizeObserver !== 'undefined' && fieldRef.value) {
    resizeObserver = new ResizeObserver(updateFieldWidth)
    resizeObserver.observe(fieldRef.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.ds-field { width: 100%; }

.ds-field :deep(.arco-select-view) {
  min-height: 36px;
  border-radius: 8px;
}

.ds-field :deep(.arco-select-view-multiple) {
  padding-top: 2px;
  padding-bottom: 2px;
  align-items: center;
}

.role-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  min-height: 44px;
}
.role-option--single { align-items: center; }
.role-option__meta { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.role-option__name {
  color: var(--ds-color-text-primary);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
}
.role-option__desc { color: var(--ds-color-text-secondary); font-size: 12px; line-height: 1.4; }
.role-option__check {
  flex: 0 0 auto;
  color: var(--ds-color-primary);
  font-size: 14px;
  font-weight: 700;
}
.role-option--selected .role-option__name {
  color: var(--ds-color-primary);
  font-weight: 600;
}
.ds-field :deep(.arco-select-view-tag) {
  margin: 4px 8px 4px 0;
  padding: 0 10px;
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 20%, var(--ds-color-bg-card));
  border-radius: 999px;
  background: var(--ds-color-bg-card);
  color: var(--ds-color-primary);
  font-size: 12px;
  font-weight: 500;
  line-height: 24px;
}
.ds-field :deep(.arco-select-view-tag:hover) {
  background: var(--ds-color-bg-hover);
}
.role-selected-tag {
  display: inline-flex;
  align-items: center;
  max-width: 220px;
  min-width: 0;
  overflow: hidden;
  color: var(--ds-color-primary);
  font-size: 12px;
  font-weight: 500;
  line-height: 24px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.role-selected-tag--more {
  color: var(--ds-color-primary);
  font-weight: 600;
  cursor: default;
}
.role-selected-text {
  color: var(--ds-color-text-primary);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.4;
}
.ds-field :deep(.arco-select-popup) {
  border-radius: 10px;
}
.role-overflow-popover {
  width: min(260px, calc(100vw - 32px));
  padding: 10px 12px;
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
  background: var(--ds-color-bg-card);
  box-shadow: 0 8px 20px var(--ds-color-shadow);
}
.role-overflow-popover__title {
  margin-bottom: 8px;
  color: var(--ds-color-text-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}
.role-overflow-popover__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.role-overflow-popover__item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.role-overflow-popover__name {
  color: var(--ds-color-text-primary);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}
.role-overflow-popover__desc {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.4;
}
</style>
