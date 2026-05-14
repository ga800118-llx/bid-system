<template>
  <aside class="department-tree-panel">
    <header class="department-tree-panel__header">
      <div>
        <h2 class="department-tree-panel__title">组织架构</h2>
        <p class="department-tree-panel__desc">按部门层级查看用户归属</p>
      </div>
      <DsIconButton tooltip="刷新组织架构" @click="$emit('refresh')">
        <IconRefresh />
      </DsIconButton>
    </header>

    <div class="department-tree-panel__search">
      <DsKeywordSearch
        :model-value="keyword"
        label=""
        placeholder="搜索部门名称"
        @update:model-value="$emit('update:keyword', $event)"
        @search="$emit('search')"
      />
      <DsIconButton tooltip="筛选能力预留">
        <IconFilter />
      </DsIconButton>
    </div>

    <div class="department-tree-panel__body">
      <a-tree
        :data="nodes"
        :field-names="{ key: 'id', title: 'name' }"
        :selected-keys="selectedKeys"
        :expanded-keys="expandedKeys"
        :default-expand-all="true"
        blockNode
        @select="$emit('select', $event)"
      >
        <template #title="node">
          <a-dropdown trigger="contextMenu" @select="key => $emit('node-action', key, node)">
            <div class="department-tree-node" :class="{ 'department-tree-node--virtual': node.virtual }">
              <span class="department-tree-node__icon" :class="{ 'department-tree-node__icon--virtual': node.virtual }">
                <DepartmentIcon />
              </span>
              <span class="department-tree-node__content">
                <span class="department-tree-node__line">
                  <span class="department-tree-node__name" :class="{ 'department-tree-node__name--disabled': Number(node.status ?? 1) === 0 }">
                    {{ node.name || node.title }}
                  </span>
                  <span v-if="resolveNodeCount(node) !== null" class="department-tree-node__count">
                    {{ resolveNodeCount(node) }}
                  </span>
                </span>
                <span class="department-tree-node__meta">
                  <span v-if="node.virtual">暂未归属组织</span>
                  <span v-else-if="node.managerName">负责人：{{ node.managerName }}</span>
                  <span v-else>未设置负责人</span>
                  <span v-if="!node.virtual && childCountOf(node) > 0">子部门 {{ childCountOf(node) }}</span>
                </span>
              </span>
              <span class="department-tree-node__tags">
                <DsStatusTag v-if="Number(node.status ?? 1) === 0" :value="node.status" :options="statusOptions" />
                <DsStatusTag v-if="node.virtual" label="未分配" tone="warning" />
              </span>
            </div>
            <template #content>
              <a-doption :disabled="node.virtual || !canCreateUser" value="addUser">新增用户</a-doption>
              <a-doption :disabled="node.virtual || !canEditDept" value="editDept">编辑部门</a-doption>
              <a-doption :disabled="node.virtual || !canDeleteDept" value="deleteDept" class="department-tree-panel__danger">删除部门</a-doption>
            </template>
          </a-dropdown>
        </template>

        <template #extra="node">
          <div v-if="!node.virtual && (canEditDept || canDeleteDept)" class="department-tree-node-actions" @click.stop>
            <button
              v-if="canEditDept"
              class="department-tree-node-actions__button"
              type="button"
              title="编辑部门"
              @click="$emit('edit', node)"
            >
              <IconEdit />
            </button>
            <a-dropdown v-if="canDeleteDept" @select="key => $emit('node-action', key, node)">
              <button class="department-tree-node-actions__button" type="button" title="更多操作">
                <span>...</span>
              </button>
              <template #content>
                <a-doption :disabled="!canCreateUser" value="addUser">新增用户</a-doption>
                <a-doption value="deleteDept" class="department-tree-panel__danger">删除部门</a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
      </a-tree>
    </div>

    <footer class="department-tree-panel__footer">
      <a-button v-if="canCreateDept" type="outline" long class="department-tree-panel__add" @click="$emit('add-root')">
        <template #icon><IconPlus /></template>
        新增根部门
      </a-button>
    </footer>
  </aside>
</template>

<script setup>
import DsIconButton from '@/design-system/display/DsIconButton.vue'
import DsKeywordSearch from '@/design-system/filter/DsKeywordSearch.vue'
import DsStatusTag from '@/design-system/display/DsStatusTag.vue'
import DepartmentIcon from '@/design-system/icons/DepartmentIcon.vue'
import { IconEdit, IconFilter, IconPlus, IconRefresh } from '@arco-design/web-vue/es/icon'

defineProps({
  nodes: { type: Array, default: () => [] },
  selectedKeys: { type: Array, default: () => [] },
  expandedKeys: { type: Array, default: undefined },
  keyword: { type: String, default: '' },
  statusOptions: { type: Array, default: () => [] },
  canCreateDept: { type: Boolean, default: false },
  canCreateUser: { type: Boolean, default: false },
  canEditDept: { type: Boolean, default: false },
  canDeleteDept: { type: Boolean, default: false },
  unassignedCount: { type: [Number, String], default: null }
})

defineEmits([
  'update:keyword',
  'search',
  'refresh',
  'select',
  'node-action',
  'edit',
  'add-root'
])

const childCountOf = (node) => node?.children?.length || 0

const firstNumber = (...values) => {
  for (const value of values) {
    if (value === null || value === undefined || value === '') continue
    const number = Number(value)
    if (!Number.isNaN(number)) return number
  }
  return null
}

const resolveNodeCount = (node) => {
  if (node?.virtual) return firstNumber(node.userCount, node.count, node.total, node.memberCount)
  return firstNumber(node?.userCount, node?.userTotal, node?.memberCount, node?.count)
}
</script>

<style scoped>
.department-tree-panel {
  width: 360px;
  min-width: 360px;
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-border));
  border-radius: 12px;
  background: var(--ds-color-bg-card);
  box-shadow: 0 10px 28px var(--ds-color-shadow);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.department-tree-panel__header {
  min-height: 68px;
  padding: 18px 18px 14px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--ds-color-border);
}

.department-tree-panel__title {
  margin: 0;
  color: var(--ds-color-text-primary);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
}

.department-tree-panel__desc {
  margin: 5px 0 0;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.department-tree-panel__search {
  padding: 14px 16px 8px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 32px;
  gap: 10px;
  align-items: end;
}

.department-tree-panel__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 8px 10px 14px;
}

.department-tree-panel__body :deep(.arco-tree-node) {
  margin: 4px 0;
}

.department-tree-panel__body :deep(.arco-tree-node-title) {
  min-height: 52px;
  border-radius: 10px;
  position: relative;
  transition: background-color 0.16s ease, box-shadow 0.16s ease, color 0.16s ease;
}

.department-tree-panel__body :deep(.arco-tree-node-title:hover) {
  background: var(--ds-color-bg-hover);
}

.department-tree-panel__body :deep(.arco-tree-node-selected .arco-tree-node-title) {
  background: color-mix(in srgb, var(--ds-color-primary) 8%, var(--ds-color-bg-card));
}

.department-tree-panel__body :deep(.arco-tree-node-selected .arco-tree-node-title::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  bottom: 9px;
  width: 3px;
  border-radius: 999px;
  background: var(--ds-color-primary);
}

.department-tree-panel__body :deep(.arco-tree-node-selected .department-tree-node__name) {
  color: var(--ds-color-text-primary);
  font-weight: 600;
}

.department-tree-panel__body :deep(.arco-tree-node-selected .department-tree-node__meta) {
  color: var(--ds-color-text-secondary);
}

.department-tree-panel__body :deep(.arco-tree-node-switcher) {
  color: var(--ds-color-text-secondary);
}

.department-tree-node {
  width: 100%;
  min-width: 0;
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 6px 8px 6px 0;
}

.department-tree-node__icon {
  width: 24px;
  height: 24px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-color-bg-selected);
  color: var(--ds-color-primary);
}

.department-tree-node__icon :deep(svg) {
  width: 15px;
  height: 15px;
}

.department-tree-node__icon--virtual {
  background: var(--ds-color-tag-orange-bg);
  color: var(--ds-color-tag-orange-text);
}

.department-tree-node__content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.department-tree-node__line {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.department-tree-node__name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ds-color-text-primary);
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
}

.department-tree-node__name--disabled {
  color: var(--ds-color-text-placeholder);
  text-decoration: line-through;
}

.department-tree-node__count {
  min-width: 24px;
  height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-color-bg-soft);
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1;
  flex: 0 0 auto;
}

.department-tree-node__meta {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
}

.department-tree-node__tags {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex: 0 0 auto;
}

.department-tree-node-actions {
  height: 100%;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding-right: 6px;
  opacity: 0;
  transition: opacity 0.16s ease;
}

.department-tree-panel__body :deep(.arco-tree-node:hover) .department-tree-node-actions,
.department-tree-panel__body :deep(.arco-tree-node-selected) .department-tree-node-actions {
  opacity: 1;
}

.department-tree-node-actions__button {
  width: 26px;
  height: 26px;
  border: 0;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: var(--ds-color-text-secondary);
  cursor: pointer;
}

.department-tree-node-actions__button:hover {
  background: var(--ds-color-bg-hover);
  color: var(--ds-color-primary);
}

.department-tree-panel__footer {
  padding: 12px 16px 16px;
  border-top: 1px solid var(--ds-color-border);
}

.department-tree-panel__add {
  height: 36px;
  border-radius: 8px;
}

:deep(.department-tree-panel__danger) {
  color: var(--ds-color-danger);
}

@media (max-width: 1280px) {
  .department-tree-panel {
    width: 100%;
    min-width: 0;
    min-height: 380px;
  }
}
</style>
