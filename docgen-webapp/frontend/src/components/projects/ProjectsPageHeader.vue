<template>
  <section class="workspace-shell page-header">
    <div>
      <p class="eyebrow">项目管理中心</p>
      <h2>项目管理中心</h2>
      <p class="summary">先定位项目，再围绕项目框架、需求沉淀、知识状态和协同动作持续推进。这里不是只看信息，而是帮助你判断当前项目下一步该做什么。</p>
      <div class="workflow-hint">
        <span>1. 选项目</span>
        <span>2. 校准项目框架</span>
        <span>3. 沉淀需求</span>
        <span>4. 跟进知识状态</span>
      </div>
    </div>

    <div class="header-stats">
      <div class="metric-card">
        <span class="metric-label">项目总数</span>
        <strong>{{ projectCount }}</strong>
      </div>
      <div class="metric-card">
        <span class="metric-label">当前项目需求</span>
        <strong>{{ requirementCount }}</strong>
      </div>
      <div class="metric-card">
        <span class="metric-label">待处理知识任务</span>
        <strong>{{ pendingKnowledgeCount }}</strong>
      </div>
    </div>

    <div class="header-actions">
      <button class="ghost" type="button" @click="$emit('create-project')">选择创建方式</button>
      <button class="primary" type="button" @click="$emit('create-ai')">进入 AI 项目孵化</button>
      <button class="ghost" type="button" @click="$emit('go-docgen')">进入需求管理中心</button>
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
.workspace-shell {
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
  border: 1px solid #dbe2ea;
  border-radius: 18px;
  padding: 18px;
}
.page-header {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) auto auto;
  gap: 16px;
  align-items: start;
  margin-bottom: 14px;
}
.eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #0f766e;
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
  max-width: 720px;
  font-size: 14px;
  line-height: 1.6;
  color: #475569;
}
.workflow-hint {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}
.workflow-hint span {
  border-radius: 999px;
  border: 1px solid #d9e6f7;
  background: rgba(255, 255, 255, 0.82);
  color: #1e3a8a;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 600;
}
.header-stats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.metric-card {
  min-width: 118px;
  padding: 10px 12px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid #dbe2ea;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}
.metric-label {
  display: block;
  font-size: 12px;
  color: #64748b;
}
.metric-card strong {
  display: block;
  margin-top: 6px;
  font-size: 20px;
  color: #0f172a;
}
.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.primary,
.ghost {
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 10px 14px;
  cursor: pointer;
}
.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}
.ghost {
  background: #f8fafc;
  color: #0f172a;
}
@media (max-width: 1180px) {
  .page-header {
    grid-template-columns: 1fr;
  }
  .header-actions {
    justify-content: flex-start;
  }
}
</style>
