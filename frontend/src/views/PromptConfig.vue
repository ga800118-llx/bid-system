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
          <div class="field-block">
            <div class="field-label">system</div>
            <a-textarea v-model="form.system" :auto-size="{ minRows: 4 }" style="width:100%" placeholder="可以写 AI 角色设定，为空则不发送 system 信息" />
            <div class="tip">用于 MiniMax API 的 system 角色信息</div>
          </div>
          <div class="section-title">Prompt 内容 <span class="required-tag">必填</span></div>
          <div class="field-block">
            <div class="field-label">content</div>
            <a-textarea v-model="form.content" :auto-size="{ minRows: 8 }" style="width:100%" placeholder="请包含 {content} 和 {field_def} 占位符的 Prompt 模板" />
            <div class="tip">{content} 替换为 PDF 提取文字，{field_def} 替换为字段定义列表</div>
          </div>
          <div class="section-title">字段定义</div>
          <div class="field-list">
            <a-table :columns="fieldColumns" :data="fields" :pagination="false" stripe size="small">
              <template #label="{ record }">
                <a-link @click="openEditModal(record)">{{ record.label }}</a-link>
              </template>
              <template #key="{ record }">
                <code style="font-size:12px;color:#666">{{ record.key }}</code>
              </template>
              <template #semantic="{ record }">
                <span class="semantic-text" :title="record.semantic" @click="openEditModal(record)">
                  {{ truncate(record.semantic, 40) }}
                </span>
              </template>
            </a-table>
          </div>
          <a-form-item style="margin-top:16px">
            <a-button type="primary" @click="handleSave" :loading="saving">保存配置</a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </div>
    <a-modal v-model:visible="editModalVisible" title="编辑字段" :width="600" @before-ok="handleEditSave" @cancel="editModalVisible = false">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="字段名称（显示名）">
          <a-input v-model="editForm.label" placeholder="例如：项目名称" />
        </a-form-item>
        <a-form-item label="字段 Key">
          <a-input v-model="editForm.key" disabled placeholder="英文键名，不可修改" />
        </a-form-item>
        <a-form-item label="提取指令（Semantic）">
          <a-textarea v-model="editForm.semantic" :auto-size="{ minRows: 6, maxRows: 12 }" placeholder="AI 提取该字段时的指令描述" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue"
import { Message } from "@arco-design/web-vue"

const loading = ref(false)
const saving = ref(false)
const form = ref({ name: "", system: "", content: "" })
const fields = ref([])
const editModalVisible = ref(false)
const editForm = reactive({ key: "", label: "", semantic: "" })

const fieldColumns = [
  { title: "字段名称", slotName: "label", width: 160 },
  { title: "Key", slotName: "key", width: 160 },
  { title: "提取指令（点击编辑）", slotName: "semantic" }
]

const truncate = (text, len) => {
  if (!text) return ""
  return text.length > len ? text.substring(0, len) + "..." : text
}

const openEditModal = (record) => {
  editForm.key = record.key
  editForm.label = record.label
  editForm.semantic = record.semantic
  editModalVisible.value = true
}

const handleEditSave = (done) => {
  const field = fields.value.find(f => f.key === editForm.key)
  if (field) { field.label = editForm.label; field.semantic = editForm.semantic }
  editModalVisible.value = false
  done()
}

const fetchTemplate = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem("token")
    const res = await fetch("/api/prompt", { headers: { Authorization: "Bearer " + token } }).then(r => r.json())
    if (res.code === 200) {
      form.value.name = res.data.name || ""
      form.value.system = res.data.system || ""
      form.value.content = res.data.content || ""
      try { const fd = JSON.parse(res.data.fieldDef || "{}"); fields.value = fd.fields || [] }
      catch { fields.value = [] }
    } else { Message.error(res.msg || "加载失败") }
  } catch { Message.error("加载失败") }
  finally { loading.value = false }
}

const handleSave = async () => {
  if (!form.value.content || !form.value.content.trim()) { Message.error("Prompt 内容不能为空"); return }
  saving.value = true
  try {
    const token = localStorage.getItem("token")
    const res = await fetch("/api/prompt", {
      method: "PUT",
      headers: { Authorization: "Bearer " + token, "Content-Type": "application/json" },
      body: JSON.stringify({ name: form.value.name, system: form.value.system || "", content: form.value.content, fieldDef: JSON.stringify({ fields: fields.value }) })
    }).then(r => r.json())
    if (res.code === 200) { Message.success("保存成功") }
    else { Message.error(res.msg || "保存失败") }
  } catch { Message.error("保存失败") }
  finally { saving.value = false }
}

onMounted(fetchTemplate)
</script>

<style scoped>
.prompt-container { min-height: 100vh; background: #f0f2f5; }
.main { padding: 24px; max-width: 900px; margin: 0 auto; }
.section-title { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 12px; margin-top: 20px; }
.field-block { display: block; width: 100%; margin-bottom: 16px; }
.field-label { font-size: 14px; font-weight: 500; color: #333; margin-bottom: 8px; }
.tip { display: block; margin-top: 4px; color: #999; font-size: 12px; }
.field-list { border: 1px solid #e8e8e8; border-radius: 6px; overflow: hidden; }
.semantic-text { cursor: pointer; color: #555; font-size: 13px; line-height: 1.5; }
.semantic-text:hover { color: #1650ff; }
.optional-tag { font-size: 11px; color: #999; font-weight: 400; margin-left: 6px; }
.required-tag { font-size: 11px; color: #f53f3f; font-weight: 400; margin-left: 6px; }
</style>