<template>
  <section v-if="changedProposals.length > 0" class="panel proposal-panel">
    <div class="panel-head">
      <div>
        <p class="eyebrow">AI 修改建议</p>
        <h4>根据当前沟通结果，AI 建议调整这些字段</h4>
      </div>
      <button class="primary mini" :disabled="loading || selectedKeys.length === 0" @click="applySelected">
        应用所选字段
      </button>
    </div>

    <div class="proposal-list">
      <label
        v-for="proposal in changedProposals"
        :key="proposal.key"
        class="proposal-item"
        :class="{ 'proposal-item--selected': selectedKeys.includes(proposal.key) }"
      >
        <div class="proposal-head">
          <div class="checkbox-wrap">
            <input v-model="selectedKeys" type="checkbox" :value="proposal.key" class="checkbox" />
            <strong>{{ proposal.label }}</strong>
          </div>
          <span class="proposal-status">建议更新</span>
        </div>

        <div class="proposal-grid">
          <div class="proposal-block">
            <span>当前内容</span>
            <p>{{ proposal.currentValue || '当前为空' }}</p>
          </div>
          <div class="proposal-block proposal-block--next">
            <span>建议改为</span>
            <p>{{ proposal.suggestedValue }}</p>
          </div>
        </div>
      </label>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ProjectConversationView, ProjectEditFormState } from './types'

type Proposal = {
  key: keyof ProjectEditFormState
  label: string
  currentValue: string
  suggestedValue: string
  changed: boolean
}

const props = defineProps<{
  loading: boolean
  projectConversation: ProjectConversationView | null
  projectEditForm: ProjectEditFormState
}>()

const emit = defineEmits<{
  (event: 'apply-project-ai', fields: Array<keyof ProjectEditFormState>): void
}>()

const proposalDefs: Array<{ key: keyof ProjectEditFormState; label: string; readSuggested: (conversation: ProjectConversationView) => string }> = [
  { key: 'projectName', label: '项目名称', readSuggested: (conversation) => conversation.structuredInfo.projectName || '' },
  { key: 'description', label: '项目描述', readSuggested: (conversation) => conversation.structuredInfo.description || '' },
  { key: 'projectBackground', label: '项目背景', readSuggested: (conversation) => conversation.structuredInfo.projectBackground || '' },
  { key: 'targetCustomerGroups', label: '目标客户群体', readSuggested: (conversation) => conversation.structuredInfo.targetCustomerGroups || '' },
  { key: 'commercialValue', label: '商业价值', readSuggested: (conversation) => conversation.structuredInfo.commercialValue || '' },
  { key: 'coreProductValue', label: '核心产品价值', readSuggested: (conversation) => conversation.structuredInfo.coreProductValue || '' },
  { key: 'similarProducts', label: '类似产品或竞品信息', readSuggested: (conversation) => conversation.structuredInfo.similarProducts || '' }
]

const changedProposals = computed<Proposal[]>(() => {
  if (!props.projectConversation) {
    return []
  }
  return proposalDefs
    .map((definition) => {
      const currentValue = (props.projectEditForm[definition.key] || '').trim()
      const suggestedValue = definition.readSuggested(props.projectConversation).trim()
      return {
        key: definition.key,
        label: definition.label,
        currentValue,
        suggestedValue,
        changed: !!suggestedValue && suggestedValue !== currentValue
      }
    })
    .filter((proposal) => proposal.changed)
})

const selectedKeys = ref<Array<keyof ProjectEditFormState>>([])

watch(
  changedProposals,
  (proposals) => {
    selectedKeys.value = proposals.map((proposal) => proposal.key)
  },
  { immediate: true }
)

function applySelected() {
  if (selectedKeys.value.length === 0) {
    return
  }
  emit('apply-project-ai', selectedKeys.value)
}
</script>

<style scoped>
.panel {
  border: 1px solid #dbe5ef;
  border-radius: 16px;
  background: #fff;
  padding: 16px;
  display: grid;
  gap: 14px;
}

.proposal-panel {
  background: linear-gradient(180deg, rgba(15, 118, 110, 0.05) 0%, #ffffff 100%);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

h4 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.proposal-list {
  display: grid;
  gap: 12px;
}

.proposal-item {
  border: 1px solid #dbe5ef;
  border-radius: 14px;
  padding: 14px;
  background: #fff;
  display: grid;
  gap: 12px;
  cursor: pointer;
}

.proposal-item--selected {
  border-color: #86efac;
  background: #f0fdf4;
}

.proposal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.checkbox-wrap {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.checkbox {
  width: 16px;
  height: 16px;
}

.proposal-status {
  font-size: 12px;
  color: #166534;
  font-weight: 700;
}

.proposal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.proposal-block {
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.92);
}

.proposal-block--next {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.proposal-block span {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.proposal-block p {
  margin: 8px 0 0;
  color: #475569;
  line-height: 1.65;
  white-space: pre-wrap;
}

.primary {
  border-radius: 999px;
  padding: 8px 14px;
  border: 1px solid #1d4ed8;
  background: #1d4ed8;
  color: #fff;
  cursor: pointer;
}

.mini {
  font-size: 13px;
}

@media (max-width: 860px) {
  .panel-head,
  .proposal-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .proposal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
