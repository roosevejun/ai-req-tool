<template>
  <section class="card">
    <h3>在当前项目下创建需求</h3>
    <div class="form-grid">
      <input v-model.trim="reqForm.title" class="input" placeholder="需求标题" />
      <select v-model="reqForm.priority" class="input">
        <option value="P0">P0 - 紧急</option>
        <option value="P1">P1 - 高</option>
        <option value="P2">P2 - 中</option>
        <option value="P3">P3 - 低</option>
      </select>
      <select v-model="reqForm.status" class="input">
        <option value="DRAFT">草稿</option>
        <option value="CLARIFYING">澄清中</option>
        <option value="READY_REVIEW">待评审</option>
        <option value="DONE">已完成</option>
      </select>
    </div>
    <textarea v-model="reqForm.summary" class="input" placeholder="需求摘要，可选" />
    <div class="row">
      <button class="primary" :disabled="loading || !reqForm.title" @click="$emit('create-requirement')">创建需求</button>
    </div>
  </section>

  <section class="card">
    <h3>{{ project.projectName }} 的需求列表</h3>
    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>编号</th>
          <th>标题</th>
          <th>优先级</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in requirements" :key="r.id">
          <td>{{ r.id }}</td>
          <td>{{ r.requirementNo }}</td>
          <td>{{ r.title }}</td>
          <td>{{ priorityLabel(r.priority) }}</td>
          <td>{{ requirementStatusLabel(r.status) }}</td>
          <td class="ops">
            <button class="mini" @click="$emit('select-requirement', project.id, r.id)">查看</button>
            <button class="mini" @click="$emit('open-workbench', r.id)">AI 工作台</button>
            <button class="mini" @click="$emit('open-versions', r.id)">版本页</button>
          </td>
        </tr>
        <tr v-if="requirements.length === 0">
          <td colspan="6" class="empty small">暂无需求</td>
        </tr>
      </tbody>
    </table>
  </section>

  <section v-if="selectedRequirement" class="card">
    <h3>当前选中需求</h3>
    <div class="meta-grid">
      <div><strong>ID：</strong> {{ selectedRequirement.id }}</div>
      <div><strong>编号：</strong> {{ selectedRequirement.requirementNo }}</div>
      <div><strong>标题：</strong> {{ selectedRequirement.title }}</div>
      <div><strong>优先级：</strong> {{ priorityLabel(selectedRequirement.priority) }}</div>
      <div><strong>状态：</strong> {{ requirementStatusLabel(selectedRequirement.status) }}</div>
    </div>
    <p class="summary">{{ selectedRequirement.summary || '暂无需求摘要' }}</p>
    <div class="row">
      <button class="primary" @click="$emit('open-workbench', selectedRequirement.id)">进入 AI 工作台</button>
      <button class="ghost" @click="$emit('open-versions', selectedRequirement.id)">查看版本页</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ProjectItem, RequirementFormState, RequirementItem } from './types'

defineProps<{
  loading: boolean
  project: ProjectItem
  reqForm: RequirementFormState
  requirements: RequirementItem[]
  selectedRequirement: RequirementItem | null
  priorityLabel: (value?: string) => string
  requirementStatusLabel: (value?: string) => string
}>()

defineEmits<{
  (event: 'create-requirement'): void
  (event: 'select-requirement', projectId: number, requirementId: number): void
  (event: 'open-workbench', requirementId: number): void
  (event: 'open-versions', requirementId: number): void
}>()
</script>

<style scoped>
.card {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
}
.form-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 10px;
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 10px;
  font-size: 14px;
}
.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 10px;
  margin-top: 8px;
  background: #fff;
}
textarea.input {
  min-height: 70px;
  resize: vertical;
}
.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 8px;
}
.table th,
.table td {
  border: 1px solid #e5e7eb;
  padding: 8px;
  font-size: 13px;
}
.row {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.summary {
  font-size: 13px;
  color: #4b5563;
  white-space: pre-wrap;
}
.primary,
.ghost,
.mini {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  padding: 8px 12px;
  cursor: pointer;
}
.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}
.ghost,
.mini {
  background: #f3f4f6;
}
.mini {
  padding: 5px 9px;
  font-size: 12px;
}
.empty {
  color: #6b7280;
}
.small {
  font-size: 12px;
}
@media (max-width: 980px) {
  .form-grid,
  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
