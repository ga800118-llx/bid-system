<template>
  <PageHeader title="帮助文档" />
  <div class="help-container">
    <a-tabs>
      <a-tab-pane key="overview" title="系统概览">
        <div class="help-section">
          <h3>投标管理系统简介</h3>
          <p>投标管理系统是一款基于 AI 的招投标文档智能解析工具，主要用于：</p>
          <ul>
            <li>上传招标 PDF 文档，AI 自动提取关键字段</li>
            <li>管理招标项目，查看、搜索、下载文档</li>
            <li>配置 Prompt 模板，优化提取效果</li>
          </ul>
        </div>
        <div class="help-section">
          <h3>技术架构</h3>
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="前端">Vue3 + Arco Design</a-descriptions-item>
            <a-descriptions-item label="后端">Java Spring Boot 3.2</a-descriptions-item>
            <a-descriptions-item label="数据库">MySQL</a-descriptions-item>
            <a-descriptions-item label="AI 提取">MiniMax 大模型</a-descriptions-item>
            <a-descriptions-item label="PDF 解析">pdfminer (Python)</a-descriptions-item>
          </a-descriptions>
        </div>
      </a-tab-pane>

      <a-tab-pane key="upload" title="上传文档">
        <div class="help-section">
          <h3>上传招标文档</h3>
          <p>管理员用户可以通过以下步骤上传招标文件：</p>
          <a-steps :current="4" size="small">
            <a-step title="进入上传页面" description="点击侧边栏「上传文件」" />
            <a-step title="选择文件" description="支持 PDF 格式，建议文字版 PDF" />
            <a-step title="等待 AI 提取" description="系统自动调用 MiniMax 解析" />
            <a-step title="查看结果" description="提取完成后自动跳转项目详情" />
          </a-steps>
        </div>
        <div class="help-section">
          <h3>支持的 PDF 类型</h3>
          <a-alert type="success" style="margin-bottom:12px">
            <template #title>推荐：文字版 PDF</template>
            PDF 内置文字层，提取效果最佳，速度快
          </a-alert>
          <a-alert type="warning">
            <template #title>不支持：扫描件 PDF</template>
            无文字层的扫描件无法提取，系统会提示「此文件为扫描件，请上传文字版 PDF」
          </a-alert>
        </div>
        <div class="help-section">
          <h3>提取的字段说明</h3>
          <a-table :columns="fieldColumns" :data="fieldData" size="small" :pagination="false" bordered />
        </div>
      </a-tab-pane>

      <a-tab-pane key="project" title="项目管理">
        <div class="help-section">
          <h3>项目列表</h3>
          <p>在项目列表页面可以：</p>
          <ul>
            <li><strong>搜索项目</strong> — 输入项目名称关键字筛选</li>
            <li><strong>查看详情</strong> — 点击项目名称进入详情页</li>
            <li><strong>下载原文</strong> — 点击操作列下载原始 PDF</li>
            <li><strong>下载 Markdown</strong> — 点击操作列下载 AI 提取后的文本</li>
            <li><strong>删除项目</strong> — 管理员可在详情页删除</li>
          </ul>
        </div>
        <div class="help-section">
          <h3>项目详情</h3>
          <p>详情页展示 AI 提取的全部字段，包括：</p>
          <ul>
            <li>基本信息（项目名称、编号、招标代理机构、发包单位）</li>
            <li>时间信息（开标时间、质疑截止时间）</li>
            <li>价格信息（拦标价、下限价、投标保证金、履约保证金）</li>
            <li>评标信息（专家组成、价格分计算方法、主观分详情）</li>
          </ul>
        </div>
      </a-tab-pane>

      <a-tab-pane key="prompt" title="Prompt 配置">
        <div class="help-section">
          <h3>什么是 Prompt</h3>
          <p>Prompt 是发送给 AI 模型的指令模板，影响提取效果。系统支持自定义 Prompt 模板。</p>
        </div>
        <div class="help-section">
          <h3>如何配置</h3>
          <ol>
            <li>点击侧边栏「Prompt 配置」</li>
            <li>修改模板内容（注意保留 {content} 和 {field_def} 占位符）</li>
            <li>点击保存</li>
          </ol>
        </div>
        <div class="help-section">
          <a-alert type="warning">
            <template #title>修改 Prompt 的影响</template>
            修改 Prompt 后，下次上传文件会使用新模板，已提取的项目不受影响
          </a-alert>
        </div>
      </a-tab-pane>

      <a-tab-pane key="account" title="账号说明">
        <div class="help-section">
          <h3>角色权限</h3>
          <a-table :columns="roleColumns" :data="roleData" size="small" :pagination="false" bordered />
        </div>
        <div class="help-section">
          <h3>退出登录</h3>
          <p>点击页面右上角的「退出」按钮即可安全退出。</p>
        </div>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup>
import { reactive } from 'vue'

const fieldColumns = [
  { title: '字段名', dataIndex: 'name', width: 150 },
  { title: '说明', dataIndex: 'desc' }
]
const fieldData = reactive([
  { name: 'projectName', desc: '项目名称' },
  { name: 'projectCode', desc: '项目编号 / 备案证明编号' },
  { name: 'biddingAgency', desc: '招标代理机构' },
  { name: 'clientUnit', desc: '发包单位 / 招标人' },
  { name: 'bidOpenTime', desc: '开标时间' },
  { name: 'complaintDeadline', desc: '质疑截止时间' },
  { name: 'ceilingPrice', desc: '拦标价（最高投标限价）' },
  { name: 'floorPrice', desc: '下限价（成本警戒线）' },
  { name: 'bidBond', desc: '投标保证金' },
  { name: 'performanceBond', desc: '履约保证金' },
  { name: 'contractPayment', desc: '合同付款方式' },
  { name: 'expertComposition', desc: '评标专家组成' },
  { name: 'priceScoreMethod', desc: '价格分计算方法' },
  { name: 'subjectiveScoreDetails', desc: '主观分（技术部分）详情' }
])

const roleColumns = [
  { title: '角色', dataIndex: 'role', width: 120 },
  { title: '权限', dataIndex: 'perm' }
]
const roleData = reactive([
  { role: '管理员', perm: '上传文件、删除项目、查看所有项目、用户管理、Prompt 配置' },
  { role: '普通用户', perm: '查看所有项目、下载文件、查看个人信息' }
])
</script>

<style scoped>
.help-container {
  padding: 24px;
  max-width: 900px;
}
.help-section {
  margin-bottom: 28px;
}
.help-section h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1d;
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid #165DFF;
}
.help-section p {
  color: #666;
  line-height: 1.8;
  margin-bottom: 8px;
}
.help-section ul {
  color: #666;
  line-height: 2;
  padding-left: 20px;
}
.help-section ol {
  color: #666;
  line-height: 2;
  padding-left: 20px;
}
</style>