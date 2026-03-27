<template>
  <section class="card">
    <h3>创建最终项目</h3>
    <div class="grid create-grid">
      <input v-model.trim="createForm.projectKey" class="input" placeholder="项目 Key，例如 AI-INSPECT" />
      <input v-model.trim="createForm.projectName" class="input" placeholder="项目名称，默认沿用 AI 整理结果" />
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
        <option value="ORG">组织内</option>
      </select>
      <input v-model.trim="createForm.ownerUserId" class="input" placeholder="负责人 ID，可选" />
    </div>
    <div class="row">
      <button class="primary" type="button" :disabled="loading || !sessionId || !readyToCreate" @click="$emit('create-project')">
        根据 AI 会话创建项目
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
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
.card { background: #fff; border: 1px solid #dbe2ea; border-radius: 12px; padding: 14px; margin-bottom: 14px; }
.grid { display: grid; gap: 10px; }
.create-grid { grid-template-columns: repeat(3, minmax(160px, 1fr)); }
.input { width: 100%; box-sizing: border-box; border: 1px solid #d1d5db; border-radius: 8px; padding: 9px 10px; background: #fff; }
.row { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 10px; }
.primary { border-radius: 8px; border: 1px solid #2563eb; padding: 8px 12px; cursor: pointer; background: #2563eb; color: #fff; }
@media (max-width: 960px) { .create-grid { grid-template-columns: 1fr; } }
</style>
