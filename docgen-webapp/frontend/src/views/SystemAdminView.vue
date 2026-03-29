<template>
  <div class="page">
    <CenterHero
      eyebrow="System Administration Center"
      title="系统管理"
      summary="统一维护用户、角色、权限和治理动作，并从这里进入模板标准和业务工作台。"
    >
      <template #badges>
        <StatusBadge :label="`${users.length} 个用户`" variant="info" />
        <StatusBadge :label="`${roles.length} 个角色`" variant="ai" />
        <StatusBadge :label="`${permissions.length} 条权限`" variant="success" />
      </template>
    </CenterHero>

    <div class="layout">
      <main class="content">
        <WorkspaceSection
          eyebrow="管理分区"
          title="管理工作台"
          description="按对象切换用户、角色和权限管理区域，保持系统操作结构更清晰。"
          :tint="true"
        >
          <template #actions>
            <div class="tab-list">
              <button
                v-for="tab in adminTabs"
                :key="tab.key"
                class="tab"
                :class="{ 'tab--active': activeAdminTab === tab.key }"
                @click="activeAdminTab = tab.key"
              >
                {{ tab.label }}
              </button>
            </div>
          </template>

          <div class="workspace-stack">
            <UserManagementCard
              v-if="activeAdminTab === 'users'"
              :loading="loading"
              :new-user="newUser"
              :roles="roles"
              :users="users"
              @refresh="loadAll"
              @create-user="createUser"
              @open-user-edit="openUserEdit"
              @open-user-roles="openUserRoles"
              @reset-password="resetPassword"
            />

            <RolePermissionCards
              v-else
              :loading="loading"
              :new-role="newRole"
              :new-perm="newPerm"
              :roles="roles"
              :permissions="permissions"
              @create-role="createRole"
              @create-permission="createPermission"
              @open-role-edit="openRoleEdit"
              @open-role-perms="openRolePerms"
              @open-perm-edit="openPermEdit"
            />
          </div>
        </WorkspaceSection>
      </main>

      <aside class="sidebar">
        <SystemHealthPanel
          :users-count="users.length"
          :roles-count="roles.length"
          :permissions-count="permissions.length"
          :pending-forms-count="hasBindingPanels ? 1 : 0"
        />

        <SystemQuickActionsCard
          :loading="loading"
          @go-create-ai="goCreateAi"
          @go-ai-docgen="goAiDocgen"
          @go-template-center="goTemplateCenter"
        />

        <WorkspaceSection
          eyebrow="待处理操作"
          title="待处理操作"
          description="这里会承接当前打开的编辑、绑定和修正动作，避免和主列表混在一起。"
        >
          <BindingPanels
            v-if="hasBindingPanels"
            :loading="loading"
            :editing-user="editingUser"
            :edit-user-form="editUserForm"
            :binding-user="bindingUser"
            v-model:bind-user-role-ids="bindUserRoleIds"
            :editing-role="editingRole"
            :edit-role-form="editRoleForm"
            :binding-role="bindingRole"
            v-model:bind-role-perm-ids="bindRolePermIds"
            :editing-perm="editingPerm"
            :edit-perm-form="editPermForm"
            :roles="roles"
            :permissions="permissions"
            @save-user-edit="saveUserEdit"
            @close-user-edit="editingUser = null"
            @save-user-roles="saveUserRoles"
            @close-user-roles="bindingUser = null"
            @save-role-edit="saveRoleEdit"
            @close-role-edit="editingRole = null"
            @save-role-perms="saveRolePerms"
            @close-role-perms="bindingRole = null"
            @save-perm-edit="savePermEdit"
            @close-perm-edit="editingPerm = null"
          />
          <EmptyWorkspaceState
            v-else
            eyebrow="系统就绪"
            title="当前没有待处理表单"
            description="从左侧工作台选择用户、角色或权限后，对应的编辑和绑定动作会显示在这里。"
          />
        </WorkspaceSection>

        <div class="feedback-stack">
          <FeedbackPanel title="下一步建议" :message="systemAdvice" tone="warning" />
          <FeedbackPanel title="处理提示" :message="error" tone="danger" />
          <FeedbackPanel title="最新进展" :message="success" tone="success" />
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import EmptyWorkspaceState from '../components/projects/EmptyWorkspaceState.vue'
import FeedbackPanel from '../components/projects/FeedbackPanel.vue'
import StatusBadge from '../components/projects/StatusBadge.vue'
import WorkspaceSection from '../components/projects/WorkspaceSection.vue'
import CenterHero from '../components/shell/CenterHero.vue'
import BindingPanels from '../components/system-admin/BindingPanels.vue'
import RolePermissionCards from '../components/system-admin/RolePermissionCards.vue'
import SystemHealthPanel from '../components/system-admin/SystemHealthPanel.vue'
import SystemQuickActionsCard from '../components/system-admin/SystemQuickActionsCard.vue'
import UserManagementCard from '../components/system-admin/UserManagementCard.vue'
import type {
  ApiResponse,
  EditPermForm,
  EditRoleForm,
  EditUserForm,
  NewPermForm,
  NewRoleForm,
  NewUserForm,
  PermItem,
  RoleItem,
  UserItem
} from '../components/system-admin/types'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const success = ref('')
const users = ref<UserItem[]>([])
const roles = ref<RoleItem[]>([])
const permissions = ref<PermItem[]>([])
const activeAdminTab = ref<'users' | 'roles'>('users')

const adminTabs = [
  { key: 'users' as const, label: '用户' },
  { key: 'roles' as const, label: '角色与权限' }
]

const newUser = ref<NewUserForm>({ username: '', password: '', displayName: '', status: 'ENABLED', roleIds: [] })
const newRole = ref<NewRoleForm>({ roleCode: '', roleName: '', status: 'ENABLED' })
const newPerm = ref<NewPermForm>({ permCode: '', permName: '', status: 'ENABLED' })
const editingUser = ref<UserItem | null>(null)
const editUserForm = ref<EditUserForm>({ displayName: '', status: 'ENABLED' })
const bindingUser = ref<UserItem | null>(null)
const bindUserRoleIds = ref<number[]>([])
const editingRole = ref<RoleItem | null>(null)
const editRoleForm = ref<EditRoleForm>({ roleName: '', status: 'ENABLED' })
const bindingRole = ref<RoleItem | null>(null)
const bindRolePermIds = ref<number[]>([])
const editingPerm = ref<PermItem | null>(null)
const editPermForm = ref<EditPermForm>({ permName: '', status: 'ENABLED' })

const hasBindingPanels = computed(() => !!(editingUser.value || bindingUser.value || editingRole.value || bindingRole.value || editingPerm.value))
const systemAdvice = computed(() => {
  if (users.value.length === 0) return '先创建一个系统用户，保证后续权限、模板和项目资产都有明确归属。'
  if (roles.value.length === 0) return '当前还没有角色，建议先建立角色体系，再给用户分配职责。'
  if (permissions.value.length === 0) return '当前还没有权限项，建议先补充权限，再推进模板和知识能力治理。'
  if (hasBindingPanels.value) return '当前有待处理表单，建议先完成保存或绑定，避免系统配置上下文丢失。'
  return '当前治理基础链路已具备，可以继续进入模板中心或业务工作台推进标准化。'
})

function clearTips() { error.value = ''; success.value = '' }
function showError(e: any, fallback: string) { error.value = e?.response?.data?.message || e?.message || fallback }

async function loadAll() {
  loading.value = true
  clearTips()
  try {
    const [userRes, roleRes, permRes] = await Promise.all([
      axios.get<ApiResponse<UserItem[]>>('/api/system/users'),
      axios.get<ApiResponse<RoleItem[]>>('/api/system/roles'),
      axios.get<ApiResponse<PermItem[]>>('/api/system/permissions')
    ])
    users.value = userRes.data.data || []
    roles.value = roleRes.data.data || []
    permissions.value = permRes.data.data || []
  } catch (e: any) {
    showError(e, '加载系统管理数据失败。')
  } finally { loading.value = false }
}

async function createUser() {
  loading.value = true
  clearTips()
  try {
    await axios.post('/api/system/users', newUser.value)
    success.value = '用户创建成功。'
    newUser.value = { username: '', password: '', displayName: '', status: 'ENABLED', roleIds: [] }
    await loadAll()
  } catch (e: any) { showError(e, '创建用户失败。'); loading.value = false }
}
function openUserEdit(user: UserItem) { editingUser.value = user; editUserForm.value = { displayName: user.displayName || '', status: user.status || 'ENABLED' } }
async function saveUserEdit() {
  if (!editingUser.value) return
  loading.value = true
  clearTips()
  try { await axios.put(`/api/system/users/${editingUser.value.id}`, editUserForm.value); success.value = '用户更新成功。'; editingUser.value = null; await loadAll() }
  catch (e: any) { showError(e, '更新用户失败。'); loading.value = false }
}
function openUserRoles(user: UserItem) { bindingUser.value = user; const roleCodes = new Set(user.roleCodes || []); bindUserRoleIds.value = roles.value.filter((role) => roleCodes.has(role.roleCode)).map((role) => role.id) }
async function saveUserRoles() {
  if (!bindingUser.value) return
  loading.value = true
  clearTips()
  try { await axios.post(`/api/system/users/${bindingUser.value.id}/roles`, { roleIds: bindUserRoleIds.value }); success.value = '用户角色保存成功。'; bindingUser.value = null; await loadAll() }
  catch (e: any) { showError(e, '保存用户角色失败。'); loading.value = false }
}
async function resetPassword(user: UserItem) {
  const password = window.prompt(`请为 ${user.username} 输入新密码`)
  if (!password) return
  loading.value = true
  clearTips()
  try { await axios.put(`/api/system/users/${user.id}/password`, { newPassword: password }); success.value = '密码重置成功。' }
  catch (e: any) { showError(e, '重置密码失败。') }
  finally { loading.value = false }
}
async function createRole() {
  loading.value = true
  clearTips()
  try { await axios.post('/api/system/roles', newRole.value); success.value = '角色创建成功。'; newRole.value = { roleCode: '', roleName: '', status: 'ENABLED' }; await loadAll() }
  catch (e: any) { showError(e, '创建角色失败。'); loading.value = false }
}
function openRoleEdit(role: RoleItem) { editingRole.value = role; editRoleForm.value = { roleName: role.roleName || '', status: role.status || 'ENABLED' } }
async function saveRoleEdit() {
  if (!editingRole.value) return
  loading.value = true
  clearTips()
  try { await axios.put(`/api/system/roles/${editingRole.value.id}`, editRoleForm.value); success.value = '角色更新成功。'; editingRole.value = null; await loadAll() }
  catch (e: any) { showError(e, '更新角色失败。'); loading.value = false }
}
function openRolePerms(role: RoleItem) { bindingRole.value = role; bindRolePermIds.value = [] }
async function saveRolePerms() {
  if (!bindingRole.value) return
  loading.value = true
  clearTips()
  try { await axios.post(`/api/system/roles/${bindingRole.value.id}/permissions`, { permissionIds: bindRolePermIds.value }); success.value = '角色权限保存成功。'; bindingRole.value = null; await loadAll() }
  catch (e: any) { showError(e, '保存角色权限失败。'); loading.value = false }
}
async function createPermission() {
  loading.value = true
  clearTips()
  try { await axios.post('/api/system/permissions', newPerm.value); success.value = '权限创建成功。'; newPerm.value = { permCode: '', permName: '', status: 'ENABLED' }; await loadAll() }
  catch (e: any) { showError(e, '创建权限失败。'); loading.value = false }
}
function openPermEdit(permission: PermItem) { editingPerm.value = permission; editPermForm.value = { permName: permission.permName || '', status: permission.status || 'ENABLED' } }
async function savePermEdit() {
  if (!editingPerm.value) return
  loading.value = true
  clearTips()
  try { await axios.put(`/api/system/permissions/${editingPerm.value.id}`, editPermForm.value); success.value = '权限更新成功。'; editingPerm.value = null; await loadAll() }
  catch (e: any) { showError(e, '更新权限失败。'); loading.value = false }
}
function goCreateAi() { void router.push('/projects/create-ai') }
function goAiDocgen() { void router.push('/docgen') }
function goTemplateCenter() { void router.push('/templates') }

onMounted(loadAll)
</script>

<style scoped>
.page { max-width: 1480px; margin: 18px auto; padding: 0 14px 18px; font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; }
.layout { display: grid; grid-template-columns: minmax(0, 1fr) 360px; gap: 14px; align-items: start; }
.content, .sidebar { min-width: 0; }
.workspace-stack, .feedback-stack, .sidebar { display: grid; gap: 14px; }
.tab-list { display: flex; flex-wrap: wrap; gap: 8px; }
.tab { border-radius: 999px; border: 1px solid #cbd5e1; background: #fff; padding: 8px 12px; cursor: pointer; color: #334155; }
.tab--active { background: #0f172a; border-color: #0f172a; color: #fff; }
@media (max-width: 1080px) { .layout { grid-template-columns: 1fr; } }
</style>
