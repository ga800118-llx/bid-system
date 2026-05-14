<template>
  <a-tooltip v-if="tooltip" :content="tooltip">
    <a-button class="ds-icon-button" :disabled="disabled" @click="$emit('click', $event)">
      <template #icon>
        <slot />
      </template>
    </a-button>
  </a-tooltip>
  <a-button v-else class="ds-icon-button" :disabled="disabled" @click="$emit('click', $event)">
    <template #icon>
      <slot />
    </template>
  </a-button>
</template>

<script setup>
defineProps({
  tooltip: { type: String, default: '' },
  disabled: { type: Boolean, default: false }
})

defineEmits(['click'])
</script>

<style scoped>
.ds-icon-button {
  width: 32px;
  min-width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-sm);
  border: 1px solid var(--ds-color-border);
  background: var(--ds-color-bg-card);
  color: var(--ds-color-text-regular);
  box-shadow: none;
}

.ds-icon-button:hover {
  border-color: var(--ds-color-primary);
  background: var(--ds-color-bg-hover);
  color: var(--ds-color-primary);
}

.ds-icon-button:disabled {
  border-color: var(--ds-color-border);
  background: var(--ds-color-bg-soft);
  color: var(--ds-color-text-placeholder);
}
</style>
