<template>
  <aside class="insights">
    <WorkspaceSection
      eyebrow="项目洞察"
      title="当前项目驾驶舱"
      description="从完整度、知识状态和需求推进三个视角快速判断当前项目最该做什么。"
      tint
    >
      <div class="score-card" :class="`score-card--${healthTone}`">
        <div>
          <p class="score-label">项目成熟度</p>
          <strong class="score-value">{{ completenessScore }}%</strong>
        </div>
        <span class="score-meta">{{ completedFieldCount }}/{{ totalFieldCount }} 项关键信息已完善</span>
      </div>

      <div class="metric-grid">
        <div class="metric-card">
          <span>需求数量</span>
          <strong>{{ requirementCount }}</strong>
        </div>
        <div class="metric-card">
          <span>成员数量</span>
          <strong>{{ memberCount }}</strong>
        </div>
        <div class="metric-card">
          <span>待处理知识</span>
          <strong>{{ pendingKnowledgeCount }}</strong>
        </div>
        <div class="metric-card">
          <span>失败任务</span>
          <strong>{{ failedKnowledgeCount }}</strong>
        </div>
      </div>

      <div class="focus-card">
        <p class="card-label">下一步建议</p>
        <h4>{{ nextActionTitle }}</h4>
        <p>{{ nextActionDescription }}</p>
        <button class="primary" type="button" @click="$emit('jump-to-tab', recommendedWorkspaceTab)">
          进入推荐工作区
        </button>
      </div>

      <div class="detail-card">
        <p class="card-label">当前缺失项</p>
        <ul v-if="missingFieldLabels.length" class="tag-list">
          <li v-for="item in missingFieldLabels" :key="item">{{ item }}</li>
        </ul>
        <p v-else class="muted">项目定义已经比较完整，可以继续推进需求和知识沉淀。</p>
      </div>

      <div class="detail-card">
        <p class="card-label">AI 协同状态</p>
        <p class="muted">
          {{ projectConversationReady ? '当前已有 AI 会话基础，可继续补全和确认结构化信息。' : '当前尚未形成 AI 会话，建议先进入 AI 补全工作区。' }}
        </p>
      </div>
    </WorkspaceSection>
  </aside>
</template>

<script setup lang="ts">
import WorkspaceSection from './WorkspaceSection.vue'
import type { WorkspaceTab } from './types'

defineProps<{
  completenessScore: number
  completedFieldCount: number
  totalFieldCount: number
  healthTone: 'danger' | 'warning' | 'success'
  requirementCount: number
  memberCount: number
  pendingKnowledgeCount: number
  failedKnowledgeCount: number
  nextActionTitle: string
  nextActionDescription: string
  missingFieldLabels: string[]
  recommendedWorkspaceTab: WorkspaceTab
  projectConversationReady: boolean
}>()

defineEmits<{
  (event: 'jump-to-tab', tab: WorkspaceTab): void
}>()
</script>

<style scoped>
.insights {
  min-width: 0;
}

.score-card,
.focus-card,
.detail-card {
  border: 1px solid #dbe5ef;
  border-radius: 18px;
  padding: 16px;
  background: #fff;
}

.score-card {
  display: grid;
  gap: 10px;
}

.score-card--danger {
  background: linear-gradient(180deg, #fff5f5 0%, #ffffff 100%);
}

.score-card--warning {
  background: linear-gradient(180deg, #fff9eb 0%, #ffffff 100%);
}

.score-card--success {
  background: linear-gradient(180deg, #effdf5 0%, #ffffff 100%);
}

.score-label,
.card-label {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.score-value {
  display: block;
  margin-top: 8px;
  font-size: 34px;
  color: #0f172a;
}

.score-meta,
.muted,
.focus-card p {
  color: #475569;
  line-height: 1.7;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  padding: 14px;
  background: #fff;
}

.metric-card span {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  font-size: 22px;
  color: #0f172a;
}

.focus-card h4 {
  margin: 8px 0;
  color: #0f172a;
}

.tag-list {
  list-style: none;
  padding: 0;
  margin: 10px 0 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-list li {
  border-radius: 999px;
  padding: 7px 12px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  font-size: 13px;
}

.primary {
  margin-top: 12px;
  border-radius: 999px;
  border: 1px solid #1d4ed8;
  background: #1d4ed8;
  color: #fff;
  padding: 9px 14px;
  cursor: pointer;
}

@media (max-width: 1080px) {
  .metric-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
