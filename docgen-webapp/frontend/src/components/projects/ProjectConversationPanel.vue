<template>
  <section class="panel conversation-panel">
    <div class="panel-head">
      <div>
        <p class="eyebrow">AI 对话</p>
        <h4>告诉 AI 这个项目是什么、面向谁、价值在哪里</h4>
      </div>
      <button class="ghost mini" :disabled="projectConversationLoading" @click="$emit('refresh-project-ai')">
        {{ projectConversation ? '刷新会话' : '启动 AI' }}
      </button>
    </div>

    <p v-if="projectConversation?.assistantSummary" class="summary-inline">
      {{ projectConversation.assistantSummary }}
    </p>

    <div class="prompt-list">
      <button
        v-for="prompt in starterPrompts"
        :key="prompt"
        class="prompt-chip"
        type="button"
        :disabled="projectConversationLoading"
        @click="$emit('update-project-ai-input', prompt)"
      >
        {{ prompt }}
      </button>
    </div>

    <textarea
      :value="projectConversationInput"
      class="input textarea"
      placeholder="直接输入项目背景、目标客户、商业价值、竞品判断，或让 AI 帮你补全这些信息。"
      @input="$emit('update-project-ai-input', ($event.target as HTMLTextAreaElement).value)"
    />

    <div class="row">
      <button
        class="primary mini"
        :disabled="projectConversationLoading || !projectConversation || !canSendProjectConversation"
        @click="$emit('send-project-ai')"
      >
        发送给 AI
      </button>
      <button
        class="ghost mini"
        :disabled="projectConversationLoading || !projectConversation"
        @click="$emit('load-project-knowledge-preview')"
      >
        查看检索上下文
      </button>
      <div v-if="projectConversation" class="status-line">
        <span>会话状态</span>
        <StatusBadge :label="projectConversation.status || '-'" variant="ai" small />
        <StatusBadge v-if="projectConversation.readyToCreate" label="可生成建议" variant="success" small />
      </div>
    </div>

    <details v-if="projectConversation?.messages?.length" class="details-card">
      <summary>查看最近对话</summary>
      <div class="ai-history">
        <div v-for="message in projectConversation.messages.slice(-4)" :key="message.id" class="ai-message" :class="message.role">
          <div class="ai-message-role">{{ message.role === 'assistant' ? 'AI' : '你' }}</div>
          <div class="ai-message-content">{{ message.content }}</div>
        </div>
      </div>
    </details>

    <details v-if="projectKnowledgePreviewVisible" class="details-card" open>
      <summary>查看检索上下文</summary>
      <div v-if="projectKnowledgePreviewLoading" class="muted">正在加载检索上下文...</div>
      <div v-else-if="projectKnowledgePreview">
        <div class="preview-item">
          <strong>命中片段</strong>
          <p v-if="projectKnowledgePreview.items.length === 0">当前没有命中可用知识片段。</p>
          <p v-for="item in projectKnowledgePreview.items" :key="`${item.documentId}-${item.chunkId}`">
            文档 #{{ item.documentId }} / Chunk #{{ item.chunkNo }} / 分数
            {{ typeof item.score === 'number' ? item.score.toFixed(3) : '-' }}<br />
            {{ item.chunkText }}
          </p>
        </div>
        <div class="preview-item">
          <strong>拼接后的上下文</strong>
          <p>{{ projectKnowledgePreview.contextText || '-' }}</p>
        </div>
      </div>
    </details>
  </section>
</template>

<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'
import type { ProjectConversationView, ProjectKnowledgePreview } from './types'

defineProps<{
  projectConversationLoading: boolean
  projectConversation: ProjectConversationView | null
  projectConversationInput: string
  canSendProjectConversation: boolean
  projectKnowledgePreviewVisible: boolean
  projectKnowledgePreviewLoading: boolean
  projectKnowledgePreview: ProjectKnowledgePreview | null
  projectKnowledgePreviewQueryText: string
}>()

defineEmits<{
  (event: 'refresh-project-ai'): void
  (event: 'send-project-ai'): void
  (event: 'update-project-ai-input', value: string): void
  (event: 'load-project-knowledge-preview'): void
}>()

const starterPrompts = [
  '请先补全这个项目的业务背景。',
  '请梳理目标客户群体和核心痛点。',
  '请总结商业价值、竞品和差异化。'
]
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

.conversation-panel {
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.06) 0%, #ffffff 100%);
}

.panel-head,
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.row {
  flex-wrap: wrap;
}

.eyebrow {
  margin: 0 0 6px;
  color: #1d4ed8;
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

.summary-inline {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #dbe5ef;
  background: rgba(255, 255, 255, 0.88);
  color: #475569;
  line-height: 1.65;
  white-space: pre-wrap;
}

.details-card {
  border: 1px solid #dbe5ef;
  border-radius: 16px;
  background: #fff;
  padding: 14px;
}

.muted,
.preview-item p,
.ai-message-content {
  color: #475569;
  line-height: 1.65;
  white-space: pre-wrap;
}

.prompt-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.prompt-chip {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 999px;
  padding: 8px 12px;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.status-line {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: #475569;
  font-size: 13px;
}

.input {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 10px 12px;
  font: inherit;
  color: #0f172a;
  background: #fff;
  box-sizing: border-box;
}

.textarea {
  min-height: 132px;
  resize: vertical;
}

.details-card summary {
  cursor: pointer;
  font-weight: 700;
  color: #0f172a;
}

.ai-history {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.ai-message {
  border: 1px solid #dbe5ef;
  border-radius: 14px;
  padding: 12px 14px;
  background: #fff;
}

.ai-message.assistant {
  background: #f8fbff;
}

.ai-message-role {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  color: #0f766e;
  letter-spacing: 0.08em;
}

.preview-item + .preview-item {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #dbe2ea;
}

.primary,
.ghost {
  border-radius: 999px;
  padding: 8px 14px;
  border: 1px solid transparent;
  cursor: pointer;
}

.primary {
  background: #1d4ed8;
  color: #fff;
}

.ghost {
  background: #fff;
  border-color: #cbd5e1;
  color: #334155;
}

.mini {
  font-size: 13px;
}

@media (max-width: 860px) {
  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
