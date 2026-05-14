<template>
  <DsModalForm
    :visible="visible"
    :title="isEdit ? '编辑用户' : '新增用户'"
    :description="isEdit ? '维护账号基础信息、部门归属和角色授权。' : '创建系统账号并分配部门、角色与初始状态。'"
    :width="760"
    @update:visible="$emit('update:visible', $event)"
    @cancel="$emit('cancel')"
  >
    <DsFormSection title="基础信息">
      <DsFormGrid>
        <DsReadonlyField
          v-if="isEdit"
          label="账号"
          :value="form.username"
          hint="账号创建后不可直接编辑"
        />
        <DsInput
          v-else
          :model-value="form.username"
          label="账号"
          type="text"
          placeholder="请输入登录账号"
          required
          :max-length="100"
          @update:model-value="updateField('username', $event)"
        />
        <DsInput
          :model-value="form.realName"
          label="姓名"
          type="text"
          placeholder="请输入姓名"
          required
          :max-length="50"
          :disabled="!canWriteField('realName')"
          @update:model-value="updateField('realName', $event)"
        />
        <DsInput
          :model-value="form.phone"
          label="手机号"
          type="phone"
          placeholder="请输入手机号"
          :disabled="!canWriteField('phone')"
          :max-length="11"
          @update:model-value="updateField('phone', $event)"
        />
        <DsInput
          :model-value="form.email"
          label="邮箱"
          type="email"
          placeholder="请输入邮箱"
          :disabled="!canWriteField('email')"
          :max-length="100"
          @update:model-value="updateField('email', $event)"
        />
        <DsInput
          :model-value="form.idCard"
          label="身份证号"
          type="idCard"
          placeholder="请输入身份证号"
          :disabled="!canWriteField('idCard')"
          :max-length="18"
          @update:model-value="updateField('idCard', $event)"
        />
        <DsPasswordInput
          v-if="!isEdit"
          :model-value="form.password"
          label="初始密码"
          :placeholder="passwordPlaceholder"
          :hint="passwordTip"
          required
          @update:model-value="updateField('password', $event)"
        />
        <DsDeptTreeSelect
          v-if="showDeptField && !readonlyDept"
          :model-value="form.deptId"
          label="所属部门"
          placeholder="请选择部门"
          hint="请选择用户所属组织，支持搜索和未分配状态。"
          include-unassigned
          :unassigned-value="null"
          :disabled="!canMoveDept"
          @update:model-value="updateField('deptId', $event)"
        />
        <DsReadonlyField
          v-else-if="showDeptField"
          label="所属部门"
          :value="readonlyDeptName"
          :hint="readonlyDeptHint"
        />
      </DsFormGrid>
    </DsFormSection>

    <DsFormSection title="权限信息" description="角色决定用户可访问的菜单、操作和数据范围。">
      <DsFormGrid :columns="1">
        <DsRoleMultiSelect
          :model-value="form.roleIds"
          label="角色"
          placeholder="请选择角色"
          :options="roleOptions"
          :max-visible-tags="99"
          required
          :disabled="!canAssignRoles"
          @update:model-value="updateField('roleIds', $event)"
        />
      </DsFormGrid>
    </DsFormSection>

    <DsFormSection title="账号状态">
      <DsFormGrid>
        <DsStatusSelect
          :model-value="form.status"
          label="状态"
          placeholder="请选择状态"
          :include-all="false"
          :allow-clear="false"
          :disabled="!canUpdateStatus"
          @update:model-value="updateField('status', $event)"
        />
        <DsSwitch
          :model-value="forcePasswordChangeValue"
          label="强制下次登录改密"
          hint="开启后用户下次登录后需修改密码"
          :disabled="!canWriteField('forcePasswordChange')"
          @update:model-value="updateField('forcePasswordChange', $event ? 1 : 0)"
        />
      </DsFormGrid>
    </DsFormSection>

    <DsFormSection v-if="showResetPassword" title="账号安全">
      <DsFormGrid :columns="1">
        <DsPasswordInput
          :model-value="form.password"
          label="重置密码"
          :placeholder="`留空则不修改密码，${passwordPolicyText}`"
          :disabled="!canResetPassword"
          :hint="passwordTip"
          @update:model-value="updateField('password', $event)"
        />
      </DsFormGrid>
    </DsFormSection>

    <template #footer>
      <DsFormActions :loading="loading" @cancel="$emit('cancel')" @submit="$emit('submit')" />
    </template>
  </DsModalForm>
</template>

<script setup>
import { computed } from 'vue'
import {
  DsDeptTreeSelect,
  DsFormActions,
  DsFormGrid,
  DsFormSection,
  DsInput,
  DsModalForm,
  DsPasswordInput,
  DsReadonlyField,
  DsRoleMultiSelect,
  DsStatusSelect,
  DsSwitch
} from '@/design-system'

const props = defineProps({
  visible: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  form: { type: Object, required: true },
  roleOptions: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  passwordTip: { type: String, default: '' },
  passwordPlaceholder: { type: String, default: '' },
  passwordPolicyText: { type: String, default: '' },
  showDeptField: { type: Boolean, default: true },
  readonlyDept: { type: Boolean, default: false },
  readonlyDeptName: { type: String, default: '' },
  readonlyDeptHint: { type: String, default: '' },
  canMoveDept: { type: Boolean, default: true },
  canAssignRoles: { type: Boolean, default: true },
  canUpdateStatus: { type: Boolean, default: true },
  canResetPassword: { type: Boolean, default: false },
  canWriteField: { type: Function, default: () => true }
})

const emit = defineEmits(['update:visible', 'update:field', 'cancel', 'submit'])

const isEdit = computed(() => props.mode === 'edit')
const showResetPassword = computed(() => isEdit.value && props.canResetPassword)
const forcePasswordChangeValue = computed(() => Number(props.form.forcePasswordChange || 0) === 1)

const updateField = (field, value) => {
  emit('update:field', { field, value })
}
</script>
