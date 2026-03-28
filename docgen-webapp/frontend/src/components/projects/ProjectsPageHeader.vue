<template>
  <section class="workspace-shell page-header">
    <div>
      <p class="eyebrow">Project Workspace</p>
      <h2>项目工作台</h2>
      <p class="summary">
        统一查看项目、需求、AI 协同与知识状态，围绕当前选中的项目持续推进。
      </p>
    </div>

    <div class="header-stats">
      <div class="metric-card">
        <span class="metric-label">项目总数</span>
        <strong>{{ projectCount }}</strong>
      </div>
      <div class="metric-card">
        <span class="metric-label">当前需求数</span>
        <strong>{{ requirementCount }}</strong>
      </div>
      <div class="metric-card">
        <span class="metric-label">待处理知识</span>
        <strong>{{ pendingKnowledgeCount }}</strong>
      </div>
    </div>

    <div class="header-actions">
      <button class="ghost" type="button" @click="$emit('create-project')">新建项目</button>
      <button class="primary" type="button" @click="$emit('create-ai')">AI 创建项目</button>
      <button class="ghost" type="button" @click="$emit('go-docgen')">进入 AI 需求整理</button>
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
  max-width: 680px;
  font-size: 14px;
  line-height: 1.6;
  color: #475569;
}
.header-stats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.metric-card {
  min-width: 110px;
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
