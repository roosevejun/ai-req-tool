<template>
  <section class="card">
    <div class="card-head">
      <div>
        <p class="eyebrow">团队协作</p>
        <h3>成员与角色</h3>
      </div>
      <span class="count-badge">{{ members.length }} 人</span>
    </div>

    <p class="summary">
      {{ members.length > 0 ? "维护负责人和核心协作角色，确保项目资料、AI 补全和后续需求有人持续推进。" : "当前还没有项目成员，建议先补充负责人和核心协作角色。" }}
    </p>

    <div class="form-grid">
      <select v-model="memberForm.selectedUserId" class="input">
        <option value="">从用户列表中选择，可选</option>
        <option v-for="u in userOptions" :key="u.id" :value="String(u.id)">
          {{ u.displayName || u.username }} ({{ u.id }})
        </option>
      </select>
      <input v-model.trim="memberForm.userId" class="input" placeholder="用户 ID，例如 1" />
      <select v-model="memberForm.projectRole" class="input">
        <option value="OWNER">负责人</option>
        <option value="PM">产品经理</option>
        <option value="DEV">开发</option>
        <option value="QA">测试</option>
        <option value="VIEWER">只读</option>
      </select>
      <button class="primary" :disabled="loading || !(memberForm.selectedUserId || memberForm.userId)" @click="$emit('add-member')">
        添加成员
      </button>
    </div>

    <table class="table">
      <thead>
        <tr>
          <th>用户 ID</th>
          <th>用户名</th>
          <th>显示名称</th>
          <th>项目角色</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="m in members" :key="m.id">
          <td>{{ m.userId }}</td>
          <td>{{ m.username || "-" }}</td>
          <td>{{ m.displayName || "-" }}</td>
          <td>{{ projectRoleLabel(m.projectRole) }}</td>
          <td>{{ memberStatusLabel(m.status) }}</td>
          <td class="ops">
            <button class="mini" @click="$emit('remove-member', m.userId)">移除</button>
          </td>
        </tr>
        <tr v-if="members.length === 0">
          <td colspan="6" class="empty small">暂无项目成员</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import type { MemberFormState, ProjectMemberItem, UserOption } from './types'

defineProps<{
  loading: boolean
  memberForm: MemberFormState
  userOptions: UserOption[]
  members: ProjectMemberItem[]
  projectRoleLabel: (value?: string) => string
  memberStatusLabel: (value?: string) => string
}>()

defineEmits<{
  (event: 'add-member'): void
  (event: 'remove-member', userId: number): void
}>()
</script>

<style scoped>
.card {
  background: linear-gradient(180deg, #f8fcff 0%, #ffffff 42%);
  border: 1px solid #dbe2ea;
  border-radius: 18px;
  padding: 16px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

h3 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  padding: 8px 12px;
  border-radius: 999px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  font-weight: 700;
}

.summary {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.6;
}

.form-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr auto;
  gap: 10px;
  margin-top: 16px;
}

.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fff;
}

.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 14px;
}

.table th,
.table td {
  border: 1px solid #e5e7eb;
  padding: 10px;
  font-size: 13px;
}

.table th {
  background: #f8fafc;
  color: #475569;
  text-align: left;
}

.ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.primary,
.mini {
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 9px 12px;
  cursor: pointer;
}

.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
  font-weight: 600;
}

.mini {
  padding: 6px 10px;
  font-size: 12px;
  background: #f3f4f6;
}

.empty {
  color: #6b7280;
}

.small {
  font-size: 12px;
}

@media (max-width: 980px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .card-head {
    flex-direction: column;
  }
}
</style>
