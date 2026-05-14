<template>
  <div class="ds-form-actions">
    <a-button class="ds-form-actions__button" :disabled="loading" @click="$emit('cancel')">{{ cancelText }}</a-button>
    <a-button class="ds-form-actions__button" type="primary" :loading="loading" @click="$emit('submit')">{{ submitText }}</a-button>
  </div>
</template>

<script setup>
defineProps({
  cancelText: { type: String, default: '取消' },
  submitText: { type: String, default: '保存' },
  loading: { type: Boolean, default: false }
})

defineEmits(['cancel', 'submit'])
</script>

<style scoped>
.ds-form-actions {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
}

.ds-form-actions__button {
  min-width: 84px;
  height: 36px;
  border-radius: 8px;
}
</style>
