<template>
  <aside class="workspace-shell sidebar">
    <section class="insight-card insight-card--health">
      <div class="section-head">
        <h4>项目健康中心</h4>
        <span class="muted">{{ healthLabel }}</span>
      </div>
      <div class="stat-grid">
        <div class="stat-item">
          <span>需求数量</span>
          <strong>{{ requirementCount }}</strong>
        </div>
        <div class="stat-item">
          <span>知识文档</span>
          <strong>{{ knowledgeCount }}</strong>
        </div>
        <div class="stat-item">
          <span>处理中任务</span>
          <strong>{{ pendingKnowledgeCount }}</strong>
        </div>
        <div class="stat-item">
          <span>失败任务</span>
          <strong>{{ failedKnowledgeCount }}</strong>
        </div>
      </div>
      <p class="health-tip">{{ healthTip }}</p>
    </section>

    <section class="insight-card">
      <div class="section-head">
        <h4>成员快照</h4>
        <span class="muted">{{ memberCount }} 人</span>
      </div>
      <ul v-if="members.length > 0" class="snapshot-list">
        <li v-for="member in members.slice(0, 5)" :key="member.id">
          <strong>{{ member.displayName || member.username || `用户 ${member.userId}` }}</strong>
          <span>{{ projectRoleLabel(member.projectRole) }}</span>
        </li>
      </ul>
      <p v-else class="empty-text">当前项目还没有成员，建议先补充负责人和核心协作角色。</p>
    </section>

    <section class="insight-card">
      <div class="section-head">
        <h4>当前焦点</h4>
      </div>
      <p class="focus-item">
        <strong>当前项目：</strong>{{ project.projectName }}
      </p>
      <p class="focus-item">
        <strong>当前需求：</strong>{{ selectedRequirement?.title || '尚未选中需求' }}
      </p>
      <p class="focus-item">
        <strong>AI 协同：</strong>{{ conversationLabel }}
      </p>
    </section>

    <section class="insight-card">
      <div class="section-head">
        <h4>下一步建议</h4>
      </div>
      <ul class="next-list">
        <li v-for="item in nextActions" :key="item">{{ item }}</li>
      </ul>
    </section>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProjectItem, ProjectMemberItem, RequirementItem } from './types'

const props = defineProps<{
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

const conversationLabel = computed(() => {
  if (props.projectConversationStatus === 'ACTIVE') return '正在协同'
  if (props.projectConversationStatus === 'COMPLETED') return '已完成一轮整理'
  if (props.projectConversationStatus) return props.projectConversationStatus
  return '尚未启动'
})

const healthLabel = computed(() => {
  if (props.failedKnowledgeCount > 0) return '需要优先处理失败任务'
  if (props.pendingKnowledgeCount > 0) return '知识任务处理中'
  if (props.requirementCount === 0) return '项目就绪，等待沉淀需求'
  return '主链路运行正常'
})

const healthTip = computed(() => {
  if (props.failedKnowledgeCount > 0) return `当前有 ${props.failedKnowledgeCount} 个失败知识任务，建议先进入 AI 协同或知识库处理，避免后续需求整理缺关键上下文。`
  if (props.pendingKnowledgeCount > 0) return `当前有 ${props.pendingKnowledgeCount} 个知识任务仍在处理中，可以稍后刷新状态确认结果。`
  if (props.knowledgeCount === 0) return '当前还没有知识文档，建议尽快补充网址、文本或文件资料。'
  if (props.requirementCount === 0) return '项目信息和知识资料已基本具备，下一步建议开始创建需求。'
  return '当前项目、知识和需求主链路已经打通，可以继续深入推进需求生产。'
})

const nextActions = computed(() => {
  const actions: string[] = []
  if (props.knowledgeCount === 0) actions.push('补充项目资料，让 AI 后续协同建立在真实上下文之上。')
  if (!props.projectConversationStatus) actions.push('进入 AI 协同，先完成一轮项目优化和结构化回填。')
  if (props.failedKnowledgeCount > 0) actions.push('优先处理失败知识任务，避免需求整理阶段引用不到关键上下文。')
  if (props.requirementCount === 0) actions.push('从需求管理标签页创建第一条需求，开始进入需求生产流程。')
  if (props.memberCount === 0) actions.push('为项目补齐负责人或核心协作者，避免后续资产无人维护。')
  if (!actions.length) actions.push('继续选择一条需求，进入 AI 需求工作台推进文档生产。')
  return actions.slice(0, 3)
})
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

.insight-card--health {
  background: linear-gradient(180deg, #f8fcff 0%, #ffffff 100%);
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

.health-tip,
.focus-item,
.empty-text,
.next-list {
  margin: 10px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.snapshot-list,
.next-list {
  padding-left: 18px;
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
</style>
