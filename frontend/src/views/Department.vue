<template>
  <div class="dept-page">
    <div class="main">
      <DsPageHeader
        :breadcrumb="['工作台', '系统管理', '部门管理']"
        title="部门管理"
        description="维护组织架构、部门负责人和部门内用户归属，保证账号组织关系清晰。"
      >
        <template #icon><IconBranch /></template>
        <template #actions>
          <DsHeaderActions
            :tool-items="headerToolItems"
            :show-primary="canCreateDept"
            primary-label="新增部门"
            @tool-select="handleHeaderToolSelect"
            @primary-click="openContextualAddModal"
          >
            <template #primaryIcon><IconPlus /></template>
          </DsHeaderActions>
        </template>
        <template #meta>
          <span>{{ deptTotal }} 个部门节点</span>
          <span>{{ selectedDeptName || '未选择部门' }}</span>
        </template>
      </DsPageHeader>

      <section class="summary-grid">
        <DsStatsCard label="部门总数" :value="deptTotal" hint="当前组织架构节点" tone="primary">
          <template #icon><IconBranch /></template>
        </DsStatsCard>
        <DsStatsCard label="用户总数" :value="userTotal" hint="当前部门查询结果" tone="success">
          <template #icon><IconUser /></template>
        </DsStatsCard>
        <DsStatsCard label="负责人数量" :value="managerDeptCount" hint="已设置负责人的部门" tone="warning">
          <template #icon><IconUserGroup /></template>
        </DsStatsCard>
        <DsStatsCard label="未分配用户" :value="isUnassignedSelected ? userTotal : unassignedUserHint" hint="暂未归属组织的账号" tone="primary">
          <template #icon><IconQuestionCircle /></template>
        </DsStatsCard>
      </section>

      <div class="dept-layout">
        <DepartmentTreePanel
          v-model:keyword="deptKeyword"
          :nodes="treeNodes"
          :selected-keys="selectedKeys"
          :expanded-keys="deptKeyword.trim() ? searchExpandedKeys : undefined"
          :status-options="statusOptions"
          :can-create-dept="canCreateDept"
          :can-create-user="canCreateUserButton"
          :can-edit-dept="canUpdateDept || canManageDeptManager"
          :can-delete-dept="canDeleteDept"
          @refresh="fetchTree"
          @select="onTreeSelect"
          @node-action="handleNodeContextAction"
          @edit="openEditModal"
          @add-root="openAddModal(null)"
        />

        <!-- 右侧：用户列表（占位） -->
        <div class="user-list-panel">
          <div class="panel-header">
            <div class="panel-heading">
              <span class="panel-title">
                {{ selectedDeptName ? selectedDeptName + ' - 用户列表' : '请选择部门' }}
              </span>
              <span class="panel-desc">按部门查看用户归属，支持搜索、筛选、分页和批量调整。</span>
            </div>
            <div class="header-actions" v-if="selectedDeptId">
              <DsActionBar
                :selection-text="selectedUserIds.length > 0 ? `已选择 ${selectedUserIds.length} 个用户` : '未选择用户'"
                :selection-active="selectedUserIds.length > 0"
              >
                <template #batch>
                  <DsBatchActions
                    :selected-count="selectedUserIds.length"
                    :actions="departmentBatchActions"
                    @action="handleDepartmentBatchAction"
                  />
                </template>
                <template #tools>
                  <DsIconButton tooltip="刷新用户列表" @click="fetchCurrentUsers"><IconRefresh /></DsIconButton>
                </template>
              </DsActionBar>
              <a-button v-if="canCreateUserButton && !isUnassignedSelected" type="primary" size="small" class="panel-primary-button" @click="openUserModal">
                <template #icon><IconPlus /></template>
                新增用户
              </a-button>
            </div>
          </div>
          <div class="user-placeholder" v-if="!selectedDeptId">
            <IconUser class="placeholder-icon" />
            <div class="placeholder-title">请选择左侧部门查看用户</div>
            <div class="placeholder-desc">选择“未分配”可查看暂未归属部门的用户。</div>
          </div>
          <div class="user-list" v-else>
            <div class="user-table-area">
              <div class="user-toolbar">
                <DsKeywordSearch
                  v-model="userKeyword"
                  class="user-search"
                  label=""
                  placeholder="搜索姓名或账号"
                  @search="handleUserSearch"
                />
                <DsStatusSelect v-model="userStatusFilter" class="user-filter" placeholder="全部状态" @update:model-value="handleUserSearch" />
                <DsRoleMultiSelect v-model="userRoleFilter" class="user-role-filter" :options="roleFilterOptions" :multiple="false" placeholder="全部角色" @update:model-value="handleUserSearch" />
                <a-button class="toolbar-button" @click="resetUserFilters">重置</a-button>
              </div>
              <DsDataTable
                :columns="columns"
                :data="filteredUserList"
                :loading="userLoading"
                :pagination="userPaginationConfig"
                :selected-row-keys="selectedUserIds"
                row-key="id"
                :scroll="{ x: tableScrollX }"
                :resizable="true"
                @select="handleUserSelect"
                @select-all="handleUserSelectAll"
                @pagination-change="handleUserPageChange"
                @page-size-change="handleUserSizeChange"
              >
                <template #identityHeader>
                  <input
                    type="checkbox"
                    class="table-checkbox"
                    :checked="isCurrentPageSelected"
                    :class="{ 'table-checkbox--indeterminate': isCurrentPageIndeterminate }"
                    :disabled="filteredUserList.length === 0"
                    @change="event => handleUserSelectAll(event.target.checked, filteredUserList)"
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
                <template #role="{ record }">
                  <div class="role-cell">
                    <DsRoleTag :label="primaryRoleName(record)" :color="displayRoleColor(record)" />
                    <span v-if="displayRoleNames(record).length > 1" class="role-more">+{{ displayRoleNames(record).length - 1 }}</span>
                  </div>
                </template>
                <template #status="{ record }">
                  <DsStatusTag :value="record.status" :options="statusOptions" />
                </template>
                <template #createdAt="{ record }">{{ record.createdAt ? record.createdAt.replace('T',' ').substring(0,16) : '-' }}</template>
                <template #actions="{ record }">
                  <div class="row-actions">
                    <a-button v-if="canEditUserFields" type="text" size="small" @click="openUserEditModal(record)">编辑</a-button>
                    <a-dropdown v-if="canMoveUserDeptField">
                      <a-button type="text" size="small">
                        更多
                        <template #icon><IconDown /></template>
                      </a-button>
                      <template #content>
                        <a-doption @click="openMoveModal(record)">调部门</a-doption>
                      </template>
                    </a-dropdown>
                    <span v-if="!canEditUserFields && !canMoveUserDeptField" class="no-actions">无可用操作</span>
                  </div>
                </template>
                <template #empty>
                  <DsEmptyState :title="isUnassignedSelected ? '暂无未分配用户' : '当前部门暂无用户'" description="当前条件下没有可展示的用户。" />
                </template>
              </DsDataTable>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑部门弹窗 -->
    <DsModalForm
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑部门' : '新增部门'"
      :description="isEdit ? '维护部门名称、排序、负责人和启用状态。' : '新增组织架构节点，后续可继续设置负责人。'"
      :width="720"
      @cancel="modalVisible = false"
    >
      <DsFormSection title="部门信息" description="部门名称用于组织树展示，排序数字越小越靠前。">
        <DsFormGrid>
          <DsInput v-model="form.name" label="部门名称" required placeholder="请输入部门名称" :max-length="50" :disabled="isEdit && !canUpdateDept" />
          <DsNumberInput v-model="form.sortOrder" label="排序" :min="0" :max="9999" placeholder="数字越小越靠前" :disabled="isEdit && !canUpdateDept" />
        </DsFormGrid>
      </DsFormSection>
      <DsFormSection v-if="isEdit" title="管理设置" description="负责人必须是当前部门下的正常用户。">
        <DsFormGrid>
          <DsUserAvatarSelect
            v-if="canManageDeptManager"
            v-model="form.managerUserId"
            label="部门负责人"
            placeholder="请选择本部门正常用户"
            :options="managerUserOptions"
            :loading="managerLoading"
            :remote="false"
            :hint="managerCandidates.length === 0 ? '本部门暂无可选负责人，请先添加或启用用户。' : ''"
          />
          <DsStatusSelect v-model="form.status" label="状态" :include-all="false" :disabled="!canUpdateDept" />
        </DsFormGrid>
      </DsFormSection>
      <template #footer>
        <DsFormActions @cancel="modalVisible = false" @submit="submitDepartmentForm" />
      </template>
    </DsModalForm>

    <UserFormModal
      v-model:visible="userModalVisible"
      mode="create"
      :form="userForm"
      :role-options="roleSelectOptions"
      :password-tip="passwordTip"
      :password-placeholder="passwordPlaceholder"
      :password-policy-text="passwordPolicyText(passwordPolicy)"
      :show-dept-field="true"
      :readonly-dept="!isUnassignedSelected"
      :readonly-dept-name="selectedDeptName"
      :readonly-dept-hint="currentUserDeptHint"
      :can-move-dept="canMoveUserDeptField"
      :can-assign-roles="canAssignRolesField"
      :can-update-status="canUpdateUserStatusField"
      :can-reset-password="false"
      :can-write-field="canWriteUserField"
      @update:field="handleUserFormFieldUpdate"
      @cancel="userModalVisible = false"
      @submit="submitUserForm"
    />

    <!-- 编辑用户弹窗 -->
    <UserFormModal
      v-model:visible="userEditModalVisible"
      mode="edit"
      :form="userEditForm"
      :role-options="roleSelectOptions"
      :password-tip="passwordTip"
      :password-placeholder="passwordPlaceholder"
      :password-policy-text="passwordPolicyText(passwordPolicy)"
      :show-dept-field="true"
      readonly-dept
      :readonly-dept-name="selectedDeptName"
      readonly-dept-hint="如需调整部门，请使用列表行内“调部门”。"
      :can-move-dept="canMoveUserDeptField"
      :can-assign-roles="canAssignRolesField"
      :can-update-status="canUpdateUserStatusField"
      :can-reset-password="canResetPassword"
      :can-write-field="canWriteUserField"
      @update:field="handleUserEditFormFieldUpdate"
      @cancel="userEditModalVisible = false"
      @submit="submitUserEditForm"
    />

    <!-- 分配/调部门弹窗 -->
    <DsModalForm
      v-model:visible="moveModalVisible"
      :title="isUnassignedSelected ? '分配到部门' : '调整部门'"
      description="批量调整用户所属部门，保存后会刷新当前部门用户列表。"
      :width="560"
      @cancel="moveModalVisible = false"
    >
      <DsFormSection title="目标部门">
        <DsFormGrid :columns="1">
          <DsDeptTreeSelect v-model="targetDeptSelection" label="目标部门" required include-unassigned unassigned-value="__none" placeholder="请选择目标部门" />
        </DsFormGrid>
        <div class="move-tip">将移动 {{ movingUserIds.length }} 个用户。</div>
      </DsFormSection>
      <template #footer>
        <DsFormActions @cancel="moveModalVisible = false" @submit="submitMoveForm" />
      </template>
    </DsModalForm>

    <!-- 删除确认弹窗 -->
    <DsModalForm
      v-model:visible="deleteModalVisible"
      title="确认删除"
      description="删除前会检查子部门、用户和负责人影响。"
      :width="560"
      @cancel="deleteModalVisible = false"
    >
      <div class="delete-confirm">
        <p>确认删除部门「<strong>{{ deleteTarget?.name }}</strong>」吗？</p>
        <p v-if="deleteImpactLoading" class="delete-confirm__muted">正在检查删除影响...</p>
        <p v-if="deleteImpact && deleteImpact.childCount > 0" class="delete-confirm__danger">
          影响：该部门下还有 {{ deleteImpact.childCount }} 个子部门。
        </p>
        <p v-if="deleteImpact && deleteImpact.userCount > 0" class="delete-confirm__danger">
          影响：该部门及子部门下还有 {{ deleteImpact.userCount }} 个用户。
        </p>
        <p v-if="deleteImpact && deleteImpact.managerName" class="delete-confirm__muted">
          当前负责人：{{ deleteImpact.managerName }}。
        </p>
        <p v-if="deleteImpact && !deleteImpact.canDelete" class="delete-confirm__danger delete-confirm__tip">
          请先调整子部门或用户后再删除。
        </p>
        <p v-else class="delete-confirm__tip">此操作不可恢复。</p>
      </div>
      <template #footer>
        <DsFormActions cancel-text="取消" submit-text="确认删除" @cancel="deleteModalVisible = false" @submit="submitDeleteForm" />
      </template>
    </DsModalForm>

    <DsModalForm
      v-model:visible="importResultVisible"
      title="部门导入结果"
      description="查看本次导入的成功、跳过和失败结果。"
      :width="620"
      @cancel="importResultVisible = false"
    >
      <div v-if="importResult" class="import-result-box">
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
      <template #footer>
        <DsFormActions cancel-text="关闭" submit-text="刷新部门树" @cancel="importResultVisible = false" @submit="refreshAfterImportResult" />
      </template>
    </DsModalForm>

    <input ref="importFileInput" type="file" accept=".xlsx" class="hidden-file-input" @change="handleImportFileChange" />
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, onUnmounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconBranch, IconPlus, IconUser, IconUserGroup, IconQuestionCircle, IconRefresh, IconDown } from '@arco-design/web-vue/es/icon'
import { hasPermission } from '@/utils/permission'
import { deptApi, roleApi, userApi } from '@/api'
import DepartmentTreePanel from '@/components/department/DepartmentTreePanel.vue'
import UserFormModal from '@/components/user/UserFormModal.vue'
import { validateUserFormFields } from '@/components/user/userFormValidation'
import {
  DEFAULT_PASSWORD_MIN_LENGTH,
  DEFAULT_PAGE_SIZE_OPTIONS,
  applyPaginationConfig,
  applyPasswordPolicy,
  passwordPolicyText,
  validatePasswordByPolicy
} from '@/utils/systemConfig'
import { loadCommonStatusOptions, loadUserRoleTypeOptions, roleTypeColor, roleTypeLabel, statusColor, statusLabel } from '@/utils/dict'
import {
  DsDataTable,
  DsActionBar,
  DsBatchActions,
  DsDeptTreeSelect,
  DsEmptyState,
  DsFormActions,
  DsFormGrid,
  DsFormSection,
  DsHeaderActions,
  DsIconButton,
  DsInput,
  DsKeywordSearch,
  DsModalForm,
  DsNumberInput,
  DsPageHeader,
  DsRoleMultiSelect,
  DsRoleTag,
  DsStatsCard,
  DsStatusSelect,
  DsStatusTag,
  DsUserCell,
  DsUserAvatarSelect
} from '@/design-system'

const treeData = ref([])
const deptKeyword = ref('')
const selectedKeys = ref([])
const selectedDeptId = ref(null)
const selectedDeptName = ref('')
const userList = ref([])
const userLoading = ref(false)
const userKeyword = ref('')
const userStatusFilter = ref('')
const userRoleFilter = ref('')
const userPage = ref(1)
const userSize = ref(20)
const userPageSizeOptions = ref([...DEFAULT_PAGE_SIZE_OPTIONS])
const userTotal = ref(0)
const readableUserFields = ref(['username', 'realName', 'phone', 'idCard', 'email', 'dept', 'roles', 'status', 'forcePasswordChange'])
const writableUserFields = ref(['username', 'realName', 'phone', 'idCard', 'email', 'dept', 'roles', 'status', 'forcePasswordChange'])
const passwordPolicy = ref({ minLength: DEFAULT_PASSWORD_MIN_LENGTH, requireStrong: false })
const statusOptions = ref([])
const roleTypeOptions = ref([])
const roleOptions = ref([])
const modalVisible = ref(false)
const userModalVisible = ref(false)
const userEditModalVisible = ref(false)
const moveModalVisible = ref(false)
const deleteModalVisible = ref(false)
const importResultVisible = ref(false)
const importResult = ref(null)
const importFileInput = ref(null)
const isEdit = ref(false)
const deleteTarget = ref(null)
const deleteImpact = ref(null)
const deleteImpactLoading = ref(false)
const selectedUserIds = ref([])
const movingUserIds = ref([])
const targetDeptId = ref(null)
const managerCandidates = ref([])
const managerLoading = ref(false)
const userEditOriginal = ref(null)

const unassignedId = '__unassigned'
const form = reactive({ id: null, parentId: null, name: '', sortOrder: 0, status: 1, managerUserId: null })
const userForm = reactive({ username: '', realName: '', phone: '', idCard: '', email: '', password: '', role: 'user', roleIds: [], status: 1, forcePasswordChange: 0 })
const userEditForm = reactive({ id: null, username: '', realName: '', phone: '', idCard: '', email: '', password: '', role: 'user', roleIds: [], status: 1, forcePasswordChange: 0 })
const isUnassignedSelected = computed(() => selectedDeptId.value === unassignedId)
const visibleTreeData = computed(() => filterDeptTree(treeData.value, deptKeyword.value))
const searchExpandedKeys = computed(() => collectExpandedKeys(visibleTreeData.value))
const treeNodes = computed(() => [
  ...visibleTreeData.value,
  { id: unassignedId, name: '未分配', status: 1, virtual: true, children: [] }
])
const deptOptions = computed(() => flattenDeptOptions(treeData.value))
const deptTotal = computed(() => flattenDeptOptions(treeData.value).length)
const managerDeptCount = computed(() => countManagerDepts(treeData.value))
const unassignedUserHint = computed(() => selectedDeptId.value ? '查看未分配节点' : 0)
const headerToolItems = computed(() => [
  { key: 'template', label: '下载模板', visible: canImportDept.value },
  { key: 'import', label: '导入部门', visible: canImportDept.value },
  { key: 'export', label: '导出部门', visible: canExportDept.value }
])
const canCreateDept = computed(() => hasPermission('system_department:create'))
const canUpdateDept = computed(() => hasPermission('system_department:update'))
const canDeleteDept = computed(() => hasPermission('system_department:delete'))
const canImportDept = computed(() => hasPermission('system_department:import'))
const canExportDept = computed(() => hasPermission('system_department:export'))
const canManageDeptManager = computed(() => hasPermission('system_department:manager'))
const canCreateUser = computed(() => hasPermission('system_user:create'))
const canUpdateUser = computed(() => hasPermission('system_user:update'))
const canUpdateUserStatus = computed(() => hasPermission('system_user:status'))
const canMoveUserDept = computed(() => hasPermission('system_user:dept'))
const canAssignRoles = computed(() => hasPermission('system_user:roles'))
const canResetPassword = computed(() => hasPermission('system_user:password'))
const canReadUserField = (fieldCode) => readableUserFields.value.includes(fieldCode)
const canWriteUserField = (fieldCode) => writableUserFields.value.includes(fieldCode)
const canCreateUserButton = computed(() => canCreateUser.value && canWriteUserField('username') && canWriteUserField('realName') && canMoveUserDeptField.value)
const canUpdateRealNameField = computed(() => canUpdateUser.value && canWriteUserField('realName'))
const canUpdateContactFields = computed(() => canUpdateUser.value && (canWriteUserField('phone') || canWriteUserField('idCard') || canWriteUserField('email') || canWriteUserField('forcePasswordChange')))
const canUpdateUserStatusField = computed(() => canUpdateUserStatus.value && canWriteUserField('status'))
const canMoveUserDeptField = computed(() => canMoveUserDept.value && canWriteUserField('dept'))
const canAssignRolesField = computed(() => canAssignRoles.value && canWriteUserField('roles'))
const canEditUserFields = computed(() => canUpdateRealNameField.value || canUpdateContactFields.value || canAssignRolesField.value || canUpdateUserStatusField.value || canResetPassword.value)
const passwordTip = computed(() => `密码${passwordPolicyText(passwordPolicy.value)}`)
const passwordPlaceholder = computed(() => `请输入密码，${passwordPolicyText(passwordPolicy.value)}`)

const baseColumns = [
  { titleSlotName: 'identityHeader', slotName: 'identity', width: 320, fixed: 'left', fieldCodes: ['realName', 'username'] },
  { title: '角色', slotName: 'role', width: 180, fieldCode: 'roles' },
  { title: '状态', slotName: 'status', width: 112, fieldCode: 'status' },
  { title: '创建时间', slotName: 'createdAt', width: 160 },
  { title: '操作', slotName: 'actions', width: 140, fixed: 'right' }
]
const columns = computed(() => baseColumns.filter(column => {
  if (column.fieldCodes?.length) {
    return column.fieldCodes.some(code => canReadUserField(code))
  }
  return !column.fieldCode || canReadUserField(column.fieldCode)
}))
const tableScrollX = computed(() => columns.value.reduce((sum, column) => sum + Number(column.width || 120), 0))
const currentPageIds = computed(() => filteredUserList.value.map(item => item.id).filter(id => id !== undefined && id !== null))
const isCurrentPageSelected = computed(() => currentPageIds.value.length > 0 && currentPageIds.value.every(id => selectedUserIds.value.includes(id)))
const isCurrentPageIndeterminate = computed(() => {
  const selectedCount = currentPageIds.value.filter(id => selectedUserIds.value.includes(id)).length
  return selectedCount > 0 && selectedCount < currentPageIds.value.length
})
const roleFilterOptions = computed(() => roleOptions.value.map(role => ({
  value: role.id,
  label: role.roleName,
  description: role.description || role.roleCode || ''
})))
const roleSelectOptions = computed(() => roleOptions.value.map(role => ({
  value: role.id,
  label: role.roleName,
  description: role.description || role.roleCode || ''
})))
const managerUserOptions = computed(() => managerCandidates.value.map(user => ({
  value: user.id,
  label: user.realName || user.username || '未命名用户',
  subText: user.username || '',
  disabled: Number(user.status) === 0
})))
const targetDeptSelection = computed({
  get: () => targetDeptId.value == null ? '__none' : targetDeptId.value,
  set: value => { targetDeptId.value = value === '__none' ? null : value }
})
const departmentBatchActions = computed(() => [
  { key: 'enable', label: '批量启用', visible: canUpdateUserStatusField.value },
  { key: 'disable', label: '批量禁用', danger: true, visible: canUpdateUserStatusField.value },
  { key: 'move', label: isUnassignedSelected.value ? '分配到部门' : '调部门', visible: canMoveUserDeptField.value },
  { key: 'clear', label: '取消选择', visible: selectedUserIds.value.length > 0 }
])
const filteredUserList = computed(() => {
  return (userList.value || []).filter(user => {
    const statusMatched = userStatusFilter.value === '' || String(user.status) === String(userStatusFilter.value)
    const roleMatched = userRoleFilter.value === '' || (Array.isArray(user.roleIds) && user.roleIds.some(id => String(id) === String(userRoleFilter.value)))
    return statusMatched && roleMatched
  })
})
const userPaginationConfig = computed(() => ({
  current: userPage.value,
  pageSize: userSize.value,
  total: userTotal.value,
  pageSizeOptions: userPageSizeOptions.value
}))
const currentUserDeptHint = computed(() => {
  return isUnassignedSelected.value
    ? '当前在未分配节点新增用户，请选择目标部门。'
    : '用户将创建到当前选中的部门。'
})

const fetchTree = async () => {
  try {
    const res = await deptApi.tree()
    if (res.code === 200) {
      treeData.value = res.data || []
    } else { Message.error(res.msg || '加载失败') }
  } catch { Message.error('加载部门树失败') }
}

const downloadTemplate = async () => {
  try {
    const blob = await deptApi.downloadTemplate()
    downloadBlob(blob, '部门导入模板.xlsx')
    Message.success('模板已下载')
  } catch {
    Message.error('下载模板失败')
  }
}

const triggerImport = () => importFileInput.value?.click()

const fetchUsers = async (deptId) => {
  userLoading.value = true
  try {
    const params = { page: userPage.value, size: userSize.value }
    if (userKeyword.value.trim()) params.keyword = userKeyword.value.trim()
    const res = deptId === unassignedId ? await deptApi.unassignedUsers(params) : await deptApi.users(deptId, params)
    if (res.code === 200) {
      userList.value = res.data?.records || []
      userTotal.value = res.data?.total || 0
      readableUserFields.value = Array.isArray(res.data?.readableFields) ? res.data.readableFields : readableUserFields.value
      writableUserFields.value = Array.isArray(res.data?.writableFields) ? res.data.writableFields : writableUserFields.value
      clearUserSelection()
    } else { userList.value = [] }
  } catch { userList.value = [] }
  finally { userLoading.value = false }
}
const fetchCurrentUsers = () => selectedDeptId.value && fetchUsers(selectedDeptId.value)
const handleUserPageChange = (nextPage) => { userPage.value = nextPage; fetchCurrentUsers() }
const handleUserSizeChange = (nextSize) => { userSize.value = nextSize; userPage.value = 1; fetchCurrentUsers() }
const handleUserSearch = () => { userPage.value = 1; fetchCurrentUsers() }
const resetUserFilters = () => {
  userKeyword.value = ''
  userStatusFilter.value = ''
  userRoleFilter.value = ''
  handleUserSearch()
}
const handleUserFormFieldUpdate = ({ field, value }) => {
  if (field in userForm) userForm[field] = value
}
const handleUserEditFormFieldUpdate = ({ field, value }) => {
  if (field in userEditForm) userEditForm[field] = value
}
const handleHeaderToolSelect = (key) => {
  if (key === 'template') downloadTemplate()
  if (key === 'import') triggerImport()
  if (key === 'export') exportDepartments()
}

const handleDepartmentBatchAction = (key) => {
  if (key === 'enable') handleBatchStatus(1)
  if (key === 'disable') handleBatchStatus(0)
  if (key === 'move') openMoveModal()
  if (key === 'clear') clearUserSelection()
}

const refreshRuntimeConfig = async () => {
  await Promise.all([
    applyPaginationConfig(userSize, userPageSizeOptions),
    applyPasswordPolicy(passwordPolicy),
    loadStatusOptions(),
    loadRoleTypeOptions(),
    fetchRoles()
  ])
  userPage.value = 1
  await fetchTree()
  if (selectedDeptId.value) {
    const node = findNode(treeNodes.value, selectedDeptId.value)
    selectedDeptName.value = node ? node.name : ''
    await fetchCurrentUsers()
  }
}

const handleImportFileChange = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await deptApi.importDepartments(formData)
    if (res.code !== 200) {
      Message.error(res.msg || '导入失败')
      return
    }
    importResult.value = res.data || {}
    importResultVisible.value = true
    Message.success(res.data?.message || '导入完成')
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

const fetchRoles = async () => {
  try {
    const res = await roleApi.list()
    roleOptions.value = res.code === 200 ? (res.data || []).filter(role => role.status !== 0) : []
  } catch {
    roleOptions.value = []
  }
}

const onTreeSelect = (keys) => {
  if (keys.length === 0) return
  selectedDeptId.value = keys[0]
  const node = findNode(treeNodes.value, keys[0])
  selectedDeptName.value = node ? node.name : ''
  selectedKeys.value = keys
  userPage.value = 1
  if (node) fetchUsers(node.id)
}

const selectNodeForAction = (node) => {
  if (!node) return
  selectedDeptId.value = node.id
  selectedDeptName.value = node.name || node.title || ''
  selectedKeys.value = [node.id]
  if (node.id) fetchUsers(node.id)
}

const handleNodeContextAction = (key, node) => {
  selectNodeForAction(node)
  if (key === 'addUser' && !node.virtual && canCreateUserButton.value) openUserModal()
  if (key === 'editDept' && !node.virtual && (canUpdateDept.value || canManageDeptManager.value)) openEditModal(node)
  if (key === 'deleteDept' && !node.virtual && canDeleteDept.value) confirmDelete(node)
}

const findNode = (nodes, id) => {
  for (const n of nodes) {
    if (n.id == id) return n
    if (n.children?.length) {
      const f = findNode(n.children, id)
      if (f) return f
    }
  }
  return null
}

const filterDeptTree = (nodes, keyword) => {
  const kw = (keyword || '').trim().toLowerCase()
  if (!kw) return nodes
  const result = []
  for (const node of nodes || []) {
    const children = filterDeptTree(node.children || [], kw)
    const name = (node.name || '').toLowerCase()
    if (name.includes(kw) || children.length > 0) {
      result.push({ ...node, children })
    }
  }
  return result
}

const flattenDeptOptions = (nodes, level = 0) => {
  const result = []
  for (const node of nodes || []) {
    result.push({ id: node.id, label: `${'　'.repeat(level)}${node.name}` })
    if (node.children?.length) result.push(...flattenDeptOptions(node.children, level + 1))
  }
  return result
}

const collectExpandedKeys = (nodes) => {
  const keys = []
  for (const node of nodes || []) {
    if (node.children?.length) {
      keys.push(node.id)
      keys.push(...collectExpandedKeys(node.children))
    }
  }
  return keys
}

const clearUserSelection = () => {
  selectedUserIds.value = []
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

const openAddModal = (parent) => {
  isEdit.value = false
  form.id = null
  form.parentId = parent?.id || null
  form.name = ''
  form.sortOrder = 0
  form.status = 1
  form.managerUserId = null
  managerCandidates.value = []
  modalVisible.value = true
}

const openContextualAddModal = () => {
  const parent = selectedDeptId.value && !isUnassignedSelected.value ? findNode(treeNodes.value, selectedDeptId.value) : null
  openAddModal(parent)
}

const openUserModal = () => {
  userForm.username = ''
  userForm.realName = ''
  userForm.phone = ''
  userForm.idCard = ''
  userForm.email = ''
  userForm.password = ''
  userForm.role = 'user'
  userForm.roleIds = defaultRoleIds()
  userForm.deptId = isUnassignedSelected.value ? null : selectedDeptId.value
  userForm.status = 1
  userForm.forcePasswordChange = 0
  userModalVisible.value = true
}

const openUserEditModal = (record) => {
  userEditForm.id = record.id
  userEditForm.username = record.username
  userEditForm.realName = record.realName || ''
  userEditForm.phone = record.phone || ''
  userEditForm.idCard = record.idCard || ''
  userEditForm.email = record.email || ''
  userEditForm.password = ''
  userEditForm.role = record.role === 'admin' ? 'admin' : 'user'
  userEditForm.roleIds = record.roleIds?.length ? [...record.roleIds] : fallbackRoleIds(record.role)
  userEditForm.status = record.status === 0 ? 0 : 1
  userEditForm.forcePasswordChange = Number(record.forcePasswordChange || 0) === 1 ? 1 : 0
  userEditOriginal.value = {
    realName: userEditForm.realName,
    phone: userEditForm.phone,
    idCard: userEditForm.idCard,
    email: userEditForm.email,
    roleIds: [...userEditForm.roleIds],
    status: userEditForm.status,
    forcePasswordChange: userEditForm.forcePasswordChange
  }
  userEditModalVisible.value = true
}

const openMoveModal = (record) => {
  movingUserIds.value = record ? [record.id] : [...selectedUserIds.value]
  if (movingUserIds.value.length === 0) {
    Message.warning('请先选择用户')
    return
  }
  targetDeptId.value = isUnassignedSelected.value ? (deptOptions.value[0]?.id || null) : null
  moveModalVisible.value = true
}

const openEditModal = (node) => {
  isEdit.value = true
  form.id = node.id
  form.parentId = null
  form.name = node.name
  form.sortOrder = node.sortOrder || 0
  form.status = node.status
  form.managerUserId = node.managerUserId || null
  if (canManageDeptManager.value) fetchManagerCandidates(node.id)
  modalVisible.value = true
}

const fetchManagerCandidates = async (deptId) => {
  managerLoading.value = true
  try {
    const res = await deptApi.managerCandidates(deptId)
    managerCandidates.value = res.code === 200 ? (res.data || []) : []
  } catch {
    managerCandidates.value = []
  } finally {
    managerLoading.value = false
  }
}

const handleModalOk = async (done) => {
  if ((!isEdit.value || canUpdateDept.value) && !form.name.trim()) {
    Message.error('部门名称不能为空')
    done(false)
    return
  }
  try {
    const body = isEdit.value
      ? {}
      : { name: form.name, parentId: form.parentId, sortOrder: form.sortOrder }
    if (isEdit.value && canUpdateDept.value) {
      body.name = form.name
      body.sortOrder = form.sortOrder
      body.status = form.status
    }
    if (isEdit.value && canManageDeptManager.value) {
      body.managerUserId = form.managerUserId
      body.clearManager = form.managerUserId == null
    }
    const res = isEdit.value ? await deptApi.update(form.id, body) : await deptApi.add(body)
    if (res.code === 200) {
      Message.success(isEdit.value ? '更新成功' : '新增成功')
      modalVisible.value = false
      await fetchTree()
      const node = findNode(treeNodes.value, selectedDeptId.value)
      if (node) selectedDeptName.value = node.name
    } else {
      Message.error(res.msg || '操作失败')
    }
  } catch { Message.error('操作失败') }
  done()
}

const callFormHandler = (handler) => {
  handler((result) => {
    if (result === false) return
  })
}

const submitDepartmentForm = () => callFormHandler(handleModalOk)

const handleUserOk = async (done) => {
  const formError = validateUserFormFields({
    form: userForm,
    canWriteField: canWriteUserField
  })
  if (formError) {
    Message.error(formError)
    done(false)
    return
  }
  if (canAssignRolesField.value && (!userForm.roleIds || userForm.roleIds.length === 0)) {
    Message.error('请至少选择一个角色')
    done(false)
    return
  }
  const passwordError = validatePasswordByPolicy(userForm.password, passwordPolicy.value)
  if (passwordError) {
    Message.error(passwordError)
    done(false)
    return
  }
  try {
    const payload = {
      username: userForm.username,
      realName: userForm.realName,
      password: userForm.password
    }
    if (canWriteUserField('phone')) payload.phone = userForm.phone
    if (canWriteUserField('idCard')) payload.idCard = userForm.idCard
    if (canWriteUserField('email')) payload.email = userForm.email
    if (canWriteUserField('forcePasswordChange')) payload.forcePasswordChange = userForm.forcePasswordChange
    if (canAssignRolesField.value) {
      payload.roleIds = userForm.roleIds
      payload.role = resolveLegacyRole(userForm.roleIds)
    }
    if (canMoveUserDeptField.value) {
      if (!userForm.deptId) {
        Message.error('请选择所属部门')
        done(false)
        return
      }
      payload.deptId = userForm.deptId
    }
    if (canUpdateUserStatusField.value) payload.status = userForm.status
    const res = await userApi.systemAdd(payload)
    if (res.code === 200) {
      Message.success('新增用户成功')
      userModalVisible.value = false
      await fetchCurrentUsers()
      await fetchTree()
      notifyPermissionsUpdated()
    } else {
      Message.error(res.msg || '新增用户失败')
    }
  } catch { Message.error('新增用户失败') }
  done()
}

const submitUserForm = () => callFormHandler(handleUserOk)

const handleUserEditOk = async (done) => {
  const formError = validateUserFormFields({
    form: userEditForm,
    isEdit: true,
    validateRealName: canUpdateRealNameField.value,
    canWriteField: canWriteUserField
  })
  if (formError) {
    Message.error(formError)
    done(false)
    return
  }
  const passwordError = validatePasswordByPolicy(userEditForm.password, passwordPolicy.value, true)
  if (passwordError) {
    Message.error(passwordError)
    done(false)
    return
  }
  if (canAssignRolesField.value && (!userEditForm.roleIds || userEditForm.roleIds.length === 0)) {
    Message.error('请至少选择一个角色')
    done(false)
    return
  }
  try {
    await saveEditedUser()
    Message.success('用户已更新')
    userEditModalVisible.value = false
    await fetchCurrentUsers()
    await fetchTree()
    notifyPermissionsUpdated()
  } catch (e) { Message.error(e.message || '更新用户失败') }
  done()
}

const submitUserEditForm = () => callFormHandler(handleUserEditOk)

const saveEditedUser = async () => {
  const original = userEditOriginal.value || {}
  const profilePayload = {}
  if (canUpdateRealNameField.value && userEditForm.realName !== original.realName) profilePayload.realName = userEditForm.realName
  if (canWriteUserField('phone') && userEditForm.phone !== original.phone) profilePayload.phone = userEditForm.phone
  if (canWriteUserField('idCard') && userEditForm.idCard !== original.idCard) profilePayload.idCard = userEditForm.idCard
  if (canWriteUserField('email') && userEditForm.email !== original.email) profilePayload.email = userEditForm.email
  if (canWriteUserField('forcePasswordChange') && userEditForm.forcePasswordChange !== original.forcePasswordChange) profilePayload.forcePasswordChange = userEditForm.forcePasswordChange
  if (Object.keys(profilePayload).length > 0) {
    const res = await userApi.systemUpdate(userEditForm.id, profilePayload)
    if (res.code !== 200) throw new Error(res.msg || '更新用户失败')
  }
  if (canAssignRolesField.value && roleIdsChanged(userEditForm.roleIds, original.roleIds || [])) {
    const res = await userApi.systemAssignRoles(userEditForm.id, userEditForm.roleIds)
    if (res.code !== 200) throw new Error(res.msg || '分配角色失败')
  }
  if (canUpdateUserStatusField.value && userEditForm.status !== original.status) {
    const res = await userApi.systemUpdateStatus(userEditForm.id, userEditForm.status)
    if (res.code !== 200) throw new Error(res.msg || '更新状态失败')
  }
  if (canResetPassword.value && userEditForm.password) {
    const res = await userApi.systemUpdatePassword(userEditForm.id, userEditForm.password)
    if (res.code !== 200) throw new Error(res.msg || '重置密码失败')
  }
}

const roleIdsChanged = (next, prev) => {
  const a = [...(next || [])].sort().join(',')
  const b = [...(prev || [])].sort().join(',')
  return a !== b
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
      Message.success('调整成功')
      moveModalVisible.value = false
      clearUserSelection()
      await fetchCurrentUsers()
      await fetchTree()
    } else {
      Message.error(res.msg || '调整失败')
    }
  } catch { Message.error('调整失败') }
  done()
}

const submitMoveForm = () => callFormHandler(handleMoveOk)

const handleBatchStatus = async (status) => {
  if (selectedUserIds.value.length === 0) {
    Message.warning('请先选择用户')
    return
  }
  try {
    const res = await userApi.systemBatchStatus(selectedUserIds.value, status)
    if (res.code === 200) {
      Message.success(status === 1 ? '已批量启用' : '已批量禁用')
      clearUserSelection()
      await fetchCurrentUsers()
      await fetchTree()
    } else {
      Message.error(res.msg || '批量操作失败')
    }
  } catch {
    Message.error('批量操作失败')
  }
}

const displayRoleNames = (record) => {
  if (record.roleNames?.length) return record.roleNames
  return [roleTypeLabel(roleTypeOptions, record.role === 'admin' ? 'admin' : 'user')]
}

const primaryRoleName = (record) => displayRoleNames(record)[0] || '-'

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

const confirmDelete = async (node) => {
  const childCount = countChildren(node)
  deleteTarget.value = { ...node, childCount }
  deleteImpact.value = null
  deleteModalVisible.value = true
  deleteImpactLoading.value = true
  try {
    const res = await deptApi.deleteImpact(node.id)
    deleteImpact.value = res.code === 200 ? res.data : { childCount, userCount: 0, canDelete: childCount === 0 }
  } catch {
    deleteImpact.value = { childCount, userCount: 0, canDelete: childCount === 0 }
  } finally {
    deleteImpactLoading.value = false
  }
}

const exportDepartments = async () => {
  try {
    const blob = await deptApi.export()
    downloadBlob(blob, `部门列表_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}.xlsx`)
    Message.success('部门列表已开始导出')
  } catch {
    Message.error('导出失败')
  }
}

const downloadBlob = (blob, fileName) => {
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(objectUrl)
}

const countChildren = (node) => {
  let c = node.children?.length || 0
  for (const ch of node.children || []) c += countChildren(ch)
  return c
}

const countManagerDepts = (nodes) => {
  let count = 0
  for (const node of nodes || []) {
    if (node.managerName) count += 1
    if (node.children?.length) count += countManagerDepts(node.children)
  }
  return count
}

const handleDeleteOk = async (done) => {
  if (deleteImpact.value && !deleteImpact.value.canDelete) {
    Message.warning('请先调整子部门或用户后再删除')
    done(false)
    return
  }
  try {
    const res = await deptApi.delete(deleteTarget.value.id)
    if (res.code === 200) {
      Message.success('删除成功')
      deleteModalVisible.value = false
      deleteImpact.value = null
      if (selectedDeptId.value == deleteTarget.value.id) {
        selectedDeptId.value = null
        selectedDeptName.value = ''
        userList.value = []
      }
      fetchTree()
    } else {
      Message.error(res.msg || '删除失败')
    }
  } catch { Message.error('删除失败') }
  done()
}

const submitDeleteForm = () => callFormHandler(handleDeleteOk)

const refreshAfterImportResult = async () => {
  importResultVisible.value = false
  await fetchTree()
}

onMounted(async () => {
  await Promise.all([
    applyPaginationConfig(userSize, userPageSizeOptions),
    applyPasswordPolicy(passwordPolicy),
    loadStatusOptions(),
    loadRoleTypeOptions(),
    fetchRoles()
  ])
  await fetchTree()
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
.dept-page {
  min-height: 100vh;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-bg-layout-start)) 0%, var(--ds-color-bg-layout-mid) 180px, var(--ds-color-bg-layout) 100%);
}
.main { max-width: 1680px; width: 100%; margin: 0 auto; padding: 24px; display: flex; flex-direction: column; gap: 18px; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.dept-layout { display: flex; gap: 16px; min-height: calc(100vh - 280px); }
.user-list-panel { flex: 1; border: 1px solid color-mix(in srgb, var(--ds-color-primary) 4%, var(--ds-color-border)); background: var(--ds-color-bg-card); border-radius: 12px; box-shadow: 0 8px 24px var(--ds-color-shadow); display: flex; flex-direction: column; min-width: 0; }
.panel-header { padding: 16px 20px; border-bottom: 1px solid var(--ds-color-border); display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; gap: 12px; }
.panel-heading { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.panel-title { font-size: 16px; font-weight: 600; color: var(--ds-color-text-primary); }
.panel-desc { color: var(--ds-color-text-secondary); font-size: 12px; line-height: 1.5; }
.header-actions { display: flex; gap: 10px; align-items: center; justify-content: flex-end; min-width: 420px; flex-shrink: 0; }
.panel-primary-button { height: 32px; border-radius: var(--ds-radius-sm); flex-shrink: 0; }
.user-placeholder { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.placeholder-icon { font-size: 48px; color: var(--ds-color-text-placeholder); }
.placeholder-title { margin-top: 12px; color: var(--ds-color-text-regular); font-size: 14px; }
.placeholder-desc { margin-top: 6px; color: var(--ds-color-text-secondary); font-size: 12px; }
.user-table-area { flex: 1; overflow: auto; padding: 16px 20px 20px; }
.user-toolbar { margin-bottom: 14px; display: flex; align-items: flex-end; gap: 12px; flex-wrap: wrap; }
.user-search { width: 260px; }
.user-filter { width: 150px; }
.user-role-filter { width: 220px; }
.toolbar-button { min-width: 72px; height: 36px; border-radius: 8px; }
.identity-cell {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  max-width: 100%;
  white-space: nowrap;
}
.dept-page :deep(.arco-table-th .table-checkbox) {
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
.row-actions { display: flex; gap: 4px; align-items: center; justify-content: flex-start; flex-wrap: nowrap; white-space: nowrap; }
.row-actions :deep(.arco-btn-text) {
  padding: 0 10px;
  border-radius: 8px;
  color: var(--ds-color-primary);
  height: 28px;
}
.row-actions :deep(.arco-btn-text:hover) { background: var(--ds-color-bg-hover); }
.no-actions { color: var(--ds-color-text-secondary); padding: 0 8px; }
.role-cell { display: inline-flex; align-items: center; gap: 6px; max-width: 100%; white-space: nowrap; overflow: hidden; }
.role-cell :deep(.ds-role-tag) { max-width: 132px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.role-more { color: var(--ds-color-text-secondary); font-size: 12px; line-height: 18px; white-space: nowrap; }
.move-tip { margin-top: 10px; color: var(--ds-color-text-secondary); font-size: 13px; }
.form-tip { color: var(--ds-color-text-secondary); font-size: 12px; margin-top: 6px; }
.hidden-file-input { display: none; }
.import-result-box {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-start;
  margin-top: 0;
  padding: 12px;
  border: 1px solid var(--ds-color-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-color-bg-soft);
  color: var(--ds-color-text-regular);
  font-size: 13px;
}
.import-result-text { margin-top: 12px; }
.import-result-details { margin-top: 12px; color: var(--ds-color-text-regular); font-size: 13px; line-height: 1.7; }
.import-result-details ul { margin: 8px 0 0; padding-left: 18px; }
.delete-confirm {
  color: var(--ds-color-text-primary);
  font-size: 14px;
  line-height: 1.8;
}
.delete-confirm p { margin: 0 0 8px; }
.delete-confirm__muted { color: var(--ds-color-text-secondary); }
.delete-confirm__danger { color: var(--ds-color-danger); }
.delete-confirm__tip { font-size: 12px; }
@media (max-width: 1280px) {
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .dept-layout { flex-direction: column; }
  .header-actions { min-width: 0; width: 100%; justify-content: flex-start; flex-wrap: wrap; }
}
@media (max-width: 760px) {
  .summary-grid { grid-template-columns: 1fr; }
}
</style>
