<template>
  <div class="container">
    <SystemQuickActionsCard :loading="loading" @go-create-ai="goCreateAi" @go-ai-docgen="goAiDocgen" />

    <UserManagementCard
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

    <BindingPanels
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

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import BindingPanels from '../components/system-admin/BindingPanels.vue'
import RolePermissionCards from '../components/system-admin/RolePermissionCards.vue'
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

const newUser = ref<NewUserForm>({
  username: '',
  password: '',
  displayName: '',
  status: 'ENABLED',
  roleIds: []
})

const newRole = ref<NewRoleForm>({
  roleCode: '',
  roleName: '',
  status: 'ENABLED'
})

const newPerm = ref<NewPermForm>({
  permCode: '',
  permName: '',
  status: 'ENABLED'
})

const editingUser = ref<UserItem | null>(null)
const editUserForm = ref<EditUserForm>({
  displayName: '',
  status: 'ENABLED'
})

const bindingUser = ref<UserItem | null>(null)
const bindUserRoleIds = ref<number[]>([])

const editingRole = ref<RoleItem | null>(null)
const editRoleForm = ref<EditRoleForm>({
  roleName: '',
  status: 'ENABLED'
})

const bindingRole = ref<RoleItem | null>(null)
const bindRolePermIds = ref<number[]>([])

const editingPerm = ref<PermItem | null>(null)
const editPermForm = ref<EditPermForm>({
  permName: '',
  status: 'ENABLED'
})

function clearTips() {
  error.value = ''
  success.value = ''
}

function showError(e: any, fallback: string) {
  error.value = e?.response?.data?.message || e?.message || fallback
}

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
  } finally {
    loading.value = false
  }
}

async function createUser() {
  loading.value = true
  clearTips()
  try {
    await axios.post('/api/system/users', newUser.value)
    success.value = '用户创建成功。'
    newUser.value = {
      username: '',
      password: '',
      displayName: '',
      status: 'ENABLED',
      roleIds: []
    }
    await loadAll()
  } catch (e: any) {
    showError(e, '创建用户失败。')
    loading.value = false
  }
}

function openUserEdit(user: UserItem) {
  editingUser.value = user
  editUserForm.value = {
    displayName: user.displayName || '',
    status: user.status || 'ENABLED'
  }
}

async function saveUserEdit() {
  if (!editingUser.value) return
  loading.value = true
  clearTips()
  try {
    await axios.put(`/api/system/users/${editingUser.value.id}`, editUserForm.value)
    success.value = '用户更新成功。'
    editingUser.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '更新用户失败。')
    loading.value = false
  }
}

function openUserRoles(user: UserItem) {
  bindingUser.value = user
  const roleCodes = new Set(user.roleCodes || [])
  bindUserRoleIds.value = roles.value.filter((role) => roleCodes.has(role.roleCode)).map((role) => role.id)
}

async function saveUserRoles() {
  if (!bindingUser.value) return
  loading.value = true
  clearTips()
  try {
    await axios.post(`/api/system/users/${bindingUser.value.id}/roles`, {
      roleIds: bindUserRoleIds.value
    })
    success.value = '用户角色保存成功。'
    bindingUser.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '保存用户角色失败。')
    loading.value = false
  }
}

async function resetPassword(user: UserItem) {
  const password = window.prompt(`请为 ${user.username} 输入新密码`)
  if (!password) return
  loading.value = true
  clearTips()
  try {
    await axios.put(`/api/system/users/${user.id}/password`, { newPassword: password })
    success.value = '密码重置成功。'
  } catch (e: any) {
    showError(e, '重置密码失败。')
  } finally {
    loading.value = false
  }
}

async function createRole() {
  loading.value = true
  clearTips()
  try {
    await axios.post('/api/system/roles', newRole.value)
    success.value = '角色创建成功。'
    newRole.value = {
      roleCode: '',
      roleName: '',
      status: 'ENABLED'
    }
    await loadAll()
  } catch (e: any) {
    showError(e, '创建角色失败。')
    loading.value = false
  }
}

function openRoleEdit(role: RoleItem) {
  editingRole.value = role
  editRoleForm.value = {
    roleName: role.roleName || '',
    status: role.status || 'ENABLED'
  }
}

async function saveRoleEdit() {
  if (!editingRole.value) return
  loading.value = true
  clearTips()
  try {
    await axios.put(`/api/system/roles/${editingRole.value.id}`, editRoleForm.value)
    success.value = '角色更新成功。'
    editingRole.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '更新角色失败。')
    loading.value = false
  }
}

function openRolePerms(role: RoleItem) {
  bindingRole.value = role
  bindRolePermIds.value = []
}

async function saveRolePerms() {
  if (!bindingRole.value) return
  loading.value = true
  clearTips()
  try {
    await axios.post(`/api/system/roles/${bindingRole.value.id}/permissions`, {
      permissionIds: bindRolePermIds.value
    })
    success.value = '角色权限保存成功。'
    bindingRole.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '保存角色权限失败。')
    loading.value = false
  }
}

async function createPermission() {
  loading.value = true
  clearTips()
  try {
    await axios.post('/api/system/permissions', newPerm.value)
    success.value = '权限创建成功。'
    newPerm.value = {
      permCode: '',
      permName: '',
      status: 'ENABLED'
    }
    await loadAll()
  } catch (e: any) {
    showError(e, '创建权限失败。')
    loading.value = false
  }
}

function openPermEdit(permission: PermItem) {
  editingPerm.value = permission
  editPermForm.value = {
    permName: permission.permName || '',
    status: permission.status || 'ENABLED'
  }
}

async function savePermEdit() {
  if (!editingPerm.value) return
  loading.value = true
  clearTips()
  try {
    await axios.put(`/api/system/permissions/${editingPerm.value.id}`, editPermForm.value)
    success.value = '权限更新成功。'
    editingPerm.value = null
    await loadAll()
  } catch (e: any) {
    showError(e, '更新权限失败。')
    loading.value = false
  }
}

function goCreateAi() {
  void router.push('/projects/create-ai')
}

function goAiDocgen() {
  void router.push('/docgen')
}

onMounted(loadAll)
</script>

<style scoped>
.container {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 16px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, "PingFang SC", "Hiragino Sans GB",
    "Microsoft YaHei", sans-serif;
}
.error {
  color: #b91c1c;
  margin-top: 10px;
}
.success {
  color: #166534;
  margin-top: 10px;
}
</style>
