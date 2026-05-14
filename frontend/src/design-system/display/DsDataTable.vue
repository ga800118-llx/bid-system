<template>
  <div class="ds-data-table" :class="{ 'ds-data-table--with-pagination': showPagination }">
    <a-table
      class="ds-data-table__table"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="false"
      :row-key="rowKey"
      :selected-row-keys="selectedRowKeys"
      :row-class="rowClass"
      :scroll="scroll"
      stripe
      size="small"
      :resizable="resizable"
      @select="$emit('select', ...arguments)"
      @select-all="$emit('select-all', ...arguments)"
      @row-click="$emit('row-click', ...arguments)"
    >
      <template v-for="name in passthroughSlotNames" :key="name" #[name]="slotProps">
        <slot :name="name" v-bind="slotProps" />
      </template>
      <template #empty>
        <slot name="empty">
          <DsEmptyState title="暂无数据" description="当前条件下没有可展示的数据。" />
        </slot>
      </template>
    </a-table>
    <div v-if="showPagination" class="ds-data-table__pagination">
      <slot name="pagination">
        <DsPagination
          v-if="pagination"
          :current="pagination.current"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          :page-size-options="pagination.pageSizeOptions"
          :compact="pagination.compact"
          @update:current="$emit('pagination-change', $event)"
          @update:page-size="$emit('page-size-change', $event)"
        />
      </slot>
    </div>
  </div>
</template>

<script setup>
import { computed, useSlots } from 'vue'
import DsEmptyState from './DsEmptyState.vue'
import DsPagination from './DsPagination.vue'

const slots = useSlots()

defineProps({
  columns: { type: Array, default: () => [] },
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  rowKey: { type: String, default: 'id' },
  selectedRowKeys: { type: Array, default: () => [] },
  rowClass: { type: [Function, String], default: '' },
  scroll: { type: Object, default: null },
  resizable: { type: Boolean, default: true },
  showPagination: { type: Boolean, default: true },
  pagination: { type: Object, default: null }
})

defineEmits(['select', 'select-all', 'row-click', 'pagination-change', 'page-size-change'])

const passthroughSlotNames = computed(() =>
  Object.keys(slots).filter((name) => name !== 'empty' && name !== 'pagination')
)
</script>

<style scoped>
.ds-data-table {
  display: flex;
  flex-direction: column;
}

.ds-data-table__table :deep(.arco-table-container) {
  border: 1px solid var(--ds-color-border);
  border-radius: var(--ds-radius-sm);
  overflow: hidden;
  box-shadow: inset 0 1px 0 color-mix(in srgb, var(--ds-color-bg-card) 72%, transparent);
}

.ds-data-table--with-pagination .ds-data-table__table :deep(.arco-table-container) {
  border-radius: var(--ds-radius-sm) var(--ds-radius-sm) 0 0;
}

.ds-data-table__table :deep(.arco-table-th) {
  background: color-mix(in srgb, var(--ds-color-bg-soft) 90%, var(--ds-color-bg-card));
  color: var(--ds-color-text-regular);
  font-size: 13px;
  font-weight: 600;
}

.ds-data-table__table :deep(.arco-table-th),
.ds-data-table__table :deep(.arco-table-td) {
  padding: 12px 16px;
  border-right: none;
  white-space: nowrap;
  vertical-align: middle;
}

.ds-data-table__table :deep(.arco-table-cell),
.ds-data-table__table :deep(.arco-table-th-title),
.ds-data-table__table :deep(.arco-table-td-content) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ds-data-table__table :deep(.arco-table-tr) {
  height: 56px;
}

.ds-data-table__table :deep(.arco-table-tr:hover .arco-table-td) {
  background: var(--ds-color-bg-hover);
}

.ds-data-table__table :deep(.arco-table-tr.arco-table-tr-selected .arco-table-td) {
  background: color-mix(in srgb, var(--ds-color-primary) 6%, var(--ds-color-bg-card));
}

.ds-data-table__table :deep(.arco-table-td .arco-checkbox) {
  display: flex;
  justify-content: center;
}

.ds-data-table__pagination {
  border: 1px solid var(--ds-color-border);
  border-top: none;
  border-radius: 0 0 var(--ds-radius-sm) var(--ds-radius-sm);
  background: var(--ds-color-bg-card);
  box-shadow: inset 0 1px 0 color-mix(in srgb, var(--ds-color-bg-card) 72%, transparent);
}
</style>
