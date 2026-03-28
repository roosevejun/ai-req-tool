<template>
  <WorkspaceSection
    eyebrow="最终确认"
    title="确认后创建正式项目"
    description="当你认为 AI 提炼结果已经足够准确时，再补充项目标识和负责人信息，正式创建项目。"
  >
    <template #actions>
      <StatusBadge :label="readyToCreate ? '可以创建' : '暂不建议创建'" :variant="readyToCreate ? 'success' : 'warning'" small />
    </template>

    <div class="grid create-grid">
      <input v-model.trim="createForm.projectKey" class="input" placeholder="项目 Key，例如 AI-INSPECT" />
      <input v-model.trim="createForm.projectName" class="input" placeholder="项目名称，默认沿用 AI 提炼结果" />
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
        根据当前提炼结果创建项目
      </button>
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
  (event: 'create-project'): void
}>()
</script>

<style scoped>
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

.primary {
  border-radius: 8px;
  border: 1px solid #2563eb;
  padding: 8px 12px;
  cursor: pointer;
  background: #2563eb;
  color: #fff;
}

@media (max-width: 960px) {
  .create-grid {
    grid-template-columns: 1fr;
  }
}
</style>
