<template>
  <div class="ds-card ds-advanced-filter-panel">
    <div class="ds-advanced-filter-panel__body">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({})
</script>

<style scoped>
.ds-advanced-filter-panel {
  padding: 16px 20px;
  border-radius: 12px;
}

.ds-advanced-filter-panel__body {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
}
</style>
