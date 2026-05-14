<template>
  <div class="ds-user-cell">
    <UserAvatarFallback :text="avatarText" :size="28" />
    <div class="ds-user-cell__meta">
      <span class="ds-user-cell__name">{{ primaryText }}</span>
      <span v-if="secondaryText" class="ds-user-cell__sub">{{ secondaryText }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import UserAvatarFallback from '@/design-system/icons/UserAvatarFallback.vue'

const props = defineProps({
  name: { type: String, default: '' },
  account: { type: String, default: '' },
  revealAccount: { type: Boolean, default: true }
})

const primaryText = computed(() => props.name || props.account || '-')
const secondaryText = computed(() => {
  if (!props.revealAccount) return ''
  if (props.name && props.account) return props.account
  return ''
})
const avatarText = computed(() => props.name || props.account || 'U')
</script>

<style scoped>
.ds-user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.ds-user-cell__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.ds-user-cell__name {
  color: var(--ds-color-text-primary);
  font-weight: 500;
  font-size: 14px;
  line-height: 20px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ds-user-cell__sub {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
