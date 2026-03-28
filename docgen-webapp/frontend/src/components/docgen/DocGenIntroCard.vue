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

    <div class="row">
      <button class="primary" :disabled="loading" @click="$emit('create-job')">
        {{ loading ? '处理中...' : '启动 AI 整理' }}
      </button>
      <button class="ghost" :disabled="loading" @click="$emit('reset')">清空</button>
    </div>
  </WorkspaceSection>
</template>

<script setup lang="ts">
import WorkspaceSection from '../projects/WorkspaceSection.vue'
const businessDescription = defineModel<string>('businessDescription', { required: true })
const previousPrdMarkdown = defineModel<string>('previousPrdMarkdown', { required: true })

defineProps<{
  loading: boolean
}>()

defineEmits<{
  (event: 'create-job'): void
  (event: 'reset'): void
}>()
</script>

<style scoped>
.hint { color: #6b7280; margin-top: 8px; margin-bottom: 10px; }
.details { margin-bottom: 12px; }
.textarea { width: 100%; min-height: 140px; resize: vertical; padding: 10px; box-sizing: border-box; border-radius: 10px; border: 1px solid #e5e7eb; font-size: 14px; line-height: 1.4; }
.row { display: flex; gap: 10px; margin-top: 12px; flex-wrap: wrap; }
button { border-radius: 10px; padding: 10px 14px; border: 1px solid transparent; cursor: pointer; font-size: 14px; }
.primary { background: #2563eb; color: #fff; }
.ghost { background: #f3f4f6; color: #111827; border-color: #e5e7eb; }
</style>
