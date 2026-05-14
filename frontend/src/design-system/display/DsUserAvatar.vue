<template>
  <div class="ds-user-avatar">
    <UserAvatarFallback :text="displayText" :size="size" />
    <div v-if="showMeta" class="ds-user-avatar__meta">
      <span class="ds-user-avatar__name">{{ name || '-' }}</span>
      <span v-if="subText" class="ds-user-avatar__sub">{{ subText }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import UserAvatarFallback from '@/design-system/icons/UserAvatarFallback.vue'

const props = defineProps({
  name: { type: String, default: '' },
  subText: { type: String, default: '' },
  size: { type: Number, default: 32 },
  showMeta: { type: Boolean, default: true }
})

const displayText = computed(() => props.name || props.subText || 'U')
</script>

<style scoped>
.ds-user-avatar { display: inline-flex; align-items: center; gap: 10px; min-width: 0; }
.ds-user-avatar__meta { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.ds-user-avatar__name { color: var(--ds-color-text-primary); font-weight: 600; line-height: 1.4; }
.ds-user-avatar__sub { color: var(--ds-color-text-secondary); font-size: 12px; line-height: 1.4; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
