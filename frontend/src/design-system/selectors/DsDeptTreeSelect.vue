<template>
  <div class="ds-field">
    <label v-if="label" class="ds-field__label">
      <span v-if="required" class="ds-field__required">*</span>
      <span>{{ label }}</span>
    </label>
    <div class="ds-control">
      <a-tree-select
        :model-value="innerValue"
        :data="options"
        :placeholder="placeholder"
        :allow-clear="allowClear"
        :allow-search="true"
        :tree-props="treeProps"
        :disabled="disabled"
        :field-names="{ key: 'value', title: 'label', children: 'children' }"
        style="width: 100%;"
        @update:model-value="handleChange"
      />
    </div>
    <div v-if="hint" class="ds-field__hint">{{ hint }}</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { deptApi } from '@/api'

const props = defineProps({
  modelValue: { type: [String, Number, null], default: '' },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '请选择部门' },
  hint: { type: String, default: '' },
  required: { type: Boolean, default: false },
  allowClear: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
  includeAll: { type: Boolean, default: false },
  includeUnassigned: { type: Boolean, default: false },
  allValue: { type: [String, Number, null], default: '' },
  unassignedValue: { type: [String, Number, null], default: 'unassigned' }
})

const emit = defineEmits(['update:modelValue'])

const treeData = ref([])
const treeProps = {
  defaultExpandAll: false
}

const mapNodes = (nodes) => (nodes || []).map(node => ({
  value: String(node.id),
  rawValue: node.id,
  label: node.name,
  children: mapNodes(node.children)
}))

const flatOptions = (nodes = []) => nodes.flatMap(node => [node, ...flatOptions(node.children || [])])

const options = computed(() => {
  const list = mapNodes(treeData.value)
  const special = []
  if (props.includeAll) special.push({ value: props.allValue, label: '全部部门' })
  if (props.includeUnassigned) special.push({ value: props.unassignedValue, label: '未分配' })
  return [...special, ...list]
})

const innerValue = computed(() => {
  if (props.modelValue === null || props.modelValue === undefined || props.modelValue === '') return props.modelValue
  return String(props.modelValue)
})

const handleChange = (value) => {
  if (value === null || value === undefined || value === '') {
    emit('update:modelValue', value)
    return
  }
  const matched = flatOptions(options.value).find(item => String(item.value) === String(value))
  emit('update:modelValue', matched?.rawValue ?? value)
}

onMounted(async () => {
  try {
    const res = await deptApi.tree()
    treeData.value = res.code === 200 ? (res.data || []) : []
  } catch {
    treeData.value = []
  }
})
</script>

<style scoped>
.ds-field { width: 100%; }

.ds-field :deep(.arco-select-view),
.ds-field :deep(.arco-tree-select-view) {
  height: 36px;
  border-radius: 8px;
}

.ds-field :deep(.arco-tree-select-popup) {
  border-radius: 10px;
}

.ds-field :deep(.arco-tree-node-title) {
  display: inline-flex;
  align-items: center;
  min-width: 0;
}

.ds-field :deep(.arco-tree-node-selected .arco-tree-node-title) {
  color: var(--ds-color-primary);
  font-weight: 500;
}
</style>
