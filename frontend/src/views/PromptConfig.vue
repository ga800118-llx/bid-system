<template>
  <div class="prompt-container">
    <div class="header">
      <div class="header-left">
        <h2>Prompt 配置</h2>
      </div>
      <div class="header-right">
        <el-button link @click="$router.push('/')">返回首页</el-button>
      </div>
    </div>
    <div class="main">
      <el-card v-loading="loading">
        <el-form label-width="120px">

          <div class="section-title">基本信息</div>
          <el-form-item label="模板名称" required>
            <el-input v-model="form.name" placeholder="如：招标文件提取v2" />
          </el-form-item>

          <div class="section-title">System Prompt <span class="optional-tag">选填</span></div>
          <el-form-item label="system">
            <el-input
              v-model="form.system"
              type="textarea"
              :rows="4"
              placeholder="可填写 AI 角色设定等系统级指令，为空则调用接口时不发送 system 消息"
            />
            <div class="tip">作用于 MiniMax API 的 system 角色消息，用于设定 AI 角色或行为规范</div>
          </el-form-item>

          <div class="section-title">Prompt 正文 <span class="required-tag">必填</span></div>
          <el-form-item label="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="8"
              placeholder="包含 {content} 和 {field_def} 占位符的 Prompt 模板"
            />
            <div class="tip">&#123;content&#125; 会替换为 PDF 提取的文字，&#123;field_def&#125; 会替换为字段定义列表</div>
          </el-form-item>

          <div class="section-title">字段定义 <span class="required-tag">必填</span></div>
          <el-form-item label="fieldDef (JSON)">
            <el-input
              v-model="fieldDefText"
              type="textarea"
              :rows="12"
              placeholder="field_def JSON 对象，包含 fields 数组"
              style="font-family: monospace"
            />
            <div class="tip">完整的 field_def JSON 对象，数组中每个元素对应一个提取字段</div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSave" :loading="saving">保存配置</el-button>
          </el-form-item>

        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue"
import { ElMessage } from "element-plus"
import axios from "axios"

const loading = ref(false)
const saving = ref(false)
const form = ref({ name: "", system: "", content: "" })
const fieldDefText = ref("{}")

const fetchTemplate = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem("token")
    const res = await axios.get("/api/prompt", {
      headers: { Authorization: "Bearer " + token }
    })
    if (res.data.code === 200) {
      form.value.name = res.data.data.name || ""
      form.value.system = res.data.data.system || ""
      form.value.content = res.data.data.content || ""
      fieldDefText.value = res.data.data.fieldDef || "{}"
    } else {
      ElMessage.error(res.data.msg || "加载失败")
    }
  } catch (e) {
    ElMessage.error("加载失败")
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!form.value.content || !form.value.content.trim()) {
    ElMessage.error("Prompt 正文（content）为必填项，请勿留空")
    return
  }
  let fieldDefObj = null
  try {
    fieldDefObj = JSON.parse(fieldDefText.value)
  } catch (e) {
    ElMessage.error("字段定义（fieldDef）JSON 格式错误，请检查后重试")
    return
  }
  saving.value = true
  try {
    const token = localStorage.getItem("token")
    const res = await axios.put("/api/prompt", {
      name: form.value.name,
      system: form.value.system || "",
      content: form.value.content,
      fieldDef: JSON.stringify(fieldDefObj)
    }, {
      headers: { Authorization: "Bearer " + token }
    })
    if (res.data.code === 200) {
      ElMessage.success("保存成功")
    } else {
      ElMessage.error(res.data.msg || "保存失败")
    }
  } catch (e) {
    ElMessage.error("保存失败")
  } finally {
    saving.value = false
  }
}

onMounted(fetchTemplate)
</script>

<style scoped>
.prompt-container { min-height: 100vh; background: #f0f2f5; }
.header { background: #fff; padding: 0 24px; height: 60px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 1px 4px rgba(0,0,0,.1); }
.main { padding: 24px; max-width: 900px; margin: 0 auto; }
.tip { font-size: 12px; color: #999; margin-top: 4px; line-height: 1.4; }
.section-title { font-size: 14px; font-weight: 600; color: #333; margin: 16px 0 8px; padding-bottom: 4px; border-bottom: 1px solid #eee; }
.optional-tag { font-size: 12px; font-weight: normal; color: #67c23a; background: #f0f9eb; padding: 1px 6px; border-radius: 4px; margin-left: 6px; }
.required-tag { font-size: 12px; font-weight: normal; color: #e6a23c; background: #fdf6ec; padding: 1px 6px; border-radius: 4px; margin-left: 6px; }
</style>