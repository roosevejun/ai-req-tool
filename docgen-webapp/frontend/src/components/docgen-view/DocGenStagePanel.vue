<template>
  <WorkspaceSection
    eyebrow="需求生产"
    title="需求推进阶段"
    description="把选项目、选需求、AI 澄清、文档生成和交付放到一条可感知的流程里，减少“下一步做什么”的犹豫。"
    :tint="true"
  >
    <template #actions>
      <StatusBadge :label="currentStageLabel" :variant="currentStage >= 3 ? 'success' : currentStage >= 2 ? 'ai' : 'warning'" />
    </template>

    <div class="stage-strip">
      <article v-for="stage in stages" :key="stage.key" class="stage-card" :class="stageClass(stage.index)">
        <div class="stage-index">{{ stage.index }}</div>
        <div class="stage-copy">
          <p class="stage-name">{{ stage.title }}</p>
          <p class="stage-desc">{{ stage.description }}</p>
        </div>
      </article>
    </div>

    <div class="stage-footer">
      <p class="next-tip">{{ nextTip }}</p>
      <div class="stage-metrics">
        <StatusBadge :label="selectedProjectId ? `项目 #${selectedProjectId}` : '未选项目'" :variant="selectedProjectId ? 'info' : 'warning'" small />
        <StatusBadge :label="selectedRequirementId ? `需求 #${selectedRequirementId}` : '未选需求'" :variant="selectedRequirementId ? 'success' : 'warning'" small />
        <StatusBadge :label="`${requirementsCount} 条需求`" variant="neutral" small />
      </div>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StatusBadge from '../projects/StatusBadge.vue'
import WorkspaceSection from '../projects/WorkspaceSection.vue'

const props = defineProps<{
  selectedProjectId: number | null
  selectedRequirementId: number | null
  requirementsCount: number
}>()

const stages = [
  { key: 'project', index: 1, title: '选择项目', description: '先确定当前要推进的项目范围。' },
  { key: 'requirement', index: 2, title: '选择需求', description: '定位到具体需求，避免 AI 在错误对象上工作。' },
  { key: 'clarify', index: 3, title: 'AI 澄清', description: '围绕上下文持续提问、确认和补充缺失信息。' },
  { key: 'generate', index: 4, title: '生成文档', description: '把澄清结果沉淀成 PRD 和结构化资产。' },
  { key: 'deliver', index: 5, title: '导出交付', description: '完成版本确认、导出和进一步评审。' }
]

const currentStage = computed(() => {
  if (!props.selectedProjectId) return 1
  if (!props.selectedRequirementId) return 2
  return 3
})

const currentStageLabel = computed(() => {
  if (!props.selectedProjectId) return '先选择项目'
  if (!props.selectedRequirementId) return '先选择需求'
  return '进入 AI 澄清'
})

const nextTip = computed(() => {
  if (!props.selectedProjectId) return '先在左侧树中选中项目，再进入需求生产流程。'
  if (!props.selectedRequirementId) {
    return props.requirementsCount > 0
      ? '当前项目已有需求，请先选择一条需求，再继续 AI 整理。'
      : '当前项目还没有需求，请先创建一条需求，再进入 AI 整理。'
  }
  return '当前已经进入需求工作流，接下来建议在下方 AI 工作区完成澄清、结构化确认和 PRD 生成。'
})

function stageClass(index: number) {
  return {
    'stage-card--done': index < currentStage.value,
    'stage-card--active': index === currentStage.value
  }
}
</script>

<style scoped>
.stage-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.stage-card {
  display: flex;
  gap: 12px;
  padding: 14px;
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  background: #fff;
}

.stage-card--done {
  border-color: #bbf7d0;
  background: linear-gradient(180deg, #f0fdf4 0%, #ffffff 100%);
}

.stage-card--active {
  border-color: #99f6e4;
  background: linear-gradient(180deg, #ecfeff 0%, #ffffff 100%);
}

.stage-index {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #0f172a;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex: 0 0 auto;
}

.stage-name {
  margin: 0;
  color: #0f172a;
  font-weight: 700;
}

.stage-desc {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.stage-footer {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.next-tip {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

.stage-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

@media (max-width: 1200px) {
  .stage-strip {
    grid-template-columns: 1fr;
  }

  .stage-footer {
    flex-direction: column;
  }
}
</style>
