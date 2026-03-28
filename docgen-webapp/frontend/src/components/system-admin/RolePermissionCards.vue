<template>
  <section class="grid">
    <div class="panel">
      <div class="panel-head">
        <div>
          <h3>角色管理</h3>
          <p class="copy">维护角色编码、显示名称以及启用状态。</p>
        </div>
      </div>

      <div class="form-grid">
        <input v-model.trim="newRole.roleCode" class="input" placeholder="角色编码，例如 ADMIN" />
        <input v-model.trim="newRole.roleName" class="input" placeholder="角色名称" />
        <select v-model="newRole.status" class="input">
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
      </div>

      <div class="actions">
        <button class="primary" :disabled="loading || !newRole.roleCode || !newRole.roleName" @click="$emit('create-role')">
          创建角色
        </button>
      </div>

      <ul class="list">
        <li v-for="role in roles" :key="role.id">
          <span>{{ role.roleCode }} - {{ role.roleName }}（{{ statusLabel(role.status) }}）</span>
          <span class="inline-actions">
            <button class="mini" @click="$emit('open-role-edit', role)">编辑</button>
            <button class="mini" @click="$emit('open-role-perms', role)">绑定权限</button>
          </span>
        </li>
        <li v-if="roles.length === 0" class="muted">暂无角色。</li>
      </ul>
    </div>

    <div class="panel">
      <div class="panel-head">
        <div>
          <h3>权限管理</h3>
          <p class="copy">维护权限编码和权限名称，供角色绑定使用。</p>
        </div>
      </div>

      <div class="form-grid">
        <input v-model.trim="newPerm.permCode" class="input" placeholder="权限编码，例如 SYSTEM:MANAGE" />
        <input v-model.trim="newPerm.permName" class="input" placeholder="权限名称" />
        <select v-model="newPerm.status" class="input">
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
      </div>

      <div class="actions">
        <button class="primary" :disabled="loading || !newPerm.permCode || !newPerm.permName" @click="$emit('create-permission')">
          创建权限
        </button>
      </div>

      <ul class="list">
        <li v-for="permission in permissions" :key="permission.id">
          <span>{{ permission.permCode }} - {{ permission.permName }}（{{ statusLabel(permission.status) }}）</span>
          <button class="mini" @click="$emit('open-perm-edit', permission)">编辑</button>
        </li>
        <li v-if="permissions.length === 0" class="muted">暂无权限。</li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
import { statusLabel } from './helpers'
import type { NewPermForm, NewRoleForm, PermItem, RoleItem } from './types'

defineProps<{
  loading: boolean
  newRole: NewRoleForm
  newPerm: NewPermForm
  roles: RoleItem[]
  permissions: PermItem[]
}>()

defineEmits<{
  (event: 'create-role'): void
  (event: 'create-permission'): void
  (event: 'open-role-edit', role: RoleItem): void
  (event: 'open-role-perms', role: RoleItem): void
  (event: 'open-perm-edit', perm: PermItem): void
}>()
</script>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.panel {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 18px;
  padding: 18px;
}

.panel-head h3 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.copy {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.list {
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.list li {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e5edf5;
  border-radius: 12px;
  background: #f8fafc;
}

.inline-actions {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
}

.primary,
.mini {
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 8px 12px;
  cursor: pointer;
}

.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.mini {
  background: #fff;
  padding: 6px 10px;
  font-size: 12px;
}

.muted {
  color: #64748b;
}

@media (max-width: 980px) {
  .grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .list li {
    flex-direction: column;
  }
}
</style>
