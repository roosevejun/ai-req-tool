<template>
  <WorkspaceSection
    title="项目概览"
  >
    <template #actions>
      <button class="ghost mini" :disabled="loading" @click="$emit('enter-ai')">进入 AI 补全</button>
      <button class="ghost mini" :disabled="loading" @click="$emit('start-edit')">编辑项目</button>
      <button class="danger mini" :disabled="loading" @click="$emit('delete-project')">删除项目</button>
    </template>

    <div class="meta-grid">
      <div><strong>ID:</strong> {{ project.id }}</div>
      <div><strong>项目 Key:</strong> {{ project.projectKey }}</div>
      <div><strong>项目类型:</strong> {{ projectTypeLabel(project.projectType) }}</div>
      <div><strong>优先级:</strong> {{ project.priority || "-" }}</div>
      <div><strong>可见性:</strong> {{ visibilityLabel(project.visibility) }}</div>
      <div><strong>开始时间:</strong> {{ project.startDate || "-" }}</div>
      <div><strong>目标时间:</strong> {{ project.targetDate || "-" }}</div>
      <div><strong>状态:</strong> <StatusBadge :label="projectStatusLabel(project.status)" variant="info" small /></div>
      <div class="meta-full"><strong>标签:</strong> {{ project.tags || "-" }}</div>
    </div>

    <div class="detail-grid">
      <article class="detail-section">
        <h4>项目描述</h4>
        <p class="summary">{{ project.description || "当前项目还没有补充描述信息。" }}</p>
      </article>
      <article class="detail-section">
        <h4>项目背景</h4>
        <p class="summary">{{ project.projectBackground || "暂未填写项目背景。" }}</p>
      </article>
      <article class="detail-section">
        <h4>类似产品</h4>
        <p class="summary">{{ project.similarProducts || "暂未补充类似产品信息。" }}</p>
      </article>
      <article class="detail-section">
        <h4>目标客户群体</h4>
        <p class="summary">{{ project.targetCustomerGroups || "暂未说明目标客户群体。" }}</p>
      </article>
      <article class="detail-section">
        <h4>商业价值</h4>
        <p class="summary">{{ project.commercialValue || "暂未描述商业价值。" }}</p>
      </article>
      <article class="detail-section detail-section--wide">
        <h4>核心产品价值</h4>
        <p class="summary">{{ project.coreProductValue || "暂未总结核心产品价值。" }}</p>
      </article>
      <article class="detail-section detail-section--wide">
        <h4>当前缺失的项目定义</h4>
        <div v-if="missingFieldLabels.length" class="missing-list">
          <span v-for="item in missingFieldLabels" :key="item">{{ item }}</span>
        </div>
        <p v-else class="summary">当前项目定义已经比较完整，可以直接围绕需求和知识资料继续推进。</p>
      </article>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'
import WorkspaceSection from './WorkspaceSection.vue'
import type { ProjectItem } from './types'

defineProps<{
  loading: boolean
  project: ProjectItem
  projectTypeLabel: (value?: string) => string
  visibilityLabel: (value?: string) => string
  projectStatusLabel: (value?: string) => string
  missingFieldLabels: string[]
}>()

defineEmits<{
  (event: 'start-edit'): void
  (event: 'enter-ai'): void
  (event: 'delete-project'): void
}>()
</script>

<style scoped>
.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  font-size: 14px;
  color: #334155;
}

.meta-full {
  grid-column: 1 / -1;
}

.detail-section {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.section-label {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.detail-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-section h4 {
  margin: 0 0 10px;
  color: #0f172a;
}

.detail-section--wide {
  grid-column: 1 / -1;
}

.missing-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.missing-list span {
  border-radius: 999px;
  padding: 7px 12px;
  background: #fff7ed;
  border: 1px solid #fdba74;
  color: #9a3412;
  font-size: 13px;
}

.summary {
  margin: 0;
  color: #475569;
  line-height: 1.7;
  white-space: pre-wrap;
}

.ghost,
.danger {
  border-radius: 999px;
  padding: 8px 14px;
  border: 1px solid #cbd5e1;
  background: #fff;
  cursor: pointer;
}

.danger {
  border-color: #fecaca;
  color: #b91c1c;
  background: #fff5f5;
}

.mini {
  font-size: 13px;
}

@media (max-width: 860px) {
  .meta-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-section--wide {
    grid-column: auto;
  }
}
</style>
