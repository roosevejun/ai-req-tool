<template>
  <section class="grid">
    <div class="panel">
      <h3>角色管理</h3>
      <div class="form-grid">
        <input v-model.trim="newRole.roleCode" class="input" placeholder="角色编码，例如 ADMIN" />
        <input v-model.trim="newRole.roleName" class="input" placeholder="角色名称" />
        <select v-model="newRole.status" class="input">
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading || !newRole.roleCode || !newRole.roleName" @click="$emit('create-role')">
          创建角色
        </button>
      </div>
      <ul class="list">
        <li v-for="r in roles" :key="r.id">
          <span>{{ r.roleCode }} - {{ r.roleName }} ({{ statusLabel(r.status) }})</span>
          <span class="inline-actions">
            <button class="mini" @click="$emit('open-role-edit', r)">编辑</button>
            <button class="mini" @click="$emit('open-role-perms', r)">权限</button>
          </span>
        </li>
        <li v-if="roles.length === 0" class="muted">暂无角色</li>
      </ul>
    </div>

    <div class="panel">
      <h3>权限管理</h3>
      <div class="form-grid">
        <input v-model.trim="newPerm.permCode" class="input" placeholder="权限编码，例如 SYSTEM:MANAGE" />
        <input v-model.trim="newPerm.permName" class="input" placeholder="权限名称" />
        <select v-model="newPerm.status" class="input">
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
      </div>
      <div class="row">
        <button class="primary" :disabled="loading || !newPerm.permCode || !newPerm.permName" @click="$emit('create-permission')">
          创建权限
        </button>
      </div>
      <ul class="list">
        <li v-for="p in permissions" :key="p.id">
          <span>{{ p.permCode }} - {{ p.permName }} ({{ statusLabel(p.status) }})</span>
          <button class="mini" @click="$emit('open-perm-edit', p)">编辑</button>
        </li>
        <li v-if="permissions.length === 0" class="muted">暂无权限</li>
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
.grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 14px; margin-bottom: 14px; }
.panel h3 { margin: 0; }
.row { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-top: 8px; }
.input { width: 100%; box-sizing: border-box; padding: 8px 10px; border-radius: 8px; border: 1px solid #d1d5db; }
.list { margin: 12px 0 0; padding-left: 18px; }
.list li { margin-bottom: 8px; }
.inline-actions { margin-left: 8px; display: inline-flex; gap: 8px; }
.primary, .mini { border-radius: 8px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; }
.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.mini { background: #f3f4f6; padding: 5px 9px; font-size: 12px; }
.muted { color: #6b7280; }
@media (max-width: 980px) { .grid, .form-grid { grid-template-columns: 1fr; } }
</style>
