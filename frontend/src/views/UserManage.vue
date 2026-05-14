<template>
  <div class="user-page">
    <div class="main">
      <DsPageHeader
        :breadcrumb="['工作台', '系统管理', '用户管理']"
        title="用户管理"
        description="管理系统用户账号、部门归属、角色分配及账号安全状态，保障系统安全与规范使用。"
      >
        <template #icon>
          <IconLock />
        </template>
        <template #actions>
          <DsHeaderActions
            :tool-items="headerToolItems"
            :show-primary="canCreateUserButton"
            primary-label="新增用户"
            @tool-select="handleHeaderToolSelect"
            @primary-click="openAddModal"
          >
            <template #primaryIcon>
              <IconPlus />
            </template>
          </DsHeaderActions>
        </template>
        <template #meta>
          <span>{{ total }} 个用户</span>
          <span>{{ headerActiveCount }} 个启用</span>
          <span>{{ headerRiskCount }} 个风险账号</span>
        </template>
      </DsPageHeader>

      <section class="summary-grid">
        <template v-for="card in summaryCards" :key="card.key">
          <DsSecuritySummaryCard
            v-if="card.key === 'security'"
            :label="card.label"
            hint=""
            :items="securityAlertItems"
          >
            <template #icon>
              <component :is="card.icon" />
            </template>
          </DsSecuritySummaryCard>
          <DsStatsCard
            v-else
            :label="card.label"
            :value="card.value"
            :hint="card.hint"
            :tone="card.tone"
          >
            <template #icon>
              <component :is="card.icon" />
            </template>
          </DsStatsCard>
        </template>
      </section>

      <DsFilterBar title="筛选查询" description="按账号状态、部门归属和角色范围快速定位需要处理的用户。">
        <template #title>
          <span class="filter-title">
            <span class="filter-title__icon">
              <IconFilter />
            </span>
            <span>筛选查询</span>
          </span>
        </template>
        <div class="filter-fields">
          <DsKeywordSearch
            v-model="filters.keyword"
            class="filter-item filter-item--keyword"
            label="关键词"
            placeholder="搜索姓名 / 账号 / 手机号"
            @search="handleSearch"
          />
          <DsDeptTreeSelect
            v-model="filters.deptId"
            class="filter-item filter-item--dept"
            label="所属部门"
            placeholder="全部部门"
            include-all
            include-unassigned
          />
          <DsStatusSelect
            v-model="filters.status"
            class="filter-item filter-item--status"
            label="状态"
            placeholder="全部状态"
          />
          <DsRoleMultiSelect
            v-model="filters.role"
            class="filter-item filter-item--role"
            label="角色"
            placeholder="全部角色"
            :multiple="false"
            :options="roleFilterOptions"
          />
          <a-button type="text" class="filter-expand-button filter-expand-button--inline" @click="showAdvancedFilters = !showAdvancedFilters">
            {{ showAdvancedFilters ? '收起更多' : '展开更多' }}
            <template #icon><IconDown :class="{ 'filter-expand-button__icon--expanded': showAdvancedFilters }" /></template>
          </a-button>
        </div>
        <template #actions>
          <div class="filter-actions">
            <a-button type="primary" class="action-button" @click="handleSearch">查询</a-button>
            <a-button class="action-button" @click="handleResetFilters">重置</a-button>
          </div>
        </template>
      </DsFilterBar>
      <DsAdvancedFilterPanel v-if="showAdvancedFilters" class="advanced-filter-panel">
        <div class="advanced-filter-fields">
          <div class="advanced-filter-item">
            <label>安全状态</label>
            <a-select placeholder="开发中，暂不参与查询" disabled allow-clear>
              <a-option value="normal">无异常</a-option>
              <a-option value="failed">失败次数较多</a-option>
              <a-option value="locked">已锁定</a-option>
            </a-select>
          </div>
          <div class="advanced-filter-item">
            <label>最近登录时间</label>
            <a-range-picker disabled />
          </div>
          <div class="advanced-filter-item">
            <label>创建时间</label>
            <a-range-picker disabled />
          </div>
          <div class="advanced-filter-item">
            <label>组织归属</label>
            <a-select placeholder="开发中，暂不参与查询" disabled>
              <a-option value="assigned">已分配部门</a-option>
              <a-option value="unassigned">未分配部门</a-option>
            </a-select>
          </div>
        </div>
        <div class="advanced-filter-placeholder">
          <span>更多筛选字段已预留为统一扩展位，本轮仅展示容器样式，不提交额外查询参数。</span>
        </div>
      </DsAdvancedFilterPanel>

      <section class="table-card card-shell">
        <div class="section-header section-header--table">
          <div class="table-header-copy">
            <h2 class="section-title">用户列表</h2>
            <p class="section-description">共 {{ total }} 条，支持分页查询、批量处理和权限控制下的安全维护。</p>
          </div>
          <DsActionBar
            class="table-header-tools"
            :selection-text="selectedUserIds.length > 0 ? `已选择 ${selectedUserIds.length} 人` : '未选择用户'"
            :selection-active="selectedUserIds.length > 0"
          >
            <template #batch>
              <DsBatchActions
                v-if="canUpdateStatusField || canMoveDeptField"
                :selected-count="selectedUserIds.length"
                :actions="batchActionItems"
                @action="handleBatchAction"
              />
            </template>
            <template #tools>
              <DsIconButton tooltip="刷新列表" @click="fetchUsers">
                <IconRefresh />
              </DsIconButton>
              <DsIconButton tooltip="列设置待补充" disabled>
                <IconSettings />
              </DsIconButton>
            </template>
          </DsActionBar>
        </div>

        <DsDataTable
          :columns="columns"
          :data="tableData"
          :loading="loading"
          :selected-row-keys="selectedUserIds"
          row-key="id"
          :scroll="{ x: tableScrollX }"
          :resizable="true"
          :show-pagination="true"
          :pagination="paginationConfig"
          @select="handleUserSelect"
          @select-all="handleUserSelectAll"
          @pagination-change="handlePageChange"
          @page-size-change="handleSizeChange"
        >
          <template #identityHeader>
            <input
              type="checkbox"
              class="table-checkbox"
              :checked="isCurrentPageSelected"
              :class="{ 'table-checkbox--indeterminate': isCurrentPageIndeterminate }"
              :disabled="tableData.length === 0"
              @change="event => handleUserSelectAll(event.target.checked, tableData)"
            >
            <span>姓名 / 账号</span>
          </template>
          <template #identity="{ record }">
            <div class="identity-cell">
              <input
                type="checkbox"
                class="table-checkbox"
                :checked="selectedUserIds.includes(record.id)"
                @change="event => handleUserSelect(record, event.target.checked)"
              >
              <DsUserCell
                :name="canReadUserField('realName') ? record.realName || '' : ''"
                :account="canReadUserField('username') ? record.username || '' : ''"
                :reveal-account="canReadUserField('realName') && canReadUserField('username')"
              />
            </div>
          </template>
          <template #contact="{ record }">
            <div class="compact-cell">
              <span v-if="canReadUserField('phone')" class="compact-cell__primary">{{ record.phone || '-' }}</span>
              <span v-if="canReadUserField('email')" class="compact-cell__secondary">{{ record.email || '-' }}</span>
            </div>
          </template>
          <template #idCard="{ record }">
            <span class="created-at-text">{{ record.idCard || '-' }}</span>
          </template>
          <template #dept="{ record }">
            <DsStatusTag v-if="!record.deptName" label="未分配" tone="warning" />
            <span v-else>{{ record.deptName }}</span>
          </template>
          <template #role="{ record }">
            <a-tooltip :content="fullRoleText(record)" :disabled="displayRoleNames(record).length <= 1">
              <div class="role-cell">
                <DsRoleTag :label="primaryRoleName(record)" :color="displayRoleColor(record)" />
                <span v-if="displayRoleNames(record).length > 1" class="role-more">
                  +{{ displayRoleNames(record).length - 1 }}
                </span>
              </div>
            </a-tooltip>
          </template>
          <template #status="{ record }">
            <DsStatusTag :value="record.status" :options="statusOptions" />
          </template>
          <template #forcePasswordChange="{ record }">
            <DsStatusTag
              :label="Number(record.forcePasswordChange || 0) === 1 ? '需改密' : '正常'"
              :tone="Number(record.forcePasswordChange || 0) === 1 ? 'warning' : 'default'"
            />
          </template>
          <template #loginSecurity="{ record }">
            <DsSecurityStatus
              :failed-count="record.failedLoginCount"
              :locked="Number(record.status) === 0 && Number(record.failedLoginCount || 0) > 0"
              :label="loginSecuritySummary(record)"
              :last-failed-at="record.lastFailedLoginAt ? formatTime(record.lastFailedLoginAt) : '无失败记录'"
              :last-login-at="record.lastLoginAt ? formatTime(record.lastLoginAt) : '-'"
            />
          </template>
          <template #lastLoginIp="{ record }">
            <span class="created-at-text">{{ record.lastLoginIp || '-' }}</span>
          </template>
          <template #passwordUpdatedAt="{ record }">
            <a-tooltip :content="formatTime(record.passwordUpdatedAt)">
              <span class="created-at-text">{{ formatCompactCreatedAt(record.passwordUpdatedAt) }}</span>
            </a-tooltip>
          </template>
          <template #lastLoginAt="{ record }">
            <a-tooltip :content="formatTime(record.lastLoginAt)">
              <span class="created-at-text">{{ formatCompactLastLogin(record.lastLoginAt) }}</span>
            </a-tooltip>
          </template>
          <template #createdAt="{ record }">
            <a-tooltip :content="formatTime(record.createdAt)">
              <span class="created-at-text">{{ formatCompactCreatedAt(record.createdAt) }}</span>
            </a-tooltip>
          </template>
          <template #actions="{ record }">
            <div class="row-actions">
              <a-button v-if="canEditUserFields" type="text" size="small" @click="openEditModal(record)">编辑</a-button>
              <a-dropdown v-if="hasMoreRowActions">
                <a-button type="text" size="small">
                  更多
                  <template #icon><IconDown /></template>
                </a-button>
                <template #content>
                  <a-doption v-if="canResetPassword" @click="openPasswordModal(record)">重置密码</a-doption>
                  <a-doption
                    v-if="canUpdateStatusField && canReadUserField('status')"
                    @click="handleSingleStatus(record)"
                  >
                    {{ record.status === 1 ? '禁用用户' : '启用用户' }}
                  </a-doption>
                </template>
              </a-dropdown>
              <span v-if="!hasAnyRowActions" class="no-actions">无可用操作</span>
            </div>
          </template>
          <template #empty>
            <DsEmptyState title="暂无用户数据" description="当前筛选条件下没有可展示的用户账号。" />
          </template>
        </DsDataTable>
      </section>

      <section class="principle-strip">
        <span>清晰</span>
        <span>简洁</span>
        <span>高效</span>
        <span>一致</span>
      </section>
    </div>

    <UserFormModal
      v-model:visible="userModalVisible"
      :mode="isEdit ? 'edit' : 'create'"
      :form="userForm"
      :loading="userSubmitting"
      :password-tip="passwordTip"
      :password-placeholder="passwordPlaceholder"
      :password-policy-text="passwordPolicyText(passwordPolicy)"
      :can-move-dept="canMoveDeptField"
      :can-assign-roles="canAssignRolesField"
      :can-update-status="canUpdateStatusField"
      :can-reset-password="false"
      :can-write-field="canWriteUserField"
      @update:field="handleUserFormFieldUpdate"
      @cancel="closeUserModal"
      @submit="submitUserForm"
    />

    <DsModalForm
      v-model:visible="passwordModalVisible"
      title="重置密码"
      description="为指定用户设置新的登录密码。"
      :width="560"
      @cancel="closePasswordModal"
    >
      <DsFormSection title="账号信息">
        <DsFormGrid>
          <DsReadonlyField label="姓名" :value="passwordForm.realName" />
          <DsReadonlyField label="账号" :value="passwordForm.username" />
        </DsFormGrid>
      </DsFormSection>
      <DsFormSection title="密码设置">
        <DsFormGrid :columns="1">
          <DsPasswordInput
            v-model="passwordForm.password"
            label="新密码"
            :placeholder="passwordPlaceholder"
            :hint="passwordTip"
            required
          />
          <DsSwitch
            v-model="passwordForceChangeModel"
            label="强制下次登录改密"
            hint="建议重置密码后开启，确保用户自行修改新密码"
            :disabled="!canWriteUserField('forcePasswordChange')"
          />
        </DsFormGrid>
      </DsFormSection>
      <template #footer>
        <DsFormActions :loading="passwordSubmitting" @cancel="closePasswordModal" @submit="submitPasswordForm" />
      </template>
    </DsModalForm>

    <a-modal
      v-model:visible="moveModalVisible"
      title="调整部门"
      :width="420"
      @before-ok="handleMoveOk"
      @cancel="moveModalVisible = false"
    >
      <a-form layout="vertical">
        <a-form-item label="目标部门">
          <DsDeptTreeSelect v-model="targetDeptId" placeholder="请选择目标部门" include-unassigned :unassigned-value="null" />
        </a-form-item>
        <div class="form-tip">将移动 {{ movingUserIds.length }} 个用户。</div>
      </a-form>
    </a-modal>

    <a-modal
      v-model:visible="importResultVisible"
      title="用户导入结果"
      :width="560"
      :footer="false"
      @cancel="importResultVisible = false"
    >
      <div v-if="importResult" class="batch-action-bar import-result-box">
        <span>总行数 {{ importResult.totalCount || 0 }}</span>
        <span>成功 {{ importResult.successCount || 0 }}</span>
        <span>跳过 {{ importResult.skippedCount || 0 }}</span>
        <span>失败 {{ importResult.failureCount || 0 }}</span>
      </div>
      <div v-if="importResult?.message" class="form-tip import-result-text">{{ importResult.message }}</div>
      <div v-if="importResult?.details?.length" class="import-result-details">
        <div class="form-tip">明细</div>
        <ul>
          <li v-for="item in importResult.details" :key="item">{{ item }}</li>
        </ul>
      </div>
    </a-modal>

    <input ref="importFileInput" type="file" accept=".xlsx" class="hidden-file-input" @change="handleImportFileChange" />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  IconCheckCircle,
  IconDown,
  IconExclamationCircle,
  IconFilter,
  IconLock,
  IconPlus,
  IconQuestion,
  IconRefresh,
  IconSettings,
  IconUserGroup
} from '@arco-design/web-vue/es/icon'
import { deptApi, roleApi, userApi } from '@/api'
import UserFormModal from '@/components/user/UserFormModal.vue'
import { validateUserFormFields } from '@/components/user/userFormValidation'
import { hasPermission } from '@/utils/permission'
import {
  DEFAULT_PASSWORD_MIN_LENGTH,
  DEFAULT_PAGE_SIZE_OPTIONS,
  applyPaginationConfig,
  applyPasswordPolicy,
  passwordPolicyText,
  validatePasswordByPolicy
} from '@/utils/systemConfig'
import { loadCommonStatusOptions, loadUserRoleTypeOptions, roleTypeColor, roleTypeLabel } from '@/utils/dict'
import {
  DsActionBar,
  DsAdvancedFilterPanel,
  DsBatchActions,
  DsDataTable,
  DsDeptTreeSelect,
  DsEmptyState,
  DsFilterBar,
  DsFormActions,
  DsHeaderActions,
  DsIconButton,
  DsKeywordSearch,
  DsModalForm,
  DsPasswordInput,
  DsPageHeader,
  DsReadonlyField,
  DsRoleTag,
  DsSecuritySummaryCard,
  DsSecurityStatus,
  DsStatsCard,
  DsStatusSelect,
  DsStatusTag,
  DsSwitch,
  DsUserCell
} from '@/design-system'

const loading = ref(false)
const tableData = ref([])
const treeData = ref([])
const roleOptions = ref([])
const page = ref(1)
const size = ref(20)
const pageSizeOptions = ref([...DEFAULT_PAGE_SIZE_OPTIONS])
const total = ref(0)
const selectedUserIds = ref([])
const readableFields = ref(['username', 'realName', 'phone', 'idCard', 'email', 'dept', 'roles', 'status', 'forcePasswordChange', 'loginSecurity', 'lastLoginIp', 'passwordUpdatedAt'])
const writableFields = ref(['username', 'realName', 'phone', 'idCard', 'email', 'dept', 'roles', 'status', 'forcePasswordChange'])
const passwordPolicy = ref({ minLength: DEFAULT_PASSWORD_MIN_LENGTH, requireStrong: false })
const statusOptions = ref([])
const roleTypeOptions = ref([])
const userModalVisible = ref(false)
const userSubmitting = ref(false)
const passwordModalVisible = ref(false)
const passwordSubmitting = ref(false)
const moveModalVisible = ref(false)
const importResultVisible = ref(false)
const importResult = ref(null)
const importFileInput = ref(null)
const isEdit = ref(false)
const showAdvancedFilters = ref(false)
const movingUserIds = ref([])
const targetDeptId = ref(null)
const editOriginal = ref(null)

const filters = reactive({ keyword: '', deptId: '', status: '', role: '' })
const userForm = reactive({ id: null, username: '', realName: '', phone: '', idCard: '', email: '', password: '', role: 'user', roleIds: [], deptId: null, status: 1, forcePasswordChange: 0 })
const passwordForm = reactive({ id: null, username: '', realName: '', password: '', forcePasswordChange: 1 })

const baseColumns = [
  { titleSlotName: 'identityHeader', slotName: 'identity', width: 320, fixed: 'left', fieldCodes: ['realName', 'username'] },
  { title: '联系方式', slotName: 'contact', width: 210, fieldCodes: ['phone', 'email'] },
  { title: '身份证号', slotName: 'idCard', width: 170, fieldCode: 'idCard' },
  { title: '所属部门', slotName: 'dept', width: 160, fieldCode: 'dept' },
  { title: '角色', slotName: 'role', width: 190, fieldCode: 'roles' },
  { title: '状态', slotName: 'status', width: 112, fieldCode: 'status' },
  { title: '强制改密', slotName: 'forcePasswordChange', width: 124, fieldCode: 'forcePasswordChange' },
  { title: '安全状态', slotName: 'loginSecurity', width: 148, fieldCode: 'loginSecurity' },
  { title: '最近登录 IP', slotName: 'lastLoginIp', width: 140, fieldCode: 'lastLoginIp' },
  { title: '密码更新时间', slotName: 'passwordUpdatedAt', width: 150, fieldCode: 'passwordUpdatedAt' },
  { title: '最近登录', slotName: 'lastLoginAt', width: 150, fieldCode: 'loginSecurity' },
  { title: '创建时间', slotName: 'createdAt', width: 150 },
  { title: '操作', slotName: 'actions', width: 140, fixed: 'right' }
]

const roleFilterOptions = computed(() => roleOptions.value.map(item => ({
  value: item.id,
  label: item.roleName,
  description: item.description || item.roleCode || ''
})))
const canCreateUser = computed(() => hasPermission('system_user:create'))
const canImportUser = computed(() => hasPermission('system_user:import'))
const canExportUser = computed(() => hasPermission('system_user:export'))
const canUpdateUser = computed(() => hasPermission('system_user:update'))
const canUpdateStatus = computed(() => hasPermission('system_user:status'))
const canResetPassword = computed(() => hasPermission('system_user:password'))
const canMoveDept = computed(() => hasPermission('system_user:dept'))
const canAssignRoles = computed(() => hasPermission('system_user:roles'))
const canCreateUserButton = computed(() => canCreateUser.value && canWriteUserField('username') && canWriteUserField('realName'))
const canMoveDeptField = computed(() => canMoveDept.value && canWriteUserField('dept'))
const canAssignRolesField = computed(() => canAssignRoles.value && canWriteUserField('roles'))
const canUpdateStatusField = computed(() => canUpdateStatus.value && canWriteUserField('status'))
const canUpdateRealNameField = computed(() => canUpdateUser.value && canWriteUserField('realName'))
const canUpdateContactFields = computed(() => canUpdateUser.value && (canWriteUserField('phone') || canWriteUserField('idCard') || canWriteUserField('email') || canWriteUserField('forcePasswordChange')))
const canEditUserFields = computed(() => canUpdateRealNameField.value || canUpdateContactFields.value || canAssignRolesField.value || canMoveDeptField.value || canUpdateStatusField.value)
const showToolActions = computed(() => canImportUser.value || canExportUser.value)
const headerToolItems = computed(() => ([
  { key: 'template', label: '下载模板', visible: canImportUser.value },
  { key: 'import', label: '导入用户', visible: canImportUser.value },
  { key: 'export', label: '导出用户', visible: canExportUser.value }
]))
const hasMoreRowActions = computed(() => canResetPassword.value || (canUpdateStatusField.value && canReadUserField('status')))
const hasAnyRowActions = computed(() => canEditUserFields.value || hasMoreRowActions.value)
const batchActionItems = computed(() => ([
  { key: 'enable', label: '批量启用', visible: canUpdateStatusField.value },
  { key: 'disable', label: '批量禁用', visible: canUpdateStatusField.value, danger: true },
  { key: 'move', label: '调整部门', visible: canMoveDeptField.value }
]))
const passwordTip = computed(() => `密码${passwordPolicyText(passwordPolicy.value)}`)
const passwordPlaceholder = computed(() => `请输入密码，${passwordPolicyText(passwordPolicy.value)}`)
const passwordForceChangeModel = computed({
  get: () => Number(passwordForm.forcePasswordChange || 0) === 1,
  set: (value) => {
    passwordForm.forcePasswordChange = value ? 1 : 0
  }
})
const columns = computed(() => baseColumns.filter((column) => {
  if (column.fieldCodes?.length) {
    return column.fieldCodes.some(code => readableFields.value.includes(code))
  }
  return !column.fieldCode || readableFields.value.includes(column.fieldCode)
}))
const tableScrollX = computed(() => columns.value.reduce((sum, column) => sum + Number(column.width || 120), 0))
const currentPageIds = computed(() => tableData.value.map(item => item.id).filter(id => id !== undefined && id !== null))
const isCurrentPageSelected = computed(() => currentPageIds.value.length > 0 && currentPageIds.value.every(id => selectedUserIds.value.includes(id)))
const isCurrentPageIndeterminate = computed(() => {
  const selectedCount = currentPageIds.value.filter(id => selectedUserIds.value.includes(id)).length
  return selectedCount > 0 && selectedCount < currentPageIds.value.length
})
const paginationConfig = computed(() => ({
  current: page.value,
  pageSize: size.value,
  total: total.value,
  pageSizeOptions: pageSizeOptions.value,
  compact: false
}))
const canReadUserField = (fieldCode) => readableFields.value.includes(fieldCode)
const canWriteUserField = (fieldCode) => writableFields.value.includes(fieldCode)
const handleUserFormFieldUpdate = ({ field, value }) => {
  if (field in userForm) userForm[field] = value
}
const summaryCards = computed(() => {
  const rows = tableData.value || []
  const activeCount = rows.filter(item => Number(item.status) === 1).length
  const disabledCount = rows.filter(item => Number(item.status) === 0).length
  const unassignedCount = rows.filter(item => !item.deptId).length
  const failedCount = rows.filter(item => Number(item.failedLoginCount || 0) > 0).length
  const staleCount = rows.filter(item => isLongInactive(item)).length
  const securityCount = rows.filter(item => hasLoginSecurityRisk(item) || isLongInactive(item)).length
  return [
    { key: 'total', label: '用户总数', value: total.value, hint: '当前查询结果总量', tone: 'primary', icon: IconUserGroup },
    { key: 'active', label: '正常用户', value: activeCount, hint: '当前页启用账号', tone: 'success', icon: IconCheckCircle },
    { key: 'disabled', label: '禁用用户', value: disabledCount, hint: '当前页已停用账号', tone: 'danger', icon: IconLock },
    { key: 'unassigned', label: '未分配部门', value: unassignedCount, hint: '仍需归属组织的账号', tone: 'warning', icon: IconQuestion },
    {
      key: 'security',
      label: '安全提醒',
      value: securityCount,
      hint: securityCount > 0 ? `失败登录 ${failedCount} 项 / 久未登录 ${staleCount} 项` : '当前页暂无异常提醒',
      tone: 'alert',
      icon: IconExclamationCircle
    }
  ]
})
const securityAlertSummary = computed(() => {
  const rows = tableData.value || []
  return {
    failedOverLimit: rows.filter(item => Number(item.failedLoginCount || 0) >= 5).length,
    longInactive: rows.filter(item => isLongInactive(item)).length
  }
})
const securityAlertItems = computed(() => ([
  { label: '连续失败登录 ≥ 5 次', value: `${securityAlertSummary.value.failedOverLimit} 人` },
  { label: '90 天未登录用户', value: `${securityAlertSummary.value.longInactive} 人` }
]))
const headerActiveCount = computed(() => (tableData.value || []).filter(item => Number(item.status) === 1).length)
const headerRiskCount = computed(() => (tableData.value || []).filter(item => hasLoginSecurityRisk(item) || isLongInactive(item)).length)

const fetchTree = async () => {
  try {
    const res = await deptApi.tree()
    treeData.value = res.code === 200 ? (res.data || []) : []
  } catch {
    treeData.value = []
  }
}

const fetchRoles = async () => {
  try {
    const res = await roleApi.list()
    roleOptions.value = res.code === 200 ? (res.data || []).filter(role => role.status !== 0) : []
  } catch {
    roleOptions.value = []
  }
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      keyword: filters.keyword?.trim() || undefined,
      deptId: filters.deptId || undefined,
      status: filters.status === '' ? undefined : filters.status,
      roleId: filters.role || undefined
    }
    const res = await userApi.systemList(params)
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
      readableFields.value = Array.isArray(res.data?.readableFields) ? res.data.readableFields : readableFields.value
      writableFields.value = Array.isArray(res.data?.writableFields) ? res.data.writableFields : writableFields.value
      clearSelection()
    } else {
      Message.error(res.msg || '加载用户失败')
      tableData.value = []
    }
  } catch {
    Message.error('加载用户失败')
    tableData.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchUsers()
}

const handleResetFilters = () => {
  filters.keyword = ''
  filters.deptId = ''
  filters.status = ''
  filters.role = ''
  page.value = 1
  fetchUsers()
}

const handlePageChange = (nextPage) => {
  page.value = nextPage
  fetchUsers()
}

const handleSizeChange = (nextSize) => {
  if (nextSize) {
    size.value = Number(nextSize)
  }
  page.value = 1
  fetchUsers()
}

const handleExport = async () => {
  try {
    const params = {
      keyword: filters.keyword?.trim() || undefined,
      deptId: filters.deptId || undefined,
      status: filters.status === '' ? undefined : filters.status,
      roleId: filters.role || undefined
    }
    const blob = await userApi.systemExport(params)
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = `用户列表_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(objectUrl)
    Message.success('用户列表已开始导出')
  } catch {
    Message.error('导出失败')
  }
}

const handleHeaderToolSelect = (key) => {
  if (key === 'template') {
    downloadTemplate()
    return
  }
  if (key === 'import') {
    triggerImport()
    return
  }
  if (key === 'export') {
    handleExport()
  }
}

const downloadTemplate = async () => {
  try {
    const blob = await userApi.systemDownloadTemplate()
    downloadBlob(blob, '用户导入模板.xlsx')
    Message.success('模板已下载')
  } catch {
    Message.error('下载模板失败')
  }
}

const triggerImport = () => importFileInput.value?.click()

const refreshRuntimeConfig = async () => {
  await Promise.all([
    applyPaginationConfig(size, pageSizeOptions),
    applyPasswordPolicy(passwordPolicy),
    loadStatusOptions(),
    loadRoleTypeOptions()
  ])
  page.value = 1
  await fetchTree()
  await fetchUsers()
}

const handleImportFileChange = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await userApi.systemImport(formData)
    if (res.code !== 200) {
      Message.error(res.msg || '导入失败')
      return
    }
    importResult.value = res.data || {}
    importResultVisible.value = true
    Message.success(res.data?.message || '导入完成')
    await fetchUsers()
    await fetchTree()
  } catch {
    Message.error('导入失败')
  }
}

const loadStatusOptions = async () => {
  statusOptions.value = await loadCommonStatusOptions()
}

const loadRoleTypeOptions = async () => {
  roleTypeOptions.value = await loadUserRoleTypeOptions()
}

const openAddModal = () => {
  isEdit.value = false
  editOriginal.value = null
  Object.assign(userForm, { id: null, username: '', realName: '', phone: '', idCard: '', email: '', password: '', role: 'user', roleIds: defaultRoleIds(), deptId: null, status: 1, forcePasswordChange: 0 })
  userModalVisible.value = true
}

const openEditModal = (record) => {
  isEdit.value = true
  Object.assign(userForm, {
    id: record.id,
    username: record.username,
    realName: record.realName || '',
    phone: record.phone || '',
    idCard: record.idCard || '',
    email: record.email || '',
    password: '',
    role: record.role === 'admin' ? 'admin' : 'user',
    roleIds: record.roleIds?.length ? [...record.roleIds] : fallbackRoleIds(record.role),
    deptId: record.deptId || null,
    status: record.status === 0 ? 0 : 1,
    forcePasswordChange: Number(record.forcePasswordChange || 0) === 1 ? 1 : 0
  })
  editOriginal.value = {
    realName: userForm.realName,
    phone: userForm.phone,
    idCard: userForm.idCard,
    email: userForm.email,
    roleIds: [...userForm.roleIds],
    deptId: userForm.deptId,
    status: userForm.status,
    forcePasswordChange: userForm.forcePasswordChange
  }
  userModalVisible.value = true
}

const closeUserModal = () => {
  if (userSubmitting.value) return
  userModalVisible.value = false
}

const submitUserForm = async () => {
  if (userSubmitting.value) return
  userSubmitting.value = true
  await new Promise(resolve => {
    handleUserOk((ok = true) => resolve(ok))
  })
  userSubmitting.value = false
}

const handleUserOk = async (done) => {
  const formError = validateUserFormFields({
    form: userForm,
    isEdit: isEdit.value,
    validateRealName: !isEdit.value || canUpdateRealNameField.value,
    canWriteField: canWriteUserField
  })
  if (formError) {
    Message.error(formError)
    done(false)
    return
  }
  const createPasswordError = !isEdit.value ? validatePasswordByPolicy(userForm.password, passwordPolicy.value) : ''
  if (createPasswordError) {
    Message.error(createPasswordError)
    done(false)
    return
  }
  if (canAssignRolesField.value && (!userForm.roleIds || userForm.roleIds.length === 0)) {
    Message.error('请至少选择一个角色')
    done(false)
    return
  }
  try {
    if (isEdit.value) {
      await saveEditedUser()
    } else {
      const roleIds = canAssignRolesField.value ? userForm.roleIds : defaultRoleIds()
      const payload = {
        username: userForm.username,
        realName: userForm.realName,
        role: resolveLegacyRole(roleIds),
        status: canUpdateStatusField.value ? userForm.status : 1
      }
      if (canWriteUserField('phone')) payload.phone = userForm.phone
      if (canWriteUserField('idCard')) payload.idCard = userForm.idCard
      if (canWriteUserField('email')) payload.email = userForm.email
      if (canWriteUserField('forcePasswordChange')) payload.forcePasswordChange = userForm.forcePasswordChange
      if (canAssignRolesField.value) payload.roleIds = roleIds
      if (canMoveDeptField.value) payload.deptId = userForm.deptId
      const res = await userApi.systemAdd({ ...payload, password: userForm.password })
      if (res.code !== 200) {
        Message.error(res.msg || '操作失败')
        done(false)
        return
      }
    }
    Message.success(isEdit.value ? '用户已更新' : '用户已新增')
    userModalVisible.value = false
    await fetchUsers()
    await fetchTree()
    notifyPermissionsUpdated()
  } catch (e) {
    Message.error(e.message || '操作失败')
    done(false)
    return
  }
  done()
}

const saveEditedUser = async () => {
  const original = editOriginal.value || {}
  const profilePayload = {}
  if (canUpdateRealNameField.value && userForm.realName !== original.realName) profilePayload.realName = userForm.realName
  if (canWriteUserField('phone') && userForm.phone !== original.phone) profilePayload.phone = userForm.phone
  if (canWriteUserField('idCard') && userForm.idCard !== original.idCard) profilePayload.idCard = userForm.idCard
  if (canWriteUserField('email') && userForm.email !== original.email) profilePayload.email = userForm.email
  if (canWriteUserField('forcePasswordChange') && userForm.forcePasswordChange !== original.forcePasswordChange) profilePayload.forcePasswordChange = userForm.forcePasswordChange
  if (Object.keys(profilePayload).length > 0) {
    const res = await userApi.systemUpdate(userForm.id, profilePayload)
    if (res.code !== 200) throw new Error(res.msg || '更新用户失败')
  }
  if (canAssignRolesField.value && roleIdsChanged(userForm.roleIds, original.roleIds || [])) {
    const res = await userApi.systemAssignRoles(userForm.id, userForm.roleIds)
    if (res.code !== 200) throw new Error(res.msg || '分配角色失败')
  }
  if (canMoveDeptField.value && userForm.deptId !== original.deptId) {
    const res = await userApi.systemMoveDept([userForm.id], userForm.deptId)
    if (res.code !== 200) throw new Error(res.msg || '调整部门失败')
  }
  if (canUpdateStatusField.value && userForm.status !== original.status) {
    const res = await userApi.systemUpdateStatus(userForm.id, userForm.status)
    if (res.code !== 200) throw new Error(res.msg || '更新状态失败')
  }
}

const roleIdsChanged = (next, prev) => {
  const a = [...(next || [])].sort().join(',')
  const b = [...(prev || [])].sort().join(',')
  return a !== b
}

const openPasswordModal = (record) => {
  Object.assign(passwordForm, { id: record.id, username: record.username, realName: record.realName || '-', password: '', forcePasswordChange: 1 })
  passwordModalVisible.value = true
}

const closePasswordModal = () => {
  if (passwordSubmitting.value) return
  passwordModalVisible.value = false
}

const submitPasswordForm = async () => {
  if (passwordSubmitting.value) return
  passwordSubmitting.value = true
  await new Promise(resolve => {
    handlePasswordOk((ok = true) => resolve(ok))
  })
  passwordSubmitting.value = false
}

const handlePasswordOk = async (done) => {
  const passwordError = validatePasswordByPolicy(passwordForm.password, passwordPolicy.value)
  if (passwordError) {
    Message.error(passwordError)
    done(false)
    return
  }
  try {
    const res = await userApi.systemUpdatePassword(passwordForm.id, passwordForm.password, passwordForm.forcePasswordChange)
    if (res.code === 200) {
      Message.success('密码已重置')
      passwordModalVisible.value = false
    } else {
      Message.error(res.msg || '重置失败')
      done(false)
      return
    }
  } catch {
    Message.error('重置失败')
    done(false)
    return
  }
  done()
}

const handleSingleStatus = async (record) => {
  const nextStatus = record.status === 1 ? 0 : 1
  try {
    const res = await userApi.systemUpdateStatus(record.id, nextStatus)
    if (res.code === 200) {
      Message.success(nextStatus === 1 ? '已启用' : '已禁用')
      await fetchUsers()
      await fetchTree()
    } else {
      Message.error(res.msg || '操作失败')
    }
  } catch {
    Message.error('操作失败')
  }
}

const handleBatchStatus = async (status) => {
  if (selectedUserIds.value.length === 0) {
    Message.warning('请先选择用户')
    return
  }
  try {
    const res = await userApi.systemBatchStatus(selectedUserIds.value, status)
    if (res.code === 200) {
      Message.success(status === 1 ? '已批量启用' : '已批量禁用')
      await fetchUsers()
      await fetchTree()
    } else {
      Message.error(res.msg || '批量操作失败')
    }
  } catch {
    Message.error('批量操作失败')
  }
}

const handleBatchAction = (key) => {
  if (key === 'enable') {
    handleBatchStatus(1)
    return
  }
  if (key === 'disable') {
    handleBatchStatus(0)
    return
  }
  if (key === 'move') {
    openMoveModal()
  }
}

const openMoveModal = (record) => {
  movingUserIds.value = record ? [record.id] : [...selectedUserIds.value]
  if (movingUserIds.value.length === 0) {
    Message.warning('请先选择用户')
    return
  }
  targetDeptId.value = record ? (record.deptId || null) : null
  moveModalVisible.value = true
}

const handleMoveOk = async (done) => {
  if (movingUserIds.value.length === 0) {
    Message.error('请先选择用户')
    done(false)
    return
  }
  try {
    const res = await userApi.systemMoveDept(movingUserIds.value, targetDeptId.value)
    if (res.code === 200) {
      Message.success('部门已调整')
      moveModalVisible.value = false
      await fetchUsers()
      await fetchTree()
    } else {
      Message.error(res.msg || '调整失败')
      done(false)
      return
    }
  } catch {
    Message.error('调整失败')
    done(false)
    return
  }
  done()
}

const handleUserSelect = (record, checked) => {
  if (checked) {
    if (!selectedUserIds.value.includes(record.id)) selectedUserIds.value.push(record.id)
  } else {
    selectedUserIds.value = selectedUserIds.value.filter(id => id !== record.id)
  }
}

const handleUserSelectAll = (selected, records) => {
  if (selected) {
    const ids = records.map(r => r.id).filter(id => !selectedUserIds.value.includes(id))
    selectedUserIds.value = [...selectedUserIds.value, ...ids]
  } else {
    const ids = records.map(r => r.id)
    selectedUserIds.value = selectedUserIds.value.filter(id => !ids.includes(id))
  }
}

const clearSelection = () => {
  selectedUserIds.value = []
}

const formatTime = (value) => {
  return value ? value.replace('T', ' ').substring(0, 16) : '-'
}

const formatCompactCreatedAt = (value) => {
  if (!value) return '-'
  const normalized = String(value).replace('T', ' ')
  const [datePart = '', timePart = ''] = normalized.split(' ')
  const currentYear = String(new Date().getFullYear())
  if (datePart.startsWith(currentYear + '-')) {
    return `${datePart.slice(5)} ${timePart.slice(0, 5)}`
  }
  return datePart
}

const formatCompactLastLogin = (value) => {
  if (!value) return '-'
  return formatCompactCreatedAt(value)
}

const primaryRoleName = (record) => {
  const names = displayRoleNames(record)
  return names[0] || '-'
}

const fullRoleText = (record) => {
  return displayRoleNames(record).join(' / ')
}

const hasLoginSecurityRisk = (record) => {
  const failedCount = Number(record.failedLoginCount || 0)
  return failedCount > 0 || Number(record.status) === 0
}

const loginSecuritySummary = (record) => {
  const failedCount = Number(record.failedLoginCount || 0)
  if (Number(record.status) === 0 && failedCount > 0) return '已锁定'
  if (failedCount > 0) return `失败 ${failedCount} 次`
  return '无异常'
}

const isLongInactive = (record) => {
  if (!record.lastLoginAt) return false
  const lastLoginDate = new Date(String(record.lastLoginAt).replace(' ', 'T'))
  if (Number.isNaN(lastLoginDate.getTime())) return false
  const days = (Date.now() - lastLoginDate.getTime()) / (1000 * 60 * 60 * 24)
  return days >= 90
}

const displayRoleNames = (record) => {
  if (record.roleNames?.length) return record.roleNames
  return [roleTypeLabel(roleTypeOptions, record.role === 'admin' ? 'admin' : 'user')]
}

const displayRoleColor = (record) => {
  return roleTypeColor(roleTypeOptions, record.role === 'admin' ? 'admin' : 'user')
}

const defaultRoleIds = () => {
  const userRole = roleOptions.value.find(role => role.roleCode === 'user' || role.roleName === '普通用户' || role.roleName === 'user')
  return userRole ? [userRole.id] : []
}

const fallbackRoleIds = (legacyRole) => {
  const code = legacyRole === 'admin' ? 'admin' : 'user'
  const role = roleOptions.value.find(item => item.roleCode === code || item.roleName === code)
  return role ? [role.id] : []
}

const resolveLegacyRole = (roleIds) => {
  const selected = roleOptions.value.filter(role => roleIds.includes(role.id))
  return selected.some(role => role.roleCode === 'admin' || role.roleName === 'admin' || role.roleName === '系统管理员') ? 'admin' : 'user'
}

const notifyPermissionsUpdated = () => {
  window.dispatchEvent(new CustomEvent('user-permissions-updated'))
}

const downloadBlob = (blob, fileName) => {
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(objectUrl)
}

onMounted(async () => {
  await Promise.all([
    applyPaginationConfig(size, pageSizeOptions),
    applyPasswordPolicy(passwordPolicy),
    loadStatusOptions(),
    loadRoleTypeOptions()
  ])
  await fetchRoles()
  await fetchTree()
  await fetchUsers()
  window.addEventListener('system-config-updated', refreshRuntimeConfig)
  window.addEventListener('system-dict-updated', refreshRuntimeConfig)
  window.addEventListener('user-context-refreshed', refreshRuntimeConfig)
})

onUnmounted(() => {
  window.removeEventListener('system-config-updated', refreshRuntimeConfig)
  window.removeEventListener('system-dict-updated', refreshRuntimeConfig)
  window.removeEventListener('user-context-refreshed', refreshRuntimeConfig)
})
</script>

<style scoped>
.user-page {
  min-height: 100vh;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-bg-layout-start)) 0%, var(--ds-color-bg-layout-mid) 180px, var(--ds-color-bg-layout) 100%);
}
.main { max-width: 1680px; width: 100%; margin: 0 auto; padding: 24px; display: flex; flex-direction: column; gap: 18px; }
.card-shell {
  background: var(--ds-color-bg-card);
  border: 1px solid color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-border));
  border-radius: 12px;
  box-shadow: 0 10px 28px var(--ds-color-shadow);
}
.hero-secondary-button,
.hero-primary-button { min-width: 108px; }
.summary-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 16px; align-items: stretch; }
.table-card { padding: 16px 20px 12px; }
.section-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 12px; }
.section-header--table { margin-bottom: 12px; }
.table-header-copy {
  min-width: 420px;
  flex: 1 1 auto;
}
.section-title { margin: 0; color: var(--ds-color-text-primary); font-size: 18px; line-height: 1.3; }
.section-description { margin: 8px 0 0; color: var(--ds-color-text-secondary); font-size: 13px; line-height: 1.6; white-space: nowrap; }
.filter-title { display: inline-flex; align-items: center; gap: 8px; }
.filter-title__icon { width: 18px; height: 18px; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center; background: var(--ds-color-bg-selected); color: var(--ds-color-primary); flex: 0 0 auto; }
.filter-title__icon :deep(svg) { width: 14px; height: 14px; }
.filter-expand-button { padding: 0 4px; color: var(--ds-color-text-regular); }
.filter-expand-button--inline {
  height: 36px;
  align-self: flex-end;
  margin-left: 2px;
  border-radius: 8px;
}
.filter-expand-button__icon--expanded { transform: rotate(180deg); }
.filter-fields { display: flex; gap: 16px; flex-wrap: wrap; min-width: 0; align-items: flex-end; width: auto; flex: 0 1 auto; }
.filter-item { min-width: 0; flex: 0 0 auto; }
.filter-item--keyword { width: 260px; min-width: 260px; }
.filter-item--dept { width: 220px; }
.filter-item--status { width: 180px; }
.filter-item--role { width: 220px; }
.user-page :deep(.ds-filter-bar__body) {
  grid-template-columns: minmax(0, 1fr) auto;
  justify-content: space-between;
  align-items: end;
}
.user-page :deep(.ds-filter-bar__actions) {
  justify-content: flex-end;
}
.table-header-tools { display: flex; align-items: center; justify-content: flex-end; gap: 8px; flex-wrap: nowrap; flex-shrink: 0; min-width: fit-content; }
.table-header-tools :deep(.ds-action-bar__selection) {
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--ds-color-primary) 6%, transparent);
}
.table-header-tools :deep(.ds-action-bar) {
  flex-shrink: 0;
}
.identity-cell {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  max-width: 100%;
  white-space: nowrap;
}
.user-page :deep(.arco-table-th .table-checkbox) {
  margin-right: 12px;
}
.table-checkbox {
  appearance: none;
  -webkit-appearance: none;
  display: inline-grid;
  place-content: center;
  width: 16px;
  height: 16px;
  margin: 0;
  border: 1px solid var(--ds-color-border);
  border-radius: 4px;
  background: var(--ds-color-bg-card);
  cursor: pointer;
  vertical-align: middle;
}
.table-checkbox::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 2px;
  transform: scale(0);
  background: var(--ds-color-bg-card);
}
.table-checkbox:checked {
  border-color: var(--ds-color-primary);
  background: var(--ds-color-primary);
}
.table-checkbox:checked::before {
  transform: scale(1);
  clip-path: polygon(14% 44%, 0 60%, 38% 100%, 100% 18%, 84% 4%, 36% 68%);
}
.table-checkbox--indeterminate {
  border-color: var(--ds-color-primary);
  background: var(--ds-color-primary);
}
.table-checkbox--indeterminate::before {
  width: 8px;
  height: 2px;
  border-radius: 999px;
  transform: scale(1);
  clip-path: none;
}
.table-checkbox:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
.filter-item :deep(.ds-field) { gap: 6px; }
.filter-item :deep(.ds-field__label) { color: var(--ds-color-text-regular); font-weight: 500; }
.filter-item :deep(.arco-input-wrapper),
.filter-item :deep(.arco-select-view) { height: 36px; border-radius: 8px; }
.filter-actions { display: flex; align-items: center; gap: 8px; justify-content: flex-end; align-self: end; padding-bottom: 1px; }
.filter-actions :deep(.arco-btn),
.hero-secondary-button :deep(.arco-btn),
.hero-primary-button :deep(.arco-btn) { min-width: 76px; height: 36px; border-radius: 8px; }
.advanced-filter-panel { margin-top: -4px; }
.advanced-filter-fields {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px 16px;
  width: 100%;
}
.advanced-filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.advanced-filter-item label {
  color: var(--ds-color-text-regular);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
}
.advanced-filter-item :deep(.arco-select-view),
.advanced-filter-item :deep(.arco-picker) {
  height: 36px;
  border-radius: 8px;
}
.advanced-filter-placeholder {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  color: var(--ds-color-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}
.row-actions { display: flex; gap: 4px; align-items: center; justify-content: flex-start; flex-wrap: nowrap; white-space: nowrap; }
.row-actions :deep(.arco-btn-text) {
  padding: 0 10px;
  border-radius: 8px;
  color: var(--ds-color-primary);
  height: 28px;
}
.row-actions :deep(.arco-btn-text:hover) {
  background: var(--ds-color-bg-hover);
}
.role-cell { display: inline-flex; align-items: center; gap: 6px; max-width: 100%; white-space: nowrap; overflow: hidden; }
.role-cell :deep(.ds-role-tag) { max-width: 132px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.role-more { color: var(--ds-color-text-secondary); font-size: 12px; line-height: 18px; white-space: nowrap; }
.compact-cell { display: flex; flex-direction: column; gap: 3px; min-width: 0; line-height: 1.35; }
.compact-cell__primary { color: var(--ds-color-text-primary); font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.compact-cell__secondary { color: var(--ds-color-text-secondary); font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.created-at-text { color: var(--ds-color-text-secondary); font-size: 12px; line-height: 18px; white-space: nowrap; }
.no-actions { color: var(--ds-color-text-secondary); padding: 0 8px; }
.batch-action-bar { margin-top: 12px; padding: 10px 12px; background: var(--ds-color-bg-soft); border-radius: 6px; display: flex; align-items: center; justify-content: flex-end; gap: 10px; color: var(--ds-color-text-regular); }
.principle-strip { display: flex; align-items: center; justify-content: center; gap: 28px; padding: 4px 0 0; color: var(--ds-color-text-secondary); font-size: 12px; letter-spacing: 0; }
.principle-strip span { position: relative; }
.principle-strip span + span::before { content: ''; position: absolute; left: -14px; top: 50%; width: 4px; height: 4px; border-radius: 50%; background: var(--ds-color-text-placeholder); transform: translateY(-50%); }
.form-tip { color: var(--ds-color-text-secondary); font-size: 13px; }
.hidden-file-input { display: none; }
.import-result-box { justify-content: flex-start; margin-top: 0; }
.import-result-text { margin-top: 12px; }
.import-result-details { margin-top: 12px; color: var(--ds-color-text-regular); font-size: 13px; line-height: 1.7; }
.import-result-details ul { margin: 8px 0 0; padding-left: 18px; }
@media (max-width: 1440px) {
  .summary-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 1080px) {
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .user-page :deep(.ds-filter-bar__body) { grid-template-columns: 1fr; }
  .advanced-filter-fields { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .filter-actions { justify-content: flex-end; }
  .section-description { white-space: normal; }
}
@media (max-width: 720px) {
  .main { padding: 16px; }
  .table-card { padding: 16px; }
  .summary-grid { grid-template-columns: 1fr; }
  .filter-fields { width: 100%; }
  .filter-item,
  .filter-item--keyword,
  .filter-item--dept,
  .filter-item--status,
  .filter-item--role { width: 100%; min-width: 0; }
  .table-header-copy { min-width: 0; }
  .section-description { white-space: normal; }
  .advanced-filter-fields { grid-template-columns: 1fr; }
}
</style>
