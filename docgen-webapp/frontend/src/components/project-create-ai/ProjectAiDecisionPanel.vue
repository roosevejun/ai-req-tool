<template>
  <WorkspaceSection
    eyebrow="结果决策"
    title="根据当前项目框架结果做决策"
    description="先看建议与状态，再决定暂存还是正式立项。"
    tint
  >
    <template #actions>
      <StatusBadge :label="readyToCreate ? '可正式立项' : '建议继续孵化'" :variant="readyToCreate ? 'success' : 'warning'" small />
    </template>

    <section class="decision-stack">
      <article class="advice-banner">
        <div class="advice-head">
          <h4>当前建议</h4>
          <span class="advice-tag">{{ readyToCreate ? '可以推进' : '优先补充' }}</span>
        </div>
        <p class="advice-copy">{{ accuracyAdvice }}</p>
      </article>

      <div class="status-strip">
        <article class="status-chip">
          <span class="status-chip__label">待补充</span>
          <strong class="status-chip__count">{{ missingItems.length }}</strong>
          <p class="status-chip__copy">{{ missingSummary }}</p>
        </article>

        <article class="status-chip">
          <span class="status-chip__label">待确认</span>
          <strong class="status-chip__count">{{ pendingQuestions.length }}</strong>
          <p class="status-chip__copy">{{ pendingSummary }}</p>
        </article>

        <article class="status-chip">
          <span class="status-chip__label">已形成</span>
          <strong class="status-chip__count">{{ confirmedItems.length }}</strong>
          <p class="status-chip__copy">{{ confirmedSummary }}</p>
        </article>
      </div>

      <section class="decision-card">
        <div class="decision-card__head">
          <h4>核心信息与立项信息</h4>
        </div>

        <div class="decision-layout" :class="{ 'decision-layout--single': launchHighlights.length === 0 }">
          <section v-if="launchHighlights.length > 0" class="summary-panel">
            <div class="panel-head">
              <h5>核心字段摘要</h5>
              <span class="panel-count">{{ launchHighlights.length }} 项</span>
            </div>

            <div class="summary-list">
              <div v-for="item in launchHighlights" :key="item.key" class="summary-item">
                <span class="summary-item__label">{{ item.label }}</span>
                <strong class="summary-item__value">{{ item.value }}</strong>
              </div>
            </div>
          </section>

          <section class="form-panel">
            <div class="panel-head">
              <h5>项目信息</h5>
              <span class="panel-count">{{ projectInfoDetails.length }} 项</span>
            </div>

            <div v-if="projectInfoDetails.length > 0" class="project-info-list">
              <div v-for="item in projectInfoDetails" :key="item.key" class="project-info-item">
                <span class="project-info-item__label">{{ item.label }}</span>
                <strong class="project-info-item__value">{{ item.value }}</strong>
              </div>
            </div>
            <p v-else class="empty-copy">当前还没有形成足够完整的项目信息。</p>

            <div class="create-grid">
              <input v-model.trim="createForm.projectKey" class="input" placeholder="项目 Key，例如 AI-INSIGHT" />
              <input v-model.trim="createForm.projectName" class="input" placeholder="项目名称，默认沿用当前框架" />

              <select v-model="createForm.projectType" class="input">
                <option value="">项目类型</option>
                <option value="PRODUCT">产品型</option>
                <option value="PLATFORM">平台型</option>
                <option value="OPS">运营型</option>
                <option value="INTEGRATION">集成型</option>
              </select>

              <select v-model="createForm.priority" class="input">
                <option value="">优先级</option>
                <option value="P0">P0</option>
                <option value="P1">P1</option>
                <option value="P2">P2</option>
                <option value="P3">P3</option>
              </select>

              <select v-model="createForm.visibility" class="input">
                <option value="PRIVATE">私有</option>
                <option value="ORG">组织内可见</option>
              </select>

              <input v-model.trim="createForm.ownerUserId" class="input" placeholder="负责人 ID，可选" />
            </div>

            <div class="decision-actions">
              <button class="ghost" type="button" :disabled="loading || !sessionId" @click="$emit('save-framework')">
                暂存项目信息
              </button>
              <button class="primary" type="button" :disabled="loading || !sessionId || !readyToCreate" @click="$emit('create-project')">
                正式立项
              </button>
            </div>
          </section>
        </div>
      </section>
    </section>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StatusBadge from '../projects/StatusBadge.vue'
import WorkspaceSection from '../projects/WorkspaceSection.vue'
import type { CreateFormState, StructuredFieldItem } from './types'

const props = defineProps<{
  loading: boolean
  sessionId: number | null
  readyToCreate: boolean
  confirmedItems: StructuredFieldItem[]
  pendingQuestions: string[]
  missingItems: StructuredFieldItem[]
  accuracyAdvice: string
  createForm: CreateFormState
}>()

defineEmits<{
  (event: 'save-framework'): void
  (event: 'create-project'): void
}>()

const launchHighlights = computed(() => {
  const preferredOrder = ['projectName', 'targetCustomerGroups', 'coreProductValue', 'commercialValue', 'projectBackground']
  return preferredOrder
    .map((key) => props.confirmedItems.find((item) => item.key === key))
    .filter((item): item is StructuredFieldItem => !!item)
    .slice(0, 4)
})

const projectInfoDetails = computed(() => {
  const preferredOrder = [
    'projectName',
    'description',
    'projectBackground',
    'targetCustomerGroups',
    'commercialValue',
    'coreProductValue',
    'similarProducts'
  ]

  return preferredOrder
    .map((key) => props.confirmedItems.find((item) => item.key === key))
    .filter((item): item is StructuredFieldItem => !!item)
    .slice(0, 6)
})

const missingSummary = computed(() => {
  if (props.missingItems.length === 0) return '当前没有待补充的关键字段。'
  return props.missingItems
    .slice(0, 2)
    .map((item) => item.label)
    .join('、')
})

const pendingSummary = computed(() => {
  if (props.pendingQuestions.length === 0) return '当前没有待确认问题。'
  return props.pendingQuestions[0]
})

const confirmedSummary = computed(() => {
  if (props.confirmedItems.length === 0) return '当前还没有形成稳定的项目关键信息。'
  return props.confirmedItems
    .slice(0, 2)
    .map((item) => item.label)
    .join('、')
})
</script>

<style scoped>
.decision-stack {
  display: grid;
  gap: 14px;
}

.advice-banner {
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  padding: 14px 16px;
  background: #f8fbff;
}

.advice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.advice-head h4,
.decision-card__head h4,
.panel-head h5 {
  margin: 0;
  color: #0f172a;
}

.advice-tag,
.status-chip__count,
.panel-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.2;
}

.advice-tag {
  background: #fef3c7;
  color: #92400e;
}

.advice-copy {
  margin: 10px 0 0;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
}

.status-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.status-chip {
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  padding: 12px 13px;
  background: #fff;
  display: grid;
  gap: 6px;
}

.status-chip__label {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.status-chip__count,
.panel-count {
  justify-self: start;
  background: #eff6ff;
  color: #1d4ed8;
}

.status-chip__copy {
  margin: 0;
  color: #475569;
  line-height: 1.6;
}

.decision-card {
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.decision-card__head {
  margin-bottom: 12px;
}

.decision-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(360px, 1.05fr);
  gap: 14px;
  align-items: start;
}

.decision-layout--single {
  grid-template-columns: 1fr;
}

.summary-panel,
.form-panel {
  display: grid;
  gap: 12px;
}

.form-panel {
  padding-left: 14px;
  border-left: 1px solid #e2e8f0;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.summary-list,
.project-info-list {
  display: grid;
  gap: 10px;
}

.summary-item,
.project-info-item {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  background: #f8fbff;
}

.summary-item__label,
.project-info-item__label {
  color: #64748b;
  font-size: 12px;
}

.summary-item__value,
.project-info-item__value {
  color: #0f172a;
  font-size: 14px;
  line-height: 1.6;
}

.empty-copy {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.create-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 10px;
}

.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fff;
}

.decision-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.primary,
.ghost {
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 9px 14px;
  cursor: pointer;
  font-weight: 700;
}

.primary {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
}

.ghost {
  background: #f8fafc;
  color: #0f172a;
}

.primary:disabled,
.ghost:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

@media (max-width: 960px) {
  .status-strip,
  .decision-layout,
  .create-grid {
    grid-template-columns: 1fr;
  }

  .form-panel {
    padding-left: 0;
    border-left: 0;
    border-top: 1px solid #e2e8f0;
    padding-top: 14px;
  }
}
</style>
