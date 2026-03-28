<template>
  <div class="progress-panel">
    <div class="progress-head">
      <div>
        <p class="eyebrow">当前推进</p>
        <h3>需求整理进度</h3>
      </div>
      <div class="badges">
        <StatusBadge :label="`${confirmedCount} 项已确认`" variant="success" small />
        <StatusBadge :label="`${pendingCount} 项待补充`" :variant="pendingCount > 0 ? 'warning' : 'info'" small />
      </div>
    </div>

    <div class="steps">
      <div v-for="step in steps" :key="step.key" class="step" :class="stepStateClass(step.state)">
        <div class="step-index">{{ step.index }}</div>
        <div class="step-copy">
          <p class="step-title">{{ step.title }}</p>
          <p class="step-desc">{{ step.description }}</p>
        </div>
      </div>
    </div>

    <p class="next-tip">{{ nextTip }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StatusBadge from '../projects/StatusBadge.vue'

const props = defineProps<{
  status: string
  confirmedCount: number
  pendingCount: number
  hasTemplate: boolean
}>()

const steps = computed(() => {
  const isCompleted = props.status === 'COMPLETED'
  const hasPending = props.pendingCount > 0
  return [
    {
      key: 'template',
      index: 1,
      title: '确定模板',
      description: props.hasTemplate ? '已绑定模板版本，后续生成会基于当前快照。' : '建议先明确模板，保证文档标准一致。',
      state: props.hasTemplate ? 'done' : 'active'
    },
    {
      key: 'clarify',
      index: 2,
      title: 'AI 澄清',
      description: hasPending ? '当前仍有待补充项，需要继续和 AI 确认上下文。' : '澄清项已经收敛，可以继续整理结果。',
      state: hasPending ? 'active' : 'done'
    },
    {
      key: 'generate',
      index: 3,
      title: '生成 PRD',
      description: isCompleted ? 'PRD 已生成，可继续导出和评审。' : '待补充项清零后，就可以开始生成 PRD。',
      state: isCompleted ? 'done' : hasPending ? 'pending' : 'active'
    }
  ]
})

const nextTip = computed(() => {
  if (!props.hasTemplate) return '建议先确认模板版本，避免后续文档结构和变量口径不一致。'
  if (props.pendingCount > 0) return `当前还有 ${props.pendingCount} 项待补充，优先完成结构化补充再生成 PRD。`
  if (props.status !== 'COMPLETED') return '当前已经具备生成条件，下一步建议直接生成 PRD 并查看差异结果。'
  return '当前 PRD 已生成，下一步建议导出 Markdown 或继续发起版本迭代。'
})

function stepStateClass(state: string) {
  return {
    'step--done': state === 'done',
    'step--active': state === 'active'
  }
}
</script>

<style scoped>
.progress-panel {
  border: 1px solid #dbe2ea;
  border-radius: 14px;
  padding: 14px;
  background: linear-gradient(180deg, #f8fcff 0%, #ffffff 100%);
  margin-bottom: 14px;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: .08em;
  text-transform: uppercase;
  font-weight: 700;
}

h3 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}

.badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.step {
  display: flex;
  gap: 10px;
  border: 1px solid #dbe2ea;
  border-radius: 14px;
  padding: 12px;
  background: #fff;
}

.step--done {
  background: linear-gradient(180deg, #f0fdf4 0%, #ffffff 100%);
  border-color: #bbf7d0;
}

.step--active {
  background: linear-gradient(180deg, #ecfeff 0%, #ffffff 100%);
  border-color: #99f6e4;
}

.step-index {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: #e2e8f0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
  flex: 0 0 auto;
}

.step-title {
  margin: 0;
  color: #0f172a;
  font-weight: 700;
}

.step-desc,
.next-tip {
  margin: 6px 0 0;
  color: #475569;
  line-height: 1.6;
}

@media (max-width: 980px) {
  .progress-head {
    flex-direction: column;
  }

  .steps {
    grid-template-columns: 1fr;
  }
}
</style>
