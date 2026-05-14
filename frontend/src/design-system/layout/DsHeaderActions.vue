<template>
  <div class="ds-header-actions">
    <a-button
      v-for="item in visibleActionItems"
      :key="item.key"
      class="ds-header-actions__secondary"
      :disabled="item.disabled"
      @click="$emit('action-click', item.key)"
    >
      {{ item.label }}
    </a-button>

    <a-dropdown v-if="showTools">
      <a-button class="ds-header-actions__secondary">
        {{ toolsLabel }}
        <template #icon>
          <IconDown />
        </template>
      </a-button>
      <template #content>
        <a-doption v-for="item in visibleToolItems" :key="item.key" @click="$emit('tool-select', item.key)">
          {{ item.label }}
        </a-doption>
      </template>
    </a-dropdown>

    <a-button v-if="showPrimary" type="primary" class="ds-header-actions__primary" @click="$emit('primary-click')">
      <template #icon v-if="$slots.primaryIcon">
        <slot name="primaryIcon" />
      </template>
      {{ primaryLabel }}
    </a-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { IconDown } from '@arco-design/web-vue/es/icon'

const props = defineProps({
  actionItems: {
    type: Array,
    default: () => []
  },
  toolItems: {
    type: Array,
    default: () => []
  },
  toolsLabel: {
    type: String,
    default: '更多工具'
  },
  primaryLabel: {
    type: String,
    default: '新增'
  },
  showPrimary: {
    type: Boolean,
    default: true
  }
})

defineEmits(['tool-select', 'primary-click', 'action-click'])

const visibleToolItems = computed(() => (props.toolItems || []).filter(item => item && item.visible !== false))
const visibleActionItems = computed(() => (props.actionItems || []).filter(item => item && item.visible !== false))
const showTools = computed(() => visibleToolItems.value.length > 0)
</script>

<style scoped>
.ds-header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.ds-header-actions__secondary,
.ds-header-actions__primary {
  height: 36px;
  min-width: 100px;
  border-radius: 8px;
}

.ds-header-actions__secondary {
  border-color: var(--ds-color-border);
  color: var(--ds-color-text-primary);
  background: var(--ds-color-bg-card);
}

.ds-header-actions__secondary:hover {
  background: var(--ds-color-bg-hover);
  border-color: color-mix(in srgb, var(--ds-color-primary) 24%, var(--ds-color-border));
  color: var(--ds-color-primary);
}

.ds-header-actions__primary {
  box-shadow: 0 8px 18px color-mix(in srgb, var(--ds-color-primary) 16%, transparent);
}
</style>
