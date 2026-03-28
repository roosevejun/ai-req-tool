<template>
  <WorkspaceSection
    eyebrow="需求整理启动"
    title="开始 AI 需求整理"
    description="围绕业务背景和上下文描述，让 AI 持续澄清、整理并生成 PRD。"
  >
    <details class="details">
      <summary>如果已有历史 PRD，可以先粘贴进来作为参考。</summary>
      <p class="hint">AI 会结合已有 PRD 和新的业务描述，帮助你继续推进需求整理。</p>
      <textarea
        v-model="previousPrdMarkdown"
        class="textarea"
        placeholder="粘贴已有 PRD Markdown 内容"
      />
    </details>

    <textarea
      v-model="businessDescription"
      class="textarea"
      placeholder="请描述当前需求背景、业务目标、已知约束和你希望 AI 帮你继续梳理的重点。"
    />

    <div class="template-grid">
      <label class="field">
        <span>选择模板</span>
        <select v-model.number="selectedTemplateId" class="select">
          <option :value="0">使用默认模板</option>
          <option v-for="item in templates" :key="item.id" :value="item.id">
            {{ item.templateName }}（{{ item.templateCode }}）
          </option>
        </select>
      </label>

      <label class="field">
        <span>选择版本</span>
        <select v-model.number="selectedTemplateVersionId" class="select" :disabled="templateVersions.length === 0">
          <option :value="0">优先使用已发布版本</option>
          <option v-for="item in templateVersions" :key="item.id" :value="item.id">
            {{ item.versionLabel || `v${item.versionNo}` }}{{ item.isPublished ? '（已发布）' : '' }}
          </option>
        </select>
      </label>
    </div>

    <div class="template-hint">
      <div class="template-hint__head">
        <strong>模板使用说明</strong>
        <span class="hint-badge">{{ selectedTemplate ? selectedTemplate.templateCode : '默认模板' }}</span>
      </div>
      <p class="hint-copy">{{ templateReasonText(!!selectedTemplate, !!selectedVersion) }}</p>
      <ul class="hint-list">
        <li v-if="selectedTemplate">
          当前模板：{{ selectedTemplate.templateName }}（{{ selectedTemplate.templateCode }}）
        </li>
        <li v-if="selectedVersion">
          当前版本：{{ selectedVersion.versionLabel || `v${selectedVersion.versionNo}` }}{{ selectedVersion.isPublished ? '，已发布' : '，草稿版' }}
        </li>
        <li v-else>
          未指定版本时，系统会优先使用已发布版本，保证输出更稳定。
        </li>
      </ul>
    </div>

    <div class="row">
      <button class="primary" :disabled="loading" @click="$emit('create-job')">
        {{ loading ? '处理中...' : '启动 AI 整理' }}
      </button>
      <button class="ghost" :disabled="loading" @click="$emit('reset')">清空</button>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import WorkspaceSection from '../projects/WorkspaceSection.vue'
import { templateReasonText } from './helpers'
import type { TemplateOption, TemplateVersionOption } from './types'
const businessDescription = defineModel<string>('businessDescription', { required: true })
const previousPrdMarkdown = defineModel<string>('previousPrdMarkdown', { required: true })
const selectedTemplateId = defineModel<number>('selectedTemplateId', { required: true })
const selectedTemplateVersionId = defineModel<number>('selectedTemplateVersionId', { required: true })

const props = defineProps<{
  loading: boolean
  templates: TemplateOption[]
  templateVersions: TemplateVersionOption[]
}>()

const selectedTemplate = computed(() => props.templates.find((item) => item.id === selectedTemplateId.value) || null)
const selectedVersion = computed(() => props.templateVersions.find((item) => item.id === selectedTemplateVersionId.value) || null)

defineEmits<{
  (event: 'create-job'): void
  (event: 'reset'): void
}>()
</script>

<style scoped>
.hint { color: #6b7280; margin-top: 8px; margin-bottom: 10px; }
.details { margin-bottom: 12px; }
.template-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-bottom: 12px; }
.field { display: grid; gap: 6px; }
.field span { color: #334155; font-size: 13px; font-weight: 600; }
.select { width: 100%; padding: 10px; box-sizing: border-box; border-radius: 10px; border: 1px solid #e5e7eb; background: #fff; }
.textarea { width: 100%; min-height: 140px; resize: vertical; padding: 10px; box-sizing: border-box; border-radius: 10px; border: 1px solid #e5e7eb; font-size: 14px; line-height: 1.4; }
.row { display: flex; gap: 10px; margin-top: 12px; flex-wrap: wrap; }
.template-hint { margin-bottom: 12px; border: 1px solid #dbe2ea; border-radius: 14px; padding: 14px; background: linear-gradient(180deg, #f8fcff 0%, #ffffff 100%); }
.template-hint__head { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.hint-badge { font-size: 12px; color: #0f766e; font-weight: 700; }
.hint-copy { margin: 10px 0 0; color: #475569; line-height: 1.7; }
.hint-list { margin: 10px 0 0; padding-left: 18px; color: #64748b; line-height: 1.8; }
button { border-radius: 10px; padding: 10px 14px; border: 1px solid transparent; cursor: pointer; font-size: 14px; }
.primary { background: #2563eb; color: #fff; }
.ghost { background: #f3f4f6; color: #111827; border-color: #e5e7eb; }
@media (max-width: 900px) { .template-grid { grid-template-columns: 1fr; } }
</style>
