<template>
  <header class="ds-top-bar">
    <div class="ds-top-bar__brand">
      <div class="ds-top-bar__logo">
        <slot name="logo">
          <span class="ds-top-bar__logo-mark">C</span>
        </slot>
      </div>
      <div class="ds-top-bar__brand-copy">
        <span class="ds-top-bar__brand-title">{{ title }}</span>
        <span v-if="subtitle" class="ds-top-bar__brand-subtitle">{{ subtitle }}</span>
      </div>
    </div>

    <div class="ds-top-bar__actions">
      <button
        v-if="showThemeToggle"
        type="button"
        class="ds-top-bar__theme-toggle"
        :aria-label="themeLabel"
        :title="themeLabel"
        @click="$emit('theme-toggle')"
      >
        <span class="ds-top-bar__theme-icon">{{ themeMode === 'dark' ? '月' : '日' }}</span>
        <span class="ds-top-bar__theme-text">{{ themeMode === 'dark' ? '暗色' : '亮色' }}</span>
      </button>

      <a-dropdown>
        <button type="button" class="ds-top-bar__icon-button" aria-label="通知">
          <a-badge :count="notificationCount" :dot="notificationCount > 0" :max-count="99" class="ds-top-bar__badge">
            <slot name="notificationIcon">
              <span class="ds-top-bar__icon">N</span>
            </slot>
          </a-badge>
        </button>
        <template #content>
          <a-doption
            v-for="item in notificationItems"
            :key="item.key"
            @click="$emit('notification-select', item.key)"
          >
            {{ item.label }}
          </a-doption>
        </template>
      </a-dropdown>

      <a-dropdown>
        <button type="button" class="ds-top-bar__user-button">
          <span class="ds-top-bar__avatar">{{ avatarText }}</span>
          <span class="ds-top-bar__user-name">{{ userName }}</span>
          <span class="ds-top-bar__caret">▾</span>
        </button>
        <template #content>
          <a-doption
            v-for="item in userItems"
            :key="item.key"
            @click="$emit('user-select', item.key)"
          >
            {{ item.label }}
          </a-doption>
        </template>
      </a-dropdown>

      <button type="button" class="ds-top-bar__icon-button ds-top-bar__logout" :aria-label="logoutLabel" @click="$emit('logout')">
        <slot name="logoutIcon">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M10 5.75H7.75A1.75 1.75 0 0 0 6 7.5v9A1.75 1.75 0 0 0 7.75 18.25H10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M13 8.5L16.5 12L13 15.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M10.5 12H17" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </slot>
        <span class="ds-top-bar__logout-label">{{ logoutLabel }}</span>
      </button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  userName: { type: String, default: '用户' },
  avatarText: { type: String, default: 'U' },
  notificationCount: { type: Number, default: 0 },
  notificationItems: { type: Array, default: () => [] },
  userItems: { type: Array, default: () => [] },
  logoutLabel: { type: String, default: '退出' },
  showThemeToggle: { type: Boolean, default: false },
  themeMode: { type: String, default: 'light' }
})

const themeLabel = computed(() => props.themeMode === 'dark' ? '切换到亮色模式' : '切换到暗黑模式')

defineEmits(['notification-select', 'user-select', 'logout', 'theme-toggle'])
</script>

<style scoped>
.ds-top-bar {
  min-height: 72px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--ds-color-primary) 3%, var(--ds-color-bg-card)) 0%, var(--ds-color-bg-card) 100%);
  border-bottom: 1px solid color-mix(in srgb, var(--ds-color-primary) 6%, var(--ds-color-border));
  box-shadow: var(--ds-shadow-card);
}

.ds-top-bar__brand {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.ds-top-bar__logo {
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: color-mix(in srgb, var(--ds-color-primary) 12%, var(--ds-color-bg-card));
  color: var(--ds-color-primary);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--ds-color-primary) 14%, transparent);
}

.ds-top-bar__logo-mark {
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}

.ds-top-bar__brand-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ds-top-bar__brand-title {
  color: var(--ds-color-text-primary);
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
}

.ds-top-bar__brand-subtitle {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.ds-top-bar__actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.ds-top-bar__theme-toggle {
  height: 36px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--ds-color-border);
  border-radius: 999px;
  color: var(--ds-color-text-regular);
  background: var(--ds-color-bg-card);
  cursor: pointer;
}

.ds-top-bar__theme-toggle:hover {
  border-color: color-mix(in srgb, var(--ds-color-primary) 28%, var(--ds-color-border));
  color: var(--ds-color-primary);
  background: var(--ds-color-bg-hover);
}

.ds-top-bar__theme-icon {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--ds-color-bg-selected);
  color: var(--ds-color-primary);
  font-size: 12px;
  font-weight: 700;
}

.ds-top-bar__theme-text {
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
}

.ds-top-bar__icon-button,
.ds-top-bar__user-button {
  border: 0;
  background: transparent;
  cursor: pointer;
}

.ds-top-bar__icon-button {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: var(--ds-color-text-regular);
}

.ds-top-bar__icon-button:hover,
.ds-top-bar__user-button:hover {
  background: var(--ds-color-bg-hover);
}

.ds-top-bar__badge :deep(.arco-badge-number),
.ds-top-bar__badge :deep(.arco-badge-dot) {
  box-shadow: 0 0 0 2px var(--ds-color-bg-card);
}

.ds-top-bar__icon {
  font-size: 16px;
  font-weight: 600;
  line-height: 1;
}

.ds-top-bar__icon-button :deep(svg),
.ds-top-bar__logout svg {
  width: 18px;
  height: 18px;
}

.ds-top-bar__user-button {
  min-height: 40px;
  padding: 4px 10px 4px 6px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border-radius: 999px;
}

.ds-top-bar__avatar {
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--ds-color-primary);
  color: var(--ds-color-inverse-text);
  font-size: 13px;
  font-weight: 700;
}

.ds-top-bar__user-name {
  color: var(--ds-color-text-primary);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
}

.ds-top-bar__caret {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1;
}

.ds-top-bar__logout {
  width: auto;
  padding: 0 12px;
  gap: 8px;
  color: var(--ds-color-text-regular);
}

.ds-top-bar__logout-label {
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
}

.ds-top-bar__logout:hover {
  color: var(--ds-color-danger);
}

@media (max-width: 960px) {
  .ds-top-bar {
    padding: 0 16px;
  }

  .ds-top-bar__brand-subtitle,
  .ds-top-bar__theme-text,
  .ds-top-bar__user-name {
    display: none;
  }
}
</style>
