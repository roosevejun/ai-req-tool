<template>
  <section v-if="editingUser" class="panel">
    <h3>编辑用户 {{ editingUser.username }}</h3>
    <div class="form-grid">
      <input v-model.trim="editUserForm.displayName" class="input" placeholder="显示名称" />
      <select v-model="editUserForm.status" class="input">
        <option value="ENABLED">启用</option>
        <option value="DISABLED">禁用</option>
      </select>
    </div>
    <div class="row">
      <button class="primary" :disabled="loading" @click="$emit('save-user-edit')">保存</button>
      <button class="ghost" :disabled="loading" @click="$emit('close-user-edit')">取消</button>
    </div>
  </section>

  <section v-if="bindingUser" class="panel">
    <h3>为 {{ bindingUser.username }} 绑定角色</h3>
    <div class="checkbox-row">
      <label v-for="r in roles" :key="`bur-${r.id}`" class="check-item">
        <input v-model="bindUserRoleIds" type="checkbox" :value="r.id" />
        {{ r.roleCode }} - {{ r.roleName }}
      </label>
    </div>
    <div class="row">
      <button class="primary" :disabled="loading" @click="$emit('save-user-roles')">保存角色</button>
      <button class="ghost" :disabled="loading" @click="$emit('close-user-roles')">取消</button>
    </div>
  </section>

  <section v-if="editingRole" class="panel">
    <h3>编辑角色 {{ editingRole.roleCode }}</h3>
    <div class="form-grid">
      <input v-model.trim="editRoleForm.roleName" class="input" placeholder="角色名称" />
      <select v-model="editRoleForm.status" class="input">
        <option value="ENABLED">启用</option>
        <option value="DISABLED">禁用</option>
      </select>
    </div>
    <div class="row">
      <button class="primary" :disabled="loading" @click="$emit('save-role-edit')">保存</button>
      <button class="ghost" :disabled="loading" @click="$emit('close-role-edit')">取消</button>
    </div>
  </section>

  <section v-if="bindingRole" class="panel">
    <h3>为 {{ bindingRole.roleCode }} 绑定权限</h3>
    <div class="checkbox-row">
      <label v-for="p in permissions" :key="`brp-${p.id}`" class="check-item">
        <input v-model="bindRolePermIds" type="checkbox" :value="p.id" />
        {{ p.permCode }} - {{ p.permName }}
      </label>
    </div>
    <div class="row">
      <button class="primary" :disabled="loading" @click="$emit('save-role-perms')">保存权限</button>
      <button class="ghost" :disabled="loading" @click="$emit('close-role-perms')">取消</button>
    </div>
  </section>

  <section v-if="editingPerm" class="panel">
    <h3>编辑权限 {{ editingPerm.permCode }}</h3>
    <div class="form-grid">
      <input v-model.trim="editPermForm.permName" class="input" placeholder="权限名称" />
      <select v-model="editPermForm.status" class="input">
        <option value="ENABLED">启用</option>
        <option value="DISABLED">禁用</option>
      </select>
    </div>
    <div class="row">
      <button class="primary" :disabled="loading" @click="$emit('save-perm-edit')">保存</button>
      <button class="ghost" :disabled="loading" @click="$emit('close-perm-edit')">取消</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { EditPermForm, EditRoleForm, EditUserForm, PermItem, RoleItem, UserItem } from './types'

const bindUserRoleIds = defineModel<number[]>('bindUserRoleIds', { required: true })
const bindRolePermIds = defineModel<number[]>('bindRolePermIds', { required: true })

defineProps<{
  loading: boolean
  editingUser: UserItem | null
  editUserForm: EditUserForm
  bindingUser: UserItem | null
  editingRole: RoleItem | null
  editRoleForm: EditRoleForm
  bindingRole: RoleItem | null
  editingPerm: PermItem | null
  editPermForm: EditPermForm
  roles: RoleItem[]
  permissions: PermItem[]
}>()

defineEmits<{
  (event: 'save-user-edit'): void
  (event: 'close-user-edit'): void
  (event: 'save-user-roles'): void
  (event: 'close-user-roles'): void
  (event: 'save-role-edit'): void
  (event: 'close-role-edit'): void
  (event: 'save-role-perms'): void
  (event: 'close-role-perms'): void
  (event: 'save-perm-edit'): void
  (event: 'close-perm-edit'): void
}>()
</script>

<style scoped>
.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 14px; margin-bottom: 14px; }
.panel h3 { margin: 0; }
.row { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.form-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-top: 8px; }
.checkbox-row { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.check-item { border: 1px solid #e5e7eb; border-radius: 8px; padding: 6px 8px; font-size: 13px; }
.input { width: 100%; box-sizing: border-box; padding: 8px 10px; border-radius: 8px; border: 1px solid #d1d5db; }
.primary, .ghost { border-radius: 8px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; }
.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.ghost { background: #f3f4f6; }
@media (max-width: 980px) { .form-grid { grid-template-columns: 1fr; } }
</style>
