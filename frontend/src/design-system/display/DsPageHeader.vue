<template>
  <section class="ds-page-header ds-card">
    <div class="ds-page-header__main">
      <a-breadcrumb v-if="breadcrumbs?.length" class="ds-page-header__breadcrumb">
        <a-breadcrumb-item v-for="item in breadcrumbs" :key="item">{{ item }}</a-breadcrumb-item>
      </a-breadcrumb>
      <div class="ds-page-header__title-row">
        <span v-if="$slots.icon" class="ds-page-header__icon">
          <slot name="icon" />
        </span>
        <div class="ds-page-header__title-block">
          <h1 class="ds-page-header__title">{{ title }}</h1>
          <p v-if="description" class="ds-page-header__description">{{ description }}</p>
        </div>
      </div>
    </div>
    <div v-if="$slots.actions" class="ds-page-header__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup>
defineProps({
  breadcrumbs: { type: Array, default: () => [] },
  title: { type: String, default: '' },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.ds-page-header {
  padding: 22px 26px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.ds-page-header__main {
  min-width: 0;
}

.ds-page-header__breadcrumb {
  margin-bottom: 12px;
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
}

.ds-page-header__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.ds-page-header__title {
  margin: 0;
  color: var(--ds-color-text-primary);
  font-size: 26px;
  line-height: 1.2;
}

.ds-page-header__description {
  margin: 10px 0 0;
  max-width: 760px;
  color: var(--ds-color-text-regular);
  font-size: 14px;
  line-height: 1.7;
}

.ds-page-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding-left: 16px;
}

@media (max-width: 960px) {
  .ds-page-header {
    flex-direction: column;
  }

  .ds-page-header__actions {
    width: 100%;
    justify-content: flex-start;
    padding-left: 0;
  }
}
</style>
