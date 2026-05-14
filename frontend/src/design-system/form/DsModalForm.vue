<template>
  <a-modal
    :visible="visible"
    :width="modalWidth"
    :footer="false"
    :closable="false"
    :mask-closable="maskClosable"
    unmount-on-close
    modal-class="ds-modal-form-modal"
    @update:visible="$emit('update:visible', $event)"
    @cancel="$emit('cancel')"
  >
    <div class="ds-modal-form">
      <header class="ds-modal-form__header">
        <div class="ds-modal-form__heading">
          <h3 class="ds-modal-form__title">{{ title }}</h3>
          <p v-if="description" class="ds-modal-form__description">{{ description }}</p>
        </div>
        <button class="ds-modal-form__close" type="button" aria-label="关闭" @click="$emit('cancel')">×</button>
      </header>
      <main class="ds-modal-form__body">
        <slot />
      </main>
      <footer v-if="$slots.footer" class="ds-modal-form__footer">
        <slot name="footer" />
      </footer>
    </div>
  </a-modal>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  width: { type: [Number, String], default: 760 },
  maskClosable: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'cancel'])

const modalWidth = computed(() => {
  if (typeof props.width === 'number') {
    return `min(${props.width}px, calc(100vw - 32px))`
  }
  return props.width
})
</script>

<style scoped>
:deep(.ds-modal-form-modal) {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 50px var(--ds-color-shadow-strong);
}

:deep(.ds-modal-form-modal .arco-modal-body) {
  padding: 0;
}

.ds-modal-form {
  background: var(--ds-color-bg-card);
}

.ds-modal-form__header {
  min-height: 64px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 24px 16px;
  border-bottom: 1px solid var(--ds-color-border);
}

.ds-modal-form__title {
  margin: 0;
  color: var(--ds-color-text-primary);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
}

.ds-modal-form__description {
  margin: 6px 0 0;
  color: var(--ds-color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.ds-modal-form__close {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--ds-color-text-secondary);
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.ds-modal-form__close:hover {
  background: var(--ds-color-bg-hover);
  color: var(--ds-color-text-primary);
}

.ds-modal-form__body {
  padding: 24px;
}

.ds-modal-form__footer {
  min-height: 64px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 14px 24px;
  border-top: 1px solid var(--ds-color-border);
  background: var(--ds-color-bg-card);
}
</style>
