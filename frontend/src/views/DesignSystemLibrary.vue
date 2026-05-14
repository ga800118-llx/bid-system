<template>
  <div class="design-system-page">
    <section class="hero-card ds-card">
      <div>
        <a-breadcrumb class="hero-breadcrumb">
          <a-breadcrumb-item>系统管理</a-breadcrumb-item>
          <a-breadcrumb-item>基础平台 UI 组件库</a-breadcrumb-item>
        </a-breadcrumb>
        <h1 class="hero-title">基础平台 UI 组件库</h1>
        <p class="hero-description">
          统一沉淀文本框、下拉框、业务选择器、筛选查询区和状态展示规范，后续后台页面优先复用 design-system 组件，避免重复实现。
        </p>
      </div>
    </section>

    <section class="preview-grid">
      <article class="preview-card ds-card">
        <div class="preview-card__header">
          <h2>基础输入组件</h2>
          <p>统一高度、圆角、边框和提示文案。</p>
        </div>
        <FormComponentShowcase />
      </article>

      <article class="preview-card ds-card">
        <div class="preview-card__header">
          <h2>选择类组件</h2>
          <p>覆盖部门、人员、角色、字典、状态和文件分类选择场景。</p>
        </div>
        <DropdownComponentShowcase />
      </article>

      <article class="preview-card ds-card preview-card--full">
        <div class="preview-card__header">
          <h2>筛选查询区示例</h2>
          <p>列表页统一使用白底卡片、左侧筛选、右侧主操作。</p>
        </div>
        <FilterBarExample />
      </article>

      <article class="preview-card ds-card">
        <div class="preview-card__header">
          <h2>勾选与状态组件</h2>
          <p>统一角色状态、优先级、空状态和文件类型表达。</p>
        </div>
        <div class="status-demo">
          <DsStatusTag :value="1" :options="statusOptions" />
          <DsStatusTag :value="0" :options="statusOptions" />
          <DsRoleTag label="系统管理员" color="red" />
          <DsRoleTag label="普通用户" color="arcoblue" />
          <DsPriorityStars :value="4" />
        </div>
        <div class="icon-demo">
          <FileTypeIcon type="pdf" />
          <FileTypeIcon type="excel" />
          <FileTypeIcon type="word" />
          <FileTypeIcon type="image" />
          <FileTypeIcon type="other" />
        </div>
      </article>

      <article class="preview-card ds-card">
        <div class="preview-card__header">
          <h2>控件状态展示</h2>
          <p>禁用态、只读感和空结果状态统一可预期。</p>
        </div>
        <div class="state-demo">
          <DsInput model-value="可编辑输入框" label="默认状态" />
          <DsInput model-value="已禁用输入框" label="禁用状态" disabled />
          <DsDateRangePicker label="统计周期" />
        </div>
        <div class="empty-demo">
          <DsEmptyState title="暂无数据" description="当前筛选条件下没有可展示的数据。" />
        </div>
      </article>

      <article class="preview-card ds-card preview-card--full">
        <div class="preview-card__header">
          <h2>典型新增用户表单</h2>
          <p>新增用户、部门分配、角色选择、状态管理统一复用。</p>
        </div>
        <UserFormExample />
      </article>
    </section>
  </div>
</template>

<script setup>
import {
  DsDateRangePicker,
  DsEmptyState,
  DsInput,
  DsPriorityStars,
  DsRoleTag,
  DsStatusTag
} from '@/design-system'
import FileTypeIcon from '@/design-system/icons/FileTypeIcon.vue'
import DropdownComponentShowcase from '@/design-system/examples/DropdownComponentShowcase.vue'
import FilterBarExample from '@/design-system/examples/FilterBarExample.vue'
import FormComponentShowcase from '@/design-system/examples/FormComponentShowcase.vue'
import UserFormExample from '@/design-system/examples/UserFormExample.vue'

const statusOptions = [
  { value: 1, label: '启用', color: 'green' },
  { value: 0, label: '禁用', color: 'red' }
]
</script>

<style scoped>
.design-system-page {
  min-height: 100vh;
  background: var(--ds-color-bg-page);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-card,
.preview-card {
  padding: 22px 24px;
}

.hero-breadcrumb {
  margin-bottom: 10px;
}

.hero-title {
  margin: 0;
  color: var(--ds-color-text-primary);
  font-size: 28px;
  line-height: 1.2;
}

.hero-description {
  margin: 10px 0 0;
  max-width: 820px;
  color: var(--ds-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.preview-card--full {
  grid-column: 1 / -1;
}

.preview-card__header {
  margin-bottom: 16px;
}

.preview-card__header h2 {
  margin: 0;
  color: var(--ds-color-text-primary);
  font-size: 18px;
  line-height: 1.3;
}

.preview-card__header p {
  margin: 8px 0 0;
  color: var(--ds-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.status-demo,
.icon-demo {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.icon-demo {
  margin-top: 18px;
}

.state-demo {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.empty-demo {
  margin-top: 18px;
  border-top: 1px solid var(--ds-color-border);
  padding-top: 18px;
}

@media (max-width: 1080px) {
  .preview-grid,
  .state-demo {
    grid-template-columns: 1fr;
  }
}
</style>
