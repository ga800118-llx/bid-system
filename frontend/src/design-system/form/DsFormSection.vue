<template>
  <section class="ds-form-section">
    <div class="ds-form-section__header">
      <h4 class="ds-form-section__title">{{ title }}</h4>
      <p v-if="description" class="ds-form-section__description">{{ description }}</p>
    </div>
    <slot />
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.ds-form-section + .ds-form-section {
  margin-top: 24px;
}

.ds-form-section__header {
  margin-bottom: 14px;
}

.ds-form-section__title {
  margin: 0;
  color: var(--ds-color-text-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
}

.ds-form-section__description {
  margin: 4px 0 0;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
