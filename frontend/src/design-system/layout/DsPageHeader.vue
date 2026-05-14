<template>
  <section class="ds-page-header ds-card">
    <div class="ds-page-header__bg" />
    <div class="ds-page-header__main">
      <a-breadcrumb v-if="breadcrumbItems.length" class="ds-page-header__breadcrumb">
        <a-breadcrumb-item v-for="(item, index) in breadcrumbItems" :key="`${item}-${index}`">
          <span :class="{ 'ds-page-header__breadcrumb-current': index === breadcrumbItems.length - 1 }">{{ item }}</span>
        </a-breadcrumb-item>
      </a-breadcrumb>
      <div class="ds-page-header__title-row">
        <span v-if="$slots.icon || icon" class="ds-page-header__icon">
          <slot name="icon">
            <component :is="icon" v-if="icon" />
          </slot>
        </span>
        <div class="ds-page-header__title-block">
          <h1 class="ds-page-header__title">{{ title }}</h1>
          <p v-if="description" class="ds-page-header__description">{{ description }}</p>
          <div v-if="$slots.meta" class="ds-page-header__meta">
            <slot name="meta" />
          </div>
        </div>
      </div>
    </div>
    <div v-if="$slots.actions" class="ds-page-header__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  breadcrumb: { type: Array, default: () => [] },
  breadcrumbs: { type: Array, default: () => [] },
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  icon: { type: [Object, Function], default: null }
})

const breadcrumbItems = computed(() => {
  if (props.breadcrumb?.length) return props.breadcrumb
  return props.breadcrumbs || []
})
</script>

<style scoped>
.ds-page-header {
  position: relative;
  overflow: hidden;
  min-height: 108px;
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 6%, var(--ds-color-border));
  box-shadow: 0 10px 28px var(--ds-color-shadow);
}

.ds-page-header__bg {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(120deg, color-mix(in srgb, var(--ds-color-primary) 5%, var(--ds-color-bg-card)) 0%, transparent 36%),
    radial-gradient(circle at 100% 0%, color-mix(in srgb, var(--ds-color-primary) 7%, var(--ds-color-bg-card)) 0%, transparent 42%);
  pointer-events: none;
}

.ds-page-header__main,
.ds-page-header__actions {
  position: relative;
  z-index: 1;
}

.ds-page-header__main {
  min-width: 0;
  flex: 1 1 auto;
}

.ds-page-header__breadcrumb {
  margin-bottom: 10px;
}

.ds-page-header__breadcrumb :deep(.arco-breadcrumb-item),
.ds-page-header__breadcrumb :deep(.arco-breadcrumb-item a),
.ds-page-header__breadcrumb :deep(.arco-breadcrumb-item-last) {
  font-size: 13px;
  color: var(--ds-color-text-secondary);
}

.ds-page-header__breadcrumb-current {
  color: var(--ds-color-text-primary);
  font-weight: 500;
}

.ds-page-header__title-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.ds-page-header__icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-color-bg-selected);
  color: var(--ds-color-primary);
  flex: 0 0 auto;
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--ds-color-primary) 10%, transparent);
}

.ds-page-header__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.ds-page-header__title {
  margin: 0;
  color: var(--ds-color-text-primary);
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
}

.ds-page-header__description {
  margin: 8px 0 0;
  max-width: 780px;
  color: var(--ds-color-text-regular);
  font-size: 14px;
  line-height: 1.6;
}

.ds-page-header__meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.ds-page-header__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex: 0 0 auto;
  min-width: 0;
}

@media (max-width: 960px) {
  .ds-page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .ds-page-header__actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
