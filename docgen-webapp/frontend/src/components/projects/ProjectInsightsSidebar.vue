<template>
  <aside class="sidebar">
    <section class="panel panel--health">
      <div class="section-head">
        <h4>项目运行态势</h4>
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

    <section class="panel">
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

    <section class="panel">
      <div class="section-head">
        <h4>当前焦点</h4>
      </div>
      <p class="focus-item"><strong>当前项目：</strong>{{ project.projectName }}</p>
      <p class="focus-item"><strong>当前需求：</strong>{{ selectedRequirement?.title || '尚未选中需求' }}</p>
      <p class="focus-item"><strong>AI 协同：</strong>{{ conversationLabel }}</p>
    </section>

    <section class="panel">
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
  if (props.failedKnowledgeCount > 0) return '存在异常事项'
  if (props.pendingKnowledgeCount > 0) return '事项处理中'
  if (props.requirementCount === 0) return '待沉淀需求'
  return '运行正常'
})

const healthTip = computed(() => {
  if (props.failedKnowledgeCount > 0) {
    return `当前有 ${props.failedKnowledgeCount} 个知识任务失败，建议优先处理异常后再推进需求整理。`
  }
  if (props.pendingKnowledgeCount > 0) {
    return `当前有 ${props.pendingKnowledgeCount} 个知识任务处理中，可稍后刷新状态确认结果。`
  }
  if (props.knowledgeCount === 0) {
    return '当前还没有知识文档，建议尽快补充网址、文本或文件资料。'
  }
  if (props.requirementCount === 0) {
    return '项目框架和知识资料已基本具备，下一步建议开始创建需求。'
  }
  return '项目、知识和需求主链路已打通，可继续推进重点需求的文档生产。'
})

const nextActions = computed(() => {
  const actions: string[] = []
  if (props.knowledgeCount === 0) actions.push('补充项目资料，让 AI 校准建立在真实上下文之上。')
  if (!props.projectConversationStatus) actions.push('进入 AI 协同，先完成一轮项目框架整理与回填。')
  if (props.failedKnowledgeCount > 0) actions.push('优先处理失败的知识任务，避免需求整理时缺少有效证据。')
  if (props.requirementCount === 0) actions.push('从需求管理区创建第一条需求，开始进入需求生产流程。')
  if (props.memberCount === 0) actions.push('补充负责人或核心协作成员，保证后续资产有人维护。')
  if (!actions.length) actions.push('选择一条需求进入需求管理中心，继续推进文档整理和版本跟踪。')
  return actions.slice(0, 3)
})
</script>

<style scoped>
.sidebar {
  display: grid;
  gap: 14px;
  align-content: start;
}

.panel {
  padding: 14px;
  background: #ffffff;
  border: 1px solid #d4dde8;
  border-radius: 8px;
}

.panel--health {
  border-top: 4px solid #1d4ed8;
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
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
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
.empty-text {
  margin: 10px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
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
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  font-size: 13px;
}

.snapshot-list span {
  color: #475569;
}

.next-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}
</style>
