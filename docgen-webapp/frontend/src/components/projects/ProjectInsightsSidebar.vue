<template>
  <aside class="workspace-shell sidebar">
    <section class="insight-card">
      <div class="section-head">
        <h4>当前洞察</h4>
        <span class="muted">{{ memberCount }} 位成员</span>
      </div>
      <div class="stat-grid">
        <div class="stat-item">
          <span>需求</span>
          <strong>{{ requirementCount }}</strong>
        </div>
        <div class="stat-item">
          <span>知识文档</span>
          <strong>{{ knowledgeCount }}</strong>
        </div>
        <div class="stat-item">
          <span>待处理</span>
          <strong>{{ pendingKnowledgeCount }}</strong>
        </div>
        <div class="stat-item">
          <span>失败</span>
          <strong>{{ failedKnowledgeCount }}</strong>
        </div>
      </div>
    </section>

    <section class="insight-card">
      <div class="section-head">
        <h4>成员快照</h4>
        <span class="muted">{{ memberCount }} 条</span>
      </div>
      <ul v-if="members.length > 0" class="snapshot-list">
        <li v-for="member in members.slice(0, 5)" :key="member.id">
          <strong>{{ member.displayName || member.username || `用户 ${member.userId}` }}</strong>
          <span>{{ projectRoleLabel(member.projectRole) }}</span>
        </li>
      </ul>
      <p v-else class="empty-text">当前项目还没有成员。</p>
    </section>

    <section class="insight-card">
      <div class="section-head">
        <h4>最近焦点</h4>
      </div>
      <p class="focus-item">
        <strong>当前项目：</strong>{{ project.projectName }}
      </p>
      <p class="focus-item">
        <strong>当前需求：</strong>{{ selectedRequirement?.title || '尚未选中需求' }}
      </p>
      <p class="focus-item">
        <strong>最近 AI 状态：</strong>{{ projectConversationStatus || '尚未启动 AI 协同' }}
      </p>
    </section>
  </aside>
</template>

<script setup lang="ts">
import type { ProjectItem, ProjectMemberItem, RequirementItem } from './types'

defineProps<{
  project: ProjectItem
  selectedRequirement: RequirementItem | null
  members: ProjectMemberItem[]
  memberCount: number
  requirementCount: number
  knowledgeCount: number
  pendingKnowledgeCount: number
  failedKnowledgeCount: number
  projectConversationStatus: string
  projectRoleLabel: (value?: string) => string
}>()
</script>

<style scoped>
.workspace-shell {
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 18px;
  padding: 18px;
}
.sidebar {
  display: grid;
  gap: 14px;
  align-content: start;
}
.insight-card {
  border: 1px solid #e5edf5;
  border-radius: 14px;
  padding: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}
h4 {
  margin: 0;
  font-size: 16px;
  color: #0f172a;
}
.muted {
  font-size: 12px;
  color: #64748b;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.stat-item {
  border-radius: 12px;
  border: 1px solid #dbe2ea;
  background: #fff;
  padding: 10px 12px;
}
.stat-item span {
  display: block;
  font-size: 12px;
  color: #64748b;
}
.stat-item strong {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  color: #0f172a;
}
.snapshot-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}
.snapshot-list li {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #e5edf5;
  font-size: 13px;
}
.snapshot-list span {
  color: #475569;
}
.focus-item,
.empty-text {
  margin: 0 0 8px;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}
</style>
