<template>
  <section class="card">
    <div class="section-head">
      <h3>项目详情</h3>
      <div class="row compact">
        <button v-if="!editingProject" class="ghost mini" @click="$emit('start-edit')">编辑项目</button>
        <button v-if="!editingProject" class="danger mini" :disabled="loading" @click="$emit('delete-project')">删除项目</button>
        <template v-else>
          <button class="primary mini" :disabled="loading || !projectEditForm.projectName" @click="$emit('save-edit')">
            保存修改
          </button>
          <button class="ghost mini" :disabled="loading" @click="$emit('cancel-edit')">取消</button>
        </template>
      </div>
    </div>

    <template v-if="!editingProject">
      <div class="meta-grid">
        <div><strong>ID：</strong> {{ project.id }}</div>
        <div><strong>项目 Key：</strong> {{ project.projectKey }}</div>
        <div><strong>项目名称：</strong> {{ project.projectName }}</div>
        <div><strong>项目类型：</strong> {{ projectTypeLabel(project.projectType) }}</div>
        <div><strong>优先级：</strong> {{ project.priority || '-' }}</div>
        <div><strong>可见性：</strong> {{ visibilityLabel(project.visibility) }}</div>
        <div><strong>计划开始：</strong> {{ project.startDate || '-' }}</div>
        <div><strong>计划结束：</strong> {{ project.targetDate || '-' }}</div>
        <div><strong>状态：</strong> {{ projectStatusLabel(project.status) }}</div>
        <div><strong>标签：</strong> {{ project.tags || '-' }}</div>
      </div>
      <p class="summary">{{ project.description || '暂无项目描述' }}</p>
      <div class="detail-section">
        <h4>项目背景</h4>
        <p class="summary">{{ project.projectBackground || '暂无项目背景' }}</p>
      </div>
      <div class="detail-section">
        <h4>类似产品参考</h4>
        <p class="summary">{{ project.similarProducts || '暂无类似产品参考' }}</p>
      </div>
      <div class="detail-section">
        <h4>目标客户群体</h4>
        <p class="summary">{{ project.targetCustomerGroups || '暂无目标客户群体' }}</p>
      </div>
      <div class="detail-section">
        <h4>商业价值</h4>
        <p class="summary">{{ project.commercialValue || '暂无商业价值描述' }}</p>
      </div>
      <div class="detail-section">
        <h4>核心产品价值</h4>
        <p class="summary">{{ project.coreProductValue || '暂无核心产品价值' }}</p>
      </div>
    </template>

    <template v-else>
      <div class="meta-grid">
        <div><strong>ID：</strong> {{ project.id }}</div>
        <div><strong>项目 Key：</strong> {{ project.projectKey }}</div>
      </div>

      <div class="form-grid detail-edit-grid">
        <input v-model.trim="projectEditForm.projectName" class="input" placeholder="项目名称" />
        <select v-model="projectEditForm.projectType" class="input">
          <option value="">项目类型，可选</option>
          <option value="PRODUCT">产品型</option>
          <option value="PLATFORM">平台型</option>
          <option value="OPS">运维型</option>
          <option value="INTEGRATION">集成型</option>
        </select>
        <select v-model="projectEditForm.priority" class="input">
          <option value="P0">P0</option>
          <option value="P1">P1</option>
          <option value="P2">P2</option>
          <option value="P3">P3</option>
        </select>
        <select v-model="projectEditForm.visibility" class="input">
          <option value="PRIVATE">私有</option>
          <option value="ORG">组织内</option>
        </select>
        <select v-model="projectEditForm.status" class="input">
          <option value="ACTIVE">进行中</option>
          <option value="ARCHIVED">已归档</option>
          <option value="PAUSED">已暂停</option>
        </select>
        <select v-model="projectEditForm.ownerUserId" class="input">
          <option value="">负责人，保持不变</option>
          <option v-for="u in userOptions" :key="u.id" :value="String(u.id)">
            {{ u.displayName || u.username }} ({{ u.username }})
          </option>
        </select>
        <input v-model="projectEditForm.startDate" type="date" class="input" />
        <input v-model="projectEditForm.targetDate" type="date" class="input" />
        <input v-model.trim="projectEditForm.tags" class="input" placeholder="标签，逗号分隔" />
      </div>

      <textarea v-model="projectEditForm.description" class="input" placeholder="项目描述，可选" />

      <div class="detail-section">
        <h4>产品信息维护</h4>
        <textarea v-model="projectEditForm.projectBackground" class="input" placeholder="项目背景，可选" />
        <textarea v-model="projectEditForm.similarProducts" class="input" placeholder="类似产品 / 参考产品，可选" />
        <textarea v-model="projectEditForm.targetCustomerGroups" class="input" placeholder="目标客户群体，可选" />
        <textarea v-model="projectEditForm.commercialValue" class="input" placeholder="商业价值，可选" />
        <textarea v-model="projectEditForm.coreProductValue" class="input" placeholder="核心产品价值，可选" />
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import type { ProjectEditFormState, ProjectItem, UserOption } from './types'

defineProps<{
  loading: boolean
  editingProject: boolean
  project: ProjectItem
  projectEditForm: ProjectEditFormState
  userOptions: UserOption[]
  projectTypeLabel: (value?: string) => string
  visibilityLabel: (value?: string) => string
  projectStatusLabel: (value?: string) => string
}>()

defineEmits<{
  (event: 'start-edit'): void
  (event: 'cancel-edit'): void
  (event: 'save-edit'): void
  (event: 'delete-project'): void
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
.section-head,
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.row {
  margin-top: 10px;
  flex-wrap: wrap;
}
.compact {
  margin-top: 0;
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 10px;
  font-size: 14px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 10px;
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
.summary {
  font-size: 13px;
  color: #4b5563;
  white-space: pre-wrap;
}
.detail-section {
  margin-top: 12px;
}
.detail-section h4 {
  margin: 0;
  font-size: 14px;
}
.primary,
.ghost,
.danger,
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
.danger,
.mini {
  background: #f3f4f6;
}
.danger {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}
.mini {
  padding: 5px 9px;
  font-size: 12px;
}
@media (max-width: 980px) {
  .meta-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
