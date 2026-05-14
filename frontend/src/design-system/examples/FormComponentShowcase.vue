<template>
  <div class="example-grid">
    <DsInput v-model="form.name" label="文本输入" placeholder="请输入内容" required />
    <DsPasswordInput v-model="form.password" label="密码输入" placeholder="请输入密码" />
    <DsNumberInput v-model="form.number" label="数字输入" />
    <DsAmountInput v-model="form.amount" label="金额输入" />
    <DsTextarea v-model="form.remark" label="多行文本" />
    <DsSwitch v-model="form.enabled" label="启用状态" hint="禁用态将灰化展示" />
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { DsAmountInput, DsInput, DsNumberInput, DsPasswordInput, DsSwitch, DsTextarea } from '@/design-system'

const form = reactive({
  name: '',
  password: '',
  number: 5,
  amount: 1000,
  remark: '',
  enabled: true
})
</script>

<style scoped>
.example-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
@media (max-width: 960px) {
  .example-grid { grid-template-columns: 1fr; }
}
</style>
