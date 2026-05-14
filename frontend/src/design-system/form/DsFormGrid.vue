<template>
  <div class="ds-form-grid" :class="{ 'ds-form-grid--single': columns === 1 }">
    <slot />
  </div>
</template>

<script setup>
defineProps({
  columns: { type: Number, default: 2 }
})
</script>

<style scoped>
.ds-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 20px;
}

.ds-form-grid--single {
  grid-template-columns: 1fr;
}

@media (max-width: 720px) {
  .ds-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
