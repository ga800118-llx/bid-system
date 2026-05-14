<template>
  <div class="design-system-page">
    <div class="design-system-page__inner">
      <div class="hero ds-card">
        <a-breadcrumb>
          <a-breadcrumb-item>系统管理</a-breadcrumb-item>
          <a-breadcrumb-item>基础平台 UI 组件库</a-breadcrumb-item>
        </a-breadcrumb>
        <h1>基础平台 UI 组件库</h1>
        <p>沉淀基础表单、选择器、筛选区和状态展示规范，后续后台页面统一复用。</p>
      </div>

      <section class="example-section ds-card">
        <h2>基础输入组件</h2>
        <FormComponentShowcase />
      </section>

      <section class="example-section ds-card">
        <h2>选择类组件</h2>
        <DropdownComponentShowcase />
      </section>

      <section class="example-section ds-card">
        <h2>典型新增用户表单</h2>
        <UserFormExample />
      </section>

      <section class="example-section ds-card">
        <h2>筛选查询区示例</h2>
        <FilterBarExample />
      </section>
    </div>
  </div>
</template>

<script setup>
import FilterBarExample from '@/design-system/examples/FilterBarExample.vue'
import DropdownComponentShowcase from '@/design-system/examples/DropdownComponentShowcase.vue'
import FormComponentShowcase from '@/design-system/examples/FormComponentShowcase.vue'
import UserFormExample from '@/design-system/examples/UserFormExample.vue'
</script>

<style scoped>
.design-system-page {
  min-height: 100vh;
  background: var(--ds-color-bg-page);
}
.design-system-page__inner {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.hero,
.example-section {
  padding: 20px 24px;
}
.hero h1 {
  margin: 16px 0 0;
  color: var(--ds-color-text-primary);
  font-size: 28px;
  line-height: 1.2;
}
.hero p {
  margin: 10px 0 0;
  color: var(--ds-color-text-regular);
  font-size: 14px;
  line-height: 1.7;
}
.example-section h2 {
  margin: 0 0 16px;
  color: var(--ds-color-text-primary);
  font-size: 18px;
  line-height: 1.3;
}
</style>
