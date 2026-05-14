<template>
  <div class="ds-pagination">
    <div class="ds-pagination__total">共 {{ total }} 条</div>
    <div class="ds-pagination__controls">
      <a-select
        class="ds-pagination__size"
        :model-value="pageSize"
        :options="sizeOptions"
        :disabled="disabled"
        @update:model-value="$emit('update:pageSize', Number($event))"
      />
      <a-pagination
        class="ds-pagination__pages"
        :current="current"
        :page-size="pageSize"
        :total="total"
        :disabled="disabled"
        @change="$emit('update:current', $event)"
      />
      <div v-if="!compact" class="ds-pagination__jumper">
        <span>前往</span>
        <a-input-number
          class="ds-pagination__jumper-input"
          :model-value="jumpPage"
          :min="1"
          :max="maxPage"
          :disabled="disabled"
          hide-button
          @update:model-value="jumpPage = $event"
          @press-enter="handleJump"
        />
        <span>页</span>
        <a-button class="ds-pagination__jump-button" :disabled="disabled" @click="handleJump">跳转</a-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  current: { type: Number, default: 1 },
  pageSize: { type: Number, default: 20 },
  total: { type: Number, default: 0 },
  pageSizeOptions: { type: Array, default: () => [10, 20, 50, 100] },
  compact: { type: Boolean, default: false }
})

const emit = defineEmits(['update:current', 'update:pageSize'])
const jumpPage = ref(props.current)

watch(() => props.current, (value) => {
  jumpPage.value = value
})

const maxPage = computed(() => Math.max(1, Math.ceil((props.total || 0) / (props.pageSize || 1))))
const disabled = computed(() => props.total === 0)
const sizeOptions = computed(() =>
  props.pageSizeOptions.map((item) => ({
    value: Number(item),
    label: `${item} 条/页`
  }))
)

const handleJump = () => {
  const next = Number(jumpPage.value || 1)
  const safePage = Math.min(Math.max(next, 1), maxPage.value)
  emit('update:current', safePage)
}
</script>

<style scoped>
.ds-pagination {
  min-height: 56px;
  padding: 0 16px;
  border-top: 1px solid var(--ds-color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.ds-pagination__total {
  color: var(--ds-color-text-regular);
  font-size: 13px;
  line-height: 1.5;
  white-space: nowrap;
}

.ds-pagination__controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: nowrap;
  white-space: nowrap;
  min-width: 0;
}

.ds-pagination__size {
  width: 96px;
  flex: 0 0 auto;
}

.ds-pagination__size :deep(.arco-select-view),
.ds-pagination__jumper-input :deep(.arco-input-number),
.ds-pagination__jump-button {
  height: 32px;
  border-radius: 6px;
}

.ds-pagination__size :deep(.arco-select-view) {
  min-width: 96px;
}

.ds-pagination__pages {
  flex: 0 0 auto;
}

.ds-pagination__pages :deep(.arco-pagination) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ds-pagination__pages :deep(.arco-pagination-item),
.ds-pagination__pages :deep(.arco-pagination-item-step),
.ds-pagination__pages :deep(.arco-pagination-item-previous),
.ds-pagination__pages :deep(.arco-pagination-item-next) {
  min-width: 32px;
  height: 32px;
  border-radius: 6px;
}

.ds-pagination__pages :deep(.arco-pagination-item),
.ds-pagination__pages :deep(.arco-pagination-item-previous),
.ds-pagination__pages :deep(.arco-pagination-item-next) {
  border: 1px solid var(--ds-color-border);
  background: var(--ds-color-bg-card);
  color: var(--ds-color-text-regular);
}

.ds-pagination__pages :deep(.arco-pagination-item-active) {
  background: var(--ds-color-primary);
  border-color: var(--ds-color-primary);
  color: var(--ds-color-bg-card);
}

.ds-pagination__pages :deep(.arco-pagination-item:hover),
.ds-pagination__pages :deep(.arco-pagination-item-previous:hover),
.ds-pagination__pages :deep(.arco-pagination-item-next:hover) {
  background: var(--ds-color-bg-hover);
  color: var(--ds-color-primary);
}

.ds-pagination__pages :deep(.arco-pagination-item-disabled),
.ds-pagination__pages :deep(.arco-pagination-item-previous.arco-pagination-item-disabled),
.ds-pagination__pages :deep(.arco-pagination-item-next.arco-pagination-item-disabled) {
  background: var(--ds-color-bg-soft);
  color: var(--ds-color-text-placeholder);
  border-color: var(--ds-color-border);
}

.ds-pagination__jumper {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--ds-color-text-regular);
  font-size: 13px;
  line-height: 1.5;
  flex: 0 0 auto;
}

.ds-pagination__jumper-input {
  width: 44px;
}

.ds-pagination__jump-button {
  min-width: 56px;
  height: 32px;
  border-radius: 6px;
}

@media (max-width: 720px) {
  .ds-pagination {
    flex-direction: column;
    align-items: flex-start;
    padding: 12px 16px;
  }

  .ds-pagination__controls {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}

@media (max-width: 720px) {
  .ds-pagination__jumper {
    display: none;
  }
}
</style>
