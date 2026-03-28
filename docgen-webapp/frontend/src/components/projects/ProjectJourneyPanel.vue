<template>
  <WorkspaceSection
    eyebrow="项目推进"
    title="项目进度摘要"
    description="把项目、AI 协同、知识资料和需求沉淀放到同一条推进链路里，帮助你判断下一步该先做什么。"
    :tint="true"
  >
    <template #actions>
      <StatusBadge :label="`${completionRate}% 已完成`" :variant="completionRate >= 75 ? 'success' : completionRate >= 40 ? 'info' : 'warning'" />
    </template>

    <div class="progress-grid">
      <article v-for="item in steps" :key="item.key" class="progress-card" :class="{ 'progress-card--done': item.done }">
        <div class="progress-head">
          <StatusBadge :label="item.done ? '已完成' : '待推进'" :variant="item.done ? 'success' : 'warning'" small />
          <span class="metric">{{ item.metric }}</span>
        </div>
        <h4>{{ item.title }}</h4>
        <p>{{ item.description }}</p>
      </article>
    </div>

    <div class="next-actions">
      <div>
        <p class="next-title">下一步建议</p>
        <ul>
          <li v-for="action in nextActions" :key="action">{{ action }}</li>
        </ul>
      </div>
      <div class="support-metrics">
        <StatusBadge :label="`${memberCount} 位成员`" variant="neutral" small />
        <StatusBadge :label="`${knowledgeCount} 份知识资料`" variant="ai" small />
        <StatusBadge :label="`${requirementCount} 条需求`" variant="info" small />
      </div>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StatusBadge from './StatusBadge.vue'
import WorkspaceSection from './WorkspaceSection.vue'
import type { ProjectItem } from './types'

const props = defineProps<{
  project: ProjectItem
  memberCount: number
  requirementCount: number
  knowledgeCount: number
  pendingKnowledgeCount: number
  failedKnowledgeCount: number
  hasConversation: boolean
}>()

const hasBaseInfo = computed(() => {
  return !!(
    props.project.description?.trim()
    || props.project.projectBackground?.trim()
    || props.project.similarProducts?.trim()
  )
})

const hasBusinessInfo = computed(() => {
  return !!(
    props.project.targetCustomerGroups?.trim()
    && props.project.commercialValue?.trim()
    && props.project.coreProductValue?.trim()
  )
})

const steps = computed(() => [
  {
    key: 'project-info',
    title: '项目信息',
    done: hasBaseInfo.value && hasBusinessInfo.value,
    metric: hasBaseInfo.value || hasBusinessInfo.value ? '已补充核心背景' : '基础信息待完善',
    description: '项目背景、目标客户、商业价值和核心价值是否已经沉淀到项目档案。'
  },
  {
    key: 'ai-collaboration',
    title: 'AI 协同',
    done: props.hasConversation,
    metric: props.hasConversation ? '会话已建立' : '尚未开启',
    description: '是否已经和 AI 建立连续会话，用来持续优化项目描述和结构化信息。'
  },
  {
    key: 'knowledge',
    title: '知识资料',
    done: props.knowledgeCount > 0 && props.failedKnowledgeCount === 0,
    metric: props.knowledgeCount > 0 ? `${props.knowledgeCount} 份资料` : '暂无资料',
    description: props.failedKnowledgeCount > 0
      ? `当前有 ${props.failedKnowledgeCount} 份资料处理失败，需要优先重试。`
      : '网站、文本和文件资料是否已经进入知识库并可被 AI 检索。'
  },
  {
    key: 'requirements',
    title: '需求沉淀',
    done: props.requirementCount > 0,
    metric: props.requirementCount > 0 ? `${props.requirementCount} 条需求` : '还未创建需求',
    description: '是否已经把项目目标进一步沉淀成可整理、可版本化的需求资产。'
  }
])

const completionRate = computed(() => {
  const doneCount = steps.value.filter((item) => item.done).length
  return Math.round((doneCount / steps.value.length) * 100)
})

const nextActions = computed(() => {
  const actions: string[] = []
  if (!(hasBaseInfo.value && hasBusinessInfo.value)) {
    actions.push('先完善项目背景、目标客户、商业价值和核心价值，让项目档案更完整。')
  }
  if (!props.hasConversation) {
    actions.push('进入 AI 协同标签页，和 AI 连续对话一次，生成更完整的项目信息。')
  }
  if (props.knowledgeCount === 0) {
    actions.push('补充网址、文本或文件资料，让 AI 有足够上下文参与项目优化。')
  }
  if (props.failedKnowledgeCount > 0) {
    actions.push(`处理 ${props.failedKnowledgeCount} 个失败资料任务，避免知识链路中断。`)
  } else if (props.pendingKnowledgeCount > 0) {
    actions.push(`关注 ${props.pendingKnowledgeCount} 个处理中资料任务，确认它们成功进入知识库。`)
  }
  if (props.requirementCount === 0) {
    actions.push('从需求管理标签页创建第一条需求，把项目目标转换成可生产的需求资产。')
  }
  if (!actions.length) {
    actions.push('当前项目主链路已经打通，可以继续进入需求工作台，开展澄清、版本和文档生成。')
  }
  return actions.slice(0, 3)
})
</script>

<style scoped>
.progress-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.progress-card {
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  padding: 14px;
  background: #fff;
}

.progress-card--done {
  border-color: #bbf7d0;
  background: linear-gradient(180deg, #f0fdf4 0%, #ffffff 100%);
}

.progress-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.metric {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.progress-card h4 {
  margin: 12px 0 8px;
  color: #0f172a;
  font-size: 18px;
}

.progress-card p {
  margin: 0;
  color: #475569;
  line-height: 1.6;
}

.next-actions {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  border-top: 1px solid #e2e8f0;
  padding-top: 16px;
}

.next-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

ul {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}

.support-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

@media (max-width: 920px) {
  .progress-grid {
    grid-template-columns: 1fr;
  }

  .next-actions {
    flex-direction: column;
  }
}
</style>
