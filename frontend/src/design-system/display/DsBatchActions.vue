<template>
  <div class="ds-batch-actions" :class="{ 'ds-batch-actions--inactive': selectedCount === 0 }">
    <a-button
      v-for="action in visibleActions"
      :key="action.key"
      size="small"
      class="ds-batch-actions__button"
      :class="{ 'ds-batch-actions__button--danger': action.danger }"
      :status="action.danger ? 'danger' : undefined"
      :disabled="selectedCount === 0 || action.disabled"
      @click="$emit('action', action.key)"
    >
      {{ action.label }}
    </a-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  selectedCount: { type: Number, default: 0 },
  actions: {
    type: Array,
    default: () => []
  }
})

defineEmits(['action'])

const visibleActions = computed(() => props.actions.filter((item) => item?.visible !== false))
</script>

<style scoped>
.ds-batch-actions {
  display: inline-flex;
  align-items: center;
  gap: var(--ds-space-xs);
  transition: opacity 0.2s ease;
}

.ds-batch-actions--inactive {
  opacity: 0.56;
}

.ds-batch-actions__button {
  height: 32px;
  border-radius: var(--ds-radius-sm);
}

.ds-batch-actions__button--danger:not(:disabled) {
  background: var(--ds-color-tag-red-bg);
  border-color: rgba(245, 63, 63, 0.2);
  color: var(--ds-color-tag-red-text);
}

.ds-batch-actions__button--danger:not(:disabled):hover {
  background: #ffdcd6;
  border-color: rgba(245, 63, 63, 0.28);
}

.ds-batch-actions--inactive .ds-batch-actions__button {
  pointer-events: none;
}
</style>
