<template>
  <span class="ds-tag" :class="{ 'ds-tag--closable': closable }">
    <span class="ds-tag__text">{{ label }}</span>
    <button v-if="closable" class="ds-tag__close" type="button" aria-label="移除" @click.stop="$emit('close')">×</button>
  </span>
</template>

<script setup>
defineProps({
  label: { type: String, default: '' },
  closable: { type: Boolean, default: false }
})

defineEmits(['close'])
</script>

<style scoped>
.ds-tag {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 24px;
  padding: 0 9px;
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 42%, var(--ds-color-bg-card));
  border-radius: 999px;
  background: var(--ds-color-bg-card);
  color: var(--ds-color-primary);
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  transition: background 0.16s ease, border-color 0.16s ease;
}

.ds-tag:hover {
  background: var(--ds-color-bg-selected);
  border-color: color-mix(in srgb, var(--ds-color-primary) 58%, var(--ds-color-bg-card));
}

.ds-tag--closable {
  gap: 6px;
  padding-right: 6px;
}

.ds-tag__text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ds-tag__close {
  width: 14px;
  height: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--ds-color-primary);
  cursor: pointer;
  font-size: 13px;
  line-height: 1;
}

.ds-tag__close:hover {
  background: color-mix(in srgb, var(--ds-color-primary) 12%, var(--ds-color-bg-card));
}
</style>
