<template>
  <section class="card">
    <h3>创建项目</h3>

    <div class="form-grid form-grid-project">
      <input v-model.trim="projectForm.projectKey" class="input" placeholder="项目 Key，例如 AI-REQ" />
      <input v-model.trim="projectForm.projectName" class="input" placeholder="项目名称" />
      <select v-model="projectForm.projectType" class="input">
        <option value="">项目类型，可选</option>
        <option value="PRODUCT">产品型</option>
        <option value="PLATFORM">平台型</option>
        <option value="OPS">运维型</option>
        <option value="INTEGRATION">集成型</option>
      </select>
      <select v-model="projectForm.priority" class="input">
        <option value="P0">P0</option>
        <option value="P1">P1</option>
        <option value="P2">P2</option>
        <option value="P3">P3</option>
      </select>
      <input v-model="projectForm.startDate" type="date" class="input" />
      <input v-model="projectForm.targetDate" type="date" class="input" />
      <select v-model="projectForm.ownerUserId" class="input">
        <option value="">负责人，默认当前登录用户</option>
        <option v-for="u in userOptions" :key="u.id" :value="String(u.id)">
          {{ u.displayName || u.username }} ({{ u.username }})
        </option>
      </select>
      <select v-model="projectForm.visibility" class="input">
        <option value="PRIVATE">私有</option>
        <option value="ORG">组织内</option>
      </select>
      <input v-model.trim="projectForm.tags" class="input" placeholder="标签，逗号分隔，例如 车联网, 地图" />
    </div>

    <textarea v-model="projectForm.description" class="input" placeholder="项目描述，可选" />

    <div class="product-info-box">
      <div class="section-head">
        <h4>产品信息</h4>
        <div class="row compact">
          <button class="ghost mini" :disabled="projectAiLoading || !canGuideProjectInfo" @click="$emit('guide-project-info')">
            {{ projectAiQuestions.length > 0 ? '继续 AI 补全' : 'AI 引导补全' }}
          </button>
          <button class="ghost mini" :disabled="projectAiLoading" @click="$emit('apply-ai-suggestions')">
            应用 AI 建议
          </button>
        </div>
      </div>

      <textarea v-model="projectForm.projectBackground" class="input" placeholder="项目背景，可选" />
      <textarea v-model="projectForm.similarProducts" class="input" placeholder="类似产品 / 参考产品，可选" />
      <textarea v-model="projectForm.targetCustomerGroups" class="input" placeholder="目标客户群体，可选" />
      <textarea v-model="projectForm.commercialValue" class="input" placeholder="商业价值，可选" />
      <textarea v-model="projectForm.coreProductValue" class="input" placeholder="核心产品价值，可选" />

      <div v-if="projectAiMessage" class="ai-assistant">
        <strong>AI 建议：</strong> {{ projectAiMessage }}
      </div>

      <div v-if="projectAiQuestions.length > 0" class="ai-qa-list">
        <div v-for="(question, idx) in projectAiQuestions" :key="question + idx" class="ai-question-card">
          <div class="ai-question">{{ idx + 1 }}. {{ question }}</div>
          <textarea
            v-model="projectAiAnswers[idx]"
            class="input"
            :placeholder="`请补充回答：${question}`"
          />
        </div>
      </div>

      <div v-if="hasProjectAiSuggestions" class="ai-preview">
        <h4>AI 补全建议预览</h4>
        <div class="preview-grid">
          <div class="preview-item">
            <strong>项目背景</strong>
            <p>{{ projectAiSuggestions.projectBackground || '-' }}</p>
          </div>
          <div class="preview-item">
            <strong>类似产品</strong>
            <p>{{ projectAiSuggestions.similarProducts || '-' }}</p>
          </div>
          <div class="preview-item">
            <strong>目标客户群体</strong>
            <p>{{ projectAiSuggestions.targetCustomerGroups || '-' }}</p>
          </div>
          <div class="preview-item">
            <strong>商业价值</strong>
            <p>{{ projectAiSuggestions.commercialValue || '-' }}</p>
          </div>
          <div class="preview-item">
            <strong>核心产品价值</strong>
            <p>{{ projectAiSuggestions.coreProductValue || '-' }}</p>
          </div>
        </div>
      </div>
    </div>

    <button class="primary block" :disabled="loading" @click="$emit('create-project')">
      创建项目
    </button>
  </section>
</template>

<script setup lang="ts">
import type { ProjectAiSuggestions, ProjectFormState, UserOption } from './types'

defineProps<{
  loading: boolean
  projectAiLoading: boolean
  canGuideProjectInfo: boolean
  hasProjectAiSuggestions: boolean
  projectAiMessage: string
  projectAiQuestions: string[]
  projectAiAnswers: string[]
  projectAiSuggestions: ProjectAiSuggestions
  projectForm: ProjectFormState
  userOptions: UserOption[]
}>()

defineEmits<{
  (event: 'guide-project-info'): void
  (event: 'apply-ai-suggestions'): void
  (event: 'create-project'): void
}>()
</script>

<style scoped>
.card { background: #fff; border: 1px solid #dbe2ea; border-radius: 12px; padding: 12px; margin-bottom: 12px; }
.form-grid { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 10px; }
.form-grid-project { grid-template-columns: repeat(2, minmax(120px, 1fr)); }
.input { width: 100%; box-sizing: border-box; border: 1px solid #d1d5db; border-radius: 8px; padding: 8px 10px; margin-top: 8px; background: #fff; }
textarea.input { min-height: 70px; resize: vertical; }
.row { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.compact { margin-top: 0; }
.primary, .ghost, .mini { border-radius: 8px; border: 1px solid #d1d5db; padding: 8px 12px; cursor: pointer; }
.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.ghost, .mini { background: #f3f4f6; }
.mini { padding: 5px 9px; font-size: 12px; }
.block { width: 100%; }
.product-info-box, .ai-preview { margin-top: 12px; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.section-head h4, .ai-preview h4 { margin: 0; font-size: 14px; }
.ai-assistant { margin-top: 10px; padding: 10px; border-radius: 8px; background: #eef4ff; color: #1e3a8a; font-size: 13px; }
.ai-qa-list { display: grid; gap: 10px; margin-top: 10px; }
.ai-question-card { border: 1px solid #dbe2ea; border-radius: 10px; padding: 10px; background: #fafcff; }
.ai-question { font-size: 13px; color: #1f2937; margin-bottom: 6px; }
.preview-grid { display: grid; grid-template-columns: repeat(2, minmax(180px, 1fr)); gap: 10px; margin-top: 10px; }
.preview-item { border: 1px solid #e5e7eb; border-radius: 10px; padding: 10px; background: #fbfdff; }
.preview-item p { margin: 6px 0 0; font-size: 13px; color: #4b5563; white-space: pre-wrap; }
</style>
