<template>
  <WorkspaceSection eyebrow="需求管理" title="需求管理" description="在当前项目下创建需求、查看需求列表，并快速进入 AI 工作台或版本页。">
    <div class="split-grid">
      <section class="panel">
        <div class="panel-head">
          <h4>新建需求</h4>
          <StatusBadge :label="`${requirements.length} 条需求`" variant="info" small />
        </div>
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
        <textarea v-model="reqForm.summary" class="input textarea" placeholder="需求摘要，可选" />
        <div class="row">
          <button class="primary" :disabled="loading || !reqForm.title" @click="$emit('create-requirement')">创建需求</button>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <h4>{{ project.projectName }} 的需求列表</h4>
          <span class="muted">按优先级和状态查看当前项目需求</span>
        </div>
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
              <td colspan="6" class="empty small">当前项目下还没有需求。</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>

    <section v-if="selectedRequirement" class="selected-panel">
      <div class="panel-head">
        <div>
          <p class="section-label">当前选中</p>
          <h4>当前选中需求</h4>
        </div>
        <div class="row compact">
          <button class="primary mini" @click="$emit('open-workbench', selectedRequirement.id)">进入 AI 工作台</button>
          <button class="ghost mini" @click="$emit('open-versions', selectedRequirement.id)">查看版本页</button>
        </div>
      </div>
      <div class="meta-grid">
        <div><strong>ID：</strong>{{ selectedRequirement.id }}</div>
        <div><strong>编号：</strong>{{ selectedRequirement.requirementNo }}</div>
        <div><strong>标题：</strong>{{ selectedRequirement.title }}</div>
        <div><strong>优先级：</strong>{{ priorityLabel(selectedRequirement.priority) }}</div>
        <div><strong>状态：</strong>{{ requirementStatusLabel(selectedRequirement.status) }}</div>
      </div>
      <p class="summary">{{ selectedRequirement.summary || '当前还没有补充需求摘要。' }}</p>
    </section>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'
import WorkspaceSection from './WorkspaceSection.vue'
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
.split-grid { display: grid; grid-template-columns: 360px minmax(0, 1fr); gap: 16px; }
.panel, .selected-panel { border: 1px solid #e2e8f0; border-radius: 16px; background: #fff; padding: 14px; }
.panel-head, .row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.row { flex-wrap: wrap; }
.compact { margin-top: 0; }
.muted, .summary { color: #64748b; }
.muted { font-size: 13px; }
.form-grid, .meta-grid { display: grid; gap: 10px; }
.form-grid { grid-template-columns: 1fr; margin-top: 12px; }
.meta-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); margin-top: 12px; }
.input { width: 100%; box-sizing: border-box; border: 1px solid #d1d5db; border-radius: 10px; padding: 10px 12px; background: #fff; }
.textarea { min-height: 96px; resize: vertical; }
.table { width: 100%; border-collapse: collapse; margin-top: 12px; }
.table th, .table td { border: 1px solid #e5e7eb; padding: 8px; font-size: 13px; text-align: left; }
.ops { display: flex; gap: 8px; flex-wrap: wrap; }
.section-label { margin: 0 0 6px; font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: #64748b; }
.summary { margin: 12px 0 0; white-space: pre-wrap; line-height: 1.7; }
.primary, .ghost, .mini { border-radius: 10px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; }
.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.ghost, .mini { background: #f8fafc; }
.mini { padding: 5px 9px; font-size: 12px; }
.empty { color: #6b7280; }
.small { font-size: 12px; }
@media (max-width: 980px) { .split-grid, .meta-grid { grid-template-columns: 1fr; } .panel-head { align-items: flex-start; flex-direction: column; } }
</style>
