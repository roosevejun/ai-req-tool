<template>
  <section class="workspace-header">
    <div class="hero-panel">
      <div class="hero-copy">
        <p class="eyebrow">当前项目对象</p>
        <div class="title-row">
          <h3>{{ project.projectName }}</h3>
          <StatusBadge :label="projectStatusLabel(project.status)" variant="info" />
        </div>
        <p class="summary">
          {{ project.description || "当前项目还没有形成完整说明，建议优先完善项目信息。" }}
        </p>
        <dl class="meta-grid">
          <div class="meta-item">
            <dt>项目 Key</dt>
            <dd>{{ project.projectKey || "-" }}</dd>
          </div>
          <div class="meta-item">
            <dt>项目类型</dt>
            <dd>{{ projectTypeLabel(project.projectType) }}</dd>
          </div>
          <div class="meta-item">
            <dt>可见范围</dt>
            <dd>{{ visibilityLabel(project.visibility) }}</dd>
          </div>
          <div class="meta-item">
            <dt>优先级</dt>
            <dd>{{ project.priority || "-" }}</dd>
          </div>
        </dl>
      </div>

      <div class="hero-side">
        <div class="status-grid">
          <div class="status-card">
            <span>完整度</span>
            <strong>{{ completenessScore }}%</strong>
          </div>
          <div class="status-card">
            <span>需求数</span>
            <strong>{{ requirementCount }}</strong>
          </div>
          <div class="status-card">
            <span>知识处理中</span>
            <strong>{{ pendingKnowledgeCount }}</strong>
          </div>
          <div class="status-card">
            <span>成员数</span>
            <strong>{{ memberCount }}</strong>
          </div>
          <div class="status-card">
            <span>缺失项</span>
            <strong>{{ missingFieldCount }}</strong>
          </div>
        </div>
      </div>
    </div>

    <nav class="section-nav" aria-label="项目工作区导航">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="section-link"
        :class="{ active: activeTab === tab.value }"
        type="button"
        @click="$emit('change-tab', tab.value)"
      >
        <strong>{{ tab.label }}</strong>
        <span>{{ tab.description }}</span>
      </button>
    </nav>
  </section>
</template>

<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'
import type { ProjectItem, WorkspaceTab } from './types'

defineProps<{
  project: ProjectItem
  activeTab: WorkspaceTab
  projectTypeLabel: (value?: string) => string
  visibilityLabel: (value?: string) => string
  projectStatusLabel: (value?: string) => string
  completenessScore: number
  requirementCount: number
  pendingKnowledgeCount: number
  memberCount: number
  missingFieldCount: number
}>()

defineEmits<{
  (event: 'change-tab', tab: WorkspaceTab): void
}>()

const tabs: Array<{ value: WorkspaceTab; label: string; description: string }> = [
  { value: 'overview', label: '项目概览', description: '查看项目基础信息、产品背景和推进状态。' },
  { value: 'collaboration', label: '团队协作', description: '维护负责人、角色分工和项目成员关系。' },
  { value: 'ai', label: 'AI 补全', description: '围绕缺失字段做结构化补全，而不是单纯聊天。' },
  { value: 'materials', label: '资料知识', description: '管理上下文资料、知识处理状态和异常重试。' }
]
</script>

<style scoped>
.workspace-header {
  display: grid;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 14px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.14), transparent 22%),
    linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
  border: 1px solid #d4dde8;
  border-radius: 18px;
}

.eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #1d4ed8;
  font-weight: 700;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.95fr);
  gap: 16px;
  align-items: start;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

h3 {
  margin: 0;
  font-size: 32px;
  color: #0f172a;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0 0;
}

.meta-item {
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.meta-item dt {
  font-size: 12px;
  color: #64748b;
}

.meta-item dd {
  margin: 6px 0 0;
  color: #0f172a;
  font-weight: 600;
}

.summary {
  margin: 14px 0 0;
  color: #475569;
  line-height: 1.7;
}

.hero-side {
  display: grid;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.status-card {
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #dbe5ef;
}

.status-card span {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.status-card strong {
  display: block;
  margin-top: 8px;
  font-size: 22px;
  color: #0f172a;
}

.section-nav {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
}

.section-link {
  display: grid;
  gap: 6px;
  min-height: 84px;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #dbe5f0;
  border-radius: 12px;
  text-align: left;
  cursor: pointer;
}

.section-link strong {
  color: #0f172a;
}

.section-link span {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.section-link.active {
  background: #eff6ff;
  border-color: #60a5fa;
}

@media (max-width: 1100px) {
  .hero-panel,
  .meta-grid,
  .section-nav {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .hero-panel,
  .meta-grid,
  .section-nav {
    grid-template-columns: 1fr;
  }
}
</style>
