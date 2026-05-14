<template>
  <a-popover position="top" trigger="hover">
    <template #content>
      <div class="ds-security-popover">
        <span class="ds-security-popover__arrow" />
        <div class="ds-security-popover__title">{{ title }}</div>
        <div class="ds-security-popover__list">
          <div class="ds-security-popover__item">
            <span class="ds-security-popover__label">状态</span>
            <span class="ds-security-popover__value">
              <DsStatusTag :label="statusText" :tone="abnormal ? 'danger' : 'success'" />
            </span>
          </div>
          <div class="ds-security-popover__item">
            <span class="ds-security-popover__label">连续失败</span>
            <strong class="ds-security-popover__value" :class="{ 'ds-security-popover__value--danger': failedCount > 0 }">
              {{ failedCount }} 次
            </strong>
          </div>
          <div class="ds-security-popover__item">
            <span class="ds-security-popover__label">最近失败</span>
            <strong class="ds-security-popover__value">{{ lastFailedText }}</strong>
          </div>
          <div class="ds-security-popover__item">
            <span class="ds-security-popover__label">最近登录</span>
            <strong class="ds-security-popover__value">{{ lastLoginText }}</strong>
          </div>
        </div>
        <div v-if="abnormal && suggestion" class="ds-security-popover__suggestion">{{ suggestion }}</div>
      </div>
    </template>
    <slot />
  </a-popover>
</template>

<script setup>
import DsStatusTag from './DsStatusTag.vue'

const props = defineProps({
  title: { type: String, default: '登录安全' },
  statusText: { type: String, default: '正常' },
  failedCount: { type: Number, default: 0 },
  lastFailedText: { type: String, default: '无失败记录' },
  lastLoginText: { type: String, default: '-' },
  abnormal: { type: Boolean, default: false },
  suggestion: { type: String, default: '' }
})
</script>

<style scoped>
.ds-security-popover {
  position: relative;
  width: min(260px, calc(100vw - 32px));
  padding: 10px 12px;
  background: var(--ds-color-bg-card);
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
  box-shadow: 0 8px 20px var(--ds-color-shadow);
}

.ds-security-popover__arrow {
  position: absolute;
  left: 50%;
  bottom: -5px;
  width: 9px;
  height: 9px;
  background: var(--ds-color-bg-card);
  border-right: 1px solid var(--ds-color-border);
  border-bottom: 1px solid var(--ds-color-border);
  transform: translateX(-50%) rotate(45deg);
}

.ds-security-popover__title {
  color: var(--ds-color-text-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  margin-bottom: 8px;
}

.ds-security-popover__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ds-security-popover__item {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  align-items: center;
  column-gap: 12px;
}

.ds-security-popover__label {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.ds-security-popover__value {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  color: var(--ds-color-text-primary);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  word-break: break-word;
}

.ds-security-popover__value--danger {
  color: var(--ds-color-danger);
}

.ds-security-popover__value--success {
  color: var(--ds-color-success);
}

.ds-security-popover__suggestion {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--ds-color-border);
  color: var(--ds-color-text-regular);
  font-size: 12px;
  line-height: 1.5;
}
</style>
