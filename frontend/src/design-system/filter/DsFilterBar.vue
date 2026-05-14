<template>
  <section class="ds-filter-bar ds-card">
    <div class="ds-filter-bar__header" v-if="title || $slots.headerActions">
      <div>
        <h3 v-if="title || $slots.title" class="ds-filter-bar__title">
          <slot name="title">
            {{ title }}
          </slot>
        </h3>
        <p v-if="description" class="ds-filter-bar__description">{{ description }}</p>
      </div>
      <div v-if="$slots.headerActions" class="ds-filter-bar__header-actions">
        <slot name="headerActions" />
      </div>
    </div>
    <div class="ds-filter-bar__body">
      <div class="ds-filter-bar__fields">
        <slot />
      </div>
      <div class="ds-filter-bar__actions">
        <slot name="actions" />
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.ds-filter-bar {
  padding: 20px;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-border));
  box-shadow: 0 8px 24px var(--ds-color-shadow);
}

.ds-filter-bar__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.ds-filter-bar__title { margin: 0; color: var(--ds-color-text-primary); font-size: 16px; font-weight: 600; }
.ds-filter-bar__description { margin: 6px 0 0; color: var(--ds-color-text-secondary); font-size: 12px; line-height: 1.6; }
.ds-filter-bar__header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.ds-filter-bar__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: end;
}

.ds-filter-bar__fields {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  min-width: 0;
}

.ds-filter-bar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex: 0 0 auto;
  white-space: nowrap;
}

@media (max-width: 960px) {
  .ds-filter-bar__body { grid-template-columns: 1fr; }
  .ds-filter-bar__actions { justify-content: flex-end; }
}
</style>
