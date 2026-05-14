<template>
  <div class="ds-action-bar">
    <div class="ds-action-bar__selection" :class="{ 'ds-action-bar__selection--active': selectionActive }">
      {{ selectionText }}
    </div>
    <div class="ds-action-bar__main">
      <slot name="batch" />
    </div>
    <div class="ds-action-bar__divider" />
    <div class="ds-action-bar__tools">
      <slot name="tools" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  selectionText: { type: String, default: '已选 0 项' },
  selectionActive: { type: Boolean, default: false }
})
</script>

<style scoped>
.ds-action-bar {
  display: flex;
  align-items: center;
  gap: var(--ds-space-sm);
  min-height: 40px;
  flex-wrap: wrap;
  justify-content: space-between;
  width: 100%;
}

.ds-action-bar__selection {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--ds-color-bg-soft);
  color: var(--ds-color-text-regular);
  font-size: 12px;
  line-height: 1;
}

.ds-action-bar__selection--active {
  background: var(--ds-color-bg-selected);
  color: var(--ds-color-primary);
}

.ds-action-bar__main,
.ds-action-bar__tools {
  display: inline-flex;
  align-items: center;
  gap: var(--ds-space-xs);
}

.ds-action-bar__main {
  flex: 1 1 auto;
}

.ds-action-bar__divider {
  width: 1px;
  height: 18px;
  background: var(--ds-color-border);
}

@media (max-width: 960px) {
  .ds-action-bar {
    width: 100%;
    justify-content: flex-start;
  }

  .ds-action-bar__divider {
    display: none;
  }
}
</style>
