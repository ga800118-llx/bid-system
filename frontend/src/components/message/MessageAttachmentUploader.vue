<template>
  <div class="message-attachment-uploader">
    <div class="message-attachment-uploader__header">
      <span>{{ label }}</span>
      <a-button size="small" :loading="uploading" @click="triggerSelect">上传附件</a-button>
      <input ref="fileInputRef" type="file" multiple class="message-attachment-uploader__input" @change="handleFileChange" />
    </div>
    <div v-if="files.length" class="message-attachment-uploader__list">
      <div v-for="file in files" :key="file.id" class="message-attachment-uploader__item">
        <span class="message-attachment-uploader__icon"><FileTypeIcon :type="file.extension" /></span>
        <span class="message-attachment-uploader__name">{{ file.originalName || file.name || '-' }}</span>
        <span class="message-attachment-uploader__size">{{ formatFileSize(file.fileSize || file.size) }}</span>
        <button type="button" class="message-attachment-uploader__remove" @click="removeFile(file.id)">移除</button>
      </div>
    </div>
    <DsEmptyState v-else title="暂无附件" description="可上传图片、PDF、Word、Excel 等文件作为消息附件。" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { fileApi } from '@/api'
import { DsEmptyState } from '@/design-system'
import FileTypeIcon from '@/design-system/icons/FileTypeIcon.vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  label: { type: String, default: '附件' }
})

const emit = defineEmits(['update:modelValue'])
const fileInputRef = ref(null)
const files = ref([])
const uploading = ref(false)

const triggerSelect = () => fileInputRef.value?.click()

const handleFileChange = async event => {
  const selectedFiles = Array.from(event.target?.files || [])
  if (!selectedFiles.length) return
  uploading.value = true
  try {
    for (const file of selectedFiles) {
      const formData = new FormData()
      formData.append('file', file)
      const res = await fileApi.upload(formData)
      if (res.code === 200 && res.data?.id) {
        files.value.push(res.data)
      } else {
        Message.error(res.msg || `${file.name} 上传失败`)
      }
    }
    emit('update:modelValue', files.value.map(file => file.id))
  } catch (error) {
    Message.error(error?.response?.data?.msg || '附件上传失败')
  } finally {
    uploading.value = false
    if (event.target) event.target.value = ''
  }
}

const removeFile = id => {
  files.value = files.value.filter(file => Number(file.id) !== Number(id))
  emit('update:modelValue', files.value.map(file => file.id))
}

const formatFileSize = value => {
  const size = Number(value || 0)
  if (!Number.isFinite(size) || size <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let current = size
  let index = 0
  while (current >= 1024 && index < units.length - 1) {
    current /= 1024
    index++
  }
  return `${current >= 100 || index === 0 ? current.toFixed(0) : current.toFixed(1)} ${units[index]}`
}

watch(() => props.modelValue, value => {
  if (!value?.length) files.value = []
})
</script>

<style scoped>
.message-attachment-uploader {
  border: 1px solid var(--ds-color-border);
  border-radius: 10px;
  background: var(--ds-color-bg-card);
  overflow: hidden;
}

.message-attachment-uploader__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--ds-color-border);
  color: var(--ds-color-text-primary);
  font-size: 13px;
  font-weight: 600;
}

.message-attachment-uploader__input {
  display: none;
}

.message-attachment-uploader__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
}

.message-attachment-uploader__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--ds-color-border);
  border-radius: 8px;
  background: var(--ds-color-bg-soft);
}

.message-attachment-uploader__icon {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.message-attachment-uploader__icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.message-attachment-uploader__name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ds-color-text-primary);
}

.message-attachment-uploader__size {
  color: var(--ds-color-text-secondary);
  font-size: 12px;
}

.message-attachment-uploader__remove {
  border: 0;
  background: transparent;
  color: var(--ds-color-danger);
  cursor: pointer;
}
</style>
