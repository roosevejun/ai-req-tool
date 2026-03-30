<template>
  <section class="panel conversation-panel">
    <div class="panel-head">
      <div>
        <p class="eyebrow">AI 协同</p>
        <h4>通过沟通补全项目信息</h4>
      </div>
      <div class="row compact">
        <button class="ghost mini" :disabled="projectConversationLoading" @click="$emit('refresh-project-ai')">
          {{ projectConversation ? '刷新会话' : '启动 AI' }}
        </button>
        <button class="ghost mini" :disabled="projectConversationLoading || !projectConversation" @click="$emit('apply-project-ai')">
          应用 AI 结果
        </button>
      </div>
    </div>

    <p class="summary">
      目标是补全项目名称背后的业务背景、客户对象、商业价值和核心产品价值。先沟通，再把 AI 结果应用到左侧表单。
    </p>

    <div class="guide-card">
      <p class="guide-title">推荐沟通方式</p>
      <ul class="guide-list">
        <li>先说明项目做什么、解决谁的问题。</li>
        <li>再补充行业背景、竞品、目标客户和价值判断。</li>
        <li>最后让 AI 帮你整理成适合保存的项目描述。</li>
      </ul>
    </div>

    <div class="prompt-group">
      <span class="prompt-label">快捷引导</span>
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
    </div>

    <div v-if="projectConversation" class="ai-status">
      <strong>会话状态</strong>
      <StatusBadge :label="projectConversation.status || '-'" variant="ai" small />
      <StatusBadge v-if="projectConversation.readyToCreate" label="可回填表单" variant="success" small />
    </div>

    <div v-if="projectConversation?.assistantSummary" class="ai-assistant">
      <strong>AI 当前整理结果</strong> {{ projectConversation.assistantSummary }}
    </div>

    <div v-if="projectConversation?.messages?.length" class="ai-history">
      <div v-for="message in projectConversation.messages.slice(-6)" :key="message.id" class="ai-message" :class="message.role">
        <div class="ai-message-role">{{ message.role === 'assistant' ? 'AI' : '你' }}</div>
        <div class="ai-message-content">{{ message.content }}</div>
      </div>
    </div>

    <textarea
      :value="projectConversationInput"
      class="input textarea"
      placeholder="告诉 AI 项目做什么、面向谁、为什么值得做，或直接要求它帮你补全项目背景/客户/价值描述。"
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
      <button class="ghost mini" :disabled="projectConversationLoading || !projectConversation" @click="$emit('apply-project-ai')">
        回填到表单
      </button>
      <button class="ghost mini" :disabled="projectConversationLoading || !projectConversation" @click="$emit('load-project-knowledge-preview')">
        查看检索上下文
      </button>
    </div>

    <div v-if="projectKnowledgePreviewVisible" class="knowledge-preview">
      <div class="panel-head">
        <h4>检索上下文</h4>
        <span class="muted" v-if="projectKnowledgePreviewQueryText">检索词：{{ projectKnowledgePreviewQueryText }}</span>
      </div>
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
    </div>
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
  (event: 'apply-project-ai'): void
  (event: 'update-project-ai-input', value: string): void
  (event: 'load-project-knowledge-preview'): void
}>()

const starterPrompts = [
  '请根据我当前的项目名称，帮我补全项目背景。',
  '请帮我梳理这个项目的目标客户群体和核心痛点。',
  '请帮我整理商业价值、类似产品和差异化能力。',
  '请把我们的对话整理成适合保存到项目信息表单的内容。'
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
  min-height: 108px;
  resize: vertical;
}

.summary,
.muted,
.preview-item p,
.ai-message-content,
.ai-assistant {
  color: #475569;
  line-height: 1.65;
  white-space: pre-wrap;
}

.guide-card,
.knowledge-preview {
  border: 1px solid #dbe5ef;
  border-radius: 16px;
  background: #fff;
  padding: 14px;
}

.guide-title,
.prompt-label {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.guide-list {
  margin: 10px 0 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.65;
}

.prompt-group {
  display: grid;
  gap: 10px;
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

.muted {
  font-size: 13px;
}

.ai-status,
.ai-assistant {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  border-radius: 14px;
  padding: 12px 14px;
}

.ai-status {
  background: #eff6ff;
  color: #1e3a8a;
}

.ai-history {
  display: grid;
  gap: 10px;
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
