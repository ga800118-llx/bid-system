<template>
  <div class="prompt-container">
    <PageHeader title="Prompt配置" />
    <div class="main">
      <a-card :loading="loading">
        <a-form :model="form" layout="vertical">
          <div class="section-title">基本信息</div>
          <a-form-item label="模板名称" required>
            <a-input v-model="form.name" placeholder="例如：招标文档提取v2" />
          </a-form-item>
          <div class="section-title">System Prompt <span class="optional-tag">可选</span></div>
          <a-form-item label="system">
            <a-textarea v-model="form.system" :rows="4" style="width:100%" placeholder="可以写 AI 角色设定，为空则不发送 system 信息" />
            <div class="tip">用于 MiniMax API 的 system 角色信息</div>
          </a-form-item>
          <div class="section-title">Prompt 内容 <span class="required-tag">必填</span></div>
          <a-form-item label="content">
            <a-textarea v-model="form.content" :rows="8" style="width:100%" placeholder="请包含 {content} 和 {field_def} 占位符的 Prompt 模板" />
            <div class="tip">{content} 替换为 PDF 提取文字，{field_def} 替换为字段定义列表</div>
          </a-form-item>
          <div class="section-title">字段定义 <span class="required-tag">必填</span></div>
          <a-form-item label="fieldDef (JSON)">
            <a-textarea v-model="fieldDefText" :rows="12" placeholder="field_def JSON 对象" style="width:100%;font-family:monospace" />
            <div class="tip">用于 field_def JSON 模板</div>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="handleSave" :loading="saving">保存配置</a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import axios from 'axios'
const loading = ref(false)
const saving = ref(false)
const form = ref({ name: '', system: '', content: '' })
const fieldDefText = ref('{}')
const fetchTemplate = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get('/api/prompt', { headers: { Authorization: 'Bearer ' + token } })
    if (res.data.code === 200) {
      form.value.name = res.data.data.name || ''
      form.value.system = res.data.data.system || ''
      form.value.content = res.data.data.content || ''
      fieldDefText.value = res.data.data.fieldDef || '{}'
    } else { Message.error(res.data.msg || '加载失败') }
  } catch (e) { Message.error('加载失败') }
  finally { loading.value = false }
}
const handleSave = async () => {
  if (!form.value.content || !form.value.content.trim()) {
    Message.error('Prompt 内容不能为空')
    return
  }
  let fieldDefObj = null
  try { fieldDefObj = JSON.parse(fieldDefText.value) }
  catch (e) { Message.error('字段定义 JSON 格式错误'); return }
  saving.value = true
  try {
    const token = localStorage.getItem('token')
    const res = await axios.put('/api/prompt', {
      name: form.value.name, system: form.value.system||'', content: form.value.content,
      fieldDef: JSON.stringify(fieldDefObj)
    }, { headers: { Authorization: 'Bearer ' + token } })
    if (res.data.code === 200) { Message.success('保存成功') }
    else { Message.error(res.data.msg||'保存失败') }
  } catch (e) { Message.error('保存失败') }
  finally { saving.value = false }
}
onMounted(fetchTemplate)
</script>
<style scoped>
.prompt-container { min-height: 100vh; background: #f0f2f5; }
.header { background: #fff; padding: 0 24px; height: 60px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 1px 4px rgba(0,0,0,.1); }
.header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #1d1d1d; }
.main { padding: 24px; max-width: 900px; margin: 0 auto; }
.tip { display:block; margin-top:4px; color:#999; font-size:12px; clear:both; }
</style>
