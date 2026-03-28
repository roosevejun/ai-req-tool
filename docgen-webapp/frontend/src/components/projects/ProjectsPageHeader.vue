<template>
  <section class="page-header">
    <div class="title-panel">
      <p class="eyebrow">项目管理中心</p>
      <h2>项目管理中心</h2>
      <p class="summary">
        面向项目立项、框架校准、需求沉淀与知识跟踪的统一业务入口。左侧选择业务入口和项目对象，右侧查看项目状态、办理进展与协同结果。
      </p>
      <div class="workflow-strip">
        <span>1. 选择业务入口</span>
        <span>2. 定位项目对象</span>
        <span>3. 查看办理状态</span>
        <span>4. 推进协同事项</span>
      </div>
    </div>

    <div class="stat-panel">
      <div class="stat-card">
        <span class="stat-label">项目总数</span>
        <strong>{{ projectCount }}</strong>
      </div>
      <div class="stat-card">
        <span class="stat-label">当前项目需求</span>
        <strong>{{ requirementCount }}</strong>
      </div>
      <div class="stat-card">
        <span class="stat-label">待处理知识任务</span>
        <strong>{{ pendingKnowledgeCount }}</strong>
      </div>
    </div>

    <div class="action-panel">
      <button class="primary" type="button" @click="$emit('create-project')">选择创建方式</button>
      <button class="secondary" type="button" @click="$emit('create-ai')">进入 AI 项目孵化</button>
      <button class="secondary" type="button" @click="$emit('go-docgen')">进入需求管理中心</button>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  projectCount: number
  requirementCount: number
  pendingKnowledgeCount: number
}>()

defineEmits<{
  (event: 'create-project'): void
  (event: 'create-ai'): void
  (event: 'go-docgen'): void
}>()
</script>

<style scoped>
.page-header {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) 320px;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 14px;
  background: #ffffff;
  border: 1px solid #d4dde8;
  border-top: 4px solid #1d4ed8;
  border-radius: 10px;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
}

.title-panel {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #1d4ed8;
  font-weight: 700;
}

h2 {
  margin: 0;
  font-size: 30px;
  line-height: 1.1;
  color: #0f172a;
}

.summary {
  margin: 10px 0 0;
  max-width: 760px;
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
}

.workflow-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.workflow-strip span {
  padding: 6px 12px;
  border: 1px solid #dbe5f0;
  background: #f8fbff;
  color: #1e3a8a;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.stat-panel {
  display: grid;
  gap: 10px;
  align-content: start;
}

.stat-card {
  padding: 12px 14px;
  border: 1px solid #dbe5f0;
  background: #f8fbff;
  border-radius: 8px;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.stat-card strong {
  display: block;
  margin-top: 6px;
  font-size: 24px;
  color: #0f172a;
}

.action-panel {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
}

.primary,
.secondary {
  min-width: 160px;
  padding: 10px 16px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.primary {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #ffffff;
}

.primary:hover {
  background: #1e40af;
  border-color: #1e40af;
}

.secondary {
  background: #ffffff;
  color: #0f172a;
}

.secondary:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

@media (max-width: 1180px) {
  .page-header {
    grid-template-columns: 1fr;
  }
}
</style>
