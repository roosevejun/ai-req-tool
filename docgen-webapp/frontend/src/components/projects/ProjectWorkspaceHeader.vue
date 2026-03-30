<template>
  <section class="workspace-header">
    <div class="summary-panel">
      <p class="eyebrow">当前项目对象</p>
      <div class="title-row">
        <h3>{{ project.projectName }}</h3>
        <StatusBadge :label="projectStatusLabel(project.status)" variant="info" />
      </div>

      <dl class="meta-grid">
        <div class="meta-item">
          <dt>项目 Key</dt>
          <dd>{{ project.projectKey || '-' }}</dd>
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
          <dd>{{ project.priority || '-' }}</dd>
        </div>
      </dl>

      <p class="summary">
        {{ project.description || '当前项目还没有形成完整说明，建议优先进入 AI 协同校准项目框架。' }}
      </p>
    </div>

    <div class="action-panel">
      <button class="primary" type="button" @click="$emit('enter-ai')">进入 AI 协同</button>
    </div>

    <nav class="section-nav" aria-label="项目业务分区">
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
import type { ProjectItem } from './types'

defineProps<{
  project: ProjectItem
  activeTab: 'overview' | 'ai' | 'materials'
  projectTypeLabel: (value?: string) => string
  visibilityLabel: (value?: string) => string
  projectStatusLabel: (value?: string) => string
}>()

defineEmits<{
  (event: 'change-tab', tab: 'overview' | 'ai' | 'materials'): void
  (event: 'enter-ai'): void
}>()

const tabs: Array<{ value: 'overview' | 'ai' | 'materials'; label: string; description: string }> = [
  { value: 'overview', label: '项目概览', description: '查看项目状态、成员信息和关键业务描述。' },
  { value: 'ai', label: 'AI 补全', description: '通过沟通补全项目背景、客户、价值和描述，并回填表单。' },
  { value: 'materials', label: '资料与知识', description: '管理补充资料和知识处理状态，为 AI 补全提供上下文。' }
]
</script>

<style scoped>
.workspace-header {
  display: grid;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 14px;
  background: #ffffff;
  border: 1px solid #d4dde8;
  border-radius: 10px;
}

.eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #1d4ed8;
  font-weight: 700;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

h3 {
  margin: 0;
  font-size: 28px;
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

.action-panel {
  display: flex;
  justify-content: flex-start;
}

.primary {
  padding: 10px 16px;
  border-radius: 6px;
  border: 1px solid #0f766e;
  background: #0f766e;
  color: #ffffff;
  font-weight: 600;
  cursor: pointer;
}

.primary:hover {
  background: #115e59;
  border-color: #115e59;
}

.section-nav {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  border-radius: 8px;
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
  .meta-grid,
  .section-nav {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .meta-grid,
  .section-nav {
    grid-template-columns: 1fr;
  }
}
</style>
