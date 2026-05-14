<template>
  <span class="ds-priority-stars">
    <span v-for="index in max" :key="index" class="ds-priority-stars__item" :class="{ 'is-active': index <= value }">★</span>
  </span>
</template>

<script setup>
defineProps({
  value: { type: Number, default: 0 },
  max: { type: Number, default: 5 }
})
</script>

<style scoped>
.ds-priority-stars { display: inline-flex; gap: 2px; color: var(--ds-color-border-strong); }
.ds-priority-stars__item.is-active { color: var(--ds-color-warning); }
</style>
