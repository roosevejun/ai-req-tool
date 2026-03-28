<template>
  <WorkspaceSection
    eyebrow="结果决策"
    title="保留当前项目框架，或正式立项"
    description="当前 AI 会话会持续保留项目框架。你可以先保存这一阶段结果，后续继续孵化；也可以在信息足够准确时正式立项。"
  >
    <template #actions>
      <StatusBadge :label="readyToCreate ? '可以正式立项' : '更适合先保留框架'" :variant="readyToCreate ? 'success' : 'warning'" small />
    </template>

    <div class="decision-card decision-card--draft">
      <div>
        <h4>保留当前项目框架</h4>
        <p>保留这次 AI 孵化结果，后续可以回来继续补资料、补对话、再决定是否正式立项。</p>
      </div>
      <button class="ghost" type="button" :disabled="loading || !sessionId" @click="$emit('save-framework')">
        保留当前框架
      </button>
    </div>

    <div class="decision-card decision-card--launch">
      <div class="launch-copy">
        <h4>基于当前框架正式立项</h4>
        <p>当你确认 AI 提炼结果已经足够准确时，再补充项目标识和负责人信息，把当前框架转成正式项目。</p>
      </div>

      <div class="grid create-grid">
        <input v-model.trim="createForm.projectKey" class="input" placeholder="项目 Key，例如 AI-INSPECT" />
        <input v-model.trim="createForm.projectName" class="input" placeholder="项目名称，默认沿用当前提炼结果" />
        <select v-model="createForm.projectType" class="input">
          <option value="">项目类型</option>
          <option value="PRODUCT">产品型</option>
          <option value="PLATFORM">平台型</option>
          <option value="OPS">运维型</option>
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

      <div class="row">
        <button class="primary" type="button" :disabled="loading || !sessionId || !readyToCreate" @click="$emit('create-project')">
          正式立项
        </button>
      </div>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import StatusBadge from '../projects/StatusBadge.vue'
import WorkspaceSection from '../projects/WorkspaceSection.vue'
import type { CreateFormState } from './types'

defineProps<{
  loading: boolean
  sessionId: number | null
  readyToCreate: boolean
  createForm: CreateFormState
}>()

defineEmits<{
  (event: 'save-framework'): void
  (event: 'create-project'): void
}>()
</script>

<style scoped>
.decision-card {
  border: 1px solid #dbe2ea;
  border-radius: 16px;
  padding: 14px;
  background: #fff;
}

.decision-card + .decision-card {
  margin-top: 12px;
}

.decision-card--draft {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  background: #f8fbff;
}

.decision-card--launch {
  background: #fff;
}

.launch-copy {
  margin-bottom: 12px;
}

h4 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
}

p {
  margin: 8px 0 0;
  color: #475569;
  line-height: 1.7;
}

.grid {
  display: grid;
  gap: 10px;
}

.create-grid {
  grid-template-columns: repeat(3, minmax(160px, 1fr));
}

.input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 9px 10px;
  background: #fff;
}

.row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.primary,
.ghost {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  padding: 8px 12px;
  cursor: pointer;
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

@media (max-width: 960px) {
  .decision-card--draft {
    flex-direction: column;
  }

  .create-grid {
    grid-template-columns: 1fr;
  }
}
</style>
